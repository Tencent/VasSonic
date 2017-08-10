<!--
    Tencent is pleased to support the open source community by making VasSonic available.
    Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
    Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
    https://opensource.org/licenses/BSD-3-Clause
    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
-->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <script type="application/javascript">
        var _pageTime = {};
        _pageTime.startTime = new Date;
    </script>
    <title>SONIC</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-size: 14px;
            color: #777;
            margin-top: 20px;
        }
        .sonic-wrapper {
            padding: 0 12px;
        }
        .sonic-wrapper h1 {
            font-size: 18px;
            font-weight: 400;
            color: #000;
        }
        .sonic-wrapper h2 {
            font-size: 14px;
            color: #000;
        }
        .sonic-wrapper p {
            font-size: 14px;
            color: #777;
            line-height: 1.6em;
        }
        .sonic-wrapper img {
            width: 100%;
        }
        .sonic-wrapper table {
            width: 100%;
        }
        .sonic-wrapper table img {
            width: 100%;
        }
        .sonic_des {display:none;}
    </style>
</head>
<body>
<div class="sonic-wrapper">
    <h1>Sonic：轻量级的高性能的Hybrid框架</h1>
    <p>Sonic是腾讯QQ会员团队研发的一个轻量级的高性能的Hybrid框架，专注于提升H5页面首屏加载速度，让H5页面的体验更加接近原生，提升用户体验及用户留存率。</p>
    <span id="data1Content">
    <!--sonicdiff-data1-->
    <p>示例：</p>
    <img src="//mc.vip.qq.com/img/img-<?php echo intval($dataImg)?>.png?max_age=2592000" alt="">
    <!--sonicdiff-data1-end-->
    </span>
    <span id="des0" class="sonic_des">
        <h2>非Sonic模式 点击到页面打开耗时:<span id="pageTime0"></span></h2>
        <p>普通直出的方式</p>
    </span>
    <span id="des1" class="sonic_des">
        <h2>首次访问 点击到页面打开耗时:<span id="pageTime1"></span></h2>
        <p>用户第一次访问，本地无缓存;使用直出的方式，终端生成缓存。</p>
    </span>
    <span id="des2" class="sonic_des">
        <h2>模版更新 点击到页面打开耗时:<span id="pageTime2"></span></h2>
        <p>本地模版跟服务器模版不一样;缓存失效，清除缓存，重新加载页面。</p>
    </span>
    <span id="des3" class="sonic_des">
        <h2>数据更新 点击到页面打开耗时:<span id="pageTime3"></span></h2>
        <p>模板一致，数据变更;针对页面局部数据变化的场景，Sonic会预先加载本地缓存再将变化部分的数据异步更新，提升用户体验。</p>
    </span>
    <span id="des4" class="sonic_des">
        <h2>完全缓存 点击到页面打开耗时:<span id="pageTime4"></span></h2>
        <p>本地数据与服务器数据完全一样;直接使用缓存，页面秒开。</p>
    </span>
    <?php if($templateFlag){?>
    <h2>页面打开速度效果对比</h2>
    <p>以手机QQ-VIP中心首页为例，在接入Sonic框架之后，页面打开速度在数据更新场景下优化提升42%，页面内容不变的场景下(完全cache模式)优化提升50%以上。</p>

    <table>
        <tr>
            <td>原有直出页面：</td>
            <td>Sonic改造页面：</td>
        </tr>
        <tr>
            <td><img src="//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049810_nosonic.gif?max_age=2592000" alt=""></td>
            <td><img src="//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049823_sonic.gif?max_age=2592000" alt=""></td>
        </tr>
    </table>
    <?php }?>
    <h2>Sonic实现原理简介</h2>
    <p>Sonic框架使用终端应用层原生传输通道取代系统浏览器内核自身资源传输通道来请求页面主资源，在移动终端初始化的同时并行请求页面主资源并做到流式拦截，减少传统方案上终端初始化耗时长导致页面主资源发起请求时机慢或传统并行方案下必须等待主资源完成下载才能交给内核加载的影响。另外通过客户端和服务器端双方遵守Sonic格式规范(通过在html内增加注释代码区分模板和数据)，该框架能做到智能地对页面内容进行动态缓存和增量更新，减少对网络的依赖，节省用户流量，加快页面打开速度。</p>
</div>
<script>
    _pageTime.jsendtTime = new Date();
</script>
<script src="http://open.mobile.qq.com/sdk/qqapi.js?_bid=152"></script>
<script src="http://imgcache.gtimg.cn/club/platform/lib/seajs/sea-with-plugin-2.2.1.js?_bid=250&max_age=2592000" id="seajsnode"></script>
<script>
    seajs.config({
        base: 'http://imgcache.gtimg.cn/club/platform/examples/',
        localcache:{
            //浏览器缓存时间
            maxAge: 2592000,
            openLocalStorageCache: 0
        },
        maxFile : {

        },
        debug:1,
        //别名
        alias:{
            'zepto': 'lib/zepto/zepto'
        },
        paths:{
            'lib' : 'http://imgcache.gtimg.cn/club/platform/lib'
        },
        manifest:{
            "lib/zepto/zepto": "1.1.3",
            "lib/sonic/sonic": "3-1"
        }
    });

    seajs.use(["zepto", "lib/sonic/sonic"],function($, sonic){
        /**
         * 后置函数 sonic或普通模式 执行页面初始化等操作
         */
        function afterInit(sonicStatus){
            $('.sonic_des').css('display', 'none');
            $('#des'+sonicStatus).css('display', 'block');
            //耗时分析(上报)
            var performanceJson;
            if (window.sonic && window.sonic.getPerformance) {
                performanceJson = JSON.parse(window.sonic.getPerformance());//clickTime;loadUrlTime
            } else if (window.performance && window.performance.timing) {
                performanceJson = {clickTime: window.performance.timing.navigationStart, loadUrlTime: window.performance.timing.fetchStart};
            } else {
                performanceJson = {clickTime: 0, loadUrlTime: 0};
            }
            var pageTime = _pageTime.jsendtTime - performanceJson.clickTime;
            $("#pageTime"+sonicStatus).text(pageTime+'ms');
        }
        <?php if($sonicStatus != 0) {?>
        /**
         * sonic业务逻辑 diff数据处理，后置函数执行，状态上报
         * @param sonicStatus
         * @param reportSonicStatus
         * @param sonicUpdateData
         */
        window.sonicStartTime = new Date;
        //0-状态获取失败 1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache
        sonic.getSonicData(function(sonicStatus, reportSonicStatus, sonicUpdateData){
            if(sonicStatus == 1){
                //首次没有特殊的逻辑处理，直接执行sonic完成后的逻辑，比如上报等
            }else if(sonicStatus == 2){

            }else if(sonicStatus == 3){
                //局部刷新的时候需要更新页面的数据块和一些JS操作
                var html = '';
                var id = '';
                var elementObj = '';
                for(var key in sonicUpdateData){
                    id = key.substring(1,key.length-1);
                    html = sonicUpdateData[key];
                    elementObj = document.getElementById(id+'Content');
                    elementObj.innerHTML = html;
                }

            }else if(sonicStatus == 4){

            }
            afterInit(reportSonicStatus);
        });
        <?php } else {?>
            afterInit(0);
        <?php }?>
    });

</script>
</body>
</html>