//
//  SonicCache.m
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

#import "SonicCache.h"
#import "SonicEngine.h"
#import "SonicUitil.h"
#import <UIKit/UIKit.h>
#import <CommonCrypto/CommonDigest.h>
#import "SonicDatabase.h"

typedef NS_ENUM(NSUInteger, SonicCacheType) {
    /*
     * template
     */
    SonicCacheTypeTemplate,
    /*
     * html
     */
    SonicCacheTypeHtml,
    /*
     * dynamic data
     */
    SonicCacheTypeData,
    /*
     * config
     */
    SonicCacheTypeConfig,
    /*
     * response header
     */
    SonicCacheTypeResponseHeader,
};

@interface SonicCache ()

@property (nonatomic,readonly)NSString *rootCachePath;

/*
 * memory cache item manage lock
 */
@property (nonatomic,retain)NSRecursiveLock *lock;
/*
 * cache items
 */
@property (nonatomic,retain)NSMutableDictionary *memoryCache;
/*
 * the max cache item count in memory
 */
@property (nonatomic,assign)NSInteger            maxCacheCount;
/*
 * save the key recently used
 */
@property (nonatomic,retain)NSMutableArray      *recentlyUsedKey;
/*
 * save the server disable/enable sonic request timestamp
 */
@property (nonatomic,retain)NSMutableDictionary *offlineCacheTimeCfg;

@property (nonatomic,retain)SonicDatabase *database;

@end

@implementation SonicCache

+ (SonicCache *)shareCache
{
    static SonicCache *_cacheInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _cacheInstance = [[self alloc]init];
    });
    return _cacheInstance;
}

- (instancetype)init
{
    if (self = [super init]) {
        [self setupInit];
    }
    return self;
}

+ (NSOperationQueue *)fileQueue
{
    static NSOperationQueue *_fileQueue = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _fileQueue = [NSOperationQueue new];
        _fileQueue.qualityOfService = NSQualityOfServiceDefault;
        _fileQueue.name = @"Sonic.File.Cache";
        _fileQueue.maxConcurrentOperationCount = 1;
    });
    return _fileQueue;
}

- (void)setupInit
{
    self.maxCacheCount = [SonicEngine sharedEngine].configuration.maxMemroyCacheItemCount;
    self.lock = [NSRecursiveLock new];
    self.memoryCache = [NSMutableDictionary dictionaryWithCapacity:self.maxCacheCount];
    self.recentlyUsedKey = [NSMutableArray arrayWithCapacity:self.maxCacheCount];
    
    //setup cache dir
    [self setupCacheDirectory];
    
    //read server disable sonic request timestamps
    [self setupCacheOfflineTimeCfgDict];
    
    //setup database
    NSString *dbPath = [self.rootCachePath stringByAppendingPathComponent:SonicCacheDatabase];
    self.database = [[SonicDatabase alloc]initWithPath:dbPath];
    
    //release the memory cache when did recieved memory warning
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(memoryWarningClearCache) name:UIApplicationDidReceiveMemoryWarningNotification object:nil];
}

- (void)memoryWarningClearCache
{
    [self clearAllCache];
}

- (void)clearAllCache
{
    //we need clear or setup file in file queue
    dealInFileQueue(^{
        [SonicFileManager removeItemAtPath:_rootCachePath error:nil];
        [self setupCacheDirectory];
    });
    
    //we need clear or create memory cache in sonic queue
    dispatchToSonicSessionQueue(^{
        [self.lock lock];
        [self.memoryCache removeAllObjects];
        [self.recentlyUsedKey removeAllObjects];
        [self.lock unlock];
    });
}

- (void)removeCacheBySessionID:(NSString *)sessionID
{
    //we need clear or setup file in file queue
    dealInFileQueue(^{
        NSString *fileDir = [self sessionSubCacheDir:sessionID];
        [SonicFileManager removeItemAtPath:fileDir error:nil];
        [self.database clearWithSessionID:sessionID];
    });
    
    //we need clear or create memory cache in sonic queue
    dispatchToSonicSessionQueue(^{
        [self.lock lock];
        [self.memoryCache removeObjectForKey:sessionID];
        [self.recentlyUsedKey removeObject:sessionID];
        [self.lock unlock];
    });
}

