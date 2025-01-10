package com.github.kiulian.downloader.downloader.proxy;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ProxyAuthenticator extends Authenticator {

    private static volatile ProxyAuthenticator instance;

    private final ProxyCredentials proxyCredentials;

    public ProxyAuthenticator(final ProxyCredentials proxyCredentials) {
        this.proxyCredentials = proxyCredentials;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        return this.proxyCredentials.getAuthentication(this.getRequestingHost(), this.getRequestingPort());
    }

    public static synchronized void setDefault(final ProxyAuthenticator authenticator) {
        instance = authenticator;
        Authenticator.setDefault(instance);
    }

    public static synchronized ProxyAuthenticator getDefault() {
        return instance;
    }

    public static void addAuthentication(final String host, final int port, final String userName, final String password) {
        if (instance == null) {
            throw new NullPointerException("ProxyAuthenticator instance is null. Use ProxyAuthenticator.setDefault() to init");
        }
        instance.proxyCredentials.addAuthentication(host, port, userName, password);
    }

}
