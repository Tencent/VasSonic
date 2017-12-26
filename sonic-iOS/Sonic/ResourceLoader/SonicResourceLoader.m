//
//  SonicResourceLoader.m
//  Sonic
//
//  Created by zyvincenthu on 2017/12/18.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicResourceLoader.h"
#import "SonicResourceLoadOperation.h"
#import "SonicUtil.h"
#import "SonicCache.h"

@interface SonicResourceLoader()

@property (nonatomic,readonly)NSOperationQueue *operationQueue;

@end

@implementation SonicResourceLoader

+ (NSOperationQueue *)resourceQueue
{
    static NSOperationQueue *_resourceQueueInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _resourceQueueInstance = [[NSOperationQueue alloc]init];
        _resourceQueueInstance.name = @"SonicResourceQueue";
        _resourceQueueInstance.maxConcurrentOperationCount = 3;
    });
    return _resourceQueueInstance;
}

- (instancetype)initWithSessionID:(NSString *)sessionID
{
    if (self = [super init]) {
        _mainDocumentSessionID = [sessionID copy];
    }
    return self;
}

- (void)dealloc
{
    [_mainDocumentSessionID release];
    _mainDocumentSessionID = nil;
    [super dealloc];
}

- (NSOperationQueue *)operationQueue
{
    return [SonicResourceLoader resourceQueue];
}

+ (BOOL)isResourceRequest:(NSURLRequest *)request
{
    return ![request.URL.absoluteString isEqualToString:request.mainDocumentURL.absoluteString];
}

- (BOOL)canInterceptResourceWithUrl:(NSString *)url
{
    SonicResourceLoadOperation *findOperation = nil;
    if (self.operationQueue.operationCount > 0) {
        for (NSOperation *item in self.operationQueue.operations) {
            if ([NSStringFromClass(item.class) isEqualToString:NSStringFromClass(SonicResourceLoadOperation.class)]) {
                if ([[(SonicResourceLoadOperation*)item url] isEqualToString:url]) {
                    findOperation = (SonicResourceLoadOperation *)item;
                    break;
                }
            }
        }
    }
    if (findOperation.hasStartNetwork || findOperation.isCacheExist ) {
        NSLog(@"find resource connection hasStartNetwork:%d isCacheExist:%d",findOperation.hasStartNetwork,findOperation.isCacheExist);
        return YES;
    }
    return [[SonicCache shareCache] resourceConfigWithSessionID:resourceSessionID(url)].count > 0;
}

- (void)loadResourceWithUrl:(NSString *)url
{
    SonicResourceLoadOperation *operation = [[SonicResourceLoadOperation alloc] initWithUrl:url];
    [self.operationQueue addOperation:operation];
}

- (void)preloadResourceWithUrl:(NSString *)url withProtocolCallBack:(SonicURLProtocolCallBack)callback
{
    SonicResourceLoadOperation *findOperation = nil;
    for (NSOperation *item in self.operationQueue.operations) {
        if ([NSStringFromClass(item.class) isEqualToString:NSStringFromClass(SonicResourceLoadOperation.class)]) {
            if ([[(SonicResourceLoadOperation*)item url] isEqualToString:url]) {
                findOperation = (SonicResourceLoadOperation *)item;
                break;
            }
        }
    }
    if (!findOperation) {
       //cache must be exist
        findOperation = [[SonicResourceLoadOperation alloc]initWithUrl:url];
        [self.operationQueue addOperation:findOperation];
    }
    [findOperation preloadDataWithProtocolCallBack:callback];
}

@end
