//
//  SonicSession.h
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
#import "SonicConstants.h"
#import "SonicSessionConfiguration.h"

@class SonicResourceLoader;

/**
 * SonicSession use this callback to transfer network data to SonicURLProtocol.
 * The parameters of the dictionary is:
 * @{
 *    kSonicProtocolAction:@(SonicURLProtocolActionRecvResponse),
 *    kSonicProtocolData:NSHTTPURLResponse
 *  }
 */
typedef void(^SonicURLProtocolCallBack) (NSDictionary *param);

/**
 * SonicSession use this callback to transfer status data to webview.
 * @{
 *   code:304,
 *   srcCode:200,
 *   result:{},
 *   local_refresh_time:,
 *  }
 */
typedef void(^SonicWebviewCallBack)  (NSDictionary *result);

/**
 * SonicSession use this callback to notify SonicEngine when it finished.
 */
typedef void(^SonicSessionCompleteCallback) (NSString *sessionID);

/**
 * SonicEngine will create one SonicSession for each URL request.
 * SonicSession will get network data from SonicConnection and split HTML data to template and dynamic data.
 */
@interface SonicSession : NSObject<SonicServerDelegate>

/** If there is no memory cache and file cache exist */
@property (nonatomic,assign)BOOL isFirstLoad;

/** Url for current session */
@property (nonatomic ,copy)NSString *url;

/** Generated from MD5 of URL. */
@property (nonatomic,readonly)NSString *sessionID;

/** Generated from local dynamic data and sever dynamic data. */
@property (nonatomic,readonly)NSDictionary *diffData;

/** SonicSession use this callback to transfer network data to SonicURLProtocol. */
@property (nonatomic,copy)SonicURLProtocolCallBack protocolCallBack;

/** Notify the webView holder what happened during the connection.*/
@property (nonatomic,assign)id<SonicSessionDelegate> delegate;

/** A NSString value to identify SonicSessionDelegate*/
@property (nonatomic,copy)NSString *delegateId;

/** SonicSession use this callback to notify SonicEngine when it finished. */
@property (nonatomic,copy)SonicSessionCompleteCallback completionCallback;

/** SonicSession use this callback to transfer status data to webview. */
@property (nonatomic,copy)SonicWebviewCallBack updateCallBack;

/** Check if all data did finish updated*/
@property (nonatomic,assign)BOOL isDataFetchFinished;

/** Sonic status code. */
@property (nonatomic,assign)SonicStatusCode sonicStatusCode;

/** SonicSession use this callback to transfer status data to webview. */
@property (nonatomic,copy)SonicWebviewCallBack webviewCallBack;

/** Set this property to connect server directly with ip. Without this property sonic will connect to server with domain normally. */
@property (nonatomic,copy)NSString *serverIP;

/** Return the session configuration */
@property (nonatomic,readonly)SonicSessionConfiguration *configuration;

@property (nonatomic,readonly)SonicResourceLoader *resourceLoader;

/**
 * Register a SonicConnection Class to provide network data.
 */
+ (BOOL)registerSonicConnection:(Class)connectionClass;

/**
 * Unregister the SonicConnection.
 */
+ (void)unregisterSonicConnection:(Class)connectionClass;

/**
 * Queue to handle network data.
 */
+ (NSOperationQueue *)sonicSessionQueue;

/**
 * Execute block in sonic session queue.
 * Return block operation hash string
 */
NSString * dispatchToSonicSessionQueue(dispatch_block_t block);

/**
 * Use an url and webDelegate to create an session
 * The webDelegate can be nil value
 */
- (instancetype)initWithUrl:(NSString *)aUrl withWebDelegate:(id<SonicSessionDelegate>)aWebDelegate Configuration:(SonicSessionConfiguration *)aConfiguration;

/**
 * Start request.
 */
- (void)start;

/**
 * Cancel request
 * You must invoke this method before dealloc the session,
 * beacause it has been retained by the NSURLSession, or it will be leaked.
 */
- (void)cancel;

/**
 * update current session by send request
 */
- (BOOL)update;

/**
 * It provide the network data by the protocolCallBack block
 */
- (void)preloadRequestActionsWithProtocolCallBack:(SonicURLProtocolCallBack)protocolCallBack;

/**
 * It provide the session state result by the resultBlock
 */
- (void)getResultWithCallBack:(SonicWebviewCallBack)webviewCallback;

@end
