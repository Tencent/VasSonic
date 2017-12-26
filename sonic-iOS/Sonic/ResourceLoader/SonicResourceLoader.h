//
//  SonicResourceLoader.h
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

#define kSonicResourceVersion @"sonic_ts"

@interface SonicResourceLoader : NSObject

/**
 * Quit all requests.
 */
- (void)cancelAll;

/**
 * Is it possible to block specific resource links by url.
 */
- (BOOL)canInterceptResourceWithUrl:(NSString *)url;

/**
 * Load the specified resource link from array.
 */
- (void)loadResourceLinks:(NSArray *)links;

/**
 * NSURLProtocol layer to read the resource link data.
 */
- (void)preloadResourceWithUrl:(NSString *)url withProtocolCallBack:(SonicURLProtocolCallBack)callback;

@end
