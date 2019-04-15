
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
#import "SonicServer.h"
#import "SonicConnection.h"
#import "SonicCache.h"
#import "SonicUtil.h"
#import "SonicEngine.h"
#import "SonicResourceLoader.h"
#import "SonicEventStatistics.h"

@interface SonicSession ()

@property (nonatomic,retain)NSDictionary  *cacheConfigHeaders;

@property (nonatomic,assign)BOOL isCompletion;
@property (nonatomic,retain)SonicServer *sonicServer;

@property (nonatomic,copy)  NSString *localRefreshTime;
@property (nonatomic,assign)SonicStatusCode sonicStatusFinalCode;

@property (nonatomic,retain)NSData *cacheFileData;
@property (nonatomic,retain)NSDictionary  *cacheResponseHeaders;
@property (nonatomic,assign)BOOL didFinishCacheRead;
@property (nonatomic,assign)BOOL isUpdate;

/**
 * Use to hold all block operation in sonic session queue
 * We need to cancel before the SonicSession dealloc
 */
@property (nonatomic,retain)NSMutableArray *sonicQueueOperationIdentifiers;

/**
 * Use to hold all block operation in main queue
 * We need to cancel before the SonicSession dealloc
 */
@property (nonatomic,retain)NSMutableArray *mainQueueOperationIdentifiers;

@end

@implementation SonicSession

#pragma mark - Custom Request

+ (BOOL)registerSonicConnection:(Class)connectionClass
{
    return [SonicServer registerSonicConnection:connectionClass];
}

+ (void)unregisterSonicConnection:(Class)connectionClass
{
    return [SonicServer unregisterSonicConnection:connectionClass];
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

- (instancetype)initWithUrl:(NSString *)aUrl withWebDelegate:(id<SonicSessionDelegate>)aWebDelegate Configuration:(SonicSessionConfiguration *)aConfiguration
{
    if (self = [super init]) {
        self.delegate = aWebDelegate;
        self.delegateId = [NSString stringWithFormat:@"%ld", (long)aWebDelegate.hash];
        self.url = aUrl;
        _configuration = [aConfiguration retain];
        _sessionID = [sonicSessionID(aUrl) copy];
        [self setupSonicServer];
        [self setupData];
    }
    return self;
}

- (void)setupData
{
    SonicCacheItem *cacheItem = [[SonicCache shareCache] cacheForSession:_sessionID];
    self.isFirstLoad = cacheItem.hasLocalCache? NO:YES;
    
    if (cacheItem.hasLocalCache) {
        self.cacheFileData = cacheItem.htmlData;
        self.cacheConfigHeaders = cacheItem.config;
        self.cacheResponseHeaders = cacheItem.cacheResponseHeaders;
        self.localRefreshTime = cacheItem.lastRefreshTime;
        
        [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionDidLoadLocalCache withEventInfo:@{@"sessionID":self.sessionID,@"url":self.url,@"dataLength":@(self.cacheFileData.length)}];
        
        //load sub resource if need
        [self preloadSubResourceWithResponseHeaders:self.cacheResponseHeaders];
    }
    
    [self setupRequestHeaders];
}

- (void)dealloc
{
    [_resourceLoader cancelAll];
    [_resourceLoader release];
    _resourceLoader = nil;
    
    if (self.sonicServer) {
        [self.sonicServer stop];
        self.sonicServer = nil;
    }
    
    if (_configuration) {
        [_configuration release];
        _configuration = nil;
    }
    
    self.cacheConfigHeaders = nil;
    self.cacheResponseHeaders = nil;
    self.cacheFileData = nil;
    
    if (self.delegate) {
        self.delegate = nil;
    }
    
    if (self.completionCallback) {
        self.completionCallback = nil;
    }
    
    if (self.protocolCallBack) {
        self.protocolCallBack = nil;
    }
    
    if (self.updateCallBack) {
        self.updateCallBack = nil;
    }
    
    [self cancel];
    
    self.url = nil;
    
    [_sessionID release];
    _sessionID = nil;

    [super dealloc];
}

#pragma mark - Open Interface

- (void)start
{
    //Check if cache expired
    if (self.configuration.supportCacheControl) {
        SonicCacheItem *cacheItem = [[SonicCache shareCache] cacheForSession:self.sessionID];
        if (![cacheItem isCacheExpired]) {
            SonicLogEvent(@"SonicSession.start finish:session(%@) is under cache expired.", self.sessionID);
            return;
        }
    }
    
    //dispatch to main , make sure syncCookies before start
    dispatch_block_t blk = ^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(sessionWillRequest:)]) {
            [self.delegate sessionWillRequest:self];
        }
        [self syncCookies];
    };
    
    if ([NSThread mainThread]) {
        blk();
    }else{
        dispatch_async(dispatch_get_main_queue(), blk);
    }

    [self.sonicServer start];
}

