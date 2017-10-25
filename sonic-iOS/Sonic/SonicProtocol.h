//
//  SonicProtocol.h
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

/**
 * @brief Use this protocal to trasfer data to sonic session, when you provide custom SonicConnection.
 */

@class SonicConnection;

@protocol SonicConnectionDelegate <NSObject>

@required

/**
 * @brief Notify when the network connection did recieve response.
 */
- (void)connection:(SonicConnection *)connection didReceiveResponse:(NSHTTPURLResponse *)response;

/**
 * @brief Notify when the network connection did recieve data.
 */
- (void)connection:(SonicConnection *)connection didReceiveData:(NSData *)data;

/**
 * @brief Call when the network connection did fail.
 */
- (void)connection:(SonicConnection *)connection didCompleteWithError:(NSError *)error;

/**
 * @brief Call when the network connection did success.
 */
- (void)connectionDidCompleteWithoutError:(SonicConnection *)connection;


@end

@class SonicServer;

@protocol SonicServerDelegate <NSObject>

@required

/**
 * @brief Call when the server did recieve response.
 */
- (void)server:(SonicServer *)server didRecieveResponse:(NSHTTPURLResponse *)response;

/**
 * @brief Call when the server did receive data.
 */
- (void)server:(SonicServer *)server didReceiveData:(NSData *)data;

/**
 * @brief Call when the server did fail.
 */
- (void)server:(SonicServer *)server didCompleteWithError:(NSError *)error;

/**
 * @brief Call when the the server did success.
 */
- (void)serverDidCompleteWithoutError:(SonicServer *)server;

@end

@class SonicSession;

/**
 * @brief Notify the webView holder what happened during the connection.
 */
@protocol SonicSessionDelegate <NSObject>

@required

/**
 * @brief Sonic session call the delegate to reload request
 */
- (void)session:(SonicSession *)session requireWebViewReload:(NSURLRequest *)request;

@optional

/**
 * @brief Sonic request will be sent, you can do some custom actions, e.g. add custom header fields, set cookie etc.
 */
- (void)sessionWillRequest:(SonicSession *)session;

@end
