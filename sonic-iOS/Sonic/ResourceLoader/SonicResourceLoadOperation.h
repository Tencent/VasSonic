//
//  SonicResourceLoadOperation.h
//  Sonic
//
//  Created by zyvincenthu on 2017/12/18.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "sonicSession.h"

typedef void(^SonicResourceOperationCompleteBlock) (BOOL state,NSError *errorMsg);

@interface SonicResourceLoadOperation : NSOperation

@property (nonatomic,readonly)NSString *sessionID;

@property (nonatomic,readonly)NSString *url;

@property (nonatomic,readonly)NSString *sha1;

@property (nonatomic,copy)SonicURLProtocolCallBack protocolCallBack;

@property (nonatomic,readonly)BOOL hasStartNetwork;

@property (nonatomic,readonly)BOOL isCacheExist;

@property (nonatomic,copy)SonicResourceOperationCompleteBlock completeBlock;

- (instancetype)initWithUrl:(NSString *)aUrl;

- (void)preloadDataWithProtocolCallBack:(SonicURLProtocolCallBack)callBack;

@end
