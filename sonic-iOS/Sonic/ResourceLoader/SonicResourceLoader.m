//
//  SonicResourceLoader.m
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

#import "SonicResourceLoader.h"
#import "SonicResourceLoadOperation.h"
#import "SonicUtil.h"
#import "SonicCache.h"

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

@interface SonicResourceLoader()

@property (nonatomic,retain)NSOperationQueue *operationQueue;

@property (nonatomic,retain)NSMutableArray *preloadLinks;

@property (nonatomic,retain)NSMutableArray *connections;

@end

@implementation SonicResourceLoader

- (instancetype)init
{
    if (self = [super init]) {
        self.preloadLinks = [NSMutableArray array];
        self.connections = [NSMutableArray array];
        NSOperationQueue *tmpQueue = [[NSOperationQueue alloc]init];
        self.operationQueue = tmpQueue;
        [tmpQueue release];
        self.operationQueue.maxConcurrentOperationCount = 3;
    }
    return self;
}

- (void)cancelAll
{
    for (NSOperation *operation in self.operationQueue.operations) {
        [operation cancel];
    }
    [self.connections removeAllObjects];
}

- (void)dealloc
{
    self.operationQueue = nil;
    self.preloadLinks = nil;
    self.connections = nil;
    [super dealloc];
}

- (void)loadResourceLinks:(NSArray *)links
{
    for (NSString *url in links) {
        [self loadResourceWithUrl:url];
    }
}

- (BOOL)canInterceptResourceWithUrl:(NSString *)url
{
   if (url.length == 0) {
        return NO;
   }
   return [_preloadLinks containsObject:url];
}

- (void)loadResourceWithUrl:(NSString *)url
{
    if (url.length == 0) {
        return;
    }
    if (![_preloadLinks containsObject:url]) {
        [_preloadLinks addObject:url];
    }else{
        return;
    }
    SonicResourceLoadOperation *operation = [[SonicResourceLoadOperation alloc] initWithUrl:url];
    [self.connections addObject:operation];
    [operation release];
    [self.operationQueue addOperation:operation];
}

- (void)preloadResourceWithUrl:(NSString *)url withProtocolCallBack:(SonicURLProtocolCallBack)callback
{
    SonicResourceLoadOperation *findOperation = nil;
    for (SonicResourceLoadOperation *item in self.connections) {
        if([item.url isEqualToString:url]){
            findOperation = item;
            break;
        }
    }
    if (!findOperation) {
        return;
    }
    [findOperation preloadDataWithProtocolCallBack:callback];
}

@end
