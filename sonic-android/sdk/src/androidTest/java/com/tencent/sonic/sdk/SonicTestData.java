/*
 * Tencent is pleased to support the open source community by making VasSonic available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package com.tencent.sonic.sdk;

public interface SonicTestData {
    String htmlString = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
            "    <script type=\"application/javascript\">\n" +
            "        var _pageTime = {};\n" +
            "        _pageTime.startTime = new Date;\n" +
            "    </script>\n" +
            "    <title>SONIC</title>\n" +
            "    <style>\n" +
            "        body {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            font-size: 14px;\n" +
            "            color: #777;\n" +
            "            margin-top: 20px;\n" +
            "        }\n" +
            "        .sonic-wrapper {\n" +
            "            padding: 0 12px;\n" +
            "        }\n" +
            "        .sonic-wrapper h1 {\n" +
            "            font-size: 18px;\n" +
            "            font-weight: 400;\n" +
            "            color: #000;\n" +
            "        }\n" +
            "        .sonic-wrapper h2 {\n" +
            "            font-size: 14px;\n" +
            "            color: #000;\n" +
            "        }\n" +
            "        .sonic-wrapper p {\n" +
            "            font-size: 14px;\n" +
            "            color: #777;\n" +
            "            line-height: 1.6em;\n" +
            "        }\n" +
            "        .sonic-wrapper img {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        .sonic-wrapper table {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        .sonic-wrapper table img {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        .sonic_des {display:none;}\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"sonic-wrapper\">\n" +
            "    <h1>Sonic：轻量级的高性能的Hybrid框架</h1>\n" +
            "    <p>Sonic是腾讯QQ会员团队研发的一个轻量级的高性能的Hybrid框架，专注于提升H5页面首屏加载速度，让H5页面的体验更加接近原生，提升用户体验及用户留存率。</p>\n" +
            "    <span id=\"data1Content\">\n" +
            "    <!--sonicdiff-data1-->\n" +
            "    <p>示例：</p>\n" +
            "    <img src=\"//mc.vip.qq.com/img/img-1.png?max_age=2592000\" alt=\"\">\n" +
            "    <!--sonicdiff-data1-end-->\n" +
            "    </span>\n" +
            "    <span id=\"des0\" class=\"sonic_des\">\n" +
            "        <h2>非Sonic模式 点击到页面打开耗时:<span id=\"pageTime0\"></span></h2>\n" +
            "        <p>普通直出的方式</p>\n" +
            "    </span>\n" +
            "    <span id=\"des1\" class=\"sonic_des\">\n" +
            "        <h2>首次访问 点击到页面打开耗时:<span id=\"pageTime1\"></span></h2>\n" +
            "        <p>用户第一次访问，本地无缓存;使用直出的方式，终端生成缓存。</p>\n" +
            "    </span>\n" +
            "    <span id=\"des2\" class=\"sonic_des\">\n" +
            "        <h2>模版更新 点击到页面打开耗时:<span id=\"pageTime2\"></span></h2>\n" +
            "        <p>本地模版跟服务器模版不一样;缓存失效，清除缓存，重新加载页面。</p>\n" +
            "    </span>\n" +
            "    <span id=\"des3\" class=\"sonic_des\">\n" +
            "        <h2>数据更新 点击到页面打开耗时:<span id=\"pageTime3\"></span></h2>\n" +
            "        <p>模板一致，数据变更;针对页面局部数据变化的场景，Sonic会预先加载本地缓存再将变化部分的数据异步更新，提升用户体验。</p>\n" +
            "    </span>\n" +
            "    <span id=\"des4\" class=\"sonic_des\">\n" +
            "        <h2>完全缓存 点击到页面打开耗时:<span id=\"pageTime4\"></span></h2>\n" +
            "        <p>本地数据与服务器数据完全一样;直接使用缓存，页面秒开。</p>\n" +
            "    </span>\n" +
            "        <h2>页面打开速度效果对比</h2>\n" +
            "    <p>以手机QQ-VIP中心首页为例，在接入Sonic框架之后，页面打开速度在数据更新场景下优化提升42%，页面内容不变的场景下(完全cache模式)优化提升50%以上。</p>\n" +
            "\n" +
            "    <table>\n" +
            "        <tr>\n" +
            "            <td>原有直出页面：</td>\n" +
            "            <td>Sonic改造页面：</td>\n" +
            "        </tr>\n" +
            "        <tr>\n" +
            "            <td><img src=\"//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049810_nosonic.gif?max_age=2592000\" alt=\"\"></td>\n" +
            "            <td><img src=\"//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049823_sonic.gif?max_age=2592000\" alt=\"\"></td>\n" +
            "        </tr>\n" +
            "    </table>\n" +
            "        <h2>Sonic实现原理简介</h2>\n" +
            "    <p>Sonic框架使用终端应用层原生传输通道取代系统浏览器内核自身资源传输通道来请求页面主资源，在移动终端初始化的同时并行请求页面主资源并做到流式拦截，减少传统方案上终端初始化耗时长导致页面主资源发起请求时机慢或传统并行方案下必须等待主资源完成下载才能交给内核加载的影响。另外通过客户端和服务器端双方遵守Sonic格式规范(通过在html内增加注释代码区分模板和数据)，该框架能做到智能地对页面内容进行动态缓存和增量更新，减少对网络的依赖，节省用户流量，加快页面打开速度。</p>\n" +
            "</div>\n" +
            "<script>\n" +
            "    _pageTime.jsendtTime = new Date();\n" +
            "</script>\n" +
            "<script src=\"//open.mobile.qq.com/sdk/qqapi.js?_bid=152\"></script>\n" +
            "<script src=\"//imgcache.gtimg.cn/club/platform/lib/seajs/sea-with-plugin-2.2.1.js?_bid=250&max_age=2592000\" id=\"seajsnode\"></script>\n" +
            "<script>\n" +
            "    seajs.config({\n" +
            "        base: location.protocol+'//imgcache.gtimg.cn/club/platform/examples/',\n" +
            "        localcache:{\n" +
            "            //浏览器缓存时间\n" +
            "            maxAge: 2592000,\n" +
            "            openLocalStorageCache: 0\n" +
            "        },\n" +
            "        maxFile : {\n" +
            "\n" +
            "        },\n" +
            "        debug:1,\n" +
            "        //别名\n" +
            "        alias:{\n" +
            "            'zepto': 'lib/zepto/zepto'\n" +
            "        },\n" +
            "        paths:{\n" +
            "            'lib' : location.protocol+'//imgcache.gtimg.cn/club/platform/lib'\n" +
            "        },\n" +
            "        manifest:{\n" +
            "            \"lib/zepto/zepto\": \"1.1.3\",\n" +
            "            \"lib/sonic/sonic\": \"3-1\"\n" +
            "        }\n" +
            "    });\n" +
            "\n" +
            "    seajs.use([\"zepto\", \"lib/sonic/sonic\"],function($, sonic){\n" +
            "        /**\n" +
            "         * 后置函数 sonic或普通模式 执行页面初始化等操作\n" +
            "         */\n" +
            "        function afterInit(sonicStatus){\n" +
            "            $('.sonic_des').css('display', 'none');\n" +
            "            $('#des'+sonicStatus).css('display', 'block');\n" +
            "            //耗时分析(上报)\n" +
            "            var performanceJson = JSON.parse(window.sonic.getPerformance());//clickTime;loadUrlTime\n" +
            "            var pageTime = _pageTime.jsendtTime - performanceJson.clickTime;\n" +
            "            $(\"#pageTime\"+sonicStatus).text(pageTime+'ms');\n" +
            "        }\n" +
            "                /**\n" +
            "         * sonic业务逻辑 diff数据处理，后置函数执行，状态上报\n" +
            "         * @param sonicStatus\n" +
            "         * @param reportSonicStatus\n" +
            "         * @param sonicUpdateData\n" +
            "         */\n" +
            "        window.sonicStartTime = new Date;\n" +
            "        //0-状态获取失败 1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache\n" +
            "        sonic.getSonicData(function(sonicStatus, reportSonicStatus, sonicUpdateData){\n" +
            "            if(sonicStatus == 1){\n" +
            "                //首次没有特殊的逻辑处理，直接执行sonic完成后的逻辑，比如上报等\n" +
            "            }else if(sonicStatus == 2){\n" +
            "\n" +
            "            }else if(sonicStatus == 3){\n" +
            "                //局部刷新的时候需要更新页面的数据块和一些JS操作\n" +
            "                var html = '';\n" +
            "                var id = '';\n" +
            "                var elementObj = '';\n" +
            "                for(var key in sonicUpdateData){\n" +
            "                    id = key.substring(1,key.length-1);\n" +
            "                    html = sonicUpdateData[key];\n" +
            "                    elementObj = document.getElementById(id+'Content');\n" +
            "                    elementObj.innerHTML = html;\n" +
            "                }\n" +
            "\n" +
            "            }else if(sonicStatus == 4){\n" +
            "\n" +
            "            }\n" +
            "            afterInit(reportSonicStatus);\n" +
            "        });\n" +
            "            });\n" +
            "\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";

    String templateString = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
            "    <script type=\"application/javascript\">\n" +
            "        var _pageTime = {};\n" +
            "        _pageTime.startTime = new Date;\n" +
            "    </script>\n" +
            "    {title}\n" +
            "    <style>\n" +
            "        body {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            font-size: 14px;\n" +
            "            color: #777;\n" +
            "            margin-top: 20px;\n" +
            "        }\n" +
            "        .sonic-wrapper {\n" +
            "            padding: 0 12px;\n" +
            "        }\n" +
            "        .sonic-wrapper h1 {\n" +
            "            font-size: 18px;\n" +
            "            font-weight: 400;\n" +
            "            color: #000;\n" +
            "        }\n" +
            "        .sonic-wrapper h2 {\n" +
            "            font-size: 14px;\n" +
            "            color: #000;\n" +
            "        }\n" +
            "        .sonic-wrapper p {\n" +
            "            font-size: 14px;\n" +
            "            color: #777;\n" +
            "            line-height: 1.6em;\n" +
            "        }\n" +
            "        .sonic-wrapper img {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        .sonic-wrapper table {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        .sonic-wrapper table img {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        .sonic_des {display:none;}\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"sonic-wrapper\">\n" +
            "    <h1>Sonic：轻量级的高性能的Hybrid框架</h1>\n" +
            "    <p>Sonic是腾讯QQ会员团队研发的一个轻量级的高性能的Hybrid框架，专注于提升H5页面首屏加载速度，让H5页面的体验更加接近原生，提升用户体验及用户留存率。</p>\n" +
            "    <span id=\"data1Content\">\n" +
            "    {data1}\n" +
            "    </span>\n" +
            "    <span id=\"des0\" class=\"sonic_des\">\n" +
            "        <h2>非Sonic模式 点击到页面打开耗时:<span id=\"pageTime0\"></span></h2>\n" +
            "        <p>普通直出的方式</p>\n" +
            "    </span>\n" +
            "    <span id=\"des1\" class=\"sonic_des\">\n" +
            "        <h2>首次访问 点击到页面打开耗时:<span id=\"pageTime1\"></span></h2>\n" +
            "        <p>用户第一次访问，本地无缓存;使用直出的方式，终端生成缓存。</p>\n" +
            "    </span>\n" +
            "    <span id=\"des2\" class=\"sonic_des\">\n" +
            "        <h2>模版更新 点击到页面打开耗时:<span id=\"pageTime2\"></span></h2>\n" +
            "        <p>本地模版跟服务器模版不一样;缓存失效，清除缓存，重新加载页面。</p>\n" +
            "    </span>\n" +
            "    <span id=\"des3\" class=\"sonic_des\">\n" +
            "        <h2>数据更新 点击到页面打开耗时:<span id=\"pageTime3\"></span></h2>\n" +
            "        <p>模板一致，数据变更;针对页面局部数据变化的场景，Sonic会预先加载本地缓存再将变化部分的数据异步更新，提升用户体验。</p>\n" +
            "    </span>\n" +
            "    <span id=\"des4\" class=\"sonic_des\">\n" +
            "        <h2>完全缓存 点击到页面打开耗时:<span id=\"pageTime4\"></span></h2>\n" +
            "        <p>本地数据与服务器数据完全一样;直接使用缓存，页面秒开。</p>\n" +
            "    </span>\n" +
            "        <h2>页面打开速度效果对比</h2>\n" +
            "    <p>以手机QQ-VIP中心首页为例，在接入Sonic框架之后，页面打开速度在数据更新场景下优化提升42%，页面内容不变的场景下(完全cache模式)优化提升50%以上。</p>\n" +
            "\n" +
            "    <table>\n" +
            "        <tr>\n" +
            "            <td>原有直出页面：</td>\n" +
            "            <td>Sonic改造页面：</td>\n" +
            "        </tr>\n" +
            "        <tr>\n" +
            "            <td><img src=\"//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049810_nosonic.gif?max_age=2592000\" alt=\"\"></td>\n" +
            "            <td><img src=\"//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049823_sonic.gif?max_age=2592000\" alt=\"\"></td>\n" +
            "        </tr>\n" +
            "    </table>\n" +
            "        <h2>Sonic实现原理简介</h2>\n" +
            "    <p>Sonic框架使用终端应用层原生传输通道取代系统浏览器内核自身资源传输通道来请求页面主资源，在移动终端初始化的同时并行请求页面主资源并做到流式拦截，减少传统方案上终端初始化耗时长导致页面主资源发起请求时机慢或传统并行方案下必须等待主资源完成下载才能交给内核加载的影响。另外通过客户端和服务器端双方遵守Sonic格式规范(通过在html内增加注释代码区分模板和数据)，该框架能做到智能地对页面内容进行动态缓存和增量更新，减少对网络的依赖，节省用户流量，加快页面打开速度。</p>\n" +
            "</div>\n" +
            "<script>\n" +
            "    _pageTime.jsendtTime = new Date();\n" +
            "</script>\n" +
            "<script src=\"//open.mobile.qq.com/sdk/qqapi.js?_bid=152\"></script>\n" +
            "<script src=\"//imgcache.gtimg.cn/club/platform/lib/seajs/sea-with-plugin-2.2.1.js?_bid=250&max_age=2592000\" id=\"seajsnode\"></script>\n" +
            "<script>\n" +
            "    seajs.config({\n" +
            "        base: location.protocol+'//imgcache.gtimg.cn/club/platform/examples/',\n" +
            "        localcache:{\n" +
            "            //浏览器缓存时间\n" +
            "            maxAge: 2592000,\n" +
            "            openLocalStorageCache: 0\n" +
            "        },\n" +
            "        maxFile : {\n" +
            "\n" +
            "        },\n" +
            "        debug:1,\n" +
            "        //别名\n" +
            "        alias:{\n" +
            "            'zepto': 'lib/zepto/zepto'\n" +
            "        },\n" +
            "        paths:{\n" +
            "            'lib' : location.protocol+'//imgcache.gtimg.cn/club/platform/lib'\n" +
            "        },\n" +
            "        manifest:{\n" +
            "            \"lib/zepto/zepto\": \"1.1.3\",\n" +
            "            \"lib/sonic/sonic\": \"3-1\"\n" +
            "        }\n" +
            "    });\n" +
            "\n" +
            "    seajs.use([\"zepto\", \"lib/sonic/sonic\"],function($, sonic){\n" +
            "        /**\n" +
            "         * 后置函数 sonic或普通模式 执行页面初始化等操作\n" +
            "         */\n" +
            "        function afterInit(sonicStatus){\n" +
            "            $('.sonic_des').css('display', 'none');\n" +
            "            $('#des'+sonicStatus).css('display', 'block');\n" +
            "            //耗时分析(上报)\n" +
            "            var performanceJson = JSON.parse(window.sonic.getPerformance());//clickTime;loadUrlTime\n" +
            "            var pageTime = _pageTime.jsendtTime - performanceJson.clickTime;\n" +
            "            $(\"#pageTime\"+sonicStatus).text(pageTime+'ms');\n" +
            "        }\n" +
            "                /**\n" +
            "         * sonic业务逻辑 diff数据处理，后置函数执行，状态上报\n" +
            "         * @param sonicStatus\n" +
            "         * @param reportSonicStatus\n" +
            "         * @param sonicUpdateData\n" +
            "         */\n" +
            "        window.sonicStartTime = new Date;\n" +
            "        //0-状态获取失败 1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache\n" +
            "        sonic.getSonicData(function(sonicStatus, reportSonicStatus, sonicUpdateData){\n" +
            "            if(sonicStatus == 1){\n" +
            "                //首次没有特殊的逻辑处理，直接执行sonic完成后的逻辑，比如上报等\n" +
            "            }else if(sonicStatus == 2){\n" +
            "\n" +
            "            }else if(sonicStatus == 3){\n" +
            "                //局部刷新的时候需要更新页面的数据块和一些JS操作\n" +
            "                var html = '';\n" +
            "                var id = '';\n" +
            "                var elementObj = '';\n" +
            "                for(var key in sonicUpdateData){\n" +
            "                    id = key.substring(1,key.length-1);\n" +
            "                    html = sonicUpdateData[key];\n" +
            "                    elementObj = document.getElementById(id+'Content');\n" +
            "                    elementObj.innerHTML = html;\n" +
            "                }\n" +
            "\n" +
            "            }else if(sonicStatus == 4){\n" +
            "\n" +
            "            }\n" +
            "            afterInit(reportSonicStatus);\n" +
            "        });\n" +
            "            });\n" +
            "\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";

    String dataString = "{\"data\":{\"{data1}\":\"<!--sonicdiff-data1-->\\n    <p>示例：<\\/p>\\n    <img src=\\\"\\/\\/mc.vip.qq.com\\/img\\/img-1.png?max_age=2592000\\\" alt=\\\"\\\">\\n    <!--sonicdiff-data1-end-->\",\"{title}\":\"<title>SONIC<\\/title>\"},\"html-sha1\":\"\",\"template-tag\":\"\"}";


}