- (void)cancel
{
    self.delegate = nil;
    
    //remove operation relation this session in SonicSessionQueue and mainQueue
    NSMutableArray *opNeedCancel = [NSMutableArray array];
    for (NSString *opIdentifier in self.mainQueueOperationIdentifiers) {
        for (NSOperation *op in [NSOperationQueue mainQueue].operations) {
            if (op.hash == [opIdentifier integerValue]) {
                [opNeedCancel addObject:op];
            }
        }
    }
    
    //cancel operation from sonic session queue
    for (NSString *opIdentifier in self.sonicQueueOperationIdentifiers) {
        for (NSOperation *op in [SonicSession sonicSessionQueue].operations) {
            if (op.hash == [opIdentifier integerValue]) {
                [opNeedCancel addObject:op];
            }
        }
    }
    
    //cancel op now
    [opNeedCancel enumerateObjectsUsingBlock:^(NSOperation *op, NSUInteger idx, BOOL * _Nonnull stop) {
        [op cancel];
    }];
    
    if (self.sonicServer) {
        [self.sonicServer stop];
    }
}

- (void)setupSonicServer
{
    if (self.sonicServer) {
        self.sonicServer = nil;
    }
    SonicServer *tServer = [[SonicServer alloc] initWithUrl:self.url delegate:self delegateQueue:[SonicSession sonicSessionQueue]];
    self.sonicServer = tServer;
    [tServer release];
    [self.sonicServer enableLocalServer:_configuration.enableLocalServer];
}

- (BOOL)update
{
    if (self.sonicServer.isRuning) {
        return NO;
    }
    [self setupSonicServer];
    self.isFirstLoad = self.cacheConfigHeaders.count > 0 ? NO:YES;
    [self setupRequestHeaders];
    [self start];
    
    //event
    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionRefresh withEventInfo:@{@"url":self.url,@"sessionID":self.sessionID}];
    
    return YES;
}

- (NSString *)cachedHTMLString
{
    if (!self.cacheFileData) {
        return @"";
    }
    
    return [[[NSString alloc]initWithData:self.cacheFileData encoding:NSUTF8StringEncoding] autorelease];
}


- (void)syncCookies
{
    NSURL *cUrl = [NSURL URLWithString:self.url];
    NSHTTPCookieStorage *sharedHTTPCookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    NSArray *cookies = [sharedHTTPCookieStorage cookiesForURL:cUrl];
    NSDictionary *cookieHeader = [NSHTTPCookie requestHeaderFieldsWithCookies:cookies];
    if (nil != cookies) {
        [self.sonicServer addRequestHeaderFields:cookieHeader];
    }
}

- (NSString *)getSonicHeaderETagWithHeaders:(NSDictionary *)headers
{
    NSString *keyETag = [headers objectForKey:[SonicHeaderKeyCustomeETag lowercaseString]];
    if (keyETag && [keyETag isKindOfClass:[NSString class]] && keyETag.length > 0) {
        // do nothing
    } else {
        keyETag = [SonicHeaderKeyETag lowercaseString];
    }
    
    return [headers objectForKey:keyETag];
}

