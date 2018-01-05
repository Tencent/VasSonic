//
//  SonicResourceLoadOperation.h
//  Sonic
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
#import "SonicSession.h"

@interface SonicResourceLoadOperation : NSOperation

/**
 * Resource sessionID
 * It is the MD5 string with the url.
 */
@property (nonatomic,readonly)NSString *sessionID;

/**
 * The resource url.
 */
@property (nonatomic,readonly)NSString *url;

/**
 * NSURLProtocol layer use this call back to get the resource data.
 */
@property (nonatomic,copy)SonicURLProtocolCallBack protocolCallBack;

/**
 * Init an operation with the resource url.
 */
- (instancetype)initWithUrl:(NSString *)aUrl;

/**
 * NSURLProtocol layer call this function to get the resource data.
 */
- (void)preloadDataWithProtocolCallBack:(SonicURLProtocolCallBack)callBack;

@end
