//
//  SonicDatabase.m
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

#import "SonicDatabase.h"
#import "sqlite3.h"
#import "SonicEventStatistics.h"

#if  __has_feature(objc_arc)
#error This file must be compiled without ARC. Use -fno-objc-arc flag.
#endif

/**
 *  create a config table to save data;important: all column used text type,because of that is easy to access and update
 */

#define SonicCreateTableSql @"create table if not exists 'config' ('sessionID' text primary key not null,'local_refresh' text,'template-tag' text,'etag' text,'sha1' text,'cache-expire-time' text)"

@interface SonicDatabase()
{
    sqlite3 *_db;
}

@end

@implementation SonicDatabase

- (instancetype)initWithPath:(NSString *)dbPath
{
    if (self = [super init]) {
        
        int ret = sqlite3_open(dbPath.UTF8String, &_db);
        
        if (ret != SQLITE_OK) {
            
            SonicLogEvent(@"database open db faild :%@ code:%d",dbPath,ret);
        }
        
        [self createConfigTableIfNotExist];
    }
    return self;
}

- (void)dealloc
{
    [self close];
    [super dealloc];
}

- (void)close
{
    if (!_db) {
        return;
    }
    sqlite3_close(_db);
    _db = nil;
}

- (void)createConfigTableIfNotExist
{
   [self execSql:SonicCreateTableSql];
}

- (BOOL)execSql:(NSString *)sql
{
    
    int ret = sqlite3_exec(_db, sql.UTF8String, NULL, NULL, NULL);
    if (ret != SQLITE_OK) {
        SonicLogEvent(@"sql error:%@",sql);
    }
    return ret == SQLITE_OK;
}

- (NSDictionary *)querySql:(NSString *)sql withQueryResultKey:(NSArray *)keys
{
    if (sql.length == 0 || keys.count == 0) {
        return nil;
    }
    
    sqlite3_stmt *stmt;
    
    int ret = sqlite3_prepare_v2(_db, [sql UTF8String], -1,&stmt, 0);
    
    if (ret != SQLITE_OK) {
        sqlite3_finalize(stmt);
        return nil;
    }
    
    for (NSString *key in keys) {
        
        int nameIndex = sqlite3_bind_parameter_index(stmt, key.UTF8String);
        
        if (nameIndex > 0) {
            
            sqlite3_bind_text(stmt, nameIndex, key.UTF8String, -1, SQLITE_STATIC);
            
        }
    }
    
    int rc = sqlite3_step(stmt);
    
    if (rc != SQLITE_ROW) {
        sqlite3_finalize(stmt);
        return nil;
    }
    
    NSMutableDictionary *resultDict = [NSMutableDictionary dictionary];
    
    NSInteger column_count = sqlite3_column_count(stmt);
    
    for (int columnIdx = 0; columnIdx < column_count; columnIdx++) {
        
        const char *key = (const char *)sqlite3_column_name(stmt, columnIdx);
        const char *value = (const char *)sqlite3_column_text(stmt, columnIdx);
        
        if (key) {
            NSString *v = @"";
            if (value && strcmp(key, value) != 0) {
                v = [NSString stringWithUTF8String:value];
            }
            NSString *keyStr = [NSString stringWithUTF8String:key];
            [resultDict setObject:v forKey:keyStr];
        }
    }
    
    sqlite3_finalize(stmt);
    
    return resultDict;
}

- (BOOL)insertWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID
{
    if (keyValues.count == 0) {
        return NO;
    }
    
    //clear if exist
    NSString *isExistSql = [NSString stringWithFormat:@"select 'sessionID' from config where sessionID = '%@'",sessionID];
    BOOL isExist = [self execSql:isExistSql];
    if (isExist) {
        [self clearWithSessionID:sessionID];
    }
    
    return [self execSqlWithKeyAndValue:keyValues withSessionID:sessionID withUpdate:NO table:@"config"];
}

- (BOOL)updateWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID
{
    return [self execSqlWithKeyAndValue:keyValues withSessionID:sessionID withUpdate:YES table:@"config"];
}