- (NSDictionary *)buildRequestHeaderFields
{
    NSDictionary *cacheHeaders = self.cacheConfigHeaders;
    NSMutableDictionary *requestHeaders = [NSMutableDictionary dictionary];
    
    if (cacheHeaders) {
        NSString *eTag = [self getSonicHeaderETagWithHeaders:cacheHeaders];
        if (eTag.length > 0) {
            [requestHeaders setObject:eTag forKey:HTTPHeaderKeyIfNoneMatch];
        }
        NSString *tempTag = cacheHeaders[SonicHeaderKeyTemplate];
        if (tempTag.length > 0 ) {
            [requestHeaders setObject:tempTag forKey:SonicHeaderKeyTemplate];
        }
    }else{
        [requestHeaders setObject:@"" forKey:HTTPHeaderKeyIfNoneMatch];
        [requestHeaders setObject:@"" forKey:SonicHeaderKeyTemplate];
    }
    
    [requestHeaders setObject:[[SonicEngine sharedEngine] getGlobalUserAgent] forKey:HTTPHeaderKeyUserAgent];
    
    NSURL *cUrl = [NSURL URLWithString:self.url];
    if (self.serverIP.length > 0) {
        NSString *host = [cUrl.scheme isEqualToString:@"https"]? [NSString stringWithFormat:@"%@:443",self.serverIP]:[NSString stringWithFormat:@"%@:80",self.serverIP];
        NSString *newUrl = [self.url stringByReplacingOccurrencesOfString:cUrl.host withString:host];
        cUrl = [NSURL URLWithString:newUrl];
        [requestHeaders setObject:cUrl.host forKey:HTTPHeaderKeyHost];
    }
    
    [requestHeaders addEntriesFromDictionary:self.configuration.customRequestHeaders];
    
    return requestHeaders;
}

- (void)setupRequestHeaders
{
    // setup headers
    NSDictionary *requestHeaderFields = [self buildRequestHeaderFields];
    [self.sonicServer setRequestHeaderFields:requestHeaderFields];
    [self.sonicServer setResponseHeaderFields:self.configuration.customResponseHeaders];
}

NSString * dispatchToSonicSessionQueue(dispatch_block_t block)
{
    NSBlockOperation *blkOp = [NSBlockOperation blockOperationWithBlock:block];
    [[SonicSession sonicSessionQueue] addOperation:blkOp];
    return [NSString stringWithFormat:@"%ld",(unsigned long)blkOp.hash];
}

#pragma mark - SonicServerDelegate
- (void)server:(SonicServer *)server didRecieveResponse:(NSHTTPURLResponse *)response
{
  [self preloadSubResourceWithResponseHeaders:response.allHeaderFields];
  dispatch_block_t opBlock = ^{
      
      NSInteger resposneCode = response.statusCode;
      [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionRecvResponse withEventInfo:@{@"statusCode":@(resposneCode),@"headers":response.allHeaderFields,@"url":self.url,@"sessionID":self.sessionID}];
      
        //sync cookie to NSHTTPCookieStorage
        dispatchToMain(^{
            NSArray *cookiesFromResp = [NSHTTPCookie cookiesWithResponseHeaderFields:response.allHeaderFields forURL:response.URL];
            [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookies:cookiesFromResp forURL:response.URL mainDocumentURL:self.sonicServer.request.mainDocumentURL];
        });
        if (self.isFirstLoad) {
            [self firstLoadRecieveResponse:response];
        }else{
            if ([self.sonicServer isSonicResponse] && !self.configuration.enableLocalServer) {
                self.cacheResponseHeaders = response.allHeaderFields;
            }
            if (self.configuration.enableLocalServer) {
                self.cacheResponseHeaders = response.allHeaderFields;
            }
        }
    };
    NSString *opIdentifier = dispatchToSonicSessionQueue(opBlock);
    [self.sonicQueueOperationIdentifiers addObject:opIdentifier];
}

