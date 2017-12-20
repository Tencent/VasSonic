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

- (BOOL)canInterceptResourceWithUrl:(NSString *)url
{
    NSLog(@"resource load canInterceptUrl:%@ operations:%@",url,self.operationQueue.operations.debugDescription);
    BOOL findOperation = NO;
    if (self.operationQueue.operations.count > 0) {
        for (NSOperation *item in self.operationQueue.operations) {
            NSString *resourceOperationClass = NSStringFromClass(SonicResourceLoadOperation.class);
            if ([NSStringFromClass(item.class) isEqualToString:resourceOperationClass]) {
                if ([[(SonicResourceLoadOperation*)item url] isEqualToString:url]) {
                    findOperation = YES;
                    break;
                }
            }
        }
    }
    if (findOperation) {
        return findOperation;
    }
    
    //is cache here
    SonicResourceLoadOperation *operation = [[SonicResourceLoadOperation alloc]initWithUrl:url];
    [self.operationQueue addOperation:operation];
    
    return operation.isCacheExist;
}

- (void)loadResourceWithUrl:(NSString *)url
{
    SonicResourceLoadOperation *operation = [[SonicResourceLoadOperation alloc] initWithUrl:url];
    [self.operationQueue addOperation:operation];
}

- (void)preloadResourceWithUrl:(NSString *)url withProtocolCallBack:(SonicURLProtocolCallBack)callback
{
    SonicResourceLoadOperation *findOperation = nil;
    for (SonicResourceLoadOperation *item in self.operationQueue.operations) {
        if ([item.url isEqualToString:url]) {
            findOperation = item;
            break;
        }
    }
    if (findOperation) {
        [findOperation preloadDataWithProtocolCallBack:callback];
    }
}

@end
