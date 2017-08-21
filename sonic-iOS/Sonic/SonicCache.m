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
#import "SonicClient.h"
#import "SonicUitil.h"
#import <UIKit/UIKit.h>
#import <CommonCrypto/CommonDigest.h>

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
    self.maxCacheCount = 3;
    self.lock = [NSRecursiveLock new];
    self.memoryCache = [NSMutableDictionary dictionaryWithCapacity:self.maxCacheCount];
    self.recentlyUsedKey = [NSMutableArray arrayWithCapacity:self.maxCacheCount];
    
    //setup cache dir
    [self setupCacheDirectory];
    
    //read server disable sonic request timestamps
    [self setupCacheOfflineTimeCfgDict];
    
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
    });
    
    //we need clear or create memory cache in sonic queue
    dispatchToSonicSessionQueue(^{
        [self.lock lock];
        [self.memoryCache removeObjectForKey:sessionID];
        [self.recentlyUsedKey removeObject:sessionID];
        [self.lock unlock];
    });
}

- (BOOL)isFirstLoad:(NSString *)sessionID;
{
    if (sessionID.length == 0) {
        return YES;
    }
    
    SonicCacheItem *session = [self cacheForSession:sessionID];
    
    return session.hasLocalCache;
}

- (BOOL)isServerDisableSonic:(NSString *)sessionID
{
    NSNumber *disableStartTime = [self.offlineCacheTimeCfg objectForKey:sessionID];
    if (!disableStartTime) {
        return NO;
    }
    
    NSTimeInterval lastTime = [disableStartTime doubleValue];
    NSTimeInterval timeNow = [[NSDate date] timeIntervalSince1970];
    
    return timeNow - lastTime < SonicCacheOfflineDefaultTime;
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

- (SonicCacheItem *)  saveFirstWithHtmlData:(NSData *)htmlData
        withResponseHeaders:(NSDictionary *)headers
            withUrl:(NSString *)url
{
    NSString *sessionID = sonicSessionID(url);
    
    if (!htmlData || headers.count == 0 || sessionID.length == 0) {
        return nil;
    }
    
    SonicCacheItem *cacheItem = [self cacheForSession:sessionID];
    
    cacheItem.htmlData = htmlData;
    
    NSString *htmlString = [[[NSString alloc]initWithData:htmlData encoding:NSUTF8StringEncoding] autorelease];
    NSDictionary *splitResult = [self splitTemplateAndDataFromHtmlData:htmlString];
    
    if (!splitResult) {
        return nil;
    }
    
    cacheItem.templateString = splitResult[@"temp"];
    cacheItem.dynamicData = splitResult[@"data"];
    NSMutableDictionary *config = [NSMutableDictionary dictionaryWithDictionary:[self createConfigFromResponseHeaders:headers]];
    NSString *sha1 = getDataSha1(htmlData);
    [config setObject:sha1 forKey:kSonicSha1];
    cacheItem.config = config;
    
    dealInFileQueue(^{
        [self saveHtmlData:htmlData withConfig:config withTemplate:splitResult[@"temp"] dynamicData:splitResult[@"data"] withSessionID:sessionID isUpdate:NO];
    });
    
    return cacheItem;
}

- (SonicCacheItem *)updateWithJsonData:(NSData *)jsonData
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
    NSDictionary *mergeResult = [self mergeDynamicData:dataDict[@"data"] withOriginData:dynamicData withTemplate:cacheItem.templateString];
    if (!mergeResult) {
        return nil;
    }
    
    cacheItem.dynamicData = dynamicData;
    NSData *htmlData =  [mergeResult[@"html"] dataUsingEncoding:NSUTF8StringEncoding];
    cacheItem.htmlData = htmlData;
    cacheItem.diffData = mergeResult[@"diff"];
    NSMutableDictionary *config = [NSMutableDictionary dictionaryWithDictionary:[self createConfigFromResponseHeaders:headers]];
    NSString *sha1 = dataDict[@"html-sha1"];
    [config setObject:sha1 forKey:kSonicSha1];
    cacheItem.config = config;
    
    dealInFileQueue(^{
        [self saveHtmlData:htmlData withConfig:config withTemplate:nil dynamicData:dynamicData withSessionID:sessionID isUpdate:YES];
    });
    
    return cacheItem;
}

- (NSDictionary *)createConfigFromResponseHeaders:(NSDictionary *)headers
{
    //Etag,template-tag
    NSString *eTag = headers[@"Etag"];
    NSString *templateTag = headers[@"template-tag"];
    NSString *csp = headers[SonicHeaderKeyCSPHeader];
    NSTimeInterval timeNow = (long)[[NSDate date ]timeIntervalSince1970]*1000;
    NSString *localRefresh = [@(timeNow) stringValue];
    
    //save configs
    eTag = eTag.length > 0? eTag:@"";
    templateTag = templateTag.length > 0? templateTag:@"";
    eTag = eTag.length > 0? eTag:@"";
    csp = csp.length > 0? csp:@"";
    
    NSDictionary *cfgDict = @{
                              SonicHeaderKeyETag:eTag,
                              SonicHeaderKeyTemplate:templateTag,
                              kSonicLocalRefreshTime:localRefresh,
                              kSonicCSP:csp
                              };
    return cfgDict;
}

