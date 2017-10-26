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
#import "SonicUitil.h"
#import "SonicEngine.h"

@interface SonicSession ()

@property (nonatomic,retain)NSDictionary  *cacheConfigHeaders;

@property (nonatomic,assign)BOOL isCompletion;
@property (nonatomic,retain)SonicServer *sonicServer;

@property (nonatomic,copy)  NSString *localRefreshTime;
@property (nonatomic,assign)SonicStatusCode sonicStatusFinalCode;

@property (nonatomic,retain)NSData *cacheFileData;
@property (nonatomic,retain)NSDictionary  *cacheResponseHeaders;
@property (nonatomic,assign)BOOL didFinishCacheRead;

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
        self.sonicServer = [[SonicServer alloc] initWithUrl:self.url delegate:self delegateQueue:[SonicSession sonicSessionQueue]];
        [self.sonicServer enableLocalServer:TRUE/*_configuration.enableLocalServer*/];
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
    
    // setup headers
    NSDictionary *requestHeaderFields = [self buildRequestHeaderFields];
    [self.sonicServer setRequestHeaderFields:requestHeaderFields];
    [self.sonicServer setResponseHeaderFields:self.configuration.customResponseHeaders];
}

- (void)dealloc
{
    if (self.sonicServer) {
        [self.sonicServer stop];
        self.sonicServer = nil;
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
            NSLog(@"SonicSession.start finish:session(%@) is under cache expired.", self.sessionID);
            return;
        }
    }
    
    dispatchToMain(^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(sessionWillRequest:)]) {
            [self.delegate sessionWillRequest:self];
        }
        [self syncCookies];
    });

    [self.sonicServer start];
}

