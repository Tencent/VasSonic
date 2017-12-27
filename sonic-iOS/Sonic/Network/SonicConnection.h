//
//  SonicConnection.h
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
#import "SonicProtocol.h"
#import "SonicConnection.h"

/**
 * Subclass this class to request the network data from custom connection.
 */

@interface SonicConnection : NSObject

/** Current request. */
@property (nonatomic,retain)NSURLRequest *request;

/** Use this protocal to transfer data to sonic session. */
@property (nonatomic,assign)id<SonicConnectionDelegate> delegate;

/** Queue for connection delegate operation. */
@property (nonatomic,retain) NSOperationQueue* delegateQueue;

/** HTTPSession redirection support, Default is NO */
@property (nonatomic,assign) BOOL supportHTTPRedirection;

/**
 * Check if this request class can use SonicConnection to load
 
 * @param request the request passed by the SonicSession
 */
+ (BOOL)canInitWithRequest:(NSURLRequest *)request;

/**
 * SonicSession will pass the request to the connection
 
 * @param aRequest the request passed by the SonicSession
* @param queue The queue which delegate operation will be called.
 */
- (instancetype)initWithRequest:(NSURLRequest *)aRequest delegate:(id<SonicConnectionDelegate>)delegate delegateQueue:(NSOperationQueue *)queue;

/**
 * Start request
 */
- (void)startLoading;

/** 
 * Cancel request
 */
- (void)stopLoading;

@end
