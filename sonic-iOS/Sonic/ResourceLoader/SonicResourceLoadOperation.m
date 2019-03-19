//
//  SonicResourceLoadOperation.m
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

#import "SonicResourceLoadOperation.h"
#import "SonicServer.h"
#import "SonicResourceLoader.h"
#import "SonicCache.h"
#import "SonicUtil.h"
#import "SonicConfiguration.h"
#import "SonicEventStatistics.h"

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

@interface SonicResourceLoadOperation()<SonicConnectionDelegate>

@property (nonatomic,retain)SonicConnection *connection;

@property (nonatomic,retain)NSData *cacheFileData;

@property (nonatomic,retain)NSDictionary *cacheResponseHeaders;

@property (nonatomic,retain)NSDictionary *config;

@property (nonatomic,retain)NSMutableData *responseData;

@property (nonatomic,retain)NSHTTPURLResponse *originResponse;

@property (nonatomic,assign)BOOL isComplete;

@property (nonatomic,retain)NSRecursiveLock *lock;


@end

@implementation SonicResourceLoadOperation

- (instancetype)initWithUrl:(NSString *)aUrl
{
    if (self = [super init]) {
        _url = [aUrl copy];
        self.responseData = [NSMutableData data];
        _sessionID = [resourceSessionID(_url) copy];
        NSRecursiveLock *tmpLock = [NSRecursiveLock new];
        self.lock = tmpLock;
        [tmpLock release];
    
        self.config = [[SonicCache shareCache] resourceConfigWithSessionID:self.sessionID];
        if (self.config) {
            long long cacheExpire = [self.config[@"cache-expire-time"] longLongValue];
            BOOL isCacheExpire = NO;
            if (cacheExpire > 0) {
                long long now = (long long)[[NSDate date] timeIntervalSince1970];
                isCacheExpire = cacheExpire <= now;
            }
            if (isCacheExpire) {
                self.config = nil;
                SonicLogEvent(@"resource expire:%@",self.url);
            }else{
                self.cacheFileData = [[SonicCache shareCache] resourceCacheWithSessionID:self.sessionID];
                NSString *cacheFileSha1 = getDataSha1(self.cacheFileData);
                NSString *sha1 = self.config[@"sha1"];
                if ([cacheFileSha1 isEqualToString:sha1]) {
                    self.cacheResponseHeaders = [[SonicCache shareCache] responseHeadersWithSessionID:self.sessionID];
                    //event
                    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SubResourceLoadLocalCache withEventInfo:@{
                                                                                                                                    @"url":self.url,
                                                                                                                        @"dataLength":@(self.cacheFileData.length)
                                                                                                                                    }];
                }else{
                    self.cacheFileData = nil;
                    self.config = nil;
                    SonicLogEvent(@"resource sha1 wrong:%@",self.url);
                }
            }
        }
    }
    return self;
}

- (void)dealloc
{
    [self cancel];
    self.lock = nil;
    [_url release];
    _url = nil;
    [_sessionID release];
    _sessionID = nil;
    self.responseData = nil;
    self.cacheFileData = nil;
    self.originResponse = nil;
    self.protocolCallBack = nil;
    self.cacheResponseHeaders = nil;
    [super dealloc];
}

- (void)preloadDataWithProtocolCallBack:(SonicURLProtocolCallBack)callBack
{
    if (!callBack) {
        return;
    }
    NSDictionary *rspItem = nil;
    NSDictionary *dataItem = nil;
    NSDictionary *finishItem = nil;
    NSMutableArray *actions = [NSMutableArray array];
    if (self.cacheFileData && self.cacheFileData.length > 0 && self.cacheResponseHeaders.count > 0) {
        NSHTTPURLResponse *resp = [[[NSHTTPURLResponse alloc]initWithURL:[NSURL URLWithString:self.url] statusCode:200 HTTPVersion:@"HTTP/1.1" headerFields:self.cacheResponseHeaders]autorelease];
        rspItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:resp];
        [self.lock lock];
        dataItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:self.cacheFileData];
        [self.lock unlock];
        finishItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
        SonicLogEvent(@"resource read from cache:%@",self.url);
        [actions addObjectsFromArray:@[rspItem,dataItem,finishItem]];
    }else{
        if (self.isComplete) {
            [self.lock lock];
            rspItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:self.originResponse];
            dataItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:self.responseData];
            [self.lock unlock];
            finishItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
            SonicLogEvent(@"resource read from network:%@",self.url);
            [actions addObjectsFromArray:@[rspItem,dataItem,finishItem]];
        }else{
            [self.lock lock];
            if (self.originResponse) {
                rspItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:self.originResponse];
                [actions addObject:rspItem];
            }
            if (self.responseData.length> 0) {
                dataItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:self.responseData];
                [actions addObject:dataItem];
            }
            [self.lock unlock];
            SonicLogEvent(@"resource read from network preload:%@",self.url);
        }
    }
    self.protocolCallBack = callBack;
    for (NSInteger index = 0; index < actions.count; index ++){
        NSDictionary *item = actions[index];
        self.protocolCallBack(item);
    }
}



