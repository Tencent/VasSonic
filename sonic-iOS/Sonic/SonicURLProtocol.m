//
//  SonicURLProtocol.m
//  sonic
//
//  Created by zyvincenthu on 17/4/1.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicURLProtocol.h"
#import "SonicConstants.h"
#import "SonicClient.h"
#import "SonicUitil.h"

@interface SonicURLProtocol ()

@property (nonatomic,assign)BOOL didFinishRecvResponse;
@property (nonatomic,assign)long long recvDataLength;

@end

@implementation SonicURLProtocol

+ (BOOL)canInitWithRequest:(NSURLRequest *)request
{    
    NSString *value = [request.allHTTPHeaderFields objectForKey:SonicHeaderKeyLoadType];
    
    if (value.length == 0) {
        return NO;
    }
    
    if ([value isEqualToString:SonicHeaderValueSonicLoad]) {
        return NO;
        
    }else if([value isEqualToString:SonicHeaderValueWebviewLoad]) {
        return YES;
        
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
    
    [[SonicClient sharedClient] registerURLProtocolCallBackWithSessionID:sessionID completion:^(NSDictionary *param) {
        
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
            if (!self.didFinishRecvResponse) {
                NSHTTPURLResponse *resp = params[kSonicProtocolData];
                [self.client URLProtocol:self didReceiveResponse:resp cacheStoragePolicy:NSURLCacheStorageNotAllowed];
                self.didFinishRecvResponse = YES;
            }
        }
            break;
        case SonicURLProtocolActionLoadData:
        {
            if (self.didFinishRecvResponse) {
                NSData *recvData = params[kSonicProtocolData];
                if (recvData.length > 0) {
                    [self.client URLProtocol:self didLoadData:recvData];
                    self.recvDataLength = self.recvDataLength + recvData.length;
                }
            }
        }
            break;
        case SonicURLProtocolActionDidFinish:
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
        default:
            break;
    }
}

@end
