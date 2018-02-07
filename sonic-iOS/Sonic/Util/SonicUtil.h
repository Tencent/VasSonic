//
//  SonicUtil.h
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
#import "SonicConstants.h"
#import "SonicSession.h"
#import "SonicEventStatistics.h"

@interface SonicUtil : NSObject

/**
 * Set sonic tag header into originRequest headers
 */
+ (NSURLRequest *)sonicWebRequestWithSession:(SonicSession* )session withOrigin:(NSURLRequest *)originRequest;

/**
 * Using MD5 to encode the URL to session ID;
 */
NSString *sonicSessionID(NSString *url);

/**
 * Using MD5 to encode the URL to session ID;
 */
NSString *resourceSessionID(NSString *url);

/**
 * Create sonic path with URL
 */
+ (NSString *)sonicUrl:(NSString *)url;

/**
 * Dispatch block to main thread.
 * Return block operation hash string
 */
NSString * dispatchToMain (dispatch_block_t block);

/**
 * Get SHA1 value from data.
 */
NSString * getDataSha1(NSData *data);

/**
 * @brief Split the HTML to dynamic data and template string.
 
 * @param html the HTML document downloaded by sonic session.
 */
+(NSDictionary *)splitTemplateAndDataFromHtmlData:(NSString *)html;

/**
 * @brief Merge the server data and local dynamic data to create new HTML and the difference data.
 * Get the difference between the "updateDict" and "existData"

 * @result Return the difference data
 */
+ (NSDictionary *)mergeDynamicData:(NSDictionary *)updateDict withOriginData:(NSMutableDictionary *)existData;

/**
 * Create an item by specifying the action type and parameters to return to the NSURLProtocol.
 */
+ (NSDictionary *)protocolActionItem:(SonicURLProtocolAction)action param:(NSObject *)param;

/**
 * Translate the URL's query into a dictionary.
 */
NSMutableDictionary * queryComponents(NSString *aUrlStr);

/**
 * Get the current timestamp.
 */
unsigned long long currentTimeStamp(void);

@end