- (void)cancel
{
    self.lock = nil;
    if (self.connection) {
        self.connection.delegate = nil;
        [self.connection stopLoading];
        self.connection = nil;
    }
    [super cancel];
}

- (void)main
{
    if (!self.cacheFileData || self.cacheFileData.length == 0) {
        [self startRequest];
    }
}

- (void)startRequest
{
    if (_url.length == 0) {
        return;
    }
    
    //build request
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc]initWithURL:[NSURL URLWithString:self.url]];
    
    //sync cookie
    dispatch_async(dispatch_get_main_queue(), ^{
        NSURL *cUrl = [NSURL URLWithString:self.url];
        NSHTTPCookieStorage *sharedHTTPCookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
        NSArray *cookies = [sharedHTTPCookieStorage cookiesForURL:cUrl];
        NSDictionary *cookieHeader = [NSHTTPCookie requestHeaderFieldsWithCookies:cookies];
        if (nil != cookies) {
            NSMutableDictionary *requestHeader = [NSMutableDictionary dictionaryWithDictionary:request.allHTTPHeaderFields];
            [requestHeader addEntriesFromDictionary:cookieHeader];
            request.allHTTPHeaderFields = requestHeader;
        }
    });
    
    //Allow custom class to intercept the request
    Class connectionClass = [SonicServer connectionClassForRequest:request];
    SonicConnection *tmpConnection = [[connectionClass alloc] initWithRequest:request delegate:self delegateQueue:[NSOperationQueue currentQueue]];
    self.connection = tmpConnection;
    [tmpConnection release];
    self.connection.supportHTTPRedirection = YES;
    [self.connection startLoading];
    [request release];
}

- (void)appendData:(NSData *)data
{
    [self.lock lock];
    [self.responseData appendData:data];
    [self.lock unlock];
}

#pragma mark - connection delegate

- (void)connection:(SonicConnection *)connection didReceiveResponse:(NSHTTPURLResponse *)response
{
    [self.lock lock];
    self.originResponse = response;
    [self.lock unlock];
    [self disaptchProtocolAction:SonicURLProtocolActionRecvResponse withParam:response];
}

- (void)connection:(SonicConnection *)connection didReceiveData:(NSData *)data
{
    [self disaptchProtocolAction:SonicURLProtocolActionLoadData withParam:data];
    [self appendData:data];
}

- (void)connection:(SonicConnection *)connection didCompleteWithError:(NSError *)error
{
    SonicLogEvent(@"resource recieve error:%@",error.debugDescription);

    [self disaptchProtocolAction:SonicURLProtocolActionDidFaild withParam:error];
    self.isComplete = YES;
    
    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SubResourceDidFail withEventInfo:@{
                                                                                                             @"msg":@"config create fail to save resource!",
                                                                                                             @"url":self.url,
                                                                                                             @"error":error.debugDescription
                                                                                                             }];
}

- (void)connectionDidCompleteWithoutError:(SonicConnection *)connection
{
    [self disaptchProtocolAction:SonicURLProtocolActionDidSuccess withParam:nil];
    
    //save resource data
    NSBlockOperation *block = [NSBlockOperation blockOperationWithBlock:^{
        NSDictionary *config = [self createConfig];
        if (config) {
           [[SonicCache shareCache] saveSubResourceData:self.responseData withConfig:config withResponseHeaders:self.originResponse.allHeaderFields withSessionID:self.sessionID];
            
            NSDictionary *headersLog = self.originResponse.allHeaderFields? self.originResponse.allHeaderFields:[NSDictionary dictionary];
            [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SubResourceDidSave withEventInfo:@{
                                                                                                                     @"url":self.url,
                                                                                                                     @"resourceID":self.sessionID,
                                                                                                                     @"dataLength":@(self.responseData.length),
                                                                                                                     @"reseponse":headersLog
                                                                                                                     }];
        }else{
            SonicLogEvent(@"config create fail to save resource:%@",self.url);
        }
    }];
    [[SonicCache subResourceQueue] addOperation:block];
    
    self.isComplete = YES;
}

- (void)disaptchProtocolAction:(SonicURLProtocolAction)action withParam:(NSObject *)param
{
    if (self.protocolCallBack) {
        NSDictionary *item = [SonicUtil protocolActionItem:action param:param];
        self.protocolCallBack(item);
    }
}

- (NSDictionary *)createConfig
{
    NSDictionary *query = queryComponents(self.url);
    NSString *maxAge = nil;
    for (NSString *key in query.allKeys) {
        NSString *lowcase = [key lowercaseString];
        if ([lowcase isEqualToString:@"max-age"]) {
            maxAge = query[key];
            break;
        }
    }
    //default resource expire time
    unsigned long long maxAgeTime = maxAge.length > 0? [maxAge longLongValue]:0;
    unsigned long long cacheExpireTime = maxAgeTime == 0? 0:currentTimeStamp() + maxAgeTime;
    NSString *tmpSha1 = getDataSha1(self.responseData);
    if (tmpSha1.length == 0) {
        return nil;
    }
    return @{@"sha1":tmpSha1,@"cache-expire-time":[@(cacheExpireTime) stringValue]};
}

@end
