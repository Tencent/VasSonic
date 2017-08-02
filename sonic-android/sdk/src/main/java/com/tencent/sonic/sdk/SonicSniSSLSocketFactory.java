/*
 * Tencent is pleased to support the open source community by making VasSonic available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package com.tencent.sonic.sdk;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * Implement IP direct support for SNI type in https scenarios
 * main method{ public Socket createSocket(Socket s, String host, int port, boolean autoClose)}
 *
 */

class SonicSniSSLSocketFactory extends SSLSocketFactory {

    /**
     * Log filter
     */
    private final static String TAG = SonicConstants.SONIC_PARAMETER_NAME_PREFIX + "SonicSniSSLSocketFactory";

    /**
     * Host name，use to certificate validation
     */
    private final String targetHostName;

    /**
     * SSLSocketFactory implementation class
     */
    private final SSLCertificateSocketFactory sslSocketFactory;

    SonicSniSSLSocketFactory(Context context, String targetHostName) {
        super();
        this.targetHostName = targetHostName;
        this.sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0, new SSLSessionCache(context));
    }

    /**
     * Returns the names of the cipher suites that are enabled by default.
     *
     * @return The names of the cipher suites that are enabled by default.
     */
    @Override
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    /**
     * Returns the names of the cipher suites that are supported and could be
     * enabled for an SSL connection.
     *
     * @return The names of the cipher suites that are supported.
     */
    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method verifies the peer's certificate hostname after connecting
     */
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        // The socket connection is completed, you need to upgrade the TLS layer, so the host will be replaced by a real domain name.
        return sslSocketFactory.createSocket(s, targetHostName, port, autoClose);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method verifies the peer's certificate hostname after connecting
     */
    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port));
        verifyHostname(socket, targetHostName);
        return socket;
    }

    /**
     * Creates a new socket which is not connected to any remote host.
     * You must use {@link Socket#connect} to connect the socket.
     *
     * <p class="caution"><b>Warning:</b> Hostname verification is not performed
     * with this method.  You MUST verify the server's identity after connecting
     * the socket to avoid man-in-the-middle attacks.</p>
     */
    @Override
    public Socket createSocket() throws IOException {
        return sslSocketFactory.createSocket();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method verifies the peer's certificate hostname after connecting
     */
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localHost, localPort));
        socket.connect(new InetSocketAddress(host, port));
        verifyHostname(socket, targetHostName);
        return socket;
    }

    /**
     * {@inheritDoc}
     *
     * <p class="caution"><b>Warning:</b> Hostname verification is not performed
     * with this method.  You MUST verify the server's identity after connecting
     * the socket to avoid man-in-the-middle attacks.</p>
     */
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslSocketFactory.createSocket(host, port);
    }
    /**
     * {@inheritDoc}
     *
     * <p class="caution"><b>Warning:</b> Hostname verification is not performed
     * with this method.  You MUST verify the server's identity after connecting
     * the socket to avoid man-in-the-middle attacks.</p>
     */
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return sslSocketFactory.createSocket(address, port, localAddress, localPort);
    }

    /**
     * Verify the hostname of the certificate used by the other end of a
     * connected socket.  You MUST call this if you did not supply a hostname
     * to {@link #createSocket()}.  It is harmless to call this method
     * redundantly if the hostname has already been verified.
     *
     * <p>Wildcard certificates are allowed to verify any matching hostname,
     * so "foo.bar.example.com" is verified if the peer has a certificate
     * for "*.example.com".
     *
     * @param socket An SSL socket which has been connected to a server
     * @param hostname The expected hostname of the remote server
     * @throws IOException if something goes wrong handshaking with the server
     * @throws SSLPeerUnverifiedException if the server cannot prove its identity
     *
     */
    public static void verifyHostname(Socket socket, String hostname) throws IOException {
        if (!(socket instanceof SSLSocket)) {
            throw new IllegalArgumentException("Attempt to verify non-SSL socket");
        }

        // The code at the start of OpenSSLSocketImpl.startHandshake()
        // ensures that the call is idempotent, so we can safely call it.
        SSLSocket ssl = (SSLSocket) socket;
        ssl.startHandshake();

        SSLSession session = ssl.getSession();
        if (session == null) {
            throw new SSLException("Cannot verify SSL socket without session");
        }

        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)) {
            SonicUtils.log(TAG, Log.ERROR, "sonic SSL error:Cannot verify hostname" + hostname + ")!");
            throw new SSLPeerUnverifiedException("Cannot verify hostname: " + hostname);
        }
    }
}
