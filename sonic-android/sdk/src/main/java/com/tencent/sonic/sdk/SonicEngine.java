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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Interacts with the overall SonicSessions running in the system.
 * Instances of this class can be used to query or fetch the information, such as SonicSession SonicRuntime.
 */
public class SonicEngine {

    /**
     * Log filter
     */
    private final static String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicEngine";

    /**
     * SonicRuntime
     */
    private final SonicRuntime runtime;

    /**
     * Global config
     */
    private final SonicConfig config;

    /**
     * Single instance
     */
    private static SonicEngine sInstance;

    /**
     * Map containing preload session with capacity limits.
     */
    private final ConcurrentHashMap<String, SonicSession> preloadSessionPool = new ConcurrentHashMap<String, SonicSession>(5);

    /**
     * Map containing weak reference of running sessions.
     */
    private final ConcurrentHashMap<String, SonicSession> runningSessionHashMap = new ConcurrentHashMap<String, SonicSession>(5);


    private SonicEngine(SonicRuntime runtime, SonicConfig config) {
        this.runtime = runtime;
        this.config = config;
    }

    /**
     * Returns a SonicEngine instance
     * <p>
     * Make sure {@link #createInstance(SonicRuntime, SonicConfig)} has been called.
     *
     * @return SonicEngine instance
     * @throws IllegalStateException if {@link #createInstance(SonicRuntime, SonicConfig)} hasn't been called
     */
    public static synchronized SonicEngine getInstance() {
        if (null == sInstance) {
            throw new IllegalStateException("SonicEngine::createInstance() needs to be called before SonicEngine::getInstance()");
        }
        return sInstance;
    }

    /**
     * Check if {@link #getInstance()} is ready or not.
     * <p><b>Note: {@link #createInstance(SonicRuntime, SonicConfig)} must be called if {@code false} is returned.</b></p>
     * @return
     *      Return <code>true</code> if {@link #sInstance} is not null, <code>false</code> otherwise
     */
    public static synchronized boolean isGetInstanceAllowed() {
        return null != sInstance;
    }

    /**
     * Create SonicEngine instance. Meanwhile it will initialize engine and SonicRuntime.
     * @param runtime SonicRuntime
     * @param config SonicConfig
     * @return SonicEngine object
     */
    public static synchronized SonicEngine createInstance(@NonNull SonicRuntime runtime, @NonNull SonicConfig config) {
        if (null == sInstance) {
            sInstance = new SonicEngine(runtime, config);
        }

        return sInstance;
    }

    /**
     * @return SonicRuntime object
     */
    public SonicRuntime getRuntime() {
        return runtime;
    }

    /**
     * @return SonicConfig object
     */
    public SonicConfig getConfig() {
        return config;
    }

    /**
     * Create session ID
     *
     * @param url    session url
     * @param isAccountRelated
     *   Session Id will contain {@link com.tencent.sonic.sdk.SonicRuntime#getCurrentUserAccount()}  if {@code isAccountRelated } is true.
     * @return String Object of session ID
     */
    public static String makeSessionId(String url, boolean isAccountRelated) {
        return getInstance().getRuntime().makeSessionId(url, isAccountRelated);
    }

