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
//  Copyright © 2017 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SonicSession.h"
#import "SonicConnection.h"

NS_ASSUME_NONNULL_BEGIN
/**
 * Subclass this class to request the network data from custom connection.
 */

@interface SonicConnection : NSObject

/** Use this protocal to transfer data to sonic session. */
@property (nonatomic,assign,nullable)id<SonicSessionProtocol> session;

/** Current request. */
@property (nonatomic,readonly)NSURLRequest *request;

/**
 * Check if this request class can use SonicConnection to load
 
 * @param request the request passed by the SonicSession
 */
+ (BOOL)canInitWithRequest:(NSURLRequest *)request;

/**
 * SonicSession will pass the request to the connection
 
 * @param aRequest the request passed by the SonicSession
 */
- (instancetype)initWithRequest:(NSURLRequest *)aRequest;

/**
 * Start request
 */
- (void)startLoading;

/** 
 * Cancel request
 */
- (void)stopLoading;

@end
NS_ASSUME_NONNULL_END
