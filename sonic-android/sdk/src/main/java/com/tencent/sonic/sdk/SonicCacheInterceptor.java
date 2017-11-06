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

import java.io.File;

/**
 * <code>SonicCacheInterceptor</code> provide local data.
 * if a {@link SonicSessionConfig} does not set a sonicCacheInterceptor
 * sonic will use {@link SonicSessionConnection.SessionConnectionDefaultImpl} as default.
 *
 */
public abstract class SonicCacheInterceptor {

    public static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicCacheInterceptor";

    private final SonicCacheInterceptor nextInterceptor;

    public SonicCacheInterceptor(SonicCacheInterceptor next) {
        nextInterceptor = next;
    }

    public SonicCacheInterceptor next() {
        return nextInterceptor;
    }

    public abstract String getCacheData(SonicSession session);

    static String getSonicCacheData(SonicSession session) {
        SonicCacheInterceptor interceptor = session.config.cacheInterceptor;
        if (null == interceptor) {
            return SonicCacheInterceptorDefaultImpl.getCacheData(session);
        }

        String htmlString = null;
        while (null != interceptor) {
            htmlString = interceptor.getCacheData(session);
            if (null != htmlString) {
                break;
            }
            interceptor = interceptor.next();
        }
        return htmlString;
    }

    /**
     * <code>SonicCacheInterceptorDefaultImpl</code> provide a default implement for SonicCacheInterceptor.
     */
    private static class SonicCacheInterceptorDefaultImpl {

        public static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "DefaultSonicCacheInterceptor";

        public static String getCacheData(SonicSession session) {
            if (session == null) {
                SonicUtils.log(TAG, Log.INFO, "getCache is null");
                return null;
            }

            SonicDataHelper.SessionData sessionData = SonicDataHelper.getSessionData(session.id);
            boolean verifyError;
            String htmlString = "";
            // verify local data
            if (TextUtils.isEmpty(sessionData.eTag) || TextUtils.isEmpty(sessionData.htmlSha1)) {
                verifyError = true;
                SonicUtils.log(TAG, Log.INFO, "session(" + session.sId + ") runSonicFlow : session data is empty.");
            } else {
                SonicDataHelper.updateSonicCacheHitCount(session.id);
                File htmlCacheFile = new File(SonicFileUtils.getSonicHtmlPath(session.id));
                htmlString = SonicFileUtils.readFile(htmlCacheFile);
                verifyError = TextUtils.isEmpty(htmlString);
                if (verifyError) {
                    SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") runSonicFlow error:cache data is null.");
                } else {
                    if (SonicEngine.getInstance().getConfig().VERIFY_CACHE_FILE_WITH_SHA1) {
                        if (!SonicFileUtils.verifyData(htmlString, sessionData.htmlSha1)) {
                            verifyError = true;
                            htmlString = "";
                            SonicEngine.getInstance().getRuntime().notifyError(session.sessionClient, session.srcUrl, SonicConstants.ERROR_CODE_DATA_VERIFY_FAIL);
                            SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") runSonicFlow error:verify html cache with sha1 fail.");
                        } else {
                            SonicUtils.log(TAG, Log.INFO, "session(" + session.sId + ") runSonicFlow verify html cache with sha1 success.");
                        }
                    } else {
                        if (sessionData.htmlSize != htmlCacheFile.length()) {
                            verifyError = true;
                            htmlString = "";
                            SonicEngine.getInstance().getRuntime().notifyError(session.sessionClient, session.srcUrl, SonicConstants.ERROR_CODE_DATA_VERIFY_FAIL);
                            SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") runSonicFlow error:verify html cache with size fail.");
                        }
                    }
                }
            }
            // if the local data is faulty, delete it
            if (verifyError) {
                long startTime = System.currentTimeMillis();
                SonicUtils.removeSessionCache(session.id);
                sessionData.reset();
                SonicUtils.log(TAG, Log.INFO, "session(" + session.sId + ") runSonicFlow:verify error so remove session cache, cost " + +(System.currentTimeMillis() - startTime) + "ms.");
            }
            return htmlString;
        }
    }
}
