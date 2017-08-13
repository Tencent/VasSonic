//
//  SonicSession.m
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

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

#import "SonicSession.h"
#import <objc/runtime.h>
#import "SonicConnection.h"
#import "SonicCache.h"
#import "SonicUitil.h"
#import "SonicClient.h"

static NSMutableArray *sonicRequestClassArray = nil;
static NSLock *sonicRequestClassLock;

@interface SonicSession ()

@property (nonatomic,retain)NSData *cacheFileData;
@property (nonatomic,retain)NSDictionary  *cacheConfigHeaders;
@property (nonatomic,retain)NSDictionary  *cacheResponseHeaders;

@property (nonatomic,retain)NSHTTPURLResponse *response;
@property (nonatomic,retain)NSMutableData *responseData;
@property (nonatomic,retain)NSError *error;
@property (nonatomic,assign)BOOL isCompletion;
@property (nonatomic,assign)SonicStatusCode sonicStatusFinalCode;
@property (nonatomic,copy)  NSString *localRefreshTime;
@property (nonatomic,retain)SonicConnection *mCustomConnection;
@property (nonatomic,retain)NSMutableURLRequest *request;
@property (nonatomic,assign)BOOL didFinishCacheRead;

@end

@implementation SonicSession

#pragma mark - Custom Request

+ (BOOL)registerSonicConnection:(Class)connectionClass
{
    if (![connectionClass isSubclassOfClass:[SonicConnection class]]) {
        return NO;
    }
    
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        if (!sonicRequestClassArray) {
            sonicRequestClassArray = [[NSMutableArray alloc]init];
            sonicRequestClassLock = [NSLock new];
        }
    });
    
    [sonicRequestClassLock lock];
    [sonicRequestClassArray removeAllObjects];
    [sonicRequestClassArray addObject:connectionClass];
    [sonicRequestClassLock unlock];
    return YES;
}

+ (void)unregisterSonicConnection:(Class)connectionClass
{
    [sonicRequestClassLock lock];
    if ([sonicRequestClassArray containsObject:connectionClass]) {
        [sonicRequestClassArray removeObject:connectionClass];
    }
    [sonicRequestClassLock unlock];
}

+ (Class)lastCanUseRequestClass
{
    Class rClass = nil;
    [sonicRequestClassLock lock];
    rClass = [sonicRequestClassArray lastObject];
    [sonicRequestClassLock unlock];
    return rClass;
}

+ (NSOperationQueue *)sonicSessionQueue
{
    static NSOperationQueue *_sonicSessionQueue = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sonicSessionQueue = [[NSOperationQueue alloc]init];
        _sonicSessionQueue.name = @"SonicSessionQueue";
        _sonicSessionQueue.maxConcurrentOperationCount = 1;
        _sonicSessionQueue.qualityOfService = NSQualityOfServiceUserInitiated;
    });
    
    return _sonicSessionQueue;
}

#pragma mark - Lify Cycle

- (instancetype)init NS_UNAVAILABLE
{
    return nil;
}

- (instancetype)initWithUrl:(NSString *)aUrl withWebDelegate:(id<SonicSessionDelegate>)aWebDelegate
{
    if (self = [super init]) {
        
        self.delegate = aWebDelegate;
        self.url = aUrl;
        self.request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.url]];
        _sessionID = [sonicSessionID(aUrl) copy];

        [self setupData];
    }
    return self;
}

- (void)setupData
{
    SonicCacheItem *cacheItem = [[SonicCache shareCache] cacheForSession:_sessionID];
    self.isFirstLoad = cacheItem.hasLocalCache;
    
    if (!cacheItem.hasLocalCache) {
        self.cacheFileData = cacheItem.htmlData;
        self.cacheConfigHeaders = cacheItem.config;
        self.cacheResponseHeaders = cacheItem.cacheResponseHeaders;
        self.localRefreshTime = cacheItem.lastRefreshTime;
    }
    
    [self setupConfigRequestHeaders];
}

- (void)dealloc
{
    if (self.mCustomConnection) {
        self.mCustomConnection.session = nil;
        [self.mCustomConnection stopLoading];
        self.mCustomConnection = nil;
    }
    
    if (self.delegate) {
        self.delegate = nil;
    }
    
    if (self.completionCallback) {
        self.completionCallback = nil;
    }
    
    if (self.protocolCallBack) {
        self.protocolCallBack = nil;
    }
    
    [self cancel];
    
    self.request = nil;
    self.url = nil;
    [_sessionID release];
    _sessionID = nil;

    self.response = nil;
    self.responseData = nil;
    self.error = nil;

    [super dealloc];
}

