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

import android.content.Intent;

/**
 * <code>SonicSessionConnectionInterceptor</code> provide a <code>SonicSessionConnection</code>.
 * If an {@link SonicSessionConfig}does not set <code>SonicSessionConnectionInterceptor</code>
 * sonic will use {@link com.tencent.sonic.sdk.SonicSessionConnection.SessionConnectionDefaultImpl}
 * as default.
 *
 */
public abstract class SonicSessionConnectionInterceptor {

    public abstract SonicSessionConnection getConnection(SonicSession session, Intent intent);

    public static SonicSessionConnection getSonicSessionConnection(SonicSession session, Intent intent) {
        SonicSessionConnectionInterceptor interceptor = session.config.connectionInterceptor;
        if (interceptor != null) {
            return interceptor.getConnection(session, intent);
        }
        return new SonicSessionConnection.SessionConnectionDefaultImpl(session, intent);
    }
}