- (void)server:(SonicServer *)server didReceiveData:(NSData *)data
{
    dispatch_block_t opBlock = ^{
        if (self.isFirstLoad) {
            [self firstLoadDidLoadData:data];
        }
    };
    NSString *opIdentifier = dispatchToSonicSessionQueue(opBlock);
    [self.sonicQueueOperationIdentifiers addObject:opIdentifier];
}

- (void)server:(SonicServer *)server didCompleteWithError:(NSError *)error
{
    dispatch_block_t opBlock = ^{
        
        //event
        NSInteger errorCode = error.code;
        [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionHttpError withEventInfo:@{@"code":@(errorCode),@"msg":error.debugDescription,@"url":self.url,@"sessionID":self.sessionID}];
        
        self.isCompletion = YES;
        if (self.isFirstLoad) {
            [self firstLoadDidFaild:error];
        } else {
            [self updateDidFaild];
        }
    };
    NSString *opIdentifier = dispatchToSonicSessionQueue(opBlock);
    [self.sonicQueueOperationIdentifiers addObject:opIdentifier];
}

- (void)serverDidCompleteWithoutError:(SonicServer *)server
{
    dispatch_block_t opBlock = ^{
        
        self.isCompletion = YES;
        
        if (self.isFirstLoad) {
            [self firstLoadDidSuccess];
        } else {
            [self updateDidSuccess];
        }
        
        //update cache expire time
        if (self.configuration.supportCacheControl) {
            
            [[SonicCache shareCache] updateCacheExpireTimeWithResponseHeaders:self.sonicServer.response.allHeaderFields withSessionID:self.sessionID];

        }
        
    };
    NSString *opIdentifier = dispatchToSonicSessionQueue(opBlock);
    [self.sonicQueueOperationIdentifiers addObject:opIdentifier];
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

- (void)firstLoadDidSuccess
{
    [self dispatchProtocolAction:SonicURLProtocolActionDidSuccess param:nil];
    
    if (200 == self.sonicServer.response.statusCode) {
        [self dealWithFirstLoad];
    } else {
        SonicLogEvent(@"firstLoadDidSuccess warning:statusCode[%ld] miss!", (long)self.sonicServer.response.statusCode);
    }
    
    [self checkAutoCompletionAction];
}

- (void)dealWithFirstLoad
{
    if ([self.sonicServer isSonicResponse]) {
        
        NSString *policy = [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
        
        self.cacheFileData = self.sonicServer.responseData;
        
        if ([policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
            
            [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionUnavilable withEventInfo:@{@"msg":@"Cache-Offline:Http",@"url":self.url,@"sessionID":self.sessionID}];
            
            [[SonicCache shareCache] saveServerDisableSonicTimeNow:self.sessionID];
            
            self.isDataFetchFinished = YES;
            
            return;
        }
        
        //cache-control
        if (self.configuration.supportCacheControl) {
            NSString *cacheControl = [self.sonicServer responseHeaderForKey:SonicHeaderValueCacheControl];
            if ([cacheControl isEqualToString:@"no-cache"] || [cacheControl isEqualToString:@"no-store"] || [cacheControl isEqualToString:@"must-revalidate"]) {
                SonicLogEvent(@"cache control need't cache!");
                return;
            }
        }
        
        if ([policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineStore] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
            
            NSDictionary *serverResult = [self.sonicServer sonicItemForCache];
            if (!serverResult) {
                SonicLogEvent(@"Sonic first load item for cache nil");
                return;
            }
            
            NSString *htmlString = [serverResult[kSonicHtmlFieldName] length]> 0? serverResult[kSonicHtmlFieldName]:@"";
            [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionFirstLoad withEventInfo:@{@"htmlString":htmlString,@"url":self.url,@"sessionID":self.sessionID}];
            
            SonicCacheItem *cacheItem = [[SonicCache shareCache] saveHtmlString:serverResult[kSonicHtmlFieldName] templateString:serverResult[kSonicTemplateFieldName] dynamicData:serverResult[kSonicDataFieldName] responseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
            
            if (cacheItem) {
                
                self.localRefreshTime = cacheItem.lastRefreshTime;
                self.sonicStatusCode = SonicStatusCodeFirstLoad;
                self.sonicStatusFinalCode = SonicStatusCodeFirstLoad;
                self.cacheConfigHeaders = cacheItem.config;
            }
            
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                [[SonicCache shareCache] removeCacheBySessionID:self.sessionID];
            }
            
            [[SonicCache shareCache] removeServerDisableSonic:self.sessionID];
        }
        
    }else{
        
        self.cacheFileData = self.sonicServer.responseData;
        
    }
    
    self.isDataFetchFinished = YES;
    
    //use the call back to tell web page which mode used
    [self dispatchUpdateCallBack];
}

- (void)dispatchProtocolAction:(SonicURLProtocolAction)action param:(NSObject *)param
{
    NSDictionary *actionParam = [SonicUtil protocolActionItem:action param:param];
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
        
        if (self.isDataFetchFinished || !self.isFirstLoad) {
                        
            if (protocolCallBack) {
                [self dispatchProtocolActions:[self cacheFileActions]];
            }
            
            if (self.isDataFetchFinished) {
                self.sonicStatusFinalCode = SonicStatusCodeAllCached;
            }
            
        } else { // self.isFirstLoad must be TRUE
            [self dispatchProtocolActions:[self preloadRequestActions]];
        }
        
    };
    NSString *opIdentifier = dispatchToSonicSessionQueue(opBlock);
    [self.sonicQueueOperationIdentifiers addObject:opIdentifier];
}

- (NSArray *)preloadRequestActions
{
    NSMutableArray *actionItems = [NSMutableArray array];
    if (self.sonicServer.response) {
        NSDictionary *respItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:self.sonicServer.response];
        [actionItems addObject:respItem];
    }
    
    if (self.isCompletion) {
        if (self.sonicServer.error) {
            NSDictionary *failItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidFaild param:self.sonicServer.error];
            [actionItems addObject:failItem];
        }else{
            if (self.sonicServer.responseData.length > 0) {
                NSData *recvCopyData = [[self.sonicServer.responseData copy]autorelease];
                NSDictionary *recvItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:recvCopyData];
                [actionItems addObject:recvItem];
            }
            NSDictionary *finishItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
            [actionItems addObject:finishItem];
        }
    }else{
        if (self.sonicServer.responseData.length > 0) {
            NSData *recvCopyData = [[self.sonicServer.responseData copy]autorelease];
            NSDictionary *recvItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:recvCopyData];
            [actionItems addObject:recvItem];
        }
    }
    
    return actionItems;
}

