package com.github.kiulian.downloader.downloader.proxy;

import java.net.PasswordAuthentication;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyCredentialsImpl implements ProxyCredentials {

    private final Map<String, PasswordAuthentication> credentials = new ConcurrentHashMap<>();

    @Override
    public PasswordAuthentication getAuthentication(final String host, final int port) {
        final String key = host + ":" + port;
        return this.credentials.get(key);
    }

    @Override
    public void addAuthentication(final String host, final int port, final String userName, final String password) {
        this.credentials.put(host + ":" + port, new PasswordAuthentication(userName, password.toCharArray()));
    }
}
