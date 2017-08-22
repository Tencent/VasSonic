/**
 *
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 * @作者 craigchen
 * @版本 2.0.0
 *  sonic前端JS类库
 */
;(function(){
    function getSonicData(callback) {
        var sonicStatus = 0, //sonic状态 0-状态获取失败 1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache
            reportSonicStatus = 0, //sonic上报状态
            sonicHadExecute = 0, //sonic执行标志位
            sonicUpdateData = {}; //sonic diff数据

        window.sonic && window.sonic.getDiffData(); //执行sonicdiff
        window['getDiffDataCallback'] = function (diffData) {
            try{
                var result = JSON.parse(diffData);
            } catch (e) {}
            if(result['code'] == 200){
                reportSonicStatus = sonicStatus = 3;
                sonicUpdateData = JSON.parse(result['result']);
                //页面完全没有变化
            } else if (result['code'] == 1000) {
                reportSonicStatus = sonicStatus = 1;
            } else if (result['code'] == 2000) {
                reportSonicStatus = sonicStatus = 2;
            } else if(result['code'] == 304) {
                sonicStatus = 4;
                switch(parseInt(result['srcCode'])) { //上报状态处理
                    case 304:
                        reportSonicStatus = 4;
                        break;
                    case 200: //局部刷新也可能返回304 但srcCode是200，当返回的局部数据足够快，终端会组装好页面返回304
                        reportSonicStatus = 3;
                        break;
                    case 1000:
                        reportSonicStatus = 1;
                        break;
                    case 2000:
                        reportSonicStatus = 2;
                        break;
                    default:
                        reportSonicStatus = sonicStatus;
                }
            }
            if (sonicHadExecute == 0) {
                callback(sonicStatus, reportSonicStatus, sonicUpdateData);
                sonicHadExecute = 1;
            }
        }
        /**
         * sonic超时处理 默认5s
         */
        setTimeout(function(){
            if(sonicHadExecute == 0){
                sonicHadExecute = 1;
                callback(sonicStatus, reportSonicStatus, sonicUpdateData);
            }
        }, 5000);
    }
    if (typeof module !== 'undefined' && typeof exports === 'object') {
        module.exports = getSonicData;
    } else if (typeof define === 'function') {
        define("lib/sonic/sonic",[], function(require, exports){ exports.getSonicData = getSonicData; });
    } else if (typeof define === 'function' && define.amd) {
        define(function() { return getSonicData; });
    } else {
        this.moduleName = getSonicData;
    }
}).call(function() {
    return this || (typeof window !== 'undefined' ? window : global);
});