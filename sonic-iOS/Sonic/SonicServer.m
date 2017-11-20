//
//  SonicServer.m
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

#import "SonicServer.h"
#import "SonicProtocol.h"
#import "SonicSession.h"
#import "SonicUtil.h"

static NSMutableArray *sonicRequestClassArray = nil;
static NSLock *sonicRequestClassLock;

@interface SonicServer()<SonicConnectionDelegate>

/** Connection instance which uses to connect web-server. */
@property (nonatomic,retain)SonicConnection *connection;

/** Use this delegate to communicate with sonic session. */
@property (nonatomic,assign)id<SonicServerDelegate> delegate;

/** Queue for connection delegate operation. */
@property (nonatomic,retain)NSOperationQueue* delegateQueue;

@property (nonatomic,assign)BOOL enableLocalSever;

@property (nonatomic,retain)NSDictionary *customResponseHeaders;

@property (nonatomic,assign)BOOL isCompletion;

/** htmlString -> templateString + data. */
@property (nonatomic,retain)NSString *htmlString;

@property (nonatomic,retain)NSString *templateString;

@property (nonatomic,retain)NSDictionary *data;

@end

@implementation SonicServer

- (instancetype)initWithUrl:(NSString *)url delegate:(id<SonicServerDelegate>) delegate delegateQueue:(NSOperationQueue *) queue
{
    if (self == [super init]) {
        self.delegate = delegate;
        self.delegateQueue = queue;
        _request = [[NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]] retain];
    }
    return self;
}

- (void)dealloc
{
    if (nil != self.connection) {
        [self.connection stopLoading];
        self.connection = nil;
    }
    
    if (nil != _request) {
        [_request release];
        _request = nil;
    }
    
    if (nil != _response) {
        [_response release];
        _request = nil;
    }
    
    if (nil != _error) {
        [_error release];
        _error = nil;
    }
    
    [super dealloc];
}

