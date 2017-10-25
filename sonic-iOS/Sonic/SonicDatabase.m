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

/**
 *  create a config table to save data;important: all column used text type,because of that is easy to access and update
 */

#define SonicCreateTableSql @"create table if not exists 'config' ('sessionID' text primary key not null,'local_refresh' text,'template_tag' text,'Etag' text,'sha1' text,'cache_expire_time' text)"

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
            
            NSLog(@"database open db faild :%@ code:%d",dbPath,ret);
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
    sqlite3_close(_db);
    sqlite3_free(_db);
    _db = nil;
}

- (void)createConfigTableIfNotExist
{
   [self execSql:SonicCreateTableSql];
}

- (BOOL)execSql:(NSString *)sql
{
    int ret = sqlite3_exec(_db, sql.UTF8String, NULL, NULL, NULL);
    NSLog(@"execSql:%@ result:%d",sql,ret);
    
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
            keyStr = [keyStr stringByReplacingOccurrencesOfString:@"_" withString:@"-"];
            [resultDict setObject:v forKey:keyStr];
        }
    }
    
    sqlite3_finalize(stmt);
    
    NSLog(@"querySql:%@ result:%@",sql,resultDict);
    
    return resultDict;
}

- (BOOL)insertWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID
{
    //clear if exist
    NSString *isExistSql = [NSString stringWithFormat:@"select 'sessionID' from config where sessionID = '%@'",sessionID];
    BOOL isExist = [self execSql:isExistSql];
    if (isExist) {
        [self clearWithSessionID:sessionID];
    }
    
    return [self execSqlWithKeyAndValue:keyValues withSessionID:sessionID withUpdate:NO];
}

- (BOOL)updateWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID
{
    return [self execSqlWithKeyAndValue:keyValues withSessionID:sessionID withUpdate:YES];
}

- (BOOL)execSqlWithKeyAndValue:(NSDictionary *)keyValues withSessionID:(NSString *)sessionID withUpdate:(BOOL)isUpdate
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
        key = [key stringByReplacingOccurrencesOfString:@"-" withString:@"_"];

        if (isUpdate) {
            if (index != keyValues.count - 1 && keyValues.count > 0) {
                [updateValues appendFormat:@"set %@ = '%@',",key,value];
            }else{
                [updateValues appendFormat:@"set %@ = '%@'",key,value];
            }
        }else{
            if (index != keyValues.count-1 && keyValues.count > 0) {
                [keySort appendFormat:@"%@,",key];
                [dataPart appendFormat:@"'%@',",value];
            }else{
                [keySort appendFormat:@"%@)",key];
                [dataPart appendFormat:@"'%@')",value];
            }
        }
    }
    
    NSString *action = isUpdate? @"update":@"insert into";
    NSString *condition = isUpdate? [NSString stringWithFormat:@"where sessionID = '%@'",sessionID]:@"";
    
    NSString *sql = nil;
    
    if (isUpdate) {
        sql = [NSString stringWithFormat:@"%@ config %@ %@",action,updateValues,condition];
    }else{
       sql = [NSString stringWithFormat:@"%@ config %@ values %@ %@",action,keySort,dataPart,condition];
    }
    
   return [self execSql:sql];
}

- (NSDictionary *)queryAllKeysWithSessionID:(NSString *)sessionID
{
    return [self queryWithKeys:@[@"sessionID",@"local_refresh",@"template_tag",@"sha1",@"Etag",@"cache_expire_time"] withSessionID:sessionID];
}

- (NSDictionary *)queryWithKeys:(NSArray *)keys withSessionID:(NSString *)sessionID
{
    NSMutableString *selectColumn = [NSMutableString string];
    for (int index = 0; index < keys.count; index++) {
        if (index != keys.count - 1) {
            [selectColumn appendFormat:@"%@,",keys[index]];
        }else{
            [selectColumn appendFormat:@"%@",keys[index]];
        }
    }
    
    NSString *sql = [NSString stringWithFormat:@"select %@ from config where sessionID = '%@'",selectColumn,sessionID];
    
    return [self querySql:sql withQueryResultKey:keys];
}

- (NSString *)queryKey:(NSString *)key withSessionID:(NSString *)sessionID
{
    NSDictionary *resultDict = [self queryWithKeys:@[key] withSessionID:sessionID];
    
    return resultDict[key];
}

- (BOOL)clearWithSessionID:(NSString *)sessionID
{
    NSString *sql = [NSString stringWithFormat:@"delete from config where 'sessionID' = '%@'",sessionID];
    
    return [self execSql:sql];
}

@end
