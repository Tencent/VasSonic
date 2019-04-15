//
//  SonicConstants.h
//  sonic
//
//  Tencent is pleased to support the open source community by making VasSonic available.
//  Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//  Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
//  in compliance with the License. You may obtain a copy of the License at
//
//  https://opensource.org/licenses/BSD-3-Clause
//
//  Unless required by applicable law or agreed to in writing, software distributed under the
//  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//  either express or implied. See the License for the specific language governing permissions
//  and limitations under the License.
//
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>

#define SonicDefaultUserAgent @"Mozilla/5.0 (iPhone; U; CPU iPhone OS 2_2 like Mac OS X;\
en-us) AppleWebKit/525.181 (KHTML, like Gecko) Version/3.1.1 Mobile/5H11 Safari/525.20"

/**
* Sonic status code.
*/
typedef NS_ENUM(NSUInteger, SonicStatusCode) {
    
    /**
     * No dynamic data will be updated.
     */
    SonicStatusCodeAllCached = 304,
    
    /**
     * The template need to update.
     */
    SonicStatusCodeTemplateUpdate = 2000,
    
    /**
     * There is no local cache, need to request all data from server.
     */
    SonicStatusCodeFirstLoad = 1000,
    
    /**
     * Only need to request dynamic data.
     */
    SonicStatusCodeDataUpdate = 200,
    
};

/** NSURLProtocol client action */
typedef NS_ENUM(NSUInteger, SonicURLProtocolAction) {
    
    SonicURLProtocolActionLoadData,
    
    SonicURLProtocolActionRecvResponse,
    
    SonicURLProtocolActionDidSuccess,
    
    SonicURLProtocolActionDidFaild,
};

/* Sonic error code */
typedef NS_ENUM(NSInteger, SonicErrorType) {
    
    /* File i/o faild */
    SonicErrorType_IOE = -901,
    
    /* Time out */
    SonicErrorType_TOE = -902,
    
    /* HTML file verify faild */
    SonicErrorType_HTML_VERIFY_FAIL = -1001,
    
    /* Setup up the cache directory faild */
    SonicErrorType_MAKE_DIR_ERROR = -1003,
    
    /* Save file faild */
    SonicErrorType_WRITE_FILE_FAIL = -1004,
    
    /* Split the HTML to template and dynamic data faild */
    SonicErrorType_SPLIT_HTML_FAIL = -1005,
    
    /* Merge the server data and local dynamic data faild */
    SonicErrorType_MERGE_DIFF_DATA_FAIL = -1006,
    
    /* Server response data can not verify */
    SonicErrorType_SERVER_DATA_EXCEPTION = -1007,
    
    /* Merge the template and dynamic data to build HTML faild */
    SonicErrorType_BUILD_HTML_ERROR = -1008,
    
    /* NSURLProtocol recieve action faild */
    SonicErrorType_URLPROTOCOL_ERROR = -1009,
    
};

/**
 * Identify the source where started this request: webview or SonicSession.
 */
#define SonicHeaderKeyLoadType        @"sonic-load-type"

/**
 * The request is started by webview.
 */
#define SonicHeaderValueWebviewLoad   @"__SONIC_HEADER_VALUE_WEBVIEW_LOAD__"

/**
 * The hash of Delegate which uses to identify wether it's owner of a SonicSession.
 */
#define SonicHeaderKeyDelegateId    @"sonic-delegate-id"

/**
 * The request is started by SonicSession.
 */
#define SonicHeaderValueSonicLoad @"__SONIC_HEADER_VALUE_SONIC_LOAD__"

/**
 * Pass session id to SonicURLProtocol through this header field.
 */
#define SonicHeaderKeySessionID  @"sonic-sessionId"

/**
 * Pass sonic sdk version through this field.
 */
#define SonicHeaderKeySDKVersion @"sonic-sdk-version"

/**
 * Current sonic version: Sonic/2.0.0
 */
#define SonicHeaderValueSDKVersion @"Sonic/2.0.0"

/**
 * Pass template tag through this field.
 */
#define SonicHeaderKeyTemplate @"template-tag"

#define SonicHeaderKeyHtmlSha1 @"html-sha1"

/**
 * Pass true/false to decide if template-change
 */
#define SonicHeaderKeyTemplateChange @"template-change"

#define SonicHeaderKeyLink @"sonic-link"

/**
 * Pass Etag through this field.
 */
#define SonicHeaderKeyETag     @"etag"

/**
 * Pass Etag through this field.
 * This header represents that the "eTag" key can be modified by service.
 */
#define SonicHeaderKeyCustomeETag     @"sonic-etag-key"

/**
 * Content-Security-Policy key for header.
 */
#define SonicHeaderKeyCSPHeader       @"content-security-policy"

/**
 * Pass cache policy through this field: SonicHeaderValueCacheOfflineStore, SonicHeaderValueCacheOfflineStoreRefresh, SonicHeaderValueCacheOfflineRefresh, SonicHeaderValueCacheOfflineDisable.
 */
#define SonicHeaderKeyCacheOffline       @"cache-offline"

/**
 * Http header cache control policy
 */
#define SonicHeaderValueCacheControl  @"cache-control"

/**
 * Store the new data and don't refresh web content.
 */
#define SonicHeaderValueCacheOfflineStore  @"store"

/**
 * Store new data and refresh web content.
 */
#define SonicHeaderValueCacheOfflineStoreRefresh   @"true"

/**
 * Don't store the new data , only use the new data to refresh web content.
 */
#define SonicHeaderValueCacheOfflineRefresh  @"false"

/**
 * Sonic is diabled, don't use sonic in the following 6 hours.
 */
#define SonicHeaderValueCacheOfflineDisable   @"http"


/**
 * Content-Security-Policy key for cache.
 */
#define kSonicCSP             @"csp"

/**
 * The last time to refresh the cache.
 */
#define kSonicLocalRefreshTime  @"local_refresh"

/**
 * The timestamp when the local cache expire
 */
#define kSonicLocalCacheExpireTime @"cache-expire-time"

/**
 * The http response header key Max-Age
 */
#define SonicHeaderMaxAge @"max-age"

/**
 * The http response header key Expires
 */
#define SonicHeaderExpire @"expires"

/**
 * Html-SHA1
 */
#define kSonicSha1          @"sha1"

/**
 * Key for SonicURLProtocolCallBack's dictionary: action type.
 */
#define kSonicProtocolAction            @"protocol-action"

/**
 * Key for SonicURLProtocolCallBack's dictionary: data.
 */
#define kSonicProtocolData              @"protocol-data"

/**
 * The file name to record Sonic disable list for each URL.
 */
#define SonicCacheOfflineDisableList       @"cache-offline-disable.cfg"

#define SonicCacheDatabase @"sonic.db"

/**
 * Quick way to get file manager.
 */
#define SonicFileManager    [NSFileManager defaultManager]

/**
 * Sonic item field name
 */
#define kSonicHtmlFieldName                 @"html"
#define kSonicTemplateFieldName             @"template"
#define kSonicDataFieldName                 @"data"
#define kSonicDiffFieldName                 @"diff"

/**
 * HTTP Header:If-None-Match
 */
#define HTTPHeaderKeyIfNoneMatch @"If-None-Match"

/**
 * HTTP Header:User-Agent
 */
#define HTTPHeaderKeyUserAgent @"User-Agent"

/**
 * HTTP Header:Host
 */
#define HTTPHeaderKeyHost @"Host"

/**
 * HTTP Header:Content-Type
 */
#define HTTPHeaderKeyContentType @"Content-Type"

