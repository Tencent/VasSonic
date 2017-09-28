//
//  SonicDatabase.h
//  Sonic
//
//  Created by zyvincenthu on 2017/9/15.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface SonicDatabase : NSObject

- (instancetype)initWithPath:(NSString *)dbPath;

- (BOOL)insertWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID;

- (BOOL)updateWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID;

- (NSDictionary *)queryAllKeysWithSessionID:(NSString *)sessionID;

- (NSString *)queryKey:(NSString *)key withSessionID:(NSString *)sessionID;

- (BOOL)clearWithSessionID:(NSString *)sessionID;

- (void)close;

@end