- (BOOL)execSqlWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID withUpdate:(BOOL)isUpdate table:(NSString *)table
{
    if (keyValues.count == 0 || sessionID.length == 0) {
        return NO;
    }
    
    NSMutableString *dataPart = [NSMutableString string];
    NSMutableString *keySort = [NSMutableString string];
    
    [keySort appendFormat:@"("];
    [dataPart appendFormat:@"("];
    
    //insert session
    if (!isUpdate) {
        [keySort appendString:@"sessionID,"];
        [dataPart appendFormat:@"'%@',",sessionID];
    }
    
    NSMutableString *updateValues = [NSMutableString string];
    for (int index = 0; index < keyValues.allKeys.count; index++) {
        
        NSString *key = keyValues.allKeys[index];
        NSString *value = keyValues[key];

        if (isUpdate) {
            if (index != keyValues.count - 1 && keyValues.count > 0) {
                if ([key rangeOfString:@"-"].location != NSNotFound) {
                    [updateValues appendFormat:@"set \"%@\" = '%@',",key,value];
                }else{
                    [updateValues appendFormat:@"set %@ = '%@',",key,value];
                }
            }else{
                if ([key rangeOfString:@"-"].location != NSNotFound) {
                    [updateValues appendFormat:@"set \"%@\" = '%@'",key,value];
                }else{
                    [updateValues appendFormat:@"set %@ = '%@'",key,value];
                }
            }
        }else{
            if (index != keyValues.count-1 && keyValues.count > 0) {
                if ([key rangeOfString:@"-"].location != NSNotFound) {
                    [keySort appendFormat:@"\"%@\",",key];
                }else{
                    [keySort appendFormat:@"'%@',",key];
                }
                [dataPart appendFormat:@"'%@',",value];
            }else{
                if ([key rangeOfString:@"-"].location != NSNotFound) {
                    [keySort appendFormat:@"\"%@\")",key];
                }else{
                    [keySort appendFormat:@"'%@')",key];
                }
                [dataPart appendFormat:@"'%@')",value];
            }
        }
    }
    
    NSString *action = isUpdate? @"update":@"insert into";
    NSString *condition = isUpdate? [NSString stringWithFormat:@"where sessionID = '%@'",sessionID]:@"";
    
    NSString *sql = nil;
    
    if (isUpdate) {
        sql = [NSString stringWithFormat:@"%@ %@ %@ %@",action,table,updateValues,condition];
    }else{
       sql = [NSString stringWithFormat:@"%@ %@ %@ values %@ %@",action,table,keySort,dataPart,condition];
    }
    
   return [self execSql:sql];
}

- (NSDictionary *)queryAllKeysWithSessionID:(NSString *)sessionID
{
    return [self queryWithKeys:@[@"sessionID",@"local_refresh",@"template-tag",@"sha1",@"Etag",@"cache-expire-time"] withSessionID:sessionID table:@"config"];
}

- (NSDictionary *)queryWithKeys:(NSArray *)keys withSessionID:(NSString *)sessionID table:(NSString *)table
{
    NSMutableString *selectColumn = [NSMutableString string];
    for (int index = 0; index < keys.count; index++) {
        NSString *key = keys[index];
        if ([key rangeOfString:@"-"].location != NSNotFound) {
            key = [NSString stringWithFormat:@"\"%@\"",key];
        }
        if (index != keys.count - 1) {
            [selectColumn appendFormat:@"%@,",key];
        }else{
            [selectColumn appendFormat:@"%@",key];
        }
    }
    
    NSString *sql = [NSString stringWithFormat:@"select %@ from %@ where sessionID = '%@'",selectColumn,table,sessionID];
    
    return [self querySql:sql withQueryResultKey:keys];
}

- (NSString *)queryKey:(NSString *)key withSessionID:(NSString *)sessionID
{
    NSDictionary *resultDict = [self queryWithKeys:@[key] withSessionID:sessionID table:@"config"];
    
    return resultDict[key];
}

- (BOOL)clearWithSessionID:(NSString *)sessionID
{
    NSString *sql = [NSString stringWithFormat:@"delete from config where sessionID = '%@'",sessionID];
    
    return [self execSql:sql];
}

@end
