//
//  SonicURLProtocol.m
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

#import "SonicURLProtocol.h"
#import "SonicConstants.h"
#import "SonicEngine.h"
#import "SonicUtil.h"

@implementation SonicURLProtocol

+ (BOOL)canInitWithRequest:(NSURLRequest *)request
{    
    NSString *value = [request.allHTTPHeaderFields objectForKey:SonicHeaderKeyLoadType];
    if (value.length != 0 && [value isEqualToString:SonicHeaderValueWebviewLoad]) {
        NSString * delegateId = [request.allHTTPHeaderFields objectForKey:SonicHeaderKeyDelegateId];
        if (delegateId.length != 0) {
            SonicSession *session = [[SonicEngine sharedEngine] sessionWithDelegateId:delegateId];
            if (session) {
                return YES;
            }
            NSLog(@"SonicURLProtocol.canInitWithRequest error:Cannot find sonic session!");
        }
    }
    return NO;
}

+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request
{
    return request;
}

- (void)startLoading
{    
    NSThread *currentThread = [NSThread currentThread];

    NSString *sessionID = [self.request valueForHTTPHeaderField:SonicHeaderKeySessionID];
    
    __weak typeof(self) weakSelf = self;
    
    [[SonicEngine sharedEngine] registerURLProtocolCallBackWithSessionID:sessionID completion:^(NSDictionary *param) {
        
        [weakSelf performSelector:@selector(callClientActionWithParams:) onThread:currentThread withObject:param waitUntilDone:NO];
        
    }];
}

- (void)stopLoading
{
    
}

- (void)dealloc
{
    [super dealloc];
}

#pragma mark - Client Action
- (void)callClientActionWithParams:(NSDictionary *)params
{
    SonicURLProtocolAction action = [params[kSonicProtocolAction]integerValue];
    switch (action) {
        case SonicURLProtocolActionRecvResponse:
        {
            NSHTTPURLResponse *resp = params[kSonicProtocolData];
            [self.client URLProtocol:self didReceiveResponse:resp cacheStoragePolicy:NSURLCacheStorageNotAllowed];
        }
            break;
        case SonicURLProtocolActionLoadData:
        {
            NSData *recvData = params[kSonicProtocolData];
            if (recvData.length > 0) {
                [self.client URLProtocol:self didLoadData:recvData];
            }
        }
            break;
        case SonicURLProtocolActionDidSuccess:
        {
            [self.client URLProtocolDidFinishLoading:self];
        }
            break;
        case SonicURLProtocolActionDidFaild:
        {
            NSError *err = params[kSonicProtocolData];
            [self.client URLProtocol:self didFailWithError:err];
        }
            break;
    }
}

@end
