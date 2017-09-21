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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SonicSession ThreadPool
 */

class SonicSessionThreadPool {

    /**
     * Log filter
     */
    private final static String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicSessionThreadPool";

    /**
     * Singleton object
     */
    private final static SonicSessionThreadPool sInstance = new SonicSessionThreadPool();

    /**
     * ExecutorService object (Executors.newCachedThreadPool())
     */
    private final ExecutorService executorServiceImpl;

    /**
     * SonicSession ThreadFactory
     */
    private static class SessionThreadFactory implements ThreadFactory {

        /**
         * Thread group
         */
        private final ThreadGroup group;

        /**
         * Thread number
         */
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        /**
         * Thread prefix name
         */
        private final static String NAME_PREFIX = "pool-sonic-session-thread-";

        /**
         * Constructor
         */
        SessionThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        /**
         * Constructs a new {@code Thread}.  Implementations may also initialize
         * priority, name, daemon status, {@code ThreadGroup}, etc.
         *
         * @param r A runnable to be executed by new thread instance
         * @return Constructed thread, or {@code null} if the request to
         * create a thread is rejected
         */
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(this.group, r, NAME_PREFIX + this.threadNumber.getAndIncrement(), 0L);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }

            if (thread.getPriority() != 5) {
                thread.setPriority(5);
            }

            return thread;
        }
    }

    /**
     * Constructor and initialize thread pool object
     * default one core pool and the maximum number of threads is 6
     *
     */
    private SonicSessionThreadPool() {
        executorServiceImpl = new ThreadPoolExecutor(1, 6,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new SessionThreadFactory());
    }

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     *
     * @param task The runnable task
     * @return Submit success or not
     */
    private boolean execute(Runnable task) {
        try {
            executorServiceImpl.execute(task);
            return true;
        } catch (Throwable e) {
            SonicUtils.log(TAG, Log.ERROR, "execute task error:" + e.getMessage());
            return false;
        }
    }

    /**
     * Post an runnable to the pool thread
     *
     * @param task The runnable task
     * @return Submit success or not
     */

    static boolean postTask(Runnable task) {
        return sInstance.execute(task);
    }

}
