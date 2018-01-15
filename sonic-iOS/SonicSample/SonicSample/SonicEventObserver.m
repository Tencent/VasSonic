//
//  SonicEventObserver.m
//  SonicSample
//
//  Created by zyvincenthu on 2018/1/15.
//  Copyright © 2018年 Tencent. All rights reserved.
//

#import "SonicEventObserver.h"

@implementation SonicEventObserver

+ (NSString *)eventTypeToString:(SonicStatisticsEvent)event
{
    NSDictionary *relation = @{
                               @(SonicStatisticsEvent_SubResourceDidFail):@"SubResourceDidFail",
                               @(SonicStatisticsEvent_SubResourceLoadLocalCache):@"SubResourceLoadLocalCache",
                               @(SonicStatisticsEvent_SubResourceDidSave):@"SubResourceDidSave",
                               @(SonicStatisticsEvent_TrimCache):@"TrimCache",
                               @(SonicStatisticsEvent_SessionDestroy):@"SessionDestroy",
                               @(SonicStatisticsEvent_SessionRefresh):@"SessionRefresh",
                               @(SonicStatisticsEvent_SessionHitCache):@"SessionHitCache",
                               @(SonicStatisticsEvent_SessionFirstLoad):@"SessionFirstLoad",
                               @(SonicStatisticsEvent_SessionHttpError):@"SessionHttpError",
                               @(SonicStatisticsEvent_SessionRecvResponse):@"SessionRecvResponse",
                               @(SonicStatisticsEvent_SessionDataUpdated):@"SessionDataUpdated",
                               @(SonicStatisticsEvent_SessionDidLoadLocalCache):@"SessionDidLoadLocalCache",
                               @(SonicStatisticsEvent_SessionUnavilable):@"SessionUnavilable",
                               @(SonicStatisticsEvent_SessionTemplateChanged):@"SessionTemplateChanged",
                               };
    return relation[@(event)];
}

- (void)handleEvent:(SonicStatisticsEvent)event withEventInfo:(NSDictionary *)info
{
    NSString *eventString = [SonicEventObserver eventTypeToString:event];
    NSLog(@"event :%@ info:%@",eventString,info.debugDescription);
}

@end