- (BOOL)isFirstLoad:(NSString *)sessionID
{
    if (sessionID.length == 0) {
        return YES;
    }
    
    SonicCacheItem *session = [self cacheForSession:sessionID];
    
    return session.hasLocalCache? NO:YES;
}

- (BOOL)isServerDisableSonic:(NSString *)sessionID
{
    NSNumber *disableStartTime = [self.offlineCacheTimeCfg objectForKey:sessionID];
    if (!disableStartTime) {
        return NO;
    }
    
    NSTimeInterval lastTime = [disableStartTime doubleValue];
    NSTimeInterval timeNow = [[NSDate date] timeIntervalSince1970];
    
    return timeNow - lastTime < [SonicEngine sharedEngine].configuration.cacheOfflineDisableTime;
}

- (void)setupCacheOfflineTimeCfgDict
{
    NSString *cfgPath = [_rootCachePath stringByAppendingPathComponent:SonicCacheOfflineDisableList];
    
    if (![SonicFileManager fileExistsAtPath:cfgPath]) {
        self.offlineCacheTimeCfg = [NSMutableDictionary dictionary];
    }else{
        NSDictionary *cacheDict = [NSDictionary dictionaryWithContentsOfFile:cfgPath];
        self.offlineCacheTimeCfg = [NSMutableDictionary dictionaryWithDictionary:cacheDict];
    }
}

- (void)saveServerDisableSonicTimeNow:(NSString *)sessionID
{
    [self removeCacheBySessionID:sessionID];
    
    NSNumber *timeNow = @([[NSDate date] timeIntervalSince1970]);
    
    [self.offlineCacheTimeCfg setObject:timeNow forKey:sessionID];
    
    NSString *cfgPath = [_rootCachePath stringByAppendingPathComponent:SonicCacheOfflineDisableList];

    dealInFileQueue(^{
        [self.offlineCacheTimeCfg writeToFile:cfgPath atomically:YES];
    });
}

- (void)removeServerDisableSonic:(NSString *)sessionID
{
    if (![self.offlineCacheTimeCfg objectForKey:sessionID]) {
        return;
    }
    
    [self.offlineCacheTimeCfg removeObjectForKey:sessionID];
    
    NSString *cfgPath = [_rootCachePath stringByAppendingPathComponent:SonicCacheOfflineDisableList];

    dealInFileQueue(^{
        [self.offlineCacheTimeCfg writeToFile:cfgPath atomically:YES];
    });
}

#pragma mark - Memory Cache

- (SonicCacheItem *)cacheForSession:(NSString *)sessionID
{
    SonicCacheItem *cacheItem = nil;
    
    [self.lock lock];
    
    cacheItem = self.memoryCache[sessionID];
    
    if (!cacheItem) {
        cacheItem = [[SonicCacheItem alloc] initWithSessionID:sessionID];
        [self memoryCacheItem:cacheItem];
        [self setupCacheItemFromFile:cacheItem];
        [cacheItem release];
    }
    
    [self.lock unlock];
    
    return cacheItem;
}

- (void)memoryCacheItem:(SonicCacheItem *)cacheItem
{
    [self.memoryCache setObject:cacheItem forKey:cacheItem.sessionID];

    NSUInteger index = [self.recentlyUsedKey indexOfObject:cacheItem.sessionID];
    
    if (index != NSNotFound) {
        [self.recentlyUsedKey removeObjectAtIndex:index];
    }
    
    [self.recentlyUsedKey insertObject:cacheItem.sessionID atIndex:0];
    
    if (self.recentlyUsedKey.count > self.maxCacheCount) {
        NSString *lastUsedKey = [self.recentlyUsedKey lastObject];
        [self.memoryCache removeObjectForKey:lastUsedKey];
        [self.recentlyUsedKey removeObject:lastUsedKey];
    }
}

#pragma mark - 接口

