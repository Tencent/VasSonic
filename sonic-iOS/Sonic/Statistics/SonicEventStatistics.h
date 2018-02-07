//
//  SonicEventStatistics.h
//  Sonic
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
#import "SonicEventConstants.h"

#define SonicLogEvent(format,...) [[SonicEventStatistics shareStatistics]addLog:format,##__VA_ARGS__]

@protocol SonicEventStatisticsObserver <NSObject>

- (void)handleEvent:(SonicStatisticsEvent)event withEventInfo:(NSDictionary *)info;

@end

@interface SonicEventStatistics : NSObject

+ (SonicEventStatistics *)shareStatistics;

- (void)addLog:(NSString *)format, ...;

/**
 * Add an observer to handle the events
 */
- (void)addEventObserver:(id<SonicEventStatisticsObserver>)eventObserver;

/**
 * remove the observer
 */
- (void)removeEventObserver:(id<SonicEventStatisticsObserver>)eventObserver;

/**
 * add an event with info
 */
- (void)addEvent:(SonicStatisticsEvent)event withEventInfo:(NSDictionary *)info;

@end
