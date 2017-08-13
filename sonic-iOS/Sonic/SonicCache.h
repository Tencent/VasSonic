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
 * @brief Split the HTML to dynamic data and template string.
 
 * @param html the HTML document downloaded by sonic session.
 */
- (NSDictionary *)splitTemplateAndDataFromHtmlData:(NSString *)html;

/**
 * @brief Merge the server data and local dynamic data to create new HTML and the difference data.
 * Step1. Get the difference between the "updateDict" and "existData",
           Step2. Update "existData" with the "updateDict",then merge with the "templateString",
           to build the new HTML cache.
 * @result Return the difference data and new HTML string.
 */
- (NSDictionary *)mergeDynamicData:(NSDictionary *)updateDict withOriginData:(NSMutableDictionary *)existData withTemplate:(NSString *)templateString;

/**
 * @brief First time to save htmlData
 
 * First time we need split the HTML in two parts: template and dynamic data
 * by the "<sonic-diff" tags.
 * @param htmlData the data download by sonic session,it is the whole HTML document.
 * @param headers the response headers from sonic session.
 * @param url the sonic URL which use to create sessionID for cache item name.
 * @result An cache item which has finish setup template string and dynamic data.
 */
- (SonicCacheItem *)saveFirstWithHtmlData:(NSData *)htmlData
            withResponseHeaders:(NSDictionary *)headers
                  withUrl:(NSString *)url;

/**
 * @brief Generate the new HTML cache with server upate "jsonData".
 
 * @param jsonData The server update data.
 * @param headers the response headers from sonic session.
 * @param url the sonic URL which use to create sessionID for cache item name.
 * @result An cache item which has finish update dynamic data and HTML document cache.
 */
- (SonicCacheItem *)updateWithJsonData:(NSData *)jsonData
                 withResponseHeaders:(NSDictionary *)headers
                       withUrl:(NSString *)url;

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

@end