- (SonicCacheItem *)updateWithJsonData:(NSData *)jsonData
                        withHtmlString:(NSString *)htmlString
                   withResponseHeaders:(NSDictionary *)headers
                         withUrl:(NSString *)url
{
    NSString *sessionID = sonicSessionID(url);

    NSError *err = nil;
    NSDictionary *dataDict = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingAllowFragments error:&err];
    if (err) {
        return nil;
    }
    
    SonicCacheItem *cacheItem = [self cacheForSession:sessionID];
    
    //dynamic data and template string should read from file so can be used
    if (!cacheItem.dynamicData) {
        cacheItem.dynamicData = [NSDictionary dictionaryWithContentsOfFile:[self filePathWithType:SonicCacheTypeData sessionID:sessionID]];
    }
    if (!cacheItem.templateString) {
        cacheItem.templateString = [[[NSString alloc]initWithData:[NSData dataWithContentsOfFile:[self filePathWithType:SonicCacheTypeTemplate sessionID:sessionID]] encoding:NSUTF8StringEncoding]autorelease];
    }
    
    NSMutableDictionary *dynamicData = [NSMutableDictionary dictionaryWithDictionary:cacheItem.dynamicData];
    NSDictionary *mergeResult = [SonicUitil mergeDynamicData:dataDict[kSonicDataFieldName] withOriginData:dynamicData];
    
    NSData *htmlData = nil;
    if (htmlString.length > 0) {
        htmlData = [htmlString dataUsingEncoding:NSUTF8StringEncoding];
    }else{
        //merge new dynamic data with template string to create new HTML cache
        NSMutableString *html = [NSMutableString stringWithString:cacheItem.templateString];
        for (NSString *key in dynamicData.allKeys) {
            [html replaceOccurrencesOfString:key withString:dynamicData[key] options:NSCaseInsensitiveSearch range:NSMakeRange(0, html.length)];
        }
        htmlData = [html dataUsingEncoding:NSUTF8StringEncoding];
    }
    
    cacheItem.dynamicData = dynamicData;
    cacheItem.htmlData = htmlData;
    cacheItem.diffData = mergeResult[@"diff"];
    NSMutableDictionary *config = [NSMutableDictionary dictionaryWithDictionary:[self createConfigFromResponseHeaders:headers]];
    NSString *sha1 = dataDict[@"html-sha1"];
    [config setObject:sha1 forKey:kSonicSha1];
    cacheItem.config = config;
    NSDictionary *filterResponseHeaders = [self filterResponseHeaders:headers];
    cacheItem.cacheResponseHeaders = filterResponseHeaders;
    
    dealInFileQueue(^{
        [self saveHtmlData:htmlData withConfig:config withTemplate:cacheItem.templateString dynamicData:dynamicData withResponseHeaders:filterResponseHeaders withSessionID:sessionID isUpdate:YES];
    });
    
    return cacheItem;
}

- (SonicCacheItem *)saveHtmlString:(NSString *)htmlString
                    templateString:(NSString *)templateString
                       dynamicData:(NSDictionary *)dataDict
                   responseHeaders:(NSDictionary *)headers
                           withUrl:(NSString *)url
{
    NSString *sessionID = sonicSessionID(url);
    
    if (!htmlString || !templateString || headers.count == 0 || sessionID.length == 0) {
        return nil;
    }
    
    NSData *htmlData = [htmlString dataUsingEncoding:NSUTF8StringEncoding];
    SonicCacheItem *cacheItem = [self cacheForSession:sessionID];
    cacheItem.htmlData = htmlData;
    cacheItem.dynamicData = dataDict;
    cacheItem.templateString = templateString;
    NSMutableDictionary *config = [NSMutableDictionary dictionaryWithDictionary:[self createConfigFromResponseHeaders:headers]];
    NSString *sha1 = getDataSha1(cacheItem.htmlData);
    [config setObject:sha1 forKey:kSonicSha1];
    cacheItem.config = config;
    NSDictionary *filterResponseHeaders = [self filterResponseHeaders:headers];
    cacheItem.cacheResponseHeaders = filterResponseHeaders;
    
    dealInFileQueue(^{
        [self saveHtmlData:htmlData withConfig:config withTemplate:templateString dynamicData:dataDict withResponseHeaders:filterResponseHeaders withSessionID:sessionID isUpdate:NO];
    });
    
    return cacheItem;
}

