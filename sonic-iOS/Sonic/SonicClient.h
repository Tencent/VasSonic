//
//  SonicClient.h
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
#import "SonicSession.h"
#import "SonicConstants.h"

/**
 * Manage all sonic sessions.
 */
@interface SonicClient : NSObject

/* Return the unique identifier for current user */
@property (nonatomic,readonly)NSString *currentUserUniq;

/* Return the global custom User-Agent */
@property (nonatomic,readonly)NSString *userAgent;

/* Share the instance */
+ (SonicClient *)sharedClient;

/**
 * Set an unique identifier for the user.
 * We can use the identifier to create different cache dir for different users.
 
 * @param userIdentifier the unique identifier for the special user
 */
- (void)setCurrentUserUniqIdentifier:(NSString *)userIdentifier;

/**
 * @brief Clear all session memory and file caches.
 */
- (void)clearAllCache;

/**
 * Clear session memory and file caches with URL.
 * @param url
 */
- (void)removeCacheByUrl:(NSString *)url;

/**
 * Check if it is the first time to load this URL.
 */
- (BOOL)isFirstLoad:(NSString *)url;

/**
 * Use this API to add a domain-ip pair to connect server directly with ip in following requests.
 
 * @param domain host from url like www.qq.com
 * @param ipAddress e.g 8.8.8.8
 */
- (void)addDomain:(NSString *)domain withIpAddress:(NSString *)ipAddress;

/**
 * Return the default user-agent used by sonic.
 */
- (NSString *)sonicDefaultUserAgent;

/**
 * Set global custom User-Agent.
 
 * @param aUserAgent the custom User-Agent
 */
- (void)setGlobalUserAgent:(NSString *)aUserAgent;

/**
 * Get the last update timestamp of this URL.
 */
- (NSString *)localRefreshTimeByUrl:(NSString *)url;

/**
 * Create a sonic session with URL.
 * All the same URLs share one single session. Duplicate calls will not create duplicate sessions.
 */
- (void)createSessionWithUrl:(NSString *)url withWebDelegate:(id<SonicSessionDelegate>)aWebDelegate;

/**
 * Remove session by delegate, it will be little safer than removing by URL, cause URL is too easy to get.
 */
- (void)removeSessionWithWebDelegate:(id<SonicSessionDelegate>)aWebDelegate;

/**
 * Find the session with webDelegate.
 */
- (SonicSession *)sessionWithWebDelegate:(id<SonicSessionDelegate>)aWebDelegate;

/**
 * Get the patch between local data and server data.
 * Web page use this patch data to update itself.
 */
- (void)sonicUpdateDiffDataByWebDelegate:(id<SonicSessionDelegate>)aWebDelegate completion:(SonicWebviewCallBack)resultBlock;

/**
 * SonicURLProtocol will regist the callback to get network data from the sonic session
 */
- (void)registerURLProtocolCallBackWithSessionID:(NSString *)sessionID completion:(SonicURLProtocolCallBack)protocolCallBack;

@end
