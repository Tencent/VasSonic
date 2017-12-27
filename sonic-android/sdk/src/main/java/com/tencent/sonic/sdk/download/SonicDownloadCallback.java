package com.tencent.sonic.sdk.download;

import java.util.List;
import java.util.Map;

/**
 * download callback.
 */

public interface SonicDownloadCallback {
    /**
     * notify the download start.
     */
    void onStart();

    /**
     * notify the download progress.
     *
     * @param pro downloaded size
     * @param total total size
     */
    void onProgress(int pro, int total);

    /**
     * notify download success.
     *
     * @param content downloaded content bytes
     * @param rspHeaders http response headers
     */
    void onSuccess(byte[] content, Map<String, List<String>> rspHeaders);

    /**
     * notify download failed.
     *
     * @param errorCode error code
     */
    void onError(int errorCode);

    /**
     * notify download finish. <code>onSuccess</code> or <code>onError</code>
     */
    void onFinish();

    /**
     * an empty implementation of {@link SonicDownloadCallback}
     */
    class SimpleDownloadCallback implements SonicDownloadCallback {

        @Override
        public void onStart() { }

        @Override
        public void onProgress(int pro, int total) { }

        @Override
        public void onSuccess(byte[] content, Map<String, List<String>> rspHeaders) { }

        @Override
        public void onError(int errorCode) { }

        @Override
        public void onFinish() { }
    }
}
