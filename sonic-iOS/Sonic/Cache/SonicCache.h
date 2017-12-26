//
//  SonicCache.h
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
#import "SonicCacheItem.h"
#import "SonicConstants.h"

/**
 * Manage the memory caches and file cache for all sonic sessions.
 */
@interface SonicCache : NSObject

/**
 * Return an cache manage instance,it can create or update memory cache and file.
 * cache for each sonic session.
 */
+ (SonicCache *)shareCache;

/**
 * Success for YES, faild for NO.
 */
- (BOOL)setupCacheDirectory;

/**
 * Clear memory and file cache.
 */
- (void)clearAllCache;

/**
 * Delete memory and file cache.
 
 * @param sessionID  an session id to find the memory cache item.
 */
- (void)removeCacheBySessionID:(NSString *)sessionID;

/**
 * Check if sever disable sonic request.
 
 * @param sessionID  an session id to find saved config params.
 * @result Return if need disable sonic mode for web request.
 */
- (BOOL)isServerDisableSonic:(NSString *)sessionID;

/**
 * Set a timestamp to record server disable sonic request start.
 * @param sessionID  an session id to find saved config params.
 */
- (void)saveServerDisableSonicTimeNow:(NSString *)sessionID;

/**
 * Remove the recorded timestamp to enable sonic request.
 
 * @param sessionID  an session id to find saved config params.
 */
- (void)removeServerDisableSonic:(NSString *)sessionID;

/**
 * Return the dynamic data which split from HTML.
 
 * @param sessionID  an session id to find memory cache item.
 * @result return the dynamic data from memory cache item,
 * if the memory cache item doesn't exist, the create and read data,
 * from file cache data.
 
 */
- (NSDictionary *)dynamicDataBySessionID:(NSString *)sessionID;

/**
 * Return the template string which split from HTML.
 
 * @param sessionID  an session id to find memory cache item.
 * @result return template string from memory cache item,
 * if the memory cache item doesn't exist, the create and read data,
 * from file cache data.
 
 */
- (NSString *)templateStringBySessionID:(NSString *)sessionID;

/**
 * Check if there is memory cache or file cache.
 
 * @param sessionID an session id to find memory cache item.
 * @result If there is not exist an memory cache item and file cache data not exist
 * either, then return NO.
 */
- (BOOL)isFirstLoad:(NSString *)sessionID;

/**
 * @brief Generate the new HTML cache with server upate "jsonData".
 
 * @param jsonData The server update data.
 * @param headers the response headers from sonic session.
 * @param url the sonic URL which use to create sessionID for cache item name.
 * @result An cache item which has finish update dynamic data and HTML document cache.
 */
- (SonicCacheItem *)updateWithJsonData:(NSData *)jsonData
                        withHtmlString:(NSString *)htmlString
                   withResponseHeaders:(NSDictionary *)headers
                               withUrl:(NSString *)url;

/**
 * Save all relation datas
 */
- (SonicCacheItem *)saveHtmlString:(NSString *)htmlString
                    templateString:(NSString *)templateString
                       dynamicData:(NSDictionary *)dataDict
                   responseHeaders:(NSDictionary *)headers
                           withUrl:(NSString *)url;

/**
 * Update cache expire time from response headers with session id
 */
- (void)updateCacheExpireTimeWithResponseHeaders:(NSDictionary *)headers withSessionID:(NSString *)sessionID;

/**
 * Save the response headers
 */
- (void)saveResponseHeaders:(NSDictionary *)headers withSessionID:(NSString *)sessionID;

/**
 * @brief Get the memory cache item by sessionID.
 * If there no memory cache exist,then create an new item
           If there has file cache exist,then read require data into memory.
 
 * @param sessionID  an session id to find memory cache item.
 * @result If cache is not exist then return nil.
 */
- (SonicCacheItem *)cacheForSession:(NSString *)sessionID;

/**
 * Get the file cache update timestamp.
 */
- (NSString *)localRefreshTimeBySessionID:(NSString *)sessionID;

#pragma mark - Sub resource load

/**
 * Save resource data and configuration information by Resource ID.
 */
- (BOOL)saveSubResourceData:(NSData *)data withConfig:(NSDictionary *)config withResponseHeaders:(NSDictionary *)responseHeader withSessionID:(NSString *)sessionID;

/**
 * Returns the binary data of the resource.
 */
- (NSData *)resourceCacheWithSessionID:(NSString *)sessionID;

/**
 * Return the resource's response header field information.
 */
- (NSDictionary *)responseHeadersWithSessionID:(NSString *)sessionID;

/**
 * Return resource configuration information.
 */
- (NSDictionary *)resourceConfigWithSessionID:(NSString *)sessionID;

/**
 * Clear the cache with the resource ID.
 */
- (BOOL)clearResourceWithSessionID:(NSString *)sessionID;

/**
 * Resource save file operation queue.
 */
+ (NSOperationQueue *)subResourceQueue;

/**
 * Check file cache size, we keep max cache size 30MB
 */
- (void)checkAndTrimCache;

/**
 * Check and update version
 */
- (BOOL)upgradeSonicVersion;

@end
