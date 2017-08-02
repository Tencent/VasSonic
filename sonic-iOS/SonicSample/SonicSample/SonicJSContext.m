//
//  SonicJSContext.m
//  SonicSample
//
//  Created by zyvincenthu on 17/6/5.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicJSContext.h"
#import <Sonic/Sonic.h>

@implementation SonicJSContext

- (void)getDiffData:(NSDictionary *)option withCallBack:(JSValue *)jscallback
{
    JSValue *callback = self.owner.jscontext.globalObject;
    
    [[SonicClient sharedClient] sonicUpdateDiffDataByWebDelegate:self.owner completion:^(NSDictionary *result) {
       
        if (result) {
            
            NSData *json = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:nil];
            NSString *jsonStr = [[NSString alloc]initWithData:json encoding:NSUTF8StringEncoding];
            
            [callback invokeMethod:@"getDiffDataCallback" withArguments:@[jsonStr]];
        }
        
    }];
}

- (NSString *)getPerformance:(NSDictionary *)option withCallBack:(JSValue *)jscallback
{
    NSDictionary *result = @{
                             @"clickTime":@(self.owner.clickTime),
                             };
    NSData *json = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:nil];
    NSString *jsonStr = [[NSString alloc]initWithData:json encoding:NSUTF8StringEncoding];
    
    return jsonStr;
}

@end