- (void)enableLocalServer:(BOOL)enable
{
    self.enableLocalSever = enable;
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

- (void)start
{
    if (nil == self.connection) {
        Class customRequest = [self canCustomRequest];
        if (!customRequest) {
            // If there no custom request ,then use the default
            customRequest = [SonicConnection class];
        }
        SonicConnection *tConnect = [[customRequest alloc]initWithRequest:self.request delegate: self delegateQueue:self.delegateQueue];
        self.connection = tConnect;
        [tConnect release];
        [self.connection startLoading];
    }
}

- (void)stop
{
    self.delegateQueue = nil;
    self.delegate = nil;
    
    if (self.connection) {
       [self.connection stopLoading];
    } else {
        NSLog(@"SonicServer.stop warning:Request headers should be set only before server start!");
    }
}

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
    if ([sonicRequestClassArray containsObject:connectionClass]) {
        if (sonicRequestClassArray.lastObject != connectionClass) {
            [sonicRequestClassArray removeObject:connectionClass]; // remove old object
            [sonicRequestClassArray addObject:connectionClass]; // add to last position
        }
    } else {
        [sonicRequestClassArray addObject:connectionClass]; // add to last position
    }
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

- (void)setRequestHeaderFields:(NSDictionary *)headers
{
    if (nil == self.connection) {
        NSMutableDictionary *requestHeaderFileds = [NSMutableDictionary dictionaryWithDictionary:self.request.allHTTPHeaderFields];
        [requestHeaderFileds setObject:@"true" forKey:@"accept-diff"];
        [requestHeaderFileds setObject:@"GET" forKey:@"method"];
        [requestHeaderFileds setObject:@"utf-8" forKey:@"accept-Encoding"];
        [requestHeaderFileds setObject:@"zh-CN,zh;" forKey:@"accept-Language"];
        [requestHeaderFileds setObject:@"gzip" forKey:@"accept-Encoding"];
        [requestHeaderFileds setObject:SonicHeaderValueSDKVersion  forKey:SonicHeaderKeySDKVersion];
        [requestHeaderFileds setObject:SonicHeaderValueSonicLoad forKey:SonicHeaderKeyLoadType];
        [requestHeaderFileds addEntriesFromDictionary:headers];
        [self.request setAllHTTPHeaderFields:requestHeaderFileds];
    } else {
        NSLog(@"setRequestHeaderFields warning:Request headers should be set only before server start!");
    }
}

- (void)setResponseHeaderFields:(NSDictionary *)headers
{
    self.customResponseHeaders = headers;
}

- (NSStringEncoding)encodingFromHeaders
{
    //content-type: text/html; charset=utf-8
    NSString *contentType = [[self responseHeaderForKey:[HTTPHeaderKeyContentType lowercaseString]] lowercaseString];
    if ([contentType containsString:@"charset="]) {
        NSRange charsetRange = [contentType rangeOfString:@"charset="];
        NSString *charsetString = [contentType substringFromIndex: charsetRange.location + charsetRange.length];
        if ([charsetString containsString:@";"] || [charsetString containsString:@" "]) {
            charsetString = [charsetString substringToIndex:charsetString.length - 1];
        }
        NSStringEncoding encoding = CFStringConvertEncodingToNSStringEncoding(CFStringConvertIANACharSetNameToEncoding((CFStringRef) charsetString));
        if (kCFStringEncodingInvalidId != encoding) {
            return encoding;
        }
    }
    return NSUTF8StringEncoding;
}

- (void)addRequestHeaderFields:(NSDictionary *)headers
{
    NSMutableDictionary *requestHeaderFileds = [NSMutableDictionary dictionaryWithDictionary:self.request.allHTTPHeaderFields];
    [requestHeaderFileds addEntriesFromDictionary:headers];
    [self.request setAllHTTPHeaderFields:requestHeaderFileds];
}

- (NSString *)responseHeaderForKey:(NSString *)aKey
{
    NSString *header = nil;
    if (_response) {
        header = [_response.allHeaderFields objectForKey:[aKey lowercaseString]];
    }
    return header;
}

- (NSDictionary *)sonicItemForCache
{
    if (self.isCompletion) {
        if (nil == _error) {
            NSMutableDictionary *sonicItemDict = [[[NSMutableDictionary alloc]init]autorelease];
            if (0 == self.htmlString.length) { // not split yet
                self.htmlString = [[[NSString alloc]initWithData:self.responseData encoding:[self encodingFromHeaders]] autorelease];
                NSDictionary *splitResult = [SonicUtil splitTemplateAndDataFromHtmlData:self.htmlString];
                self.templateString = splitResult[kSonicTemplateFieldName];
                self.data = splitResult[kSonicDataFieldName];
                
                NSMutableDictionary *headers = [[_response.allHeaderFields mutableCopy]autorelease];
                NSString *responseEtag = [headers objectForKey:[SonicHeaderKeyETag lowercaseString]];
                if (!responseEtag) {
                    responseEtag = getDataSha1([self.htmlString dataUsingEncoding:NSUTF8StringEncoding]);
                    [headers setObject:responseEtag forKey:[SonicHeaderKeyETag lowercaseString]];
                }
                
                NSString *responseTemplateTag = [headers objectForKey:[SonicHeaderKeyTemplate lowercaseString]];
                if (!responseTemplateTag) {
                    responseTemplateTag = getDataSha1([self.templateString dataUsingEncoding:NSUTF8StringEncoding]);
                    [headers setValue:responseTemplateTag forKey:[SonicHeaderKeyTemplate lowercaseString]];
                }
                
                [headers setValue:false forKey:[SonicHeaderKeyTemplateChange lowercaseString]];
                
                [sonicItemDict addEntriesFromDictionary:splitResult];
                [sonicItemDict setValue:self.htmlString forKey:kSonicHtmlFieldName];
                
                NSHTTPURLResponse *newResponse = [[[NSHTTPURLResponse alloc]initWithURL:_response.URL statusCode:200 HTTPVersion:nil headerFields:headers]autorelease];
                [_response release];
                _response = nil;
                _response = [newResponse retain];
            } else {
                [sonicItemDict setValue:self.htmlString forKey:kSonicHtmlFieldName];
                [sonicItemDict setValue:self.templateString forKey:kSonicTemplateFieldName];
                [sonicItemDict setValue:self.data forKey:kSonicDataFieldName];
            }
            return sonicItemDict;
        }
        return nil;
    }
    NSLog(@"sonicItemForCache warning:Should never call this function before connection did completion!");
    return nil;
}

#pragma Help functions

- (BOOL)isValidateSonicResponse:(NSHTTPURLResponse *)response
{
    if ([response.allHeaderFields[SonicHeaderKeyCacheOffline] length] == 0) {
        return NO;
    }
    
    if ([response.allHeaderFields[SonicHeaderKeyETag] length] == 0) {
        return NO;
    }
    
    if ([response.allHeaderFields[SonicHeaderKeyTemplateChange] length] == 0) {
        return NO;
    }
    
    if ([response.allHeaderFields[SonicHeaderKeyTemplate] length] == 0) {
        return NO;
    }
    
    return YES;
}

- (BOOL)isFirstLoadRequest
{
    return [self.request.allHTTPHeaderFields objectForKey:@"If-None-Match"].length == 0;
}

- (BOOL)isSonicResponse
{
    return [self isValidateSonicResponse:self.response];
}

#pragma Sonic Connection Delegate

/**
 * @brief Notify when the network connection did recieve response.
 */
- (void)connection:(SonicConnection *)connection didReceiveResponse:(NSHTTPURLResponse *)response
{
    // Make field names to lowercase string as Field names are case-insensitive.[https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2]
    NSMutableDictionary *headers = [[[NSMutableDictionary alloc] init] autorelease];
    if (self.customResponseHeaders.count > 0) {
        for (NSString *key in self.customResponseHeaders.allKeys) {
            [headers setValue:[self.customResponseHeaders objectForKey:key] forKey:[key lowercaseString]];
        }
    }
    for (NSString *key in response.allHeaderFields.allKeys) {
        [headers setObject:[response.allHeaderFields objectForKey:key] forKey:[key lowercaseString]];
    }
    
    // fix Weak-Etag case like -> etag: W/"66f0-m2UmCBEh78dNYPv+boO5ETXk4FU".[https://github.com/Tencent/VasSonic/issues/128]
    NSString *eTag = [headers objectForKey:[SonicHeaderKeyETag lowercaseString]];
    if ([eTag hasPrefix:@"W/"] && eTag.length > 3) {
        [headers setValue:[eTag substringWithRange:NSMakeRange(2, eTag.length - 3)] forKey:[SonicHeaderKeyETag lowercaseString]];
    }
    
    NSHTTPURLResponse *newResponse = [[[NSHTTPURLResponse alloc]initWithURL:response.URL statusCode:response.statusCode HTTPVersion:nil headerFields:headers]autorelease];
    
    _response = [newResponse retain];
    
    // Not sonic response and enabel local-server
    if ((response.statusCode == 304 || ![self isValidateSonicResponse:response]) && self.enableLocalSever) {
        _isInLocalServerMode = true;
        if (![self isFirstLoadRequest]) {
            return; // not first load request just return util all data are received.
        }
    }
    
    [self.delegate server:self didRecieveResponse:newResponse];
}

/**
 * @brief Notify when the network connection did recieve data.
 */
- (void)connection:(SonicConnection *)connection didReceiveData:(NSData *)data
{
    if (data) {
        
        if (nil == _responseData) {
            _responseData = [[NSMutableData data] retain];
        }
        NSData *copyData = [data copy];
        [_responseData appendData:copyData];
        [copyData release];
        
        if ([self isFirstLoadRequest] || !self.isInLocalServerMode) {
            [self.delegate server:self didReceiveData:data];
        }
    }
}

/**
 * @brief Call when the network connection did success.
 */
- (void)connectionDidCompleteWithoutError:(SonicConnection *)connection
{
    self.isCompletion = YES;
    if (self.isInLocalServerMode) {
        
        do {
            
            //if http status is 304, there is nothing changed
            if (self.response.statusCode == 304) {
                NSLog(@"response status 304!");
                break;
            }
            
            self.htmlString = [[[NSString alloc]initWithData:self.responseData encoding:[self encodingFromHeaders]] autorelease];
            NSDictionary *splitResult = [SonicUtil splitTemplateAndDataFromHtmlData:self.htmlString];
            if (splitResult) {
                self.templateString = splitResult[kSonicTemplateFieldName];
                self.data = splitResult[kSonicDataFieldName];
            }
            
            NSMutableDictionary *headers = [[_response.allHeaderFields mutableCopy]autorelease];
            
            if (![headers objectForKey:SonicHeaderKeyCacheOffline]) { // refresh this time
                [headers setValue:@"true" forKey:[SonicHeaderKeyCacheOffline lowercaseString]];
            }
            NSString *htmlSha1 = nil;
            NSString *responseEtag = [headers objectForKey:[SonicHeaderKeyETag lowercaseString]];
            if (!responseEtag) {
                responseEtag = htmlSha1 = getDataSha1([self.htmlString dataUsingEncoding:NSUTF8StringEncoding]);
                [headers setObject:responseEtag forKey:[SonicHeaderKeyETag lowercaseString]];
            }
            NSString *requestEtag = [self.request.allHTTPHeaderFields objectForKey:HTTPHeaderKeyIfNoneMatch];
            if ([responseEtag isEqualToString:requestEtag]) { // Case:hit 304
                [headers setValue:@"false" forKey:[SonicHeaderKeyTemplateChange lowercaseString]];
                NSHTTPURLResponse *newResponse = [[[NSHTTPURLResponse alloc]initWithURL:_response.URL statusCode:304 HTTPVersion:nil headerFields:headers]autorelease];
                // Update response data
                [_response release];
                _response = nil;
                _response = [newResponse retain];
                [_responseData release];
                _responseData = nil;
                break;
            }
            
            NSString *responseTemplateTag = [headers objectForKey:[SonicHeaderKeyTemplate lowercaseString]];
            if (!responseTemplateTag) {
                responseTemplateTag = getDataSha1([self.templateString dataUsingEncoding:NSUTF8StringEncoding]);
                [headers setValue:responseTemplateTag forKey:[SonicHeaderKeyTemplate lowercaseString]];
            }
            NSString *requestTemplateTag = [self.request.allHTTPHeaderFields objectForKey:SonicHeaderKeyTemplate];
            if ([responseTemplateTag isEqualToString:requestTemplateTag]) { // Case:data update
                NSError *jsonError = nil;
                NSMutableDictionary *jsonDict = [NSMutableDictionary dictionaryWithDictionary:self.data];
                if (!htmlSha1) {
                    htmlSha1 = getDataSha1([self.htmlString dataUsingEncoding:NSUTF8StringEncoding]);
                }
                [jsonDict setObject:htmlSha1 forKey:SonicHeaderKeyHtmlSha1];
                NSData *jsonData = [NSJSONSerialization dataWithJSONObject:jsonDict options:NSJSONWritingPrettyPrinted error:&jsonError];
                if (!jsonError) {
                    [headers setValue:@"false" forKey:[SonicHeaderKeyTemplateChange lowercaseString]];
                    NSHTTPURLResponse *newResponse = [[[NSHTTPURLResponse alloc]initWithURL:_response.URL statusCode:200 HTTPVersion:nil headerFields:headers]autorelease];
                    // Update response data
                    [_response release];
                    _response = nil;
                    _response = [newResponse retain];
                    [_responseData release];
                    _responseData = nil;
                    NSMutableData *mJsonData = [jsonData mutableCopy];
                    _responseData = [mJsonData retain];
                    [mJsonData release];
                    break;
                }
            }
            
            // Case:template-change
            [headers setValue:@"true" forKey:[SonicHeaderKeyTemplateChange lowercaseString]];
            NSHTTPURLResponse *newResponse = [[[NSHTTPURLResponse alloc]initWithURL:_response.URL statusCode:200 HTTPVersion:nil headerFields:headers]autorelease];
            [_response release];
            _response = nil;
            _response = [newResponse retain];
            break;
            
        } while (true);
        [self.delegate server:self didRecieveResponse:self.response];
        [self.delegate server:self didReceiveData:self.responseData];
    }
    [self.delegate serverDidCompleteWithoutError:self];
}

/**
 * @brief Call when the network connection did fail.
 */
- (void)connection:(SonicConnection *)connection didCompleteWithError:(NSError *)error
{
    _error = [error retain]; // update error
    self.isCompletion = YES;
    if (self.isInLocalServerMode && ![self isFirstLoadRequest]) {
        [self.delegate server:self didRecieveResponse:self.response];
        [self.delegate server:self didReceiveData:self.responseData];
    }
    [self.delegate server:self didCompleteWithError:error];
}

@end
