//
//  SonicProtocol.h
//  sonic
//
//  Tencent is pleased to support the open source community by making VasSonic available.
//  Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//  Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
//  in compliance with the License. You may obtain a copy of the License at
//  https://opensource.org/licenses/BSD-3-Clause
//  Unless required by applicable law or agreed to in writing, software distributed under the
//  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//  either express or implied. See the License for the specific language governing permissions
//  and limitations under the License.
//
//  Created by zyvincenthu on 17/4/12.
//  Review  by ianhuang
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>

/**
 * Use this protocal to trasfer data to sonic session, when you provide custom SonicConnection.
 */
@class SonicSession;
@protocol SonicSessionProtocol <NSObject>

@required

/**
 * Call when the network connection did recieve response.
 */
- (void)session:(SonicSession *)session didRecieveResponse:(NSHTTPURLResponse *)response;

/**
 * Call when the network connection did load data.
 */
- (void)session:(SonicSession *)session didLoadData:(NSData *)data;

/**
 * Call when the network connection did fail.
 */
- (void)session:(SonicSession *)session didFaild:(NSError *)error;

/**
 * Call when the network connection did finish load.
 */
- (void)sessionDidFinish:(SonicSession *)session;

@end

/**
 * Notify the webView holder what happened during the connection.
 */
@protocol SonicSessionDelegate <NSObject>

@required

/**
 * Sonic session call the delegate to reload request
 */
- (void)session:(SonicSession *)session requireWebViewReload:(NSURLRequest *)request;

@optional

/**
 * Sonic request will be sent, you can do some custom actions, e.g. add custom header fields, set cookie etc.
 */
- (void)sessionWillRequest:(SonicSession *)session;

@end
