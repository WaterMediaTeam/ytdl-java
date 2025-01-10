package com.github.kiulian.downloader.downloader;

import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;
import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

import static com.github.kiulian.downloader.model.Utils.closeSilently;

public class DownloaderImpl implements Downloader {

    private static final int BUFFER_SIZE = 4096;
    private static final int PART_LENGTH = 2 * 1024 * 1024;

    private final Config config;

    public DownloaderImpl(final Config config) {
        this.config = config;
    }

    @Override
    public ResponseImpl<String> downloadWebpage(final RequestWebpage request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<String> result = executorService.submit(() -> this.download(request));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final String result = this.download(request);
            return ResponseImpl.from(result);
        } catch (final IOException | YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private String download(final RequestWebpage request) throws IOException, YoutubeException {
        final String downloadUrl = request.getDownloadUrl();
        final Map<String, String> headers = request.getHeaders();
        final YoutubeCallback<String> callback = request.getCallback();
        int maxRetries = request.getRetries() != 0 ? request.getRetries() : this.config.getRetries();
        final Proxy proxy = request.getProxy();

        IOException exception;
        final StringBuilder result = new StringBuilder();
        do {
            try {
                final HttpURLConnection urlConnection = this.openConnection(downloadUrl, headers, proxy, this.config.isCompressionEnabled());
                urlConnection.setRequestMethod(request.getMethod());
                if (request.getBody() != null) {
                    urlConnection.setDoOutput(true);
                    try (final OutputStreamWriter outputWriter = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8)){
                        outputWriter.write(request.getBody());
                        outputWriter.flush();
                    }
                }
                final int responseCode = urlConnection.getResponseCode();
                if (responseCode != 200) {
                    final YoutubeException.DownloadException e = new YoutubeException.DownloadException("Failed to download: HTTP " + responseCode);
                    if (callback != null) {
                        callback.onError(e);
                    }
                    throw e;
                }

                final int contentLength = urlConnection.getContentLength();
                if (contentLength == 0) {
                    final YoutubeException.DownloadException e = new YoutubeException.DownloadException("Failed to download: Response is empty");
                    if (callback != null) {
                        callback.onError(e);
                    }
                    throw e;
                }

                BufferedReader br = null;
                try {
                    InputStream in = urlConnection.getInputStream();
                    if (this.config.isCompressionEnabled() && "gzip".equals(urlConnection.getHeaderField("content-encoding"))) {
                        in = new GZIPInputStream(in);
                    }
                    br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String inputLine;
                    while ((inputLine = br.readLine()) != null)
                        result.append(inputLine).append('\n');
                } finally {
                    closeSilently(br);
                }
                // reset error in case of successful retry
                exception = null;
            } catch (final IOException e) {
                exception = e;
            }
        } while (exception != null && maxRetries-- > 0);

        if (exception != null) {
            if (callback != null) {
                callback.onError(exception);
            }
            throw exception;
        }

        final String resultString = result.toString();
        if (callback != null) {
            callback.onFinished(resultString);
        }
        return resultString;
    }