#pragma mark - Open Interface

- (void)start
{
    dispatchToMain(^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(sessionWillRequest:)]) {
            [self.delegate sessionWillRequest:self];
        }
        [self syncCookies];
    });

    [self requestStartInOperation];
}

- (void)cancel
{
    if (self.mCustomConnection) {
        [self.mCustomConnection stopLoading];
    }
}

- (void)syncCookies
{
    NSURL *cUrl = [NSURL URLWithString:self.url];

    NSHTTPCookieStorage *sharedHTTPCookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    NSArray *cookies = [sharedHTTPCookieStorage cookiesForURL:cUrl];
    NSDictionary *cookieHeader = [NSHTTPCookie requestHeaderFieldsWithCookies:cookies];
    
    [self addCustomRequestHeaders:cookieHeader];
}

- (Class)canCustomRequest
{
    Class findDestClass = nil;
    
    for (NSInteger index = sonicRequestClassArray.count - 1; index >= 0; index--) {
        
        Class itemClass = sonicRequestClassArray[index];
        
        NSMethodSignature *sign = [itemClass methodSignatureForSelector:@selector(canInitWithRequest:)];
        NSInvocation *invoke = [NSInvocation invocationWithMethodSignature:sign];
        invoke.target = itemClass;
        NSURLRequest *argRequest = self.request;
        [invoke setArgument:&argRequest atIndex:2];
        invoke.selector = @selector(canInitWithRequest:);
        [invoke invoke];
        
        BOOL canCustomRequest;
        [invoke getReturnValue:&canCustomRequest];
        
        if (canCustomRequest) {
            findDestClass = itemClass;
            break;
        }
    }
    
    return findDestClass;
}

- (void)requestStartInOperation
{
    Class customRequest = [self canCustomRequest];
    if (!customRequest) {
        //If there no custom request ,then use the default
        customRequest = [SonicConnection class];
    }
    
    SonicConnection *cRequest = [[customRequest alloc]initWithRequest:self.request];
    self.mCustomConnection = cRequest;
    [cRequest release];
    self.mCustomConnection.session = self;
    [self.mCustomConnection startLoading];
}

- (void)addCustomRequestHeaders:(NSDictionary *)requestHeaders
{
    if (requestHeaders.count == 0) {
        return;
    }
    
    NSMutableDictionary *mReqHeaders = [NSMutableDictionary dictionaryWithDictionary:self.request.allHTTPHeaderFields];
    [mReqHeaders addEntriesFromDictionary:requestHeaders];
    self.request.allHTTPHeaderFields = mReqHeaders;
}

- (void)setupConfigRequestHeaders
{
    NSMutableDictionary *mCfgDict = [NSMutableDictionary dictionaryWithDictionary:self.request.allHTTPHeaderFields];
    NSDictionary *cfgDict = [self getRequestParamsFromConfigHeaders];
    if (cfgDict) {
        [mCfgDict addEntriesFromDictionary:cfgDict];
    }
    
    [mCfgDict setObject:@"true" forKey:@"accept-diff"];
    [mCfgDict setObject:@"true" forKey:@"no-Chunked"];
    [mCfgDict setObject:@"GET" forKey:@"method"];
    [mCfgDict setObject:@"utf-8" forKey:@"accept-Encoding"];
    [mCfgDict setObject:@"zh-CN,zh;" forKey:@"accept-Language"];
    [mCfgDict setObject:@"gzip" forKey:@"accept-Encoding"];
    [mCfgDict setObject:SonicHeaderValueSDKVersion  forKey:SonicHeaderKeySDKVersion];
    [mCfgDict setObject:SonicHeaderValueSonicLoad forKey:SonicHeaderKeyLoadType];
    NSString *userAgent = [SonicClient sharedClient].userAgent.length > 0? [SonicClient sharedClient].userAgent:[[SonicClient sharedClient] sonicDefaultUserAgent];
    [mCfgDict setObject:userAgent forKey:@"User-Agent"];

    NSURL *cUrl = [NSURL URLWithString:self.url];

    if (self.serverIP.length > 0) {
        NSString *host = [cUrl.scheme isEqualToString:@"https"]? [NSString stringWithFormat:@"%@:443",self.serverIP]:[NSString stringWithFormat:@"%@:80",self.serverIP];
        NSString *newUrl = [self.url stringByReplacingOccurrencesOfString:cUrl.host withString:host];
        cUrl = [NSURL URLWithString:newUrl];
        [mCfgDict setObject:cUrl.host forKey:@"Host"];
    }
    
    [self.request setAllHTTPHeaderFields:mCfgDict];
}