- (NSDictionary *)createConfigFromResponseHeaders:(NSDictionary *)headers
{
    //Etag,template-tag
    NSString *eTag = headers[SonicHeaderKeyETag];
    NSString *templateTag = headers[SonicHeaderKeyTemplate];
    NSTimeInterval timeNow = (long)[[NSDate date ]timeIntervalSince1970]*1000;
    NSString *localRefresh = [@(timeNow) stringValue];
    
    //save configs
    eTag = eTag.length > 0? eTag:@"";
    templateTag = templateTag.length > 0? templateTag:@"";
    eTag = eTag.length > 0? eTag:@"";
    
    NSDictionary *cfgDict = @{
                              SonicHeaderKeyETag:eTag,
                              SonicHeaderKeyTemplate:templateTag,
                              kSonicLocalRefreshTime:localRefresh,
                              };
    return cfgDict;
}

/**
 *  Check and save the cache expire timestamp
 */
- (unsigned long long)createCacheExpireTimeFromHeaders:(NSDictionary *)headers
{
    NSString *maxAge = headers[SonicHeaderMaxAge];
    NSString *expire = headers[SonicHeaderExpire];
    
    unsigned long long now =  (unsigned long long)[[NSDate date] timeIntervalSince1970];
    
    unsigned long long configMaxCacheTime = [SonicEngine sharedEngine].configuration.maxUnStrictModeCacheSeconds;
    
    if(maxAge.length == 0 && expire.length == 0){
        return now + configMaxCacheTime;
    }
    
    if(maxAge.length > 0){
        
        unsigned long long duration = [maxAge longLongValue];
        
        duration = MIN(duration, configMaxCacheTime);
        
        unsigned long long expireTimestamp = now + duration;
        
        return expireTimestamp;
    }
    
    if(expire.length > 0){
        
        NSDateFormatter *dateFormatter = [NSDateFormatter new];
        [dateFormatter setDateFormat:@"EEE, dd MMM yyyy HH:mm:ss zzz"];
        NSDate *date = [dateFormatter dateFromString:expire];
        unsigned long long expireTime = (unsigned long long)[date timeIntervalSince1970];
        
        if (!date) {
            return now + configMaxCacheTime;
        }
        
        //Beijing 0800 GMT 
        NSTimeZone *system = [NSTimeZone systemTimeZone];
        NSInteger seconds = [system secondsFromGMTForDate:date];
        expireTime = expireTime + seconds;
        
        unsigned long long configCacheTime = now + configMaxCacheTime;
        
        return MIN(expireTime, configCacheTime);
    }
    
    return now + [SonicEngine sharedEngine].configuration.maxUnStrictModeCacheSeconds;
}

- (void)updateCacheExpireTimeWithResponseHeaders:(NSDictionary *)headers withSessionID:(NSString *)sessionID
{
    if (headers.count == 0 || sessionID.length == 0) {
        return;
    }
    
    SonicCacheItem *cacheItem = [self cacheForSession:sessionID];
    
    NSMutableDictionary *mConfig = [NSMutableDictionary dictionaryWithDictionary:cacheItem.config];
    unsigned long long expireTimestamp = [self createCacheExpireTimeFromHeaders:headers];
    NSString *expireTime = [@(expireTimestamp) stringValue];
    [mConfig setObject:expireTime forKey:kSonicLocalCacheExpireTime];
    cacheItem.config = mConfig;
    
    //save to file
    dealInFileQueue(^{
       
        //save the config data
        [self.database updateWithKeyAndValue:@{kSonicLocalCacheExpireTime:expireTime} withSessionID:sessionID];
        
    });
}

