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

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * Interact with the overall file operations.
 */
class SonicFileUtils {

    /**
     * Log filter
     */
    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicFileUtils";

    /**
     *  Template  extensions
     */
    private static final String TEMPLATE_EXT = ".tpl";

    /**
     * Data extensions
     */
    private static final String DATA_EXT = ".data";

    /**
     * Html extensions
     */
    private static final String HTML_EXT = ".html";

    /**
     * The max percent threshold of cache.
     * If the size of cache exceed max threshold, it will trim cache to{@link SonicFileUtils#THRESHOLD_OF_CACHE_MIN_PERCENT}
     */
    private static final double THRESHOLD_OF_CACHE_MAX_PERCENT = 0.8;

    /**
     * The min percent threshold of cache.
     */
    private static final double THRESHOLD_OF_CACHE_MIN_PERCENT = 0.25;

    /**
     *
     * @return Returns the absolute path to the specific cache directory on
     *  the filesystem (including File.separator at the end of path).
     */
    static String getSonicCacheDirPath() {
        String dirPath = SonicEngine.getInstance().getRuntime().getSonicCacheDir().getAbsolutePath();
        if (!dirPath.endsWith(File.separator)) {
            dirPath += File.separator;
        }
        return dirPath;
    }

    /**
     *
     * @param sessionId
     * @return The path of the directory holding sonic template cache files.
     */
    static String getSonicTemplatePath(String sessionId) {
        return getSonicCacheDirPath() + sessionId + TEMPLATE_EXT;
    }

    /**
     *
     * @param sessionId
     * @return The path of the directory holding sonic data cache files.
     */
    static String getSonicDataPath(String sessionId) {
        return getSonicCacheDirPath() + sessionId + DATA_EXT;
    }

    /**
     *
     * @param sessionId
     * @return The path of the directory holding sonic html cache files.
     */
    static String getSonicHtmlPath(String sessionId) {
        return getSonicCacheDirPath() + sessionId + HTML_EXT;
    }

    /**
     *
     * @param sessionId
     * @return Return {@code true} if all of the cache files have been deleted, such as html template and the data cache files.
     */
    static boolean deleteSonicFiles(String sessionId) {
        boolean deleteSuccess = true;
        File htmlFile = new File(getSonicHtmlPath(sessionId));
        if (htmlFile.exists()) {
            deleteSuccess = htmlFile.delete();
        }

        File templateFile = new File(getSonicTemplatePath(sessionId));
        if (templateFile.exists()) {
            deleteSuccess &= templateFile.delete();
        }

        File dataFile = new File(getSonicDataPath(sessionId));
        if (dataFile.exists()) {
            deleteSuccess &= dataFile.delete();
        }

        return deleteSuccess;
    }

    /**
     * This method computes hash value by using specified SHA1 digest algorithm and compares hash value to the specified hash @{code targetSha1}.
     *
     * @param content    Data
     * @param targetSha1 The specified hash value
     * @return {@code true} if the given hash value
     *          equivalent to computed hash value, {@code false} otherwise
     */
    static boolean verifyData(String content, String targetSha1) {
        return !TextUtils.isEmpty(content) && !TextUtils.isEmpty(targetSha1) &&
                targetSha1.equals(SonicUtils.getSHA1(content));
    }

