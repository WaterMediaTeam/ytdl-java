package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.proxy.ProxyAuthenticator;
import com.github.kiulian.downloader.downloader.proxy.ProxyCredentials;
import com.github.kiulian.downloader.downloader.proxy.ProxyCredentialsImpl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Config {
    private static final ThreadFactory threadFactory = new ThreadFactory() {
        private static final String NAME_PREFIX = "yt-downloader-";
        private final AtomicInteger threadNumber = new AtomicInteger(0);

        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(r, NAME_PREFIX + this.threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    };

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    private static final String DEFAULT_ACCEPT_LANG = "en-US,en;";
    private static final int DEFAULT_RETRY_ON_FAILURE = 0;

    private final Map<String, String> headers;
    private int retries;
    private boolean compressionEnabled;
    private ExecutorService executorService;
    private Proxy proxy;

    private Config(final Builder builder) {
        this.headers = builder.headers;
        this.retries = builder.retries;
        this.compressionEnabled = builder.compressionEnabled;
        this.executorService = builder.executorService;
        this.proxy = builder.proxy;
    }

    private Config() {
        this.headers = new HashMap<>();
        this.retries = DEFAULT_RETRY_ON_FAILURE;
        this.compressionEnabled = true;
        this.executorService = null;

        this.setHeader("User-Agent", DEFAULT_USER_AGENT);
        this.setHeader("Accept-language", DEFAULT_ACCEPT_LANG);
    }

    static Config buildDefault() {
        return new Config();
    }

    public void setRetries(final int retries) {
        this.retries = retries;
    }

    public void setCompressionEnabled(final boolean enabled) {
        this.compressionEnabled = enabled;
    }

    public void setHeader(final String key, final String value) {
        this.headers.put(key, value);
    }

    public void setProxy(final String host, final int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    public void setProxy(final String host, final int port, final String userName, final String password) {
        if (ProxyAuthenticator.getDefault() == null) {
            ProxyAuthenticator.setDefault(new ProxyAuthenticator(new ProxyCredentialsImpl()));
        }
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        ProxyAuthenticator.addAuthentication(host, port, userName, password);
    }

    public void setExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setProxyAuthenticator(final ProxyCredentials credentials) {
        ProxyAuthenticator.setDefault(new ProxyAuthenticator(credentials));
    }

    public ExecutorService getExecutorService() {
        if (this.executorService == null) {
            this.executorService = Executors.newCachedThreadPool(threadFactory);
        }
        return this.executorService;
    }

    public int getRetries() {
        return this.retries;
    }

    public boolean isCompressionEnabled() {
        return this.compressionEnabled;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private int retries = DEFAULT_RETRY_ON_FAILURE;
        private boolean compressionEnabled = true;
        private ExecutorService executorService;
        private Proxy proxy;

        public Builder maxRetries(final int maxRetries) {
            this.retries = maxRetries;
            return this;
        }

        public Builder enableCompression(final boolean enable) {
            this.compressionEnabled = enable;
            return this;
        }

        public Builder header(final String key, final String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder proxy(final String host, final int port) {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            return this;
        }

        public Builder proxy(final String host, final int port, final String userName, final String password) {
            if (ProxyAuthenticator.getDefault() == null) {
                ProxyAuthenticator.setDefault(new ProxyAuthenticator(new ProxyCredentialsImpl()));
            }
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            ProxyAuthenticator.addAuthentication(host, port, userName, password);
            return this;
        }

        public Builder proxy(final Proxy proxy) {
            this.proxy = proxy;
            return this;
        }


        public Builder executorService(final ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder proxyCredentialsManager(final ProxyCredentials credentials) {
            ProxyAuthenticator.setDefault(new ProxyAuthenticator(credentials));
            return this;
        }

        public Config build() {
            return new Config(this);
        }

    }

}