- (void)saveResponseHeaders:(NSDictionary *)headers withSessionID:(NSString *)sessionID
{
    NSDictionary *filterHeaders = [self filterResponseHeaders:headers];
    
    //save the response headers
    if (filterHeaders.count > 0) {
        NSString *rspHeaderPath = [self filePathWithType:SonicCacheTypeResponseHeader sessionID:sessionID];
        BOOL isSuccess = [filterHeaders writeToFile:rspHeaderPath atomically:YES];
        if (!isSuccess) {
            return;
        }
    }
}

#pragma mark File Operation

void dealInFileQueue(dispatch_block_t block)
{
    NSThread *currentThread = [NSThread currentThread];
    if ([currentThread.name isEqualToString:[SonicCache fileQueue].name]) {
        block();
    }else{
        NSBlockOperation *blkOp = [NSBlockOperation blockOperationWithBlock:block];
        [[SonicCache fileQueue] addOperation:blkOp];
    }
}

- (NSDictionary *)dynamicDataBySessionID:(NSString *)sessionID
{
    return [NSDictionary dictionaryWithContentsOfFile:[self filePathWithType:SonicCacheTypeData sessionID:sessionID]];
}

- (NSString *)templateStringBySessionID:(NSString *)sessionID
{
    return [[[NSString alloc]initWithData:[NSData dataWithContentsOfFile:[self filePathWithType:SonicCacheTypeTemplate sessionID:sessionID]] encoding:NSUTF8StringEncoding]autorelease];
}

- (void)setupCacheItemFromFile:(SonicCacheItem *)item
{
    if (![self isAllCacheExist:item.sessionID]) {
        [self removeFileCacheOnly:item.sessionID];
        return;
    }
    
    NSData *htmlData = [NSData dataWithContentsOfFile:[self filePathWithType:SonicCacheTypeHtml sessionID:item.sessionID]];
    NSDictionary *config = [self.database queryAllKeysWithSessionID:item.sessionID];
    NSDictionary *cacheHeaders = [NSDictionary dictionaryWithContentsOfFile:[self filePathWithType:SonicCacheTypeResponseHeader sessionID:item.sessionID]];

    NSString *sha1 = config[kSonicSha1];
    NSString *htmlSha1 = getDataSha1(htmlData);
    if (![sha1 isEqualToString:htmlSha1]) {
        [self removeFileCacheOnly:item.sessionID];
    }else{
        item.htmlData = htmlData;
        item.config = config;
        item.cacheResponseHeaders = cacheHeaders;//sonic 1.1 read headers
        
        //sonic 1.0 use this to read
        if ([config[kSonicCSP] length] > 0) {
            item.cacheResponseHeaders = @{kSonicCSP:config[kSonicCSP]};
        }
        
        //read templateString and dynamicData where need
    }
}

- (BOOL)setupCacheDirectory
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    
    NSString *subDir = @"SonicCache";
    
    _rootCachePath = [[self createDirectoryIfNotExist:[paths objectAtIndex:0] withSubPath:subDir] copy];
    
    NSLog(@"rootCachePath:%@",_rootCachePath);
    
    return _rootCachePath.length > 0;
}

- (NSString *)createDirectoryIfNotExist:(NSString *)parent withSubPath:(NSString *)subPath
{
    if(parent.length == 0 || subPath.length == 0){
        return nil;
    }
    
    BOOL isDir = YES;
    NSString *path = [parent stringByAppendingPathComponent:subPath];
    if (![SonicFileManager fileExistsAtPath:path isDirectory:&isDir]) {
        NSError *error = nil;
        [SonicFileManager createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:&error];
        if (error) {
            return nil;
        }
    }
    return path;
}

- (NSString *)sessionSubCacheDir:(NSString *)sessionID
{
    return [self createDirectoryIfNotExist:_rootCachePath withSubPath:sessionID];
}

- (BOOL)isAllCacheExist:(NSString *)sessionID
{
    NSUInteger checkList[4] = {
        SonicCacheTypeHtml,
        SonicCacheTypeTemplate,
        SonicCacheTypeResponseHeader
    };
    
    for (int i=0; i<4; i++) {
        if (![self checkCacheTypeExist:checkList[i] sessionID:sessionID]) { return NO; }
    }
    
    return YES;
}