- (NSDictionary *)getRequestParamsFromConfigHeaders
{
    NSDictionary *cfgDict = self.cacheConfigHeaders;
    NSMutableDictionary *mCfgDict = [NSMutableDictionary dictionary];
    
    if (cfgDict) {
        NSString *eTag = cfgDict[SonicHeaderKeyETag];
        if (eTag.length > 0) {
            [mCfgDict setObject:eTag forKey:@"If-None-Match"];
        }
        NSString *tempTag = cfgDict[SonicHeaderKeyTemplate];
        if (tempTag.length > 0 ) {
            [mCfgDict setObject:tempTag forKey:@"template-tag"];
        }
    }else{
        [mCfgDict setObject:@"" forKey:@"If-None-Match"];
        [mCfgDict setObject:@"" forKey:@"template-tag"];
    }
    
    return mCfgDict;
}

#pragma mark - Sonic Session Protocol

void dispatchToSonicSessionQueue(dispatch_block_t block)
{
    NSThread *currentThread = [NSThread currentThread];
    if([currentThread.name isEqualToString:[SonicSession sonicSessionQueue].name]){
        block();
    }else{
        NSBlockOperation *blkOp = [NSBlockOperation blockOperationWithBlock:block];
        [[SonicSession sonicSessionQueue] addOperation:blkOp];
    }
}

