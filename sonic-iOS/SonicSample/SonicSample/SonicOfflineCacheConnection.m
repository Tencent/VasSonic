//
//  SonicOfflineCacheRequest.m
//  SonicSample
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

#import "SonicOfflineCacheConnection.h"

@implementation SonicOfflineCacheConnection

+ (BOOL)canInitWithRequest:(NSURLRequest *)request
{
    if ([request.URL.absoluteString isEqualToString:@"http://mc.vip.qq.com/demo/indexv3?offline=1"]) {
        return YES;
    }
    return NO;
}

- (void)startLoading
{
    NSString *offlinePath = [[NSBundle mainBundle]pathForResource:@"main" ofType:@"html"];
    NSData *htmlData = [NSData dataWithContentsOfFile:offlinePath];
    
    NSHTTPURLResponse *response = [[NSHTTPURLResponse alloc]initWithURL:self.request.URL MIMEType:@"text/html" expectedContentLength:htmlData.length textEncodingName:@"utf8"];
    
    [self.delegate connection:self didReceiveResponse:response];
    [self.delegate connection:self didReceiveData:htmlData];
    [self.delegate connectionDidCompleteWithoutError:self];
}

- (void)stopLoading
{
    
}

@end
