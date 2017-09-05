//
//  SonicClient.m
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
//  Copyright © 2017 Tencent. All rights reserved.
//

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

#import "SonicClient.h"
#import "SonicCache.h"
#import "SonicUitil.h"

NS_ASSUME_NONNULL_BEGIN
@interface SonicClient ()

@property (nonatomic,retain)NSRecursiveLock *lock;
@property (nonatomic,retain)NSMutableDictionary *tasks;
@property (nonatomic,retain)NSMutableDictionary *ipDomains;
@property (nonatomic,copy)NSString *userAgent;

@end

@implementation SonicClient

+ (SonicClient *)sharedClient
{
    static SonicClient *_client = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _client = [[self alloc]init];
    });
    return _client;
}

- (instancetype)init
{
    if (self = [super init]) {
        [self setupClient];
    }
    return self;
}

- (void)setGlobalUserAgent:(NSString *)aUserAgent
{
    if (aUserAgent.length == 0) {
        return;
    }
    if (_userAgent) {
        [_userAgent release];
        _userAgent = nil;
    }
    _userAgent = [aUserAgent copy];
}

- (void)setCurrentUserUniqIdentifier:(NSString *)userIdentifier
{
    if (userIdentifier.length == 0 || [_currentUserUniq isEqualToString:userIdentifier]) {
        return;
    }
    if (_currentUserUniq) {
        [_currentUserUniq release];
        _currentUserUniq = nil;
    }
    _currentUserUniq = [userIdentifier copy];
    
    //create root cache path by user id
    [[SonicCache shareCache] setupCacheDirectory];
}

- (NSString *)sonicDefaultUserAgent
{
    return SonicDefaultUserAgent;
}

- (void)addDomain:(NSString *)domain withIpAddress:(NSString *)ipAddress
{
    if (domain.length == 0 || ipAddress.length == 0) {
        return;
    }
    [self.ipDomains setObject:ipAddress forKey:domain];
}

- (void)setupClient
{
    self.lock = [NSRecursiveLock new];
    self.tasks = [NSMutableDictionary dictionary];
    self.ipDomains = [NSMutableDictionary dictionary];
}

- (void)clearAllCache
{
    [[SonicCache shareCache] clearAllCache];
}

- (BOOL)isFirstLoad:(NSString *)url
{
    return [[SonicCache shareCache] isFirstLoad:sonicSessionID(url)];
}

- (NSString *)localRefreshTimeByUrl:(NSString *)url
{
    return [[SonicCache shareCache] localRefreshTimeBySessionID:sonicSessionID(url)];
}

- (void)removeCacheByUrl:(NSString *)url
{
    SonicSession *existSession = [self sessionById:sonicSessionID(url)];
    if (!existSession) {
        [[SonicCache shareCache] removeCacheBySessionID:sonicSessionID(url)];
    }
}

- (void)registProtocolCallBack:(SonicURLProtocolCallBack)callBack withSessionID:(NSString *)sessionID
{
    SonicSession *existSession = [self sessionById:sessionID];
    if (existSession) {
        dispatchToSonicSessionQueue(^{
            existSession.protocolCallBack = callBack;
        });
    }
}

- (void)registerURLProtocolCallBackWithSessionID:(NSString *)sessionID completion:(SonicURLProtocolCallBack)protocolCallBack
{
    dispatchToMain(^{
        SonicSession *session = [self sessionById:sessionID];
        if (session) {
            [session preloadRequestActionsWithProtocolCallBack:protocolCallBack];
        }
    });
}

- (void)sonicUpdateDiffDataByWebDelegate:(id<SonicSessionDelegate>)aWebDelegate completion:(SonicWebviewCallBack)resultBlock
{
    if (!resultBlock) {
        return;
    }
    
    SonicSession *session = [self sessionWithWebDelegate:aWebDelegate];
    [session getResultWithCallBack:^(NSDictionary *result) {
        if (resultBlock) {
            resultBlock(result);
        }
    }];
}

#pragma mark - Safe Session Manager

static bool ValidateSessionDelegate(id<SonicSessionDelegate> aWebDelegate)
{
    return aWebDelegate && [aWebDelegate conformsToProtocol:@protocol(SonicSessionDelegate)];
}

- (void)createSessionWithUrl:(NSString *)url withWebDelegate:(nullable id<SonicSessionDelegate>)aWebDelegate
{
    if ([[SonicCache shareCache] isServerDisableSonic:sonicSessionID(url)]) {
        return;
    }
    
    [self.lock lock];
    SonicSession *existSession = self.tasks[sonicSessionID(url)];
    if (existSession && existSession.delegate != nil) {
        //session can only owned by one delegate
        [self.lock unlock];
        return;
    }
    
    if (!existSession) {
        
        existSession = [[SonicSession alloc] initWithUrl:url withWebDelegate:aWebDelegate];
        
        NSURL *cUrl = [NSURL URLWithString:url];
        existSession.serverIP = [self.ipDomains objectForKey:cUrl.host];
        
        __weak typeof(self) weakSelf = self;
        __weak typeof(existSession)weakSession = existSession;
        [existSession setCompletionCallback:^(NSString *sessionID){
            [weakSession cancel];
            [weakSelf.tasks removeObjectForKey:sessionID];
        }];
        
        [self.tasks setObject:existSession forKey:existSession.sessionID];
        [existSession start];
        [existSession release];

    }else{
        
        if (existSession.delegate == nil) {
            existSession.delegate = aWebDelegate;
        }
    }
    
    [self.lock unlock];
}


- (SonicSession *)sessionWithWebDelegate:(id<SonicSessionDelegate>)aWebDelegate
{
    if (!ValidateSessionDelegate(aWebDelegate)) {
        return nil;
    }
    
    SonicSession *findSession = nil;
    
    [self.lock lock];
    for (SonicSession *session in self.tasks.allValues) {
        if (session.delegate == aWebDelegate) {
            findSession = session;
            break;
        }
    }
    [self.lock unlock];
    
    return findSession;
}

- (SonicSession *)sessionById:(NSString *)sessionId
{
    SonicSession *session = nil;
    [self.lock lock];
    session = self.tasks[sessionId];
    [self.lock unlock];
    return session;
}

- (void)removeSessionWithWebDelegate:(id<SonicSessionDelegate>)aWebDelegate
{
    if (!ValidateSessionDelegate(aWebDelegate)) {
        return;
    }
    
    [self.lock lock];
    SonicSession *findSession = nil;
    for (SonicSession *session in self.tasks.allValues) {
        if (session.delegate == aWebDelegate) {
            findSession = session;
            break;
        }
    }
    
    if (findSession) {
        [findSession cancel];
        [self.tasks removeObjectForKey:findSession.sessionID];
    }
    [self.lock unlock];
    
}

@end
NS_ASSUME_NONNULL_END