- (void)cancel
{
    if (self.sonicServer) {
        [self.sonicServer stop];
    }
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

- (NSDictionary *)buildRequestHeaderFields
{
    NSDictionary *cacheHeaders = self.cacheConfigHeaders;
    NSMutableDictionary *requestHeaders = [NSMutableDictionary dictionary];
    
    if (cacheHeaders) {
        NSString *eTag = cacheHeaders[SonicHeaderKeyETag];
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

#pragma mark - SonicServerDelegate
- (void)server:(SonicServer *)server didRecieveResponse:(NSHTTPURLResponse *)response
{
    dispatch_block_t opBlock = ^{
        
        //sync cookie to NSHTTPCookieStorage
        dispatchToMain(^{
            NSArray *cookiesFromResp = [NSHTTPCookie cookiesWithResponseHeaderFields:response.allHeaderFields forURL:response.URL];
            [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookies:cookiesFromResp forURL:response.URL mainDocumentURL:self.sonicServer.request.mainDocumentURL];
        });
        self.cacheResponseHeaders = response.allHeaderFields;
        if (self.isFirstLoad) {
            [self firstLoadRecieveResponse:response];
        }
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (void)server:(SonicServer *)server didReceiveData:(NSData *)data
{
    dispatch_block_t opBlock = ^{
        if (self.isFirstLoad) {
            [self firstLoadDidLoadData:data];
        }
    };
    dispatchToSonicSessionQueue(opBlock);
}

- (void)server:(SonicServer *)server didCompleteWithError:(NSError *)error
{
    dispatch_block_t opBlock = ^{
        self.isCompletion = YES;
        if (self.isFirstLoad) {
            [self firstLoadDidFaild:error];
        } else {
            [self updateDidFaild];
        }
    };
    dispatchToSonicSessionQueue(opBlock);
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
        if (![self isStrictMode]) {
            
            BOOL canUpdateCacheExpire = NO;
            
            NSString *etag = [self.sonicServer.response.allHeaderFields objectForKey:SonicHeaderKeyETag];
            if(!self.configuration.supportNoEtag && (etag.length > 0 && ![etag isEqualToString:self.cacheConfigHeaders[SonicHeaderKeyETag]]))
            {
                canUpdateCacheExpire = YES;
            }
            
            if (self.configuration.supportNoEtag) {
                canUpdateCacheExpire = YES;
            }
            
            if (canUpdateCacheExpire) {
                [[SonicCache shareCache] updateCacheExpireTimeWithResponseHeaders:self.sonicServer.response.allHeaderFields withSessionID:self.sessionID];
            }else{
                NSLog(@"unstrict-mode can't update cache expire time");
            }
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

- (void)firstLoadDidSuccess
{
    [self dispatchProtocolAction:SonicURLProtocolActionDidSuccess param:nil];
    
    if (200 == self.sonicServer.response.statusCode) {
        [self dealWithFirstLoad];
    } else {
        NSLog(@"firstLoadDidSuccess warning:statusCode[%ld] miss!", (long)self.sonicServer.response.statusCode);
    }
    
    [self checkAutoCompletionAction];
}

- (void)dealWithFirstLoad
{
    if ([self isSonicResponse]) {
        
        NSString *policy = [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
        
        self.cacheFileData = self.sonicServer.responseData;
        
        if ([policy isEqualToString:SonicHeaderValueCacheOfflineDisable]) {
            
            [[SonicCache shareCache] saveServerDisableSonicTimeNow:self.sessionID];
            
            self.isDataFetchFinished = YES;
            
            return;
        }
        
        if ([policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineStore] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
            
            SonicCacheItem *cacheItem = nil;
            
            
            if ([self isStrictMode]) {
                cacheItem =  [[SonicCache shareCache] saveFirstWithHtmlData:self.sonicServer.responseData withResponseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
            }else{
                cacheItem =  [[SonicCache shareCache] saveUnStrictModeWithHtmlData:self.sonicServer.responseData withResponseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
            }
            
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
        
        self.cacheFileData = self.sonicServer.responseData;
        
    }
    
    self.isDataFetchFinished = YES;
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
    dispatchToSonicSessionQueue(opBlock);
}

- (NSArray *)preloadRequestActions
{
    NSMutableArray *actionItems = [NSMutableArray array];
    if (self.sonicServer.response) {
        NSDictionary *respItem = [self protocolActionItem:SonicURLProtocolActionRecvResponse param:self.sonicServer.response];
        [actionItems addObject:respItem];
    }
    
    if (self.isCompletion) {
        if (self.sonicServer.error) {
            NSDictionary *failItem = [self protocolActionItem:SonicURLProtocolActionDidFaild param:self.sonicServer.error];
            [actionItems addObject:failItem];
        }else{
            if (self.sonicServer.responseData.length > 0) {
                NSData *recvCopyData = [[self.sonicServer.responseData copy]autorelease];
                NSDictionary *recvItem = [self protocolActionItem:SonicURLProtocolActionLoadData param:recvCopyData];
                [actionItems addObject:recvItem];
            }
            NSDictionary *finishItem = [self protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
            [actionItems addObject:finishItem];
        }
    }else{
        if (self.sonicServer.responseData.length > 0) {
            NSData *recvCopyData = [[self.sonicServer.responseData copy]autorelease];
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
    if (self.sonicServer.response && self.isDataFetchFinished) {
        response = self.sonicServer.response;
    }else{
        NSDictionary *respHeader = self.cacheResponseHeaders;
        response = [[[NSHTTPURLResponse alloc] initWithURL:[NSURL URLWithString:self.url] statusCode:200 HTTPVersion:@"1.1" headerFields:respHeader]autorelease];
    }
    
    NSMutableData *cacheData = [[self.cacheFileData mutableCopy] autorelease];
    NSDictionary *respItem = [self protocolActionItem:SonicURLProtocolActionRecvResponse param:response];
    NSDictionary *dataItem = [self protocolActionItem:SonicURLProtocolActionLoadData param:cacheData];
    NSDictionary *finishItem = [self protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
    
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
    
    return resultDict;
}

#pragma mark - 数据更新

- (void)updateDidSuccess
{
    switch (self.sonicServer.response.statusCode) {
        case 304:
        {
            self.sonicStatusCode = SonicStatusCodeAllCached;
            self.sonicStatusFinalCode = SonicStatusCodeAllCached;
            //update headers
            [[SonicCache shareCache] saveResponseHeaders:self.sonicServer.response.allHeaderFields withSessionID:self.sessionID];
        }
            break;
        case 200:
        {
            if (![self isSonicResponse]) {
                break;
            }
            
            if ([self isTemplateChange] || ![self isStrictMode]) {
                
                /* strict mode should check Etag */
                if(![self isStrictMode]){
                    
                    NSString *etag = [self.sonicServer.response.allHeaderFields objectForKey:SonicHeaderKeyETag];
                    
                    //If not support no Etag mode, this is error
                    if(!self.configuration.supportNoEtag && (etag.length == 0 || [etag isEqualToString:self.cacheConfigHeaders[SonicHeaderKeyETag]]))
                    {
                        NSLog(@"require Etag but etag.length=0 or etag isEqual to response.etag");
                        break;
                    }
                    
                    //If support no Etag mode , check sha1 to decide is html content changed
                    NSString *sha1 = getDataSha1(self.sonicServer.responseData);
                    NSString *existSha1 = self.cacheConfigHeaders[kSonicSha1];
                    
                    //304 if html content not changed
                    if(![sha1 isEqualToString:existSha1])
                    {
                        [self dealWithTemplateChange];
                        
                    }else{
                        
                        self.sonicStatusCode = SonicStatusCodeAllCached;
                        self.sonicStatusFinalCode = SonicStatusCodeAllCached;
                        
                        //update headers
                        [[SonicCache shareCache] saveResponseHeaders:self.sonicServer.response.allHeaderFields withSessionID:self.sessionID];
                    }
                    
                }else{
                    
                    [self dealWithTemplateChange];
                }
                
            }else{
                
                if ([self isStrictMode]) {
                    [self dealWithDataUpdate];
                }else{
                    //this is exception!!!
                    [self webViewRequireloadNormalRequest];
                }
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
    SonicCacheItem *cacheItem = nil;
    if ([self isStrictMode]) {
        cacheItem = [[SonicCache shareCache] saveFirstWithHtmlData:self.sonicServer.responseData withResponseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
    }else{
        cacheItem = [[SonicCache shareCache] saveUnStrictModeWithHtmlData:self.sonicServer.responseData withResponseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
    }
    
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
        
        dispatchToMain(^{
            NSString *policy = [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
            if ([policy isEqualToString:SonicHeaderValueCacheOfflineStoreRefresh] || [policy isEqualToString:SonicHeaderValueCacheOfflineRefresh]) {
                if (self.delegate && [self.delegate respondsToSelector:@selector(session:requireWebViewReload:)]) {
                    NSURLRequest *sonicRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:self.url]];
                    [self.delegate session:self requireWebViewReload:sonicWebRequest(self, sonicRequest)];
                }
            }
        });
    }
}

- (void)dealWithDataUpdate
{
    SonicCacheItem *cacheItem = [[SonicCache shareCache] updateWithJsonData:self.sonicServer.responseData withResponseHeaders:self.sonicServer.response.allHeaderFields withUrl:self.url];
    
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
    return [self.sonicServer responseHeaderForKey:SonicHeaderKeyCacheOffline];
}

- (BOOL)isTemplateChange
{
    if (![self isSonicResponse]) {
        return NO;
    }
    NSString *tempChangeTag = [self.sonicServer responseHeaderForKey:SonicHeaderKeyTemplateChange];
    return [tempChangeTag boolValue];
}

- (BOOL)isStrictMode
{
    if ([self.sonicServer responseHeaderForKey:SonicHeaderKeyTemplateChange].length > 0 &&
        [self.sonicServer responseHeaderForKey:SonicHeaderKeyTemplate].length > 0) {
        return YES;
    }
    return NO;
}


@end