    /**
     * This method will preCreate sonic session .
     * And maps the specified session id to the specified value in this table {@link #preloadSessionPool} if there is no same sonic session.
     * At the same time, if the number of {@link #preloadSessionPool} exceeds {@link SonicConfig#MAX_PRELOAD_SESSION_COUNT},
     * preCreateSession will return false and not create any sonic session.
     *
     * <p><b>Note: this method is intended for preload scene.</b></p>
     * @param url           url for preCreate sonic session
     * @param sessionConfig SonicSession config
     * @return
     *  If this method preCreate sonic session and associated with {@code sessionId} in this table {@link #preloadSessionPool} successfully,
     *  it will return true,
     *  <code>false</code> otherwise.
     */
    public synchronized boolean preCreateSession(@NonNull String url, @NonNull SonicSessionConfig sessionConfig) {
        String sessionId = makeSessionId(url, sessionConfig.IS_ACCOUNT_RELATED);
        if (!TextUtils.isEmpty(sessionId)) {
            SonicSession sonicSession = lookupSession(sessionConfig, sessionId, false);
            if (null != sonicSession) {
                runtime.log(TAG, Log.ERROR, "preCreateSession：sessionId(" + sessionId + ") is already in preload pool.");
                return false;
            }
            if (preloadSessionPool.size() < config.MAX_PRELOAD_SESSION_COUNT) {
                if (isSessionAvailable(sessionId) && runtime.isNetworkValid()) {
                    sonicSession = internalCreateSession(sessionId, url, sessionConfig);
                    if (null != sonicSession) {
                        preloadSessionPool.put(sessionId, sonicSession);
                        return true;
                    }
                }
            } else {
                runtime.log(TAG, Log.ERROR, "create id(" + sessionId + ") fail for preload size is bigger than " + config.MAX_PRELOAD_SESSION_COUNT + ".");
            }
        }
        return false;
    }

    /**
     *
     * @param url           url for SonicSession Object
     * @param sessionConfig SSonicSession config
     * @return This method will create and return SonicSession Object when url is legal.
     */
    public synchronized SonicSession createSession(@NonNull String url, @NonNull SonicSessionConfig sessionConfig) {
        String sessionId = makeSessionId(url, sessionConfig.IS_ACCOUNT_RELATED);
        if (!TextUtils.isEmpty(sessionId)) {
            SonicSession sonicSession = lookupSession(sessionConfig, sessionId, true);
            if (null != sonicSession) {
                sonicSession.setIsPreload(url);
            } else if (isSessionAvailable(sessionId)) { // 缓存中未存在
                sonicSession = internalCreateSession(sessionId, url, sessionConfig);
            }
            return sonicSession;
        }
        return null;
    }


    /**
     *
     * @param sessionId possible sessionId
     * @param pick      When {@code pick} is true and there is SonicSession in {@link #preloadSessionPool},
     *                  it will remove from {@link #preloadSessionPool}
     * @return
     *          Return valid SonicSession Object from {@link #preloadSessionPool} if the specified sessionId is a key in {@link #preloadSessionPool}.
     */
    private SonicSession lookupSession(SonicSessionConfig config, String sessionId, boolean pick) {
        if (!TextUtils.isEmpty(sessionId) && config != null) {
            SonicSession sonicSession = preloadSessionPool.get(sessionId);
            if (sonicSession != null) {
                //判断session缓存是否过期,以及sessionConfig是否发生变化
                if (!config.equals(sonicSession.config) ||
                        sonicSession.config.PRELOAD_SESSION_EXPIRED_TIME > 0 && System.currentTimeMillis() - sonicSession.createdTime > sonicSession.config.PRELOAD_SESSION_EXPIRED_TIME) {
                    if (runtime.shouldLog(Log.ERROR)) {
                        runtime.log(TAG, Log.ERROR, "lookupSession error:sessionId(" + sessionId + ") is expired.");
                    }
                    preloadSessionPool.remove(sessionId);
                    sonicSession.destroy();
                    return null;
                }

                if (pick) {
                    preloadSessionPool.remove(sessionId);
                }
            }
            return sonicSession;
        }
        return null;
    }

    /**
     *
     * @param sessionId
     * @return Return new SonicSession if there was no mapping for the sessionId in {@link #runningSessionHashMap}
     */
    private SonicSession internalCreateSession(String sessionId, String url, SonicSessionConfig sessionConfig) {
        if (!runningSessionHashMap.containsKey(sessionId)) {
            SonicSession sonicSession;
            if (sessionConfig.sessionMode == SonicConstants.SESSION_MODE_QUICK) {
                sonicSession = new QuickSonicSession(sessionId, url, sessionConfig);
            } else {
                sonicSession = new StandardSonicSession(sessionId, url, sessionConfig);
            }
            sonicSession.addCallback(sessionCallback);

            if (sessionConfig.AUTO_START_WHEN_CREATE) {
                sonicSession.start();
            }
            return sonicSession;
        }
        if (runtime.shouldLog(Log.ERROR)) {
            runtime.log(TAG, Log.ERROR, "internalCreateSession error:sessionId(" + sessionId + ") is running now.");
        }
        return null;
    }

