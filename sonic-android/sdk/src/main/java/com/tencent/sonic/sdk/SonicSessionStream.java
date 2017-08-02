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

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 *
 * A <code>SonicSessionStream</code> obtains input bytes
 * from a <code>memStream</code> and a <code>netStream</code>.
 * <code>memStream</code>is read data from network, <code>netStream</code>is unread data from network.
 *
 */
class SonicSessionStream extends InputStream {

    /**
     * Log filter
     */
    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicSessionStream";

    /**
     * Unread data from network
     */
    private BufferedInputStream netStream;

    /**
     * Read data from network
     */
    private BufferedInputStream memStream;

    /**
     * OutputStream include <code>memStream</code> data and <code>netStream</code> data
     */
    private ByteArrayOutputStream outputStream;

    /**
     * <code>netStream</code> data completed flag
     */
    private boolean netStreamReadComplete = true;

    /**
     * <code>memStream</code> data completed flag
     */
    private boolean memStreamReadComplete = true;

    /**
     * When <code>SonicSessionStream</code> close the stream will invoke the <code>Callback</code>
     */
    public interface Callback {
        /**
         * Close callback
         *
         * @param readComplete <code>SonicSessionStream</code> data has read completed
         * @param outputStream outputStream include <code>memStream</code> data and <code>netStream</code> data
         */
        void onClose(boolean readComplete, ByteArrayOutputStream outputStream);
    }

    /**
     * Callback WeakReference
     */
    private final WeakReference<Callback> callbackWeakReference;

    /**
     * Constructor
     *
     * @param callback     Callback
     * @param outputStream Read data from network
     * @param netStream    Unread data from network
     */
    public SonicSessionStream(Callback callback, ByteArrayOutputStream outputStream, BufferedInputStream netStream) {
        if (null != netStream) {
            this.netStream = netStream;
            this.netStreamReadComplete = false;
        }

        if (outputStream != null) {
            this.outputStream = outputStream;
            this.memStream = new BufferedInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            this.memStreamReadComplete = false;
        } else {
            this.outputStream = new ByteArrayOutputStream();
        }

        callbackWeakReference = new WeakReference<Callback>(callback);
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream and invoke the callback's onClose method
     *
     */
    @Override
    public void close() throws IOException {
        if (SonicUtils.shouldLog(Log.INFO)) {
            SonicUtils.log(TAG, Log.INFO, "close: memory stream and socket stream, netStreamReadComplete=" + netStreamReadComplete + ", memStreamReadComplete=" + memStreamReadComplete);
        }

        try {
            if (null != memStream) {
                memStream.close();
                memStream = null;
            }

            if (null != netStream) {
                netStream.close();
                netStream = null;
            }

            Callback callback = callbackWeakReference.get();
            if (null != callback) {
                callback.onClose(netStreamReadComplete && memStreamReadComplete, outputStream);
            }
            outputStream = null;

        } catch (Throwable e) {
            SonicUtils.log(TAG, Log.ERROR, "close error:" + e.getMessage());
            if (e instanceof IOException) {
                throw e;
            } else { // Turn all exceptions to IO exceptions to prevent scenes that the kernel can not capture
                throw new IOException(e);
            }
        }
    }

    /**
     *
     * <p>
     * Reads a single byte from this stream and returns it as an integer in the
     * range from 0 to 255. Returns -1 if the end of the stream has been
     * reached. Blocks until one byte has been read, the end of the source
     * stream is detected or an exception is thrown.
     *
     * @throws IOException if the stream is closed or another IOException occurs.
     */
    @Override
    public synchronized int read() throws IOException {

        int c = -1;

        try {
            if (null != memStream && !memStreamReadComplete) {
                c = memStream.read();
            }

            if (-1 == c) {
                memStreamReadComplete = true;
                if (null != netStream && !netStreamReadComplete) {
                    c = netStream.read();
                    if (-1 != c) {
                        outputStream.write(c);
                    } else {
                        netStreamReadComplete = true;
                    }
                }
            }
        } catch (Throwable e) {
            SonicUtils.log(TAG, Log.ERROR, "read error:" + e.getMessage());
            if (e instanceof IOException) {
                throw e;
            } else {//Turn all exceptions to IO exceptions to prevent scenes that the kernel can not capture
                throw new IOException(e);
            }
        }

        return c;
    }

    /**
     * Reads a byte of data from this input stream
     * Equivalent to {@code read(buffer, 0, buffer.length)}.
     */
    @Override
    public synchronized int read(@NonNull byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    /**
     *
     * Reads up to {@code byteCount} bytes from this stream and stores them in
     * the byte array {@code buffer} starting at {@code byteOffset}.
     * Returns the number of bytes actually read or -1 if the end of the stream
     * has been reached.
     *
     * @throws IndexOutOfBoundsException if {@code byteOffset < 0 || byteCount < 0 || byteOffset + byteCount > buffer.length}.
     * @throws IOException               if the stream is closed or another IOException occurs.
     */
    public synchronized int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int arrayLength = buffer.length;
        if ((byteOffset | byteCount) < 0 || byteOffset > arrayLength || arrayLength - byteOffset < byteCount) {
            throw new ArrayIndexOutOfBoundsException();
        }

        for (int i = 0; i < byteCount; ++i) {
            int c;
            try {
                if ((c = read()) == -1) {
                    return i == 0 ? -1 : i;
                }
            } catch (IOException e) {
                if (i != 0) {
                    return i;
                }
                throw e;
            }
            buffer[byteOffset + i] = (byte) c;
        }
        return byteCount;
    }
}
