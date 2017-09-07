//
//  SonicSessionConfiguration.h
//  Sonic
//
//  Created by ZYVincent on 17/9/7.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface SonicSessionConfiguration : NSObject

/**
 * Pass custom request headers
 */
@property (nonatomic,retain)NSDictionary *customRequestHeaders;

/**
 * Pass custom response headers
 */
@property (nonatomic,retain)NSDictionary *customResponseHeaders;


@end
