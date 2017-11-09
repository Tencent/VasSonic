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

/**
 * Sonic constants
 */
public class SonicConstants {

    /**
     * SonicSDK log prefix
     */
    public final static String SONIC_SDK_LOG_PREFIX = "SonicSdk_";

    /**
     * SonicSDK version
     */
    public final static String SONIC_VERSION_NUM = "2.0.0";

    /**
     * Sonic parameter prefix
     */
    public final static String SONIC_PARAMETER_NAME_PREFIX = "sonic_";

    /**
     * This parameter in url will be as part of session id，and it is separated by SONIC_REMAIN_PARAMETER_SPLIT_CHAR.
     */
    public final static String SONIC_REMAIN_PARAMETER_NAMES= "sonic_remain_params";


    public final static String SONIC_REMAIN_PARAMETER_SPLIT_CHAR = ";";

    /**
     * SonicSession mode : StandardSonicSession
     */
    public static final int SESSION_MODE_DEFAULT = 0;

    /**
     * SonicSession mode : QuickSonicSession
     */
    public static final int SESSION_MODE_QUICK = 1;

    /**
     * Unknown
     */
    public static final int ERROR_CODE_UNKNOWN = -1;

    /**
     * Success
     */
    public static final int ERROR_CODE_SUCCESS = 0;

    /**
     * Http(s) connection error : IO Exception
     */
    public static final int ERROR_CODE_CONNECT_IOE = -901;

    /**
     * Http(s) connection error : time out
     */
    public static final int ERROR_CODE_CONNECT_TOE = -902;

    /**
     * Http(s) connection error : nullPointer in native
     */
    public static final int ERROR_CODE_CONNECT_NPE = -903;


    /**
     * Verify local file failed
     */
    public static final int ERROR_CODE_DATA_VERIFY_FAIL = -1001;

    /**
     * Failed to create sonic directory
     */
    public static final int ERROR_CODE_MAKE_DIR_ERROR = -1003;

    /**
     * File save failed
     */
    public static final int ERROR_CODE_WRITE_FILE_FAIL = -1004;

    /**
     * Separate html to template and data failed
     */
    public static final int ERROR_CODE_SPLIT_HTML_FAIL = -1005;

    /**
     * Obtain difference data between server and local data failed
     */
    public static final int ERROR_CODE_MERGE_DIFF_DATA_FAIL = -1006;

    /**
     * Server data exception
     */
    public static final int ERROR_CODE_SERVER_DATA_EXCEPTION = -1007;

    /**
     * Build template and data to html failed
     */
    public static final int ERROR_CODE_BUILD_HTML_ERROR = -1008;


}