- (NSArray *)cacheFileActions
{
    NSMutableArray *actionItems = [NSMutableArray array];
    
    NSHTTPURLResponse *response = nil;
    if (self.sonicServer.response && self.isDataFetchFinished) {
        response = self.sonicServer.response;
    }else{
        NSDictionary *respHeader = self.cacheResponseHeaders;
        response = [[[NSHTTPURLResponse alloc] initWithURL:[NSURL URLWithString:self.url] statusCode:200 HTTPVersion:@"1.1" headerFields:respHeader]autorelease];
    }
    
    NSMutableData *cacheData = [[self.cacheFileData mutableCopy] autorelease];
    NSDictionary *respItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:response];
    NSDictionary *dataItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:cacheData];
    NSDictionary *finishItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
    
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
    NSString *opIdentifier = dispatchToSonicSessionQueue(opBlock);
    [self.sonicQueueOperationIdentifiers addObject:opIdentifier];
}

- (NSDictionary *)sonicDiffResult
{
    if (self.sonicStatusCode == 0) {
        return nil;
    }
    
    NSMutableDictionary *resultDict = [NSMutableDictionary dictionary];

    [resultDict setObject:[@(self.sonicStatusFinalCode) stringValue] forKey:@"code"];
    
    BOOL isCacheOfflineRefresh = NO;
    NSString *policy = [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
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
    
    //Add extra info
    NSString *templateTag = self.cacheConfigHeaders[@"template-tag"];
    templateTag = templateTag.length > 0? templateTag:@"";
    NSString *etag = self.cacheConfigHeaders[@"etag"];
    etag = etag.length > 0? etag:@"";
    NSString *isRefresh = self.isUpdate? @"true":@"false";
    NSString *cacheOffline = [self.sonicServer responseHeaderForKey:@"cache-offline"];
    cacheOffline = cacheOffline.length > 0? cacheOffline:@"";
    NSDictionary *extra = @{@"template-tag":templateTag,@"eTag":etag,@"isReload":isRefresh,@"cache-offline":cacheOffline};
    [resultDict setObject:extra forKey:@"extra"];

    SonicLogEvent(@"sonic diff result :%@",resultDict);
    
    return resultDict;
}

#pragma mark - 数据更新

- (void)updateDidSuccess
{
    switch (self.sonicServer.response.statusCode) {
        case 304:
        {
            [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionHitCache withEventInfo:@{@"msg":@"Server 304!",@"url":self.url,@"sessionID":self.sessionID}];
            
            self.sonicStatusCode = SonicStatusCodeAllCached;
            self.sonicStatusFinalCode = SonicStatusCodeAllCached;
            //update headers
            [[SonicCache shareCache] saveResponseHeaders:self.sonicServer.response.allHeaderFields withSessionID:self.sessionID];
        }
            break;
        case 200:
        {
            if (![self.sonicServer isSonicResponse]) {
                [[SonicCache shareCache] removeCacheBySessionID:self.sessionID];
                SonicLogEvent(@"Clear cache because while not sonic repsonse!");
                break;
            }
            
            if ([self isTemplateChange]) {
                
                [self dealWithTemplateChange];
                
            }else{
                
                [self dealWithDataUpdate];
            }
            
            NSString *policy = [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineStore] || [policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                [[SonicCache shareCache] removeServerDisableSonic:self.sessionID];
            }
            
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
                
                if ([policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                    
                    [[SonicCache shareCache]removeCacheBySessionID:self.sessionID];
                }
                
                if ([policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
                    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionUnavilable withEventInfo:@{@"msg":@"Cache-Offline:Http",@"url":self.url,@"sessionID":self.sessionID}];
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
    if (self.isUpdate) {
        [self dispatchUpdateCallBack];
    }else{
        if (self.webviewCallBack) {
            NSDictionary *resultDict = [self sonicDiffResult];
            if (resultDict) {
                self.webviewCallBack(resultDict);
            }
        }else{
            SonicLogEvent(@"There is no webViewCallBack!");
        }
    }
    
    [self checkAutoCompletionAction];
}

- (void)dispatchUpdateCallBack
{
    if (self.isUpdate) {
        if (self.updateCallBack) {
            NSDictionary *resultDict = [self sonicDiffResult];
            if (resultDict) {
                self.updateCallBack(resultDict);
            }
        }else{
            SonicLogEvent(@"There is no updateCallBack!");
        }
    }
}

- (void)dealWithTemplateChange
{
    NSDictionary *serverResult = [self.sonicServer sonicItemForCache];
    SonicCacheItem *cacheItem = [[SonicCache shareCache] saveHtmlString:serverResult[kSonicHtmlFieldName] templateString:serverResult[kSonicTemplateFieldName] dynamicData:serverResult[kSonicDataFieldName] responseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
    
    
    //statistics
    NSString  *htmlString = @"";
    htmlString = htmlString.length > 0? htmlString:[[[NSString alloc]initWithData:self.sonicServer.responseData encoding:NSUTF8StringEncoding]autorelease];
    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionTemplateChanged withEventInfo:@{@"htmlString":htmlString,@"url":self.url,@"sessionID":self.sessionID}];
    
    if (cacheItem) {
        
        self.sonicStatusCode = SonicStatusCodeTemplateUpdate;
        self.sonicStatusFinalCode = SonicStatusCodeTemplateUpdate;
        self.localRefreshTime = cacheItem.lastRefreshTime;
        self.cacheFileData = self.sonicServer.responseData;
        self.cacheResponseHeaders = cacheItem.cacheResponseHeaders;
        
        self.isDataFetchFinished = YES;
        
        if (!self.didFinishCacheRead) {
            return;
        }
        
        NSString *opIdentifier  =  dispatchToMain(^{
            NSString *policy = [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                if (self.delegate && [self.delegate respondsToSelector:@selector(session:requireWebViewReload:)]) {
                    NSURLRequest *sonicRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:self.url]];
                    [self.delegate session:self requireWebViewReload:[SonicUtil sonicWebRequestWithSession:self withOrigin:sonicRequest]];
                }
            }
        });
        [self.mainQueueOperationIdentifiers addObject:opIdentifier];
    }
}

- (void)dealWithDataUpdate
{
    NSString *htmlString = nil;
    if (self.sonicServer.isInLocalServerMode) {
        NSDictionary *serverResult = [self.sonicServer sonicItemForCache];
        htmlString = serverResult[kSonicHtmlFieldName];
    }
    
    //statistics
    NSString *htmlLog = @"";
    htmlLog = htmlString.length > 0? htmlString:[[[NSString alloc]initWithData:self.sonicServer.responseData encoding:NSUTF8StringEncoding]autorelease];
    [[SonicEventStatistics shareStatistics] addEvent:SonicStatisticsEvent_SessionDataUpdated withEventInfo:@{@"htmlString":htmlLog,@"url":self.url,@"sessionID":self.sessionID}];
    
    SonicCacheItem *cacheItem = [[SonicCache shareCache] updateWithJsonData:self.sonicServer.responseData withHtmlString:htmlString withResponseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
    
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
        
        self.isDataFetchFinished = YES;
    }
}

- (void)updateDidFaild
{    
    [self checkAutoCompletionAction];
}

#pragma mark - Resource Loader

- (void)preloadSubResourceWithResponseHeaders:(NSDictionary *)responseHeaders
{
    NSString *linkValue = responseHeaders[SonicHeaderKeyLink];
    if (linkValue.length == 0) {
        SonicLogEvent(@"no preload link exist!");
        return;
    }
    NSArray *linkArray = [linkValue componentsSeparatedByString:@";"];
    if (linkArray.count > 0) {
        if (!_resourceLoader) {
            _resourceLoader = [SonicResourceLoader new];
        }
        [_resourceLoader loadResourceLinks:linkArray];
    }
}

#pragma mark - 公共处理

- (void)checkAutoCompletionAction
{
    NSString *opIdentifier = dispatchToMain(^{
        if (!self.delegate) {
            if (self.completionCallback) {
                self.completionCallback(self.sessionID);
            }
        }
    });
    [self.mainQueueOperationIdentifiers addObject:opIdentifier];
}

- (void)webViewRequireloadNormalRequest
{
    NSString *opIdentifier = dispatchToMain(^{
        if (self.delegate || [self.delegate respondsToSelector:@selector(session:requireWebViewReload:)]) {
            NSURLRequest *normalRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:self.url]];
            [self.delegate session:self requireWebViewReload:normalRequest];
        }
    });
    [self.mainQueueOperationIdentifiers addObject:opIdentifier];
}

- (BOOL)isTemplateChange
{
    if (![self.sonicServer isSonicResponse]) {
        return NO;
    }
    NSString *tempChangeTag = [self.sonicServer responseHeaderForKey:SonicHeaderKeyTemplateChange];
    return [tempChangeTag boolValue];
}

@end