    /**
     * If the server fails or specifies HTTP pattern, SonicSession won't use Sonic pattern Within {@link com.tencent.sonic.sdk.SonicConfig#SONIC_UNAVAILABLE_TIME} ms
     * @param sessionId
     * @return Test if the sessionId is available.
     */
    private boolean isSessionAvailable(String sessionId) {
        long unavailableTime = SonicDataHelper.getLastSonicUnavailableTime(sessionId);
        if (System.currentTimeMillis() > unavailableTime) {
            return true;
        }
        if (runtime.shouldLog(Log.ERROR)) {
            runtime.log(TAG, Log.ERROR, "sessionId(" + sessionId + ") is unavailable and unavailable time until " + unavailableTime + ".");
        }
        return false;
    }

    /**
     * Removes all of the cache from {@link #preloadSessionPool} and deletes file caches from SDCard.
     *
     * @return
     *      Returns {@code false} if {@link #runningSessionHashMap} is not empty.
     *      Returns {@code true} if all of the local file cache has been deleted, <code>false</code> otherwise
     */
    public synchronized boolean cleanCache() {
        if (!preloadSessionPool.isEmpty()) {
            runtime.log(TAG, Log.INFO, "cleanCache: remove all preload sessions, size=" + preloadSessionPool.size() + ".");
            Collection<SonicSession> sonicSessions = preloadSessionPool.values();
            for (SonicSession session : sonicSessions) {
                session.destroy();
            }
            preloadSessionPool.clear();
        }

        if (!runningSessionHashMap.isEmpty()) {
            runtime.log(TAG, Log.ERROR, "cleanCache fail, running session map's size is " + runningSessionHashMap.size() + ".");
            return false;
        }

        runtime.log(TAG, Log.INFO, "cleanCache: remove all sessions cache.");

        return SonicUtils.removeAllSessionCache();
    }

    /**
     * Removes the sessionId and its corresponding SonicSession from {@link #preloadSessionPool}.
     *
     * @param sessionId A unique session id
     * @return Return {@code true} If there is no specified sessionId in {@link #runningSessionHashMap}, <code>false</code> otherwise.
     */
    public synchronized boolean removeSessionCache(@NonNull String sessionId) {
        SonicSession sonicSession = preloadSessionPool.get(sessionId);
        if (null != sonicSession) {
            sonicSession.destroy();
            preloadSessionPool.remove(sessionId);
            runtime.log(TAG, Log.INFO, "sessionId(" + sessionId + ") removeSessionCache: remove preload session.");
        }

        if (!runningSessionHashMap.containsKey(sessionId)) {
            runtime.log(TAG, Log.INFO, "sessionId(" + sessionId + ") removeSessionCache success.");
            SonicUtils.removeSessionCache(sessionId);
            return true;
        }
        runtime.log(TAG, Log.ERROR, "sessionId(" + sessionId + ") removeSessionCache fail: session is running.");
        return false;
    }

    /**
     * <p>A callback receives notifications from a SonicSession.
     * Notifications indicate session related events, such as the running or the
     * destroy of the SonicSession.
     * It is intended to handle cache of SonicSession correctly to avoid concurrent modification.
     * </p>
     *
     */
    private final SonicSession.Callback sessionCallback = new SonicSession.Callback() {
        @Override
        public void onSessionStateChange(SonicSession session, int oldState, int newState, Bundle extraData) {
            SonicUtils.log(TAG, Log.DEBUG, "onSessionStateChange:session(" + session.sId + ") from state " + oldState + " -> " + newState);
            switch (newState) {
                case SonicSession.STATE_RUNNING:
                    runningSessionHashMap.put(session.id, session);
                    break;
                case SonicSession.STATE_DESTROY:
                    runningSessionHashMap.remove(session.id);
                    break;
            }
        }
    };

}
