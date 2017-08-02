//
//  SonicCacheItem.m
//  sonic
//
//  Created by zyvincenthu on 17/4/6.
//  Copyright © 2017年 Tencent. All rights reserved.
//

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
