package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.client.Client;
import com.github.kiulian.downloader.downloader.client.DefaultClients;
import com.github.kiulian.downloader.downloader.proxy.ProxyAuthenticator;
import com.github.kiulian.downloader.downloader.proxy.ProxyCredentialsImpl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.function.Supplier;

public abstract class Request<T extends Request<T, S>, S> {
    protected Map<String, String> headers;
    private YoutubeCallback<S> callback;
    private boolean async;
    private int retries;
    private Proxy proxy;
    private Client client = DefaultClients.defaultClient();

    public T proxy(final String host, final int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        return (T) this;
    }

    public T proxy(final String host, final int port, final String userName, final String password) {
        if (ProxyAuthenticator.getDefault() == null) {
            ProxyAuthenticator.setDefault(new ProxyAuthenticator(new ProxyCredentialsImpl()));
        }
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        ProxyAuthenticator.addAuthentication(host, port, userName, password);
        return (T) this;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public T retries(final int maxRetries) {
        this.retries = maxRetries;
        return (T) this;
    }

    public int getRetries() {
        return this.retries;
    }

    public T callback(final YoutubeCallback<S> callback) {
        this.callback = callback;
        return (T) this;
    }

    public YoutubeCallback<S> getCallback() {
        return this.callback;
    }

    public T header(final String key, final String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(key, value);
        return (T) this;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public T async() {
        this.async = true;
        return (T) this;
    }

    public boolean isAsync() {
        return this.async;
    }

    public T client(final Client client) {
        this.client = client;
        return (T) this;
    }

    public Client getClient() {
        return this.client;
    }
}
