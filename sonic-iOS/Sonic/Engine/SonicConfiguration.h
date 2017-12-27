//
//  SonicConfiguration.h
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

@interface SonicConfiguration : NSObject

/**
 * Sever default disable sonic time duration: 6 hours.
 */
@property (nonatomic,assign)unsigned long long cacheOfflineDisableTime;

/**
 * Root cache directory max size,default is 30MB
 */
@property (nonatomic,assign)unsigned long long cacheMaxDirectorySize;

/**
 * Clean up the cache if the current cache reaches the maximum cache of 0.8.
 */
@property (nonatomic,assign)float cacheDirectorySizeWarningPercent;

/**
 * Clean up the cache to the maximum cache of 0.2.
 */
@property (nonatomic,assign)float cacheDirectorySizeSafePercent;

/**
 * The memory cache maximum count.
 */
@property (nonatomic,assign)NSInteger maxMemroyCacheItemCount;

/**
 * The max cache time under strict-mode:false,default is 5 min.
 */
@property (nonatomic,assign)unsigned long long maxUnStrictModeCacheSeconds;

/**
 * The default time interval for clearing resource cache, default is 12 hours.
 */
@property (nonatomic,assign)unsigned long long resourceCacheSizeCheckDuration;

/**
 * The default time interval for clearing root cache, default is 12 hours.
 */
@property (nonatomic,assign)unsigned long long rootCacheSizeCheckDuration;

/**
 * Maximum resource cache directory size, default is 60MB.
 */
@property (nonatomic,assign)unsigned long long resourcCacheMaxDirectorySize;

/**
 * Return default configuration.
 */
+ (SonicConfiguration *)defaultConfiguration;

@end