    /**
     *
     * @param file The file path of template
     * @return Returns a string containing all of the content readed from template file.
     */
    static String readFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }

        // read
        BufferedInputStream bis = null;
        InputStreamReader reader = null;
        char[] buffer;
        String rtn = null;
        int n;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            reader = new InputStreamReader(bis, "UTF-8");
            int size = (int) file.length();
            if (size > 1024 * 12) {
                buffer = new char[1024 * 4];
                StringBuilder result = new StringBuilder(1024 * 12);
                while (-1 != (n = reader.read(buffer))) {
                    result.append(buffer, 0, n);
                }
                rtn = result.toString();
            } else {
                buffer = new char[size];
                n = reader.read(buffer);
                rtn = new String(buffer, 0, n);
            }
        } catch (Exception e) {
            SonicUtils.log(TAG, Log.ERROR, "readFile error:(" + file.getName() + ") " + e.getMessage());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception e) {
                    SonicUtils.log(TAG, Log.ERROR, "readFile close error:(" + file.getName() + ") " + e.getMessage());
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    SonicUtils.log(TAG, Log.ERROR, "readFile close error:(" + file.getName() + ") " + e.getMessage());
                }
            }
        }
        return rtn;
    }

    /**
     * Write string to the file represented by
     * the specified <code>File</code> object.
     *
     * @param str      The string is to be saved
     * @param filePath
     * @return Returns {@code true} if string is saved successfully.
     */
    static boolean writeFile(String str, String filePath) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            if (!file.exists() && !file.createNewFile()) {
                return false;
            }
            fos = new FileOutputStream(file);
            fos.write(str.getBytes());
            fos.flush();
            return true;
        } catch (Throwable e) {
            SonicUtils.log(TAG, Log.ERROR, "writeFile error:(" + filePath + ") " + e.getMessage());
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (Throwable e) {
                    SonicUtils.log(TAG, Log.ERROR, "writeFile close error:(" + filePath + ") " + e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * Deletes all of the files or directory denoted by this file path
     *
     * @param file The file to be deleted
     */
    static boolean deleteAllChildFiles(File file) {
        boolean deleteSuccess = true;
        if (null != file && file.exists()) {
            if (file.isFile()) {
                deleteSuccess = file.delete();
            } else if (file.isDirectory()) {
                File[] childFiles = file.listFiles();
                if (null != childFiles) {
                    for (File childFile : childFiles) {
                        deleteSuccess &= deleteAllChildFiles(childFile);
                    }
                }
            }
        }
        return deleteSuccess;
    }

    /**
     * Check whether the sonic cache has been exceed the limit {@link SonicConfig#SONIC_CACHE_MAX_SIZE}.
     * If the size of sonic cache exceeds, then it will remove the elder cache
     * until the size is less than threshold {@link SonicFileUtils#THRESHOLD_OF_CACHE_MIN_PERCENT}.
     */
    static void checkAndTrimCache() {
        File cacheRootDir = new File(getSonicCacheDirPath());
        if (cacheRootDir.exists() && cacheRootDir.isDirectory()) {
            File[] childFiles = cacheRootDir.listFiles();
            if (childFiles != null && childFiles.length > 0) {
                long startTime = System.currentTimeMillis();
                long cacheFileSize = 0L;
                final long MAX_CACHE_SIZE = SonicEngine.getInstance().getConfig().SONIC_CACHE_MAX_SIZE;

                for (int i = 0; i < childFiles.length; i++) {
                    cacheFileSize += childFiles[i].length();
                }

                SonicDataHelper.setLastClearCacheTime(System.currentTimeMillis());
                if (cacheFileSize > (MAX_CACHE_SIZE * THRESHOLD_OF_CACHE_MAX_PERCENT)) {
                    SonicUtils.log(TAG, Log.INFO, "now try clear cache, current cache size: " + (cacheFileSize / 1024 / 1024) + "m");
                    List<File> files = Arrays.asList(childFiles);
                    Collections.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            long diff = f1.lastModified() - f2.lastModified();
                            if (diff > 0)
                                return 1;
                            else if (diff == 0)
                                return 0;
                            else
                                return -1;
                        }
                    });

                    long fileSize ;
                    File file ;
                    String fileName;
                    for (int i = 0; i < files.size(); i++) {
                        file = files.get(i);
                        if (file.isFile() && file.exists()) {
                            fileName = file.getName();
                            fileSize = file.length();
                            if (file.delete()) {
                                cacheFileSize -= fileSize;
                                SonicDataHelper.removeSessionData(fileName);
                                SonicUtils.log(TAG, Log.INFO, "delete " + file.getAbsolutePath());
                            }
                        }
                        if (cacheFileSize <= MAX_CACHE_SIZE * THRESHOLD_OF_CACHE_MIN_PERCENT) {
                            break;
                        }
                    }

                    SonicUtils.log(TAG, Log.INFO, "checkAndTrimCache: finish , cost "
                            + (System.currentTimeMillis() - startTime) + "ms.");
                }

            }

        }
    }

    /**
     * Decide whether need check the size of sonic cache or not.
     *
     * @return  If the last time of check sonic cache exceed {@link SonicConfig#SONIC_CACHE_CHECK_TIME_INTERVAL},
     *  then return true, otherwise false.
     */
    static boolean isNeedCheckSizeOfCache() {
        long lastCheckTime = SonicDataHelper.getLastClearCacheTime();
        long now = System.currentTimeMillis();
        long interval = SonicEngine.getInstance().getConfig().SONIC_CACHE_CHECK_TIME_INTERVAL;
        if ((now - lastCheckTime) > interval) {
            return true;
        }

        return false;
    }
}
