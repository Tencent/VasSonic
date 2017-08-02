//
//  SonicOfflineCacheRequest.m
//  SonicSample
//
//  Created by zyvincenthu on 2017/7/12.
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
    
    [self.session session:self.session didRecieveResponse:response];
    [self.session session:self.session didLoadData:htmlData];
    [self.session sessionDidFinish:self.session];
}

- (void)stopLoading
{
    
}

@end
