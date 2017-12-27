//
//  SonicSessionConfiguration.h
//  sonic
//
//  Tencent is pleased to support the open source community by making VasSonic available.
//  Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//  Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
//  in compliance with the License. You may obtain a copy of the License at
//
//  https://opensource.org/licenses/BSD-3-Clause
//
//  Unless required by applicable law or agreed to in writing, software distributed under the
//  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//  either express or implied. See the License for the specific language governing permissions
//  and limitations under the License.
//
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

/**
 * Support Cache-Control or not
 */
@property (nonatomic,assign)BOOL supportCacheControl;

/**
 * Enable Local-Server mode which will simulate response as a standar sonic server.
 */
@property (nonatomic,assign)BOOL enableLocalServer;

@end
