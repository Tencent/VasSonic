//
//  SonicSessionConfiguration.m
//  Sonic
//
//  Created by ZYVincent on 17/9/7.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicSessionConfiguration.h"

@implementation SonicSessionConfiguration
- (void)dealloc
{
    self.customRequestHeaders = nil;
    self.customResponseHeaders = nil;
    [super dealloc];
}
@end
