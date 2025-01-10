package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.File;
import java.util.UUID;

import static com.github.kiulian.downloader.model.Utils.removeIllegalChars;

public class RequestVideoFileDownload extends Request<RequestVideoFileDownload, File> {

    private File outputDirectory = new File("videos");
    private boolean overwrite = false;
    private String fileName = UUID.randomUUID().toString();

    private final Format format;

    public RequestVideoFileDownload(final Format format) {
        this.format = format;
    }

    public RequestVideoFileDownload saveTo(final File directory) {
        this.outputDirectory = directory;
        return this;
    }

    public RequestVideoFileDownload renameTo(final String fileName) {
        this.fileName = fileName;
        return this;
    }

    public RequestVideoFileDownload overwriteIfExists(final boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public File getOutputDirectory() {
        return this.outputDirectory;
    }

    public Format getFormat() {
        return this.format;
    }

    public File getOutputFile() {
        final String originalName = removeIllegalChars(this.fileName);
        String fileName = originalName + "." + this.format.extension().value();
        File outputFile = new File(this.outputDirectory, fileName);

        if (!this.overwrite) {
            int i = 1;
            while (outputFile.exists()) {
                fileName = originalName + "(" + i++ + ")" + "." + this.format.extension().value();
                outputFile = new File(outputFile.getParentFile(), fileName);
            }
        }
        return outputFile;
    }
}