- (NSDictionary *)splitTemplateAndDataFromHtmlData:(NSString *)html
{
    //using sonicdiff tag to split the HTML to template and dynamic data.
    NSError *error = nil;
    NSRegularExpression *reg = [NSRegularExpression regularExpressionWithPattern:@"<!--sonicdiff-?(\\w*)-->([\\s\\S]+?)<!--sonicdiff-?(\\w*)-end-->" options:NSRegularExpressionCaseInsensitive error:&error];
    if (error) {
        return nil;
    }
    
    //create dynamic data
    NSArray *metchs = [reg matchesInString:html options:NSMatchingReportCompletion range:NSMakeRange(0, html.length)];
    
    NSMutableDictionary *dataDict = [NSMutableDictionary dictionary];
    [metchs enumerateObjectsUsingBlock:^(NSTextCheckingResult *obj, NSUInteger idx, BOOL * _Nonnull stop) {
        NSString *matchStr = [html substringWithRange:obj.range];
        NSArray *seprateArr = [matchStr componentsSeparatedByString:@"<!--sonicdiff-"];
        NSString *itemName = [[[seprateArr lastObject]componentsSeparatedByString:@"-end-->"]firstObject];
        NSString *formatKey = [NSString stringWithFormat:@"{%@}",itemName];
        [dataDict setObject:matchStr forKey:formatKey];
    }];
    
    //create template
    NSMutableString *mResult = [NSMutableString stringWithString:html];
    [dataDict enumerateKeysAndObjectsUsingBlock:^(NSString *key, NSString *value, BOOL * _Nonnull stop) {
        [mResult replaceOccurrencesOfString:value withString:key options:NSCaseInsensitiveSearch range:NSMakeRange(0, mResult.length)];
    }];
    
    //if split HTML faild , we can return nothing ,it is not a validat sonic request.
    if (dataDict.count == 0 || mResult.length == 0) {
        return nil;
    }
    
    return @{@"data":dataDict,@"temp":mResult};
}

- (NSDictionary *)mergeDynamicData:(NSDictionary *)updateDict withOriginData:(NSMutableDictionary *)existData withTemplate:(NSString *)templateString
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
    
    //merge new dynamic data with template string to create new HTML cache
    NSMutableString *html = [NSMutableString stringWithString:templateString];
    for (NSString *key in existData.allKeys) {
        [html replaceOccurrencesOfString:key withString:existData[key] options:NSCaseInsensitiveSearch range:NSMakeRange(0, html.length)];
    }
    
    if (html.length == 0) {
        return nil;
    }

    return @{@"html":html,@"diff":diffData};
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
    NSDictionary *config = [NSDictionary dictionaryWithContentsOfFile:[self filePathWithType:SonicCacheTypeConfig sessionID:item.sessionID]];
    
    NSString *sha1 = config[kSonicSha1];
    NSString *htmlSha1 = getDataSha1(htmlData);
    if (![sha1 isEqualToString:htmlSha1]) {
        [self removeFileCacheOnly:item.sessionID];
    }else{
        item.htmlData = htmlData;
        item.config = config;
        //read templateString and dynamicData where need
    }
}

- (BOOL)setupCacheDirectory
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    
    NSString *subDir = @"SonicCache";
    
    _rootCachePath = [[self createDirectoryIfNotExist:[paths objectAtIndex:0] withSubPath:subDir] copy];
    
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
        SonicCacheTypeConfig,
        SonicCacheTypeHtml,
        SonicCacheTypeTemplate,
        SonicCacheTypeData
    };
    
    for (int i=0; i<4; i++) {
        if (![self checkCacheTypeExist:checkList[i] sessionID:sessionID]) { return NO; }
    }
    
    return YES;
}

- (NSString *)localRefreshTimeBySessionID:(NSString *)sessionID
{
    if (![self checkCacheTypeExist:SonicCacheTypeConfig sessionID:sessionID]) {
        return nil;
    }
    NSString *cfgPath = [self filePathWithType:SonicCacheTypeConfig sessionID:sessionID];
    NSDictionary *cfgDict = [NSDictionary dictionaryWithContentsOfFile:cfgPath];
    return cfgDict[kSonicLocalRefreshTime];
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

- (void)saveHtmlData:(NSData *)htmlData withConfig:(NSDictionary *)config withTemplate:(NSString *)templateString dynamicData:(NSDictionary *)dynamicData withSessionID:(NSString *)sessionID isUpdate:(BOOL)isUpdate
{
    if (!htmlData || config.count == 0 || dynamicData.count == 0) {
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
    
    //save the config data
    NSString *configPath = [self filePathWithType:SonicCacheTypeConfig sessionID:sessionID];
    BOOL isSuccess = [config writeToFile:configPath atomically:YES];
    if (!isSuccess) {
        [self removeFileCacheOnly:sessionID];
        return;
    }
}


@end
