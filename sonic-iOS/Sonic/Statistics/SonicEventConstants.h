//
//  SonicEventConstants.h
//  Sonic
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


/**
 *  Sonic Event Type
 */
typedef NS_ENUM(NSUInteger, SonicStatisticsEvent) {
    
    //Throw all step log 
    SonicStatisticsEvent_EventLog,

    //Session create faild
    SonicStatisticsEvent_SessionCreateFaild,
    
    //Session did finish read cache from local file
    SonicStatisticsEvent_SessionDidLoadLocalCache,
    
    //Session data updated
    SonicStatisticsEvent_SessionDataUpdated,
    
    //Session first load and data did fetched
    SonicStatisticsEvent_SessionFirstLoad,
    
    //Session did recieve server response
    SonicStatisticsEvent_SessionRecvResponse,
    
    //Session did faild with error
    SonicStatisticsEvent_SessionHttpError,
    
    //Session did recieve server 304 response
    SonicStatisticsEvent_SessionHitCache,
    
    //Session did recieve server custom header cache-offline:http
    SonicStatisticsEvent_SessionUnavilable,
    
    //Session request result is template changed
    SonicStatisticsEvent_SessionTemplateChanged,
    
    //Session did finish save server data to cache layer
    SonicStatisticsEvent_SessionDidSaveCache,
    
    //Session will remove from engine
    SonicStatisticsEvent_SessionDestroy,
    
    //Session require to refresh current state by send new request
    SonicStatisticsEvent_SessionRefresh,
    
    //Sub resource did write to file
    SonicStatisticsEvent_SubResourceDidSave,
    
    //Sub resource did request faild
    SonicStatisticsEvent_SubResourceDidFail,
    
    //Sub resource did load cache from file
    SonicStatisticsEvent_SubResourceLoadLocalCache,
    
    //Sonic cache did reach the maximun limit size of cache directory
    SonicStatisticsEvent_TrimCache,
};
