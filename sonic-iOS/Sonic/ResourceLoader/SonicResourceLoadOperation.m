//
//  SonicResourceLoadOperation.m
//  Sonic
//
//  Created by zyvincenthu on 2017/12/18.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicResourceLoadOperation.h"
#import "SonicServer.h"
#import "SonicResourceLoader.h"
#import "SonicCache.h"
#import "SonicUtil.h"

@interface SonicResourceLoadOperation()<SonicConnectionDelegate>

@property (nonatomic,retain)SonicConnection *connection;

@property (nonatomic,retain)NSData *cacheFileData;

@property (nonatomic,retain)NSDictionary *cacheResponseHeaders;

@property (nonatomic,retain)NSDictionary *config;

@property (nonatomic,retain)NSMutableData *responseData;

@property (nonatomic,retain)NSHTTPURLResponse *originResponse;

@property (nonatomic,retain)NSLock *lock;


@end

@implementation SonicResourceLoadOperation

- (instancetype)initWithUrl:(NSString *)aUrl
{
    if (self = [super init]) {
        _url = [aUrl copy];
        self.responseData = [NSMutableData data];
        _sessionID = [resourceSessionID(_url) copy];
        self.lock = [NSLock new];
        
        long long cacheExpire = [self.config[@"cache-expire-time"] longLongValue];
        long long now = (long long)([[NSDate date] timeIntervalSince1970] * 1000);
//        if (cacheExpire <= now) {
//            [[SonicCache shareCache] clearResourceWithSessionID:sourceID];
//        }else{
            self.cacheFileData = [[SonicCache shareCache] resourceCacheWithSessionID:self.sessionID];
            self.config = [[SonicCache shareCache] resourceConfigWithSessionID:self.sessionID];
            self.cacheResponseHeaders = [[SonicCache shareCache] responseHeadersWithSessionID:self.sessionID];
//        }
    }
    return self;
}

- (void)dealloc
{
    [self cancel];
    [_url release];
    _url = nil;
    [_sessionID release];
    _sessionID = nil;
    self.responseData = nil;
    self.lock = nil;
    self.cacheFileData = nil;
    self.originResponse = nil;
    [super dealloc];
}

- (BOOL)isCacheExist
{
    return self.cacheFileData.length > 0;
}

- (void)preloadDataWithProtocolCallBack:(SonicURLProtocolCallBack)callBack
{
    if (!callBack) {
        return;
    }
    self.protocolCallBack = callBack;
    NSDictionary *rspItem = nil;
    NSDictionary *dataItem = nil;
    NSDictionary *finishItem = nil;
    if (self.cacheFileData && self.cacheFileData.length > 0 && self.cacheResponseHeaders.count > 0) {
        NSHTTPURLResponse *resp = [[[NSHTTPURLResponse alloc]initWithURL:[NSURL URLWithString:self.url] statusCode:200 HTTPVersion:@"HTTP/1.1" headerFields:self.cacheResponseHeaders]autorelease];
        rspItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:resp];
        dataItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:self.cacheFileData];
        finishItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
        NSLog(@"resource read from cache:%@",self.url);
    }else{
        rspItem = [SonicUtil protocolActionItem:SonicURLProtocolActionRecvResponse param:self.originResponse];
        [self.lock lock];
        dataItem = [SonicUtil protocolActionItem:SonicURLProtocolActionLoadData param:self.cacheFileData];
        [self.lock unlock];
        finishItem = [SonicUtil protocolActionItem:SonicURLProtocolActionDidSuccess param:nil];
        NSLog(@"resource read from network:%@",self.url);
    }
    self.protocolCallBack(rspItem);
    self.protocolCallBack(dataItem);
    self.protocolCallBack(finishItem);
}



- (void)cancel
{
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
    self.connection = [[connectionClass alloc] initWithRequest:request delegate:self delegateQueue:[SonicResourceLoader resourceQueue]];
    self.connection.supportHTTPRedirection = YES;
    [self.connection startLoading];
    
    NSLog(@"statrt load resource:%@",self.url);
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
    NSLog(@"resource recieve response:%@",response.allHeaderFields);

    self.originResponse = response;
    [self disaptchProtocolAction:SonicURLProtocolActionRecvResponse withParam:response];
}

- (void)connection:(SonicConnection *)connection didReceiveData:(NSData *)data
{
    [self disaptchProtocolAction:SonicURLProtocolActionLoadData withParam:data];
    [self appendData:data];
}

- (void)connection:(SonicConnection *)connection didCompleteWithError:(NSError *)error
{
    [self disaptchProtocolAction:SonicURLProtocolActionDidFaild withParam:error];
}

- (void)connectionDidCompleteWithoutError:(SonicConnection *)connection
{
    [self disaptchProtocolAction:SonicURLProtocolActionDidSuccess withParam:nil];
    
    //save resource data
    [[SonicCache shareCache] saveSubResourceData:self.responseData withConfig:[self createConfig] withResponseHeaders:self.originResponse.allHeaderFields withSessionID:self.sessionID];
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
    unsigned long long maxAgeTime = maxAge.length > 0? [maxAge longLongValue]:0;
    unsigned long long cacheExpireTime = currentTimeStamp() + maxAgeTime;
    NSString *sha1 = getDataSha1(self.responseData);
    sha1 = sha1.length>0? sha1:@"";
    return @{@"sha1":sha1,@"cache-expire-time":[@(cacheExpireTime) stringValue]};
}

@end