- (NSString *)localRefreshTimeBySessionID:(NSString *)sessionID
{
    return [self.database queryKey:@"local_refresh" withSessionID:sessionID];
}

- (BOOL)checkCacheTypeExist:(SonicCacheType)type sessionID:(NSString *)sessionID
{
    NSString *cachePath = [self filePathWithType:type sessionID:sessionID];
    
    return [SonicFileManager fileExistsAtPath:cachePath];
}

- (NSString *)filePathWithType:(SonicCacheType)cacheType sessionID:(NSString *)sessionID
{
    NSString *fileDir = [[SonicCache shareCache] sessionSubCacheDir:sessionID];
    if (fileDir.length == 0) {
        return nil;
    }
    NSDictionary *extMap = @{
                             @(SonicCacheTypeConfig):@"cfg",
                             @(SonicCacheTypeTemplate):@"temp",
                             @(SonicCacheTypeHtml):@"html",
                             @(SonicCacheTypeData):@"data",
                             @(SonicCacheTypeResponseHeader):@"rsp",
                             };
    NSString *cacheFileName = [sessionID stringByAppendingPathExtension:extMap[@(cacheType)]];
    return [fileDir stringByAppendingPathComponent:cacheFileName];
}

- (void)removeFileCacheOnly:(NSString *)sessionID
{
    dealInFileQueue(^{
        NSString *fileDir = [self sessionSubCacheDir:sessionID];
        [SonicFileManager removeItemAtPath:fileDir error:nil];
    });
}

- (void)saveHtmlData:(NSData *)htmlData withConfig:(NSDictionary *)config withTemplate:(NSString *)templateString dynamicData:(NSDictionary *)dynamicData withResponseHeaders:(NSDictionary *)responseHeaders withSessionID:(NSString *)sessionID isUpdate:(BOOL)isUpdate
{
    if (!htmlData || config.count == 0) {
        return;
    }
    
    //if is first time to save data, the templateString must't be nil
    if (!isUpdate && templateString.length == 0) {
        return;
    }
    
    //if the root cache path didn't created,then we create it
    if (_rootCachePath.length == 0) {
        if (![self setupCacheDirectory]) {
            return;
        }
    }
    
    //save HTML data
    if (htmlData) {
        NSString *htmlPath = [self filePathWithType:SonicCacheTypeHtml sessionID:sessionID];
        BOOL isSuccess = [htmlData writeToFile:htmlPath atomically:YES];
        if (!isSuccess) {
            return;
        }
    }
    
    //save the template string
    if (templateString) {
        NSData *templateData = [templateString dataUsingEncoding:NSUTF8StringEncoding];
        NSString *tempPath = [self filePathWithType:SonicCacheTypeTemplate sessionID:sessionID];
        BOOL isSuccess = [templateData writeToFile:tempPath atomically:YES];
        if (!isSuccess) {
            [self removeFileCacheOnly:sessionID];
            return;
        }
    }
    
    //save the dynamic data
    if (dynamicData.count > 0) {
        NSString *dataPath = [self filePathWithType:SonicCacheTypeData sessionID:sessionID];
        BOOL isSuccess = [dynamicData writeToFile:dataPath atomically:YES];
        if (!isSuccess) {
            [self removeFileCacheOnly:sessionID];
            return;
        }
    }
    
    //save the response headers
    if (responseHeaders.count > 0) {
        NSString *rspHeaderPath = [self filePathWithType:SonicCacheTypeResponseHeader sessionID:sessionID];
        BOOL isSuccess = [responseHeaders writeToFile:rspHeaderPath atomically:YES];
        if (!isSuccess) {
            [self removeFileCacheOnly:sessionID];
            return;
        }
    }
    
    //save the config data
    [self.database insertWithKeyAndValue:config withSessionID:sessionID];
}

