//
//  SonicUitil.m
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

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

#import "SonicUitil.h"
#import "SonicClient.h"
#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonDigest.h>

@implementation SonicUitil

NSString *sonicSessionID(NSString *url)
{
    if ([[SonicClient sharedClient].currentUserUniq length] > 0) {
        return stringFromMD5([NSString stringWithFormat:@"%@_%@",[SonicClient sharedClient].currentUserUniq,sonicUrl(url)]);
    }else{
        return stringFromMD5([NSString stringWithFormat:@"%@",sonicUrl(url)]);
    }
}

NSString *stringFromMD5(NSString *url)
{
    
    if(url == nil || [url length] == 0)
        return nil;
    
    const char *value = [url UTF8String];
    
    unsigned char outputBuffer[CC_MD5_DIGEST_LENGTH];
    CC_MD5(value, (CC_LONG)strlen(value), outputBuffer);
    
    NSMutableString *outputString = [[NSMutableString alloc] initWithCapacity:CC_MD5_DIGEST_LENGTH * 2];
    for(NSInteger count = 0; count < CC_MD5_DIGEST_LENGTH; count++){
        [outputString appendFormat:@"%02x",outputBuffer[count]];
    }
    
    return [outputString autorelease];
}

NSString *sonicUrl(NSString *aUrlStr)
{
    NSURL *url = [NSURL URLWithString:aUrlStr];
    if (!url) {
        return aUrlStr;
    }
    
    if([aUrlStr rangeOfString:@"sonic_"].location == NSNotFound){
        return [NSString stringWithFormat:@"%@://%@%@",url.scheme,url.host,url.path];
    }
    
    NSMutableDictionary *queryParams = queryComponents(aUrlStr);
    
    NSMutableString *sonicParamString = [NSMutableString string];
    NSMutableArray *sonicChangeParamKeys = [NSMutableArray array];
    
    NSString *remainParams = queryParams[@"sonic_remain_params"];
    if (remainParams.length > 0) {
        [sonicChangeParamKeys addObjectsFromArray:[remainParams componentsSeparatedByString:@";"]];
        [queryParams removeObjectForKey:@"sonic_remain_params"];
    }
    
    for (NSString *key in queryParams.allKeys) {
        if ([key hasPrefix:@"sonic_"]) {
            [sonicParamString appendFormat:@"%@=%@&",key,queryParams[key]];
        }
        for(NSString *sonicChangeItem in sonicChangeParamKeys){
            if ([sonicChangeItem isEqualToString:key]) {
                [sonicParamString appendFormat:@"%@=%@&",sonicChangeItem,queryParams[sonicChangeItem]];
            }
        }
    }
    
    if (sonicParamString.length > 0) {
        return [NSString stringWithFormat:@"%@://%@%@/%@",url.scheme,url.host,url.path,sonicParamString];
    }else{
        return [NSString stringWithFormat:@"%@://%@%@",url.scheme,url.host,url.path];
    }
}

NSMutableDictionary * queryComponents(NSString *aUrlStr)
{
    NSMutableDictionary *results = [NSMutableDictionary dictionary];
    
    NSURL *url = [NSURL URLWithString:aUrlStr];
    if (!url) {
        return results;
    }
    
    NSString *queryStr = url.query;
    if (queryStr && queryStr.length) {
        NSArray *components = [queryStr componentsSeparatedByString:@"&"];
        for (NSString *component in components) {
            NSRange range = [component rangeOfString:@"="];
            NSString *key, *value;
            if (range.location == NSNotFound) {
                key = component;
                value = @"";
            }
            else {
                key = [component substringToIndex:range.location];
                value = [component substringFromIndex:range.location + 1];
            }
            if (value == nil) value = @"";
            if (key && key.length && value) {
                [results setObject:value forKey:key];
            }
        }
    }
    
    return results;
}

void dispatchToMain (dispatch_block_t block)
{
    if ([NSThread isMainThread]) {
        block();
    }else{
        dispatch_async(dispatch_get_main_queue(), block);
    }
}

NSString * getDataSha1(NSData *data)
{
    if (!data) {
        return nil;
    }
    uint8_t digest[CC_SHA1_DIGEST_LENGTH];
    CC_SHA1(data.bytes, (CC_LONG)data.length, digest);
    NSMutableString* output = [NSMutableString stringWithCapacity:CC_SHA1_DIGEST_LENGTH * 2];
    for(int i = 0; i < CC_SHA1_DIGEST_LENGTH; i++)
    {
        [output appendFormat:@"%02x", digest[i]];
    }
    
    return output;
}

NSURLRequest *sonicWebRequest(NSURLRequest *originRequest)
{
    NSMutableURLRequest *request = [[originRequest mutableCopy]autorelease];
    [request setValue:SonicHeaderValueWebviewLoad forHTTPHeaderField:SonicHeaderKeyLoadType];
    [request setValue:sonicSessionID(request.URL.absoluteString) forHTTPHeaderField:SonicHeaderKeySessionID];
    return request;
}

@end