- (void)session:(SonicSession *)session didRecieveResponse:(NSHTTPURLResponse *)response
{
    dispatch_block_t opBlock = ^{
        
        self.response = response;
        self.cacheResponseHeaders = response.allHeaderFields;
        
        if (self.isFirstLoad) {
            [self firstLoadRecieveResponse:response];
        }
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (void)session:(SonicSession *)session didLoadData:(NSData *)data
{
    dispatch_block_t opBlock = ^{
        
        if (!self.responseData) {
            self.responseData = [NSMutableData data];
        }
        
        if (data) {
            
            NSData *copyData = [data copy];
            [self.responseData appendData:data];
            [copyData release];
            
            if (self.isFirstLoad) {
                [self firstLoadDidLoadData:data];
            }
        }
        
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (void)session:(SonicSession *)session didFaild:(NSError *)error
{
    dispatch_block_t opBlock = ^{
        
        self.error = error;
        self.isCompletion = YES;
        
        if (self.response.statusCode == 304) {
            if (self.isFirstLoad) {
                [self firstLoadDidFinish];
            }else{
                [self updateDidSuccess];
            }
        }else{
            if (self.isFirstLoad) {
                [self firstLoadDidFaild:error];
            }else{
                [self updateDidFaild];
            }
        }
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (void)sessionDidFinish:(SonicSession *)session
{
    dispatch_block_t opBlock = ^{
        
        self.isCompletion = YES;
        
        if (self.isFirstLoad) {
            [self firstLoadDidFinish];
        }else{
            [self updateDidSuccess];
        }
        
    };
    dispatchToSonicSessionQueue(opBlock);
}

#pragma mark - 首次加载

- (void)firstLoadRecieveResponse:(NSHTTPURLResponse *)response
{
    [self dispatchProtocolAction:SonicURLProtocolActionRecvResponse param:response];
}

- (void)firstLoadDidLoadData:(NSData *)data
{
    [self dispatchProtocolAction:SonicURLProtocolActionLoadData param:data];
}

- (void)firstLoadDidFaild:(NSError *)error
{
    [self dispatchProtocolAction:SonicURLProtocolActionDidFaild param:error];
    
    [self checkAutoCompletionAction];
}

- (void)firstLoadDidFinish
{
    [self dispatchProtocolAction:SonicURLProtocolActionDidFinish param:nil];
    
    if (![self isCompletionWithOutError]) {
        return;
    }
    
    switch (self.response.statusCode) {
        case 200:
        {
            if ([self isSonicResponse]) {
                
                NSString *policy = [self responseHeaderValueByIgnoreCaseKey:SonicHeaderKeyCacheOffline];

                self.cacheFileData = self.responseData;

                if ([policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
                    
                    [[SonicCache shareCache] saveServerDisableSonicTimeNow:self.sessionID];
                    
                    self.isDataUpdated = YES;
                    
                    break;
                }
                
                if ([policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineStore] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                    
                    SonicCacheItem *cacheItem = [[SonicCache shareCache] saveFirstWithHtmlData:self.responseData withResponseHeaders:self.response.allHeaderFields withUrl:self.url];
                    
                    if (cacheItem) {
                        
                        self.localRefreshTime = cacheItem.lastRefreshTime;
                        self.sonicStatusCode = SonicStatusCodeFirstLoad;
                        self.sonicStatusFinalCode = SonicStatusCodeFirstLoad;
                    }
                    
                    if ([policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                        [[SonicCache shareCache] removeCacheBySessionID:self.sessionID];
                    }
                    
                    [[SonicCache shareCache] removeServerDisableSonic:self.sessionID];
                }

            }else{
                
                self.cacheFileData = self.responseData;
                
            }
            
            self.isDataUpdated = YES;
        }
            break;
        case 503:
        {
            [self webViewRequireloadNormalRequest];
        }
            break;
        default:
        {
            
        }
            break;
    }
    
    [self checkAutoCompletionAction];
}

- (NSDictionary *)protocolActionItem:(SonicURLProtocolAction)action param:(NSObject *)param
{
    if (param == nil) {
        param = @"";
    }
    return @{kSonicProtocolAction:@(action),kSonicProtocolData:param};
}

- (void)dispatchProtocolAction:(SonicURLProtocolAction)action param:(NSObject *)param
{
    NSDictionary *actionParam = [self protocolActionItem:action param:param];
    if (self.protocolCallBack) {
        self.protocolCallBack(actionParam);
    }
}

- (void)dispatchProtocolActions:(NSArray *)actions
{
    for (NSDictionary *actionItem in actions) {
        if (self.protocolCallBack) {
            self.protocolCallBack(actionItem);
        }
    }
}

#pragma mark - 公开接口

- (void)preloadRequestActionsWithProtocolCallBack:(SonicURLProtocolCallBack)protocolCallBack
{
    dispatch_block_t opBlock = ^{
        
        self.protocolCallBack = protocolCallBack;

        if (self.isDataUpdated || !self.isFirstLoad) {
            
            if (protocolCallBack) {
                [self dispatchProtocolActions:[self cacheFileActions]];
            }
            
            if (self.isDataUpdated) {
                self.sonicStatusFinalCode = SonicStatusCodeAllCached;
            }
            
        }else{
            
            if (self.isFirstLoad) {
                [self dispatchProtocolActions:[self preloadRequestActions]];
            }
        }
        
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (NSArray *)preloadRequestActions
{
    NSMutableArray *actionItems = [NSMutableArray array];
    if (self.response) {
        NSDictionary *respItem = [self protocolActionItem:SonicURLProtocolActionRecvResponse param:self.response];
        [actionItems addObject:respItem];
    }
    
    if (self.isCompletion) {
        if (self.error) {
            NSDictionary *failItem = [self protocolActionItem:SonicURLProtocolActionDidFaild param:self.error];
            [actionItems addObject:failItem];
        }else{
            if (self.responseData.length > 0) {
                NSData *recvCopyData = [[self.responseData copy]autorelease];
                NSDictionary *recvItem = [self protocolActionItem:SonicURLProtocolActionLoadData param:recvCopyData];
                [actionItems addObject:recvItem];
            }
            NSDictionary *finishItem = [self protocolActionItem:SonicURLProtocolActionDidFinish param:nil];
            [actionItems addObject:finishItem];
        }
    }else{
        if (self.responseData.length > 0) {
            NSData *recvCopyData = [[self.responseData copy]autorelease];
            NSDictionary *recvItem = [self protocolActionItem:SonicURLProtocolActionLoadData param:recvCopyData];
            [actionItems addObject:recvItem];
        }
    }
    
    return actionItems;
}

- (NSArray *)cacheFileActions
{
    NSMutableArray *actionItems = [NSMutableArray array];
    
    NSHTTPURLResponse *response = nil;
    if (self.response && [self isCompletionWithOutError] && self.isDataUpdated) {
        response = self.response;
    }else{
        NSDictionary *respHeader = self.cacheResponseHeaders;
        response = [[[NSHTTPURLResponse alloc] initWithURL:[NSURL URLWithString:self.url] statusCode:200 HTTPVersion:@"1.1" headerFields:respHeader]autorelease];
    }
    
    NSMutableData *cacheData = [[self.cacheFileData mutableCopy] autorelease];
    
    NSDictionary *respItem = [self protocolActionItem:SonicURLProtocolActionRecvResponse param:response];
    NSDictionary *dataItem = [self protocolActionItem:SonicURLProtocolActionLoadData param:cacheData];
    NSDictionary *finishItem = [self protocolActionItem:SonicURLProtocolActionDidFinish param:nil];
    
    [actionItems addObject:respItem];
    [actionItems addObject:dataItem];
    [actionItems addObject:finishItem];
    
    self.didFinishCacheRead = YES;

    return actionItems;
}

- (void)getResultWithCallBack:(SonicWebviewCallBack)resultBlock
{
    dispatch_block_t opBlock = ^{
        
        self.webviewCallBack = resultBlock;
        
        NSDictionary *resultDict = [self sonicDiffResult];
        if (resultDict && resultBlock) {
            resultBlock(resultDict);
        }
        
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (NSDictionary *)sonicDiffResult
{
    if (self.sonicStatusCode == 0) {
        return nil;
    }
    
    NSMutableDictionary *resultDict = [NSMutableDictionary dictionary];

    [resultDict setObject:[@(self.sonicStatusFinalCode) stringValue] forKey:@"code"];
    
    BOOL isCacheOfflineRefresh = NO;
    NSString *policy = [self responseHeaderValueByIgnoreCaseKey:SonicHeaderKeyCacheOffline];

    if ([policy isEqualToString:SonicHeaderValueCacheOfflineStore]) {
        [resultDict setObject:[@(SonicStatusCodeAllCached) stringValue] forKey:@"code"];
    }else{
        isCacheOfflineRefresh = YES;
    }
    
    if (self.sonicStatusFinalCode == SonicStatusCodeDataUpdate && self.diffData && isCacheOfflineRefresh) {
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:self.diffData options:NSJSONWritingPrettyPrinted error:nil];
        NSString *jsonString = [[[NSString alloc]initWithData:jsonData encoding:NSUTF8StringEncoding]autorelease];
        if (jsonString.length > 0) {
            [resultDict setObject:jsonString forKey:@"result"];
        }
    }
    
    [resultDict setObject:[@(self.sonicStatusCode) stringValue] forKey:@"srcCode"];
    self.localRefreshTime = self.localRefreshTime.length > 0? self.localRefreshTime:@"";
    [resultDict setObject:self.localRefreshTime forKey:@"local_refresh_time"];
    
    return resultDict;
}

#pragma mark - 数据更新

- (void)updateDidSuccess
{
    if (![self isCompletionWithOutError]) {
        return;
    }
    
    switch (self.response.statusCode) {
        case 304:
        {
            self.sonicStatusCode = SonicStatusCodeAllCached;
            self.sonicStatusFinalCode = SonicStatusCodeAllCached;
        }
            break;
        case 200:
        {
            if (![self isSonicResponse]) {
                break;
            }
            
            if ([self isTemplateChange]) {
                
                self.cacheFileData = self.responseData;
                
                [self dealWithTemplateChange];
                
            }else{
                
                [self dealWithDataUpdate];
            }
            
            NSString *policy = [self responseHeaderValueByIgnoreCaseKey:SonicHeaderKeyCacheOffline];

            if ([policy isEqualToString:SonicHeaderValueCacheOfflineStore] || [policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                
                [[SonicCache shareCache] removeServerDisableSonic:self.sessionID];
            }
            
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
                
                if ([policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                    
                    [[SonicCache shareCache]removeCacheBySessionID:self.sessionID];
                }
                
                if ([policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
                    [[SonicCache shareCache] saveServerDisableSonicTimeNow:self.sessionID];
                }
            }
            
        }
            break;
        default:
        {
            
        }
            break;
    }
    
    //use the call back to tell web page which mode used
    if (self.webviewCallBack) {
        NSDictionary *resultDict = [self sonicDiffResult];
        if (resultDict) {
            self.webviewCallBack(resultDict);
        }
    }
    
    [self checkAutoCompletionAction];
}

- (void)dealWithTemplateChange
{
    SonicCacheItem *cacheItem = [[SonicCache shareCache] saveFirstWithHtmlData:self.responseData withResponseHeaders:self.response.allHeaderFields withUrl:self.url];
    
    if (cacheItem) {
        
        self.sonicStatusCode = SonicStatusCodeTemplateUpdate;
        self.sonicStatusFinalCode = SonicStatusCodeTemplateUpdate;
        self.localRefreshTime = cacheItem.lastRefreshTime;
        self.cacheFileData = self.responseData;
        self.cacheResponseHeaders = cacheItem.cacheResponseHeaders;
        
        self.isDataUpdated = YES;
        
        if (!self.didFinishCacheRead) {
            return;
        }
        
        dispatchToMain(^{
            
            NSString *policy = [self responseHeaderValueByIgnoreCaseKey:SonicHeaderKeyCacheOffline];
            
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                if (self.delegate && [self.delegate respondsToSelector:@selector(session:requireWebViewReload:)]) {
                    NSURLRequest *sonicRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:self.url]];
                    [self.delegate session:self requireWebViewReload:sonicWebRequest(sonicRequest)];
                }
            }
            
        });
    }
}

- (void)dealWithDataUpdate
{
    SonicCacheItem *cacheItem = [[SonicCache shareCache] updateWithJsonData:self.responseData withResponseHeaders:self.response.allHeaderFields withUrl:self.url];
    
    if (cacheItem) {
        
        self.sonicStatusCode = SonicStatusCodeDataUpdate;
        self.sonicStatusFinalCode = SonicStatusCodeDataUpdate;
        self.localRefreshTime = cacheItem.lastRefreshTime;
        self.cacheFileData = cacheItem.htmlData;
        self.cacheResponseHeaders = cacheItem.cacheResponseHeaders;
        
        if (_diffData) {
            [_diffData release];
            _diffData = nil;
        }
        _diffData = [cacheItem.diffData copy];
        
        self.isDataUpdated = YES;
    }
}

- (void)updateDidFaild
{    
    [self checkAutoCompletionAction];
}

#pragma mark - 公共处理

- (void)checkAutoCompletionAction
{
    dispatchToMain(^{
        if (!self.delegate) {
            if (self.completionCallback) {
                self.completionCallback(self.sessionID);
            }
        }
    });
}

- (void)webViewRequireloadNormalRequest
{
    dispatchToMain(^{
        if (self.delegate || [self.delegate respondsToSelector:@selector(session:requireWebViewReload:)]) {
            NSURLRequest *normalRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:self.url]];
            [self.delegate session:self requireWebViewReload:normalRequest];
        }
    });
}

- (BOOL)isSonicResponse
{
    return [self responseHeaderHasKey:SonicHeaderKeyCacheOffline];
}

- (NSString *)responseHeaderValueByIgnoreCaseKey:(NSString *)aKey
{
    if (![self responseHeaderHasKey:aKey]) {
        return nil;
    }
    
    NSString *findKey = nil;
    for (NSString *key in self.response.allHeaderFields.allKeys) {
        if ([[key lowercaseString] isEqualToString:aKey]) {
            findKey = key;
            break;
        }
    }
    
    return self.response.allHeaderFields[findKey];
}

- (BOOL)responseHeaderHasKey:(NSString *)aKey
{
    if (!self.response) {
        return NO;
    }
    
    NSInteger findKeyIndex = NSNotFound;
    for (NSString *key in self.response.allHeaderFields.allKeys) {
        if ([[key lowercaseString] isEqualToString:aKey]) {
            findKeyIndex = 1;
            break;
        }
    }
    
    return findKeyIndex != NSNotFound;
}

- (BOOL)isTemplateChange
{
    if (![self isSonicResponse]) {
        return NO;
    }
    
    BOOL hasTemplateTag = [self responseHeaderHasKey:@"template-change"];
    
    if (!hasTemplateTag) {
        return NO;
    }
    
    NSString *tempChangeTag = [self.response.allHeaderFields objectForKey:@"template-change"];
    
    return [tempChangeTag boolValue];
}

- (BOOL)isCompletionWithOutError
{
    if (self.error && self.response.statusCode != 304) {
        return NO;
    }
    return YES;
}


@end
