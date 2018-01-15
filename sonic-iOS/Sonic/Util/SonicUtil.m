//
//  SonicUtil.m
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

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

#import "SonicUtil.h"
#import "SonicEngine.h"
#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonDigest.h>

@implementation SonicUtil

NSString *sonicSessionID(NSString *url)
{
    NSString* userAccount = [SonicEngine sharedEngine].currentUserAccount;
    if ([userAccount length] > 0) {
        return stringFromMD5([NSString stringWithFormat:@"%@_%@",userAccount,[SonicUtil sonicUrl:url]]);
    }else{
        return stringFromMD5([NSString stringWithFormat:@"%@",[SonicUtil sonicUrl:url]]);
    }
}

NSString *resourceSessionID(NSString *url)
{
    if (url.length == 0) {
        return @"";
    }
    return stringFromMD5(url);
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

+ (NSString *)sonicUrl:(NSString *)aUrlStr
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

NSString * dispatchToMain (dispatch_block_t block)
{
    NSBlockOperation *blockOp = [NSBlockOperation blockOperationWithBlock:block];
    [[NSOperationQueue mainQueue] addOperation:blockOp];
    return [NSString stringWithFormat:@"%ld",(unsigned long)blockOp.hash];
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

+ (NSURLRequest *)sonicWebRequestWithSession:(SonicSession* )session withOrigin:(NSURLRequest *)originRequest
{
    NSMutableURLRequest *request = [[originRequest mutableCopy]autorelease];
    [request setValue:SonicHeaderValueWebviewLoad forHTTPHeaderField:SonicHeaderKeyLoadType];
    if (session) {
        [request setValue:session.sessionID forHTTPHeaderField:SonicHeaderKeySessionID];
        if (session.delegateId.length != 0) {
            [request setValue:session.delegateId forHTTPHeaderField:SonicHeaderKeyDelegateId];
        }
    }
    return request;
}

+ (NSDictionary *)splitTemplateAndDataFromHtmlData:(NSString *)html
{
    if (html.length == 0) {
        return [NSDictionary dictionary];
    }
    
    //using sonicdiff tag to split the HTML to template and dynamic data.
    NSError *error = nil;
    NSRegularExpression *reg = [NSRegularExpression regularExpressionWithPattern:@"<!--sonicdiff-?(\\w*)-->([\\s\\S]+?)<!--sonicdiff-?(\\w*)-end-->" options:NSRegularExpressionCaseInsensitive error:&error];
    if (!error) {
        NSMutableString *templateString = nil;
        NSMutableDictionary *data = [NSMutableDictionary dictionary];
        //create dynamic data
        NSArray *metchs = [reg matchesInString:html options:NSMatchingReportCompletion range:NSMakeRange(0, html.length)];
        [metchs enumerateObjectsUsingBlock:^(NSTextCheckingResult *obj, NSUInteger idx, BOOL * _Nonnull stop) {
            NSString *matchStr = [html substringWithRange:obj.range];
            NSArray *seprateArr = [matchStr componentsSeparatedByString:@"<!--sonicdiff-"];
            NSString *itemName = [[[seprateArr lastObject]componentsSeparatedByString:@"-end-->"]firstObject];
            NSString *formatKey = [NSString stringWithFormat:@"{%@}",itemName];
            [data setObject:matchStr forKey:formatKey];
        }];
        
        //create template
        templateString = [NSMutableString stringWithString:html];
        [data enumerateKeysAndObjectsUsingBlock:^(NSString *key, NSString *value, BOOL * _Nonnull stop) {
            [templateString replaceOccurrencesOfString:value withString:key options:NSCaseInsensitiveSearch range:NSMakeRange(0, templateString.length)];
        }];
        
       return @{kSonicDataFieldName:data,kSonicTemplateFieldName:templateString};
    }
    return @{kSonicDataFieldName:[NSDictionary dictionary],kSonicTemplateFieldName:html};
}

+ (NSDictionary *)mergeDynamicData:(NSDictionary *)updateDict withOriginData:(NSMutableDictionary *)existData
{
    NSMutableDictionary *diffData = [NSMutableDictionary dictionary];
    
    //get the diff data between server updateDict and local existData
    for (NSString *key in updateDict.allKeys) {
        NSString *updateValue = [updateDict objectForKey:key];
        if ([existData.allKeys containsObject:key]) {
            NSString *existValue = [existData objectForKey:key];
            
            if (![updateValue isEqualToString:existValue]) {
                [diffData setObject:updateValue forKey:key];
                [existData setObject:updateValue forKey:key];
            }
        }
    }
    
    return @{kSonicDiffFieldName:diffData};
}

+ (NSDictionary *)protocolActionItem:(SonicURLProtocolAction)action param:(NSObject *)param
{
    if (param == nil) {
        param = @"";
    }
    return @{kSonicProtocolAction:@(action),kSonicProtocolData:param};
}

unsigned long long currentTimeStamp(void)
{
    return (unsigned long long)([[NSDate date] timeIntervalSince1970]);
}

@end
