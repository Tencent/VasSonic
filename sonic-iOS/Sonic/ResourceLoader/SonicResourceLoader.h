//
//  SonicResourceLoader.h
//  Sonic
//
//  Created by zyvincenthu on 2017/12/18.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SonicSession.h"

#define kSonicResourceVersion @"sonic_ts"

@interface SonicResourceLoader : NSObject

@property (nonatomic,readonly)NSString *mainDocumentSessionID;

+ (NSOperationQueue *)resourceQueue;

- (instancetype)init NS_UNAVAILABLE;

- (instancetype)initWithSessionID:(NSString *)sessionID;

- (void)cancelAll;

+ (BOOL)isResourceRequest:(NSURLRequest *)request;

- (BOOL)canInterceptResourceWithUrl:(NSString *)url;

- (void)loadResourceWithUrl:(NSString *)url;

- (void)preloadResourceWithUrl:(NSString *)url withProtocolCallBack:(SonicURLProtocolCallBack)callback;

@end
