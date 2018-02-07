//
//  SonicEventStatistics.m
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

#import "SonicEventStatistics.h"

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

@interface SonicEventStatistics()

@property (nonatomic,retain)NSOperationQueue *statisticsQueue;

@property (nonatomic,retain)NSMutableArray *observers;

@end


@implementation SonicEventStatistics

- (instancetype)init
{
    if (self = [super init]) {
        self.statisticsQueue = [NSOperationQueue new];
        self.statisticsQueue.name = @"com.sonic.statistics";
        self.statisticsQueue.maxConcurrentOperationCount = 1;
        self.observers = [NSMutableArray array];
    }
    return self;
}

+ (SonicEventStatistics *)shareStatistics
{
    static SonicEventStatistics *_statistcs = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _statistcs = [[self alloc]init];
    });
    return _statistcs;
}

- (void)dealloc
{
    [self.statisticsQueue cancelAllOperations];
    self.statisticsQueue = nil;
    self.observers = nil;
    [super dealloc];
}

- (void)addLog:(NSString *)format, ...
{
    va_list args;
    va_start(args,format);
    NSString *logString = [[[NSString alloc]initWithFormat:format arguments:args]autorelease];
    va_end(args);
    logString = [NSString stringWithFormat:@"#SonicEventLog# %@",logString];
    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_EventLog withEventInfo:@{@"logMsg":logString}];
}

- (void)addEventObserver:(id<SonicEventStatisticsObserver>)eventObserver
{
    if (![eventObserver conformsToProtocol:@protocol(SonicEventStatisticsObserver)]) {
        return;
    }
    if ([self.observers containsObject:eventObserver]) {
        return;
    }
    [self.observers addObject:eventObserver];
}

- (void)removeEventObserver:(id<SonicEventStatisticsObserver>)eventObserver
{
    if (!eventObserver) {
        return;
    }
    [self.observers removeObject:eventObserver];
}

- (void)addEvent:(SonicStatisticsEvent)event withEventInfo:(NSDictionary *)info
{
    NSBlockOperation *blockOp = [NSBlockOperation blockOperationWithBlock:^{
        for (id<SonicEventStatisticsObserver> observer in self.observers) {
            [observer handleEvent:event withEventInfo:info];
        }
    }];
    [self.statisticsQueue addOperation:blockOp];
}

@end
