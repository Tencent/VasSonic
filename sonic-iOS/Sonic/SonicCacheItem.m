//
//  SonicCacheItem.m
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

#import "SonicCacheItem.h"
#import "SonicCache.h"
#import "SonicConstants.h"
#import "SonicUitil.h"


@implementation SonicCacheItem

- (instancetype)init NS_UNAVAILABLE
{
    return nil;
}

- (instancetype)initWithSessionID:(NSString *)aSessionID
{
    if (self = [super init]) {
        
        _sessionID = [aSessionID copy];
                
    }
    return self;
}

- (void)dealloc
{
    if (_sessionID) {
        [_sessionID release];
        _sessionID = nil;
    }
    if (_cacheResponseHeaders) {
        [_cacheResponseHeaders release];
        _cacheResponseHeaders = nil;
    }
    if (self.config) {
        self.config = nil;
    }
    if (self.htmlData) {
        self.htmlData = nil;
    }
    if (self.templateString) {
        self.templateString = nil;
    }
    if (self.diffData) {
        self.diffData = nil;
    }
    if (self.dynamicData) {
        self.dynamicData = nil;
    }
    [super dealloc];
}

- (BOOL)hasLocalCache
{
    return self.htmlData.length > 0? NO:YES;
}

- (void)setConfig:(NSDictionary *)config
{
    if (_config) {
        [_config release];
        _config = nil;
    }
    _config = [config retain];
    
    NSString *csp = _config[kSonicCSP];
    if (csp.length > 0) {
        if (_cacheResponseHeaders) {
            [_cacheResponseHeaders release];
            _cacheResponseHeaders = nil;
        }
        _cacheResponseHeaders = [@{SonicHeaderKeyCSPHeader:csp} retain];
    }
}

- (NSString *)lastRefreshTime
{
    if (!self.config) {
        return nil;
    }
    return self.config[kSonicLocalRefreshTime];
}

@end