    @Override
    public ResponseImpl<File> downloadVideoAsFile(final RequestVideoFileDownload request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<File> result = executorService.submit(() -> this.download(request));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final File result = this.download(request);
            return ResponseImpl.from(result);
        } catch (final IOException e) {
            return ResponseImpl.error(e);
        }
    }

    @Override
    public ResponseImpl<Void> downloadVideoAsStream(final RequestVideoStreamDownload request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<Void> result = executorService.submit(() -> this.download(request));
            return ResponseImpl.fromFuture(result);
        }
        try {
            this.download(request);
            return ResponseImpl.from(null);
        } catch (final IOException e) {
            return ResponseImpl.error(e);
        }
    }

    private File download(final RequestVideoFileDownload request) throws IOException {
        final Format format = request.getFormat();
        final File outputFile = request.getOutputFile();
        final YoutubeCallback<File> callback = request.getCallback();
        final OutputStream os = new FileOutputStream(outputFile);

        this.download(request, format, os);
        if (callback != null) {
            callback.onFinished(outputFile);
        }
        return outputFile;
    }

    private Void download(final RequestVideoStreamDownload request) throws IOException {
        final Format format = request.getFormat();
        final YoutubeCallback<Void> callback = request.getCallback();
        final OutputStream os = request.getOutputStream();

        this.download(request, format, os);
        if (callback != null) {
            callback.onFinished(null);
        }
        return null;
    }

    private void download(final Request<?, ?> request, final Format format, final OutputStream os) throws IOException {
        final Map<String, String> headers = request.getHeaders();
        final YoutubeCallback<?> callback = request.getCallback();
        final int retries = request.getRetries() != 0 ? request.getRetries() : this.config.getRetries();
        final Proxy proxy = request.getProxy();

        IOException exception;
        do {
            try {
                if (format.isAdaptive() && format.contentLength() != null) {
                    this.downloadByPart(format, os, headers, proxy, callback);
                } else {
                    this.downloadStraight(format, os, headers, proxy, callback);
                }
                // reset error in case of successful retry
                exception = null;
            } catch (final IOException e) {
                exception = e;
            } finally {
                closeSilently(os);
            }
        } while (exception != null && retries > 0);

        if (exception != null) {
            if (callback != null) {
                callback.onError(exception);
            }
            throw exception;
        }
    }

    // Downloads the format in one single request
    private void downloadStraight(final Format format, final OutputStream os, final Map<String, String> headers, final Proxy proxy, final YoutubeCallback<?> callback) throws IOException {
        final HttpURLConnection urlConnection = this.openConnection(format.url(), headers, proxy, false);
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Failed to download: HTTP " + responseCode);
        }
        final int contentLength = urlConnection.getContentLength();
        final InputStream is = urlConnection.getInputStream();

        final byte[] buffer = new byte[BUFFER_SIZE];
        if (callback == null) {
            copyAndCloseInput(is, os, buffer);
        } else {
            copyAndCloseInput(is, os, buffer, 0, contentLength, callback);
        }
    }

    // Downloads the format part by part, with as many requests as needed
    private void downloadByPart(final Format format, final OutputStream os, final Map<String, String> headers, final Proxy proxy, final YoutubeCallback<?> listener) throws IOException {
        long done = 0;
        int partNumber = 0;

        final String pathPrefix = "&cver=" + format.clientVersion() + "&range=";
        final long contentLength = format.contentLength();
        final byte[] buffer = new byte[BUFFER_SIZE];

        while (done < contentLength) {
            long toRead = PART_LENGTH;
            if (done + toRead > contentLength) {
                toRead = (int) (contentLength - done);
            }

            partNumber++;
            final String partUrl = format.url() + pathPrefix
                    + done + "-" + (done + toRead - 1)    // range first-last byte positions
                    + "&rn=" + partNumber;                // part number

            final HttpURLConnection urlConnection = this.openConnection(partUrl, headers, proxy, false);
            final int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Failed to download: HTTP " + responseCode);
            }

            final InputStream is = urlConnection.getInputStream();
            if (listener == null) {
                done += copyAndCloseInput(is, os, buffer);
            } else {
                done += copyAndCloseInput(is, os, buffer, done, contentLength, listener);
            }
        }
    }

    // Copies as many bytes as possible then closes input stream
    private static long copyAndCloseInput(final InputStream is, final OutputStream os, final byte[] buffer, final long offset, final long totalLength, final YoutubeCallback<?> listener) throws IOException {
        long done = 0;

        try {
            int read = 0;
            long lastProgress = offset == 0 ? 0 : (offset * 100) / totalLength;

            while ((read = is.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new CancellationException();
                }
                os.write(buffer, 0, read);
                done += read;
                final long progress = ((offset + done) * 100) / totalLength;
                if (progress > lastProgress) {
                    if (listener instanceof YoutubeProgressCallback) {
                        ((YoutubeProgressCallback<?>) listener).onDownloading((int) progress);
                    }
                    lastProgress = progress;
                }
            }
        } finally {
            closeSilently(is);
        }
        return done;
    }

    private static long copyAndCloseInput(final InputStream is, final OutputStream os, final byte[] buffer) throws IOException {
        long done = 0;

        try {
            int count = 0;
            while ((count = is.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new CancellationException();
                }
                os.write(buffer, 0, count);
                done += count;
            }
        } finally {
            closeSilently(is);
        }
        return done;
    }


    private HttpURLConnection openConnection(final String httpUrl, final Map<String, String> headers, final Proxy proxy, final boolean acceptCompression) throws IOException {
        final URL url = new URL(httpUrl);

        final HttpURLConnection urlConnection;
        if (proxy != null) {
            urlConnection = (HttpURLConnection) url.openConnection(proxy);
        } else if (this.config.getProxy() != null) {
            urlConnection = (HttpURLConnection) url.openConnection(this.config.getProxy());
        } else {
            urlConnection = (HttpURLConnection) url.openConnection();
        }
        for (final Map.Entry<String, String> entry : this.config.getHeaders().entrySet()) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (acceptCompression) {
            urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        }
        if (headers != null) {
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
                urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return urlConnection;
    }
}