- (NSDictionary *)filterResponseHeaders:(NSDictionary *)responseHeaders
{
    if (responseHeaders.count == 0) {
        return @{};
    }
    
    NSMutableDictionary *mRespHeaders = [NSMutableDictionary dictionaryWithDictionary:responseHeaders];
    
    __block NSMutableArray *removeKeys = [NSMutableArray arrayWithArray:@[SonicHeaderMaxAge,SonicHeaderKeyCacheOffline,SonicHeaderExpire,SonicHeaderKeyTemplate,SonicHeaderKeyTemplateChange]];
    
    [mRespHeaders enumerateKeysAndObjectsUsingBlock:^(NSString *key, id  _Nonnull obj, BOOL * _Nonnull stop) {
       
        if ([[key lowercaseString] rangeOfString:@"cookie"].location != NSNotFound) {
            [removeKeys addObject:key];
        }
        
    }];
    
    [mRespHeaders removeObjectsForKeys:removeKeys];
    
    return mRespHeaders;
}

- (void)checkAndTrimCache
{
    //Check current root cache directory size
    if (_rootCachePath.length == 0) {
        return;
    }

    unsigned long long cacheSize = [self folderSize:_rootCachePath];
    
    CGFloat percent = cacheSize/[SonicEngine sharedEngine].configuration.cacheMaxDirectorySize;
    
    if ( percent < [SonicEngine sharedEngine].configuration.cacheDirectorySizeWarningPercent ) {
        
        return;
        
    }
    
    dealInFileQueue(^{
        
        //sort sub directory by update time
        NSArray *contentArray = [SonicFileManager contentsOfDirectoryAtPath:_rootCachePath error:nil];
        
        if (contentArray.count == 0) {
            return;
        }
        
        NSArray *sortArray = [contentArray sortedArrayUsingComparator:^NSComparisonResult(NSString *fileName1, NSString *fileName2) {
            
            NSString *subDir1 = [_rootCachePath stringByAppendingPathComponent:fileName1];
            NSString *subDir2 = [_rootCachePath stringByAppendingPathComponent:fileName2];
            
            NSDictionary *fileAttrs1 = [SonicFileManager attributesOfItemAtPath:subDir1 error:nil];
            NSDictionary *fileAttrs2 = [SonicFileManager attributesOfItemAtPath:subDir2 error:nil];
            
            NSTimeInterval modifyTime1 = [[fileAttrs1 fileModificationDate] timeIntervalSince1970];
            NSTimeInterval modifyTime2 = [[fileAttrs2 fileModificationDate] timeIntervalSince1970];
            
            return modifyTime1 < modifyTime2;
        }];
        
        NSMutableArray *willClearSubDirs = [NSMutableArray array];
        NSInteger totalReadSize = 0;
        
        for (NSString *fileItem in sortArray) {
            
            NSString *subDir = [_rootCachePath stringByAppendingPathComponent:fileItem];
            
            unsigned long long fileSize = [self folderSize:subDir];
            totalReadSize = totalReadSize + fileSize;
            [willClearSubDirs addObject:fileItem];
            
            if ((cacheSize - totalReadSize)/[SonicEngine sharedEngine].configuration.cacheMaxDirectorySize < [SonicEngine sharedEngine].configuration.cacheDirectorySizeSafePercent ) {
                break;
            }
        }
        
        //do clear action
        if (totalReadSize > 0 && willClearSubDirs.count > 0) {
            
            for (NSString *fileItem in willClearSubDirs) {
                
                NSString *subDir = [_rootCachePath stringByAppendingPathComponent:fileItem];
                
                [SonicFileManager removeItemAtPath:subDir error:nil];
            }
        }
        
    });
}

- (unsigned long long int)folderSize:(NSString *)folderPath {
    NSArray *filesArray = [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:folderPath error:nil];
    NSEnumerator *filesEnumerator = [filesArray objectEnumerator];
    NSString *fileName;
    unsigned long long int fileSize = 0;
    
    while (fileName = [filesEnumerator nextObject]) {
        NSDictionary *fileDictionary = [[NSFileManager defaultManager] attributesOfItemAtPath:[folderPath stringByAppendingPathComponent:fileName] error:nil];
        fileSize += [fileDictionary fileSize];
    }
    
    return fileSize;
}

- (BOOL)upgradeSonicVersion
{
    [self clearAllCache];

    return YES;
}

@end
