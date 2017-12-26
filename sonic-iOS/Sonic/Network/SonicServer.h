//
//  SonicServer.h
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
#import "SonicConnection.h"
#import "SonicProtocol.h"

/**
 * SonicServer is a middle-layer or called proxy-layer which uses to pretreatment with connection request and response data,
 * SonicServer can be a local sonic-supported server if session enable local-server mode.
 */
@interface SonicServer : NSObject

@property (nonatomic,readonly)NSMutableURLRequest *request;

@property (nonatomic,readonly)NSHTTPURLResponse *response;

@property (nonatomic,readonly)NSMutableData *responseData;

@property (nonatomic,readonly)NSError *error;

@property (nonatomic,readonly)BOOL isInLocalServerMode;

@property (nonatomic,readonly)BOOL isRuning;

/**
 * Register a SonicConnection Class to provide network data.
 */
+ (BOOL)registerSonicConnection:(Class)connectionClass;

/**
 * Unregister the SonicConnection.
 */
+ (void)unregisterSonicConnection:(Class)connectionClass;

/**
 * Which connection class can intercept the request.
 */
+ (Class)connectionClassForRequest:(NSURLRequest *)aRequest;

/**
 * SonicServer init with params
 * @param url The target url which will request
 * @param delegate The delegate which will receiver server operation notifies
 * @param queue The queue which delegate operation will be called.
 */
- (instancetype)initWithUrl:(NSString *)url delegate:(id<SonicServerDelegate>) delegate delegateQueue:(NSOperationQueue *) queue;

/**
 * Set the specified field to the request header.
 */
- (void)setRequestHeaderFields:(NSDictionary *)headers;

/**
 * Add the specified field to the request header.
 */
- (void)addRequestHeaderFields:(NSDictionary *)headers;

/**
 * Set the specified field to the response header.
 */
- (void)setResponseHeaderFields:(NSDictionary *)headers;

/**
 * Get response header specified field for key.
 */
- (NSString *)responseHeaderForKey:(NSString *)aKey;

/**
 * Returns whether the Sonic response is currently valid.
 */
- (BOOL)isSonicResponse;

/**
 * Enable the local server mode.
 */
- (void)enableLocalServer:(BOOL)enable;

/**
 * Request data over the network.
 */
- (void)start;

/**
 * Cancel the request.
 */
- (void)stop;

/**
 * Get the sonic cache from the item
 */
- (NSDictionary *)sonicItemForCache;

@end
