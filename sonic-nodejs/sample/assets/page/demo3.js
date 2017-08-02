;(function(){var __a={'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'},
__b=/[&<>"']/g,
__e=function (s) {s = String(s);return s.replace(__b, function (m) {return __a[m]});};

module.exports = function (data, children) {
data=typeof data != "undefined"?data:{},children= typeof children != "undefined"?children:{};
var __p=[],_p=function(s){__p.push(s)};
;_p('<!DOCTYPE HTML>\r\n\
<html>\r\n\
<head>');
;_p( (require('./tpls/common_meta'))() );
;_p('    <title>SONIC</title>\r\n\
    <style>\r\n\
        body {\r\n\
            margin: 0;\r\n\
            padding: 0;\r\n\
            font-size: 14px;\r\n\
            color: #777;\r\n\
            margin-top: 20px;\r\n\
        }\r\n\
        .sonic-wrapper {\r\n\
            padding: 0 12px;\r\n\
        }\r\n\
        .sonic-wrapper h1 {\r\n\
            font-size: 18px;\r\n\
            font-weight: 400;\r\n\
            color: #000;\r\n\
        }\r\n\
        .sonic-wrapper h2 {\r\n\
            font-size: 14px;\r\n\
            color: #000;\r\n\
        }\r\n\
        .sonic-wrapper p {\r\n\
            font-size: 14px;\r\n\
            color: #777;\r\n\
            line-height: 1.6em;\r\n\
        }\r\n\
        .sonic-wrapper img {\r\n\
            width: 100%;\r\n\
        }\r\n\
        .sonic-wrapper table {\r\n\
            width: 100%;\r\n\
        }\r\n\
        .sonic-wrapper table img {\r\n\
            width: 100%;\r\n\
        }\r\n\
        .sonic_des {display:none;}\r\n\
    </style>\r\n\
    <script type="application/javascript">\r\n\
        var _pageTime = {};\r\n\
        _pageTime.startTime = new Date;\r\n\
    </script>\r\n\
</head>\r\n\
<body>\r\n\
<div class="sonic-wrapper">\r\n\
    <h1>Sonic：轻量级的高性能的Hybrid框架</h1>\r\n\
    <p>Sonic是腾讯QQ会员团队研发的一个轻量级的高性能的Hybrid框架，专注于提升H5页面首屏加载速度，让H5页面的体验更加接近原生，提升用户体验及用户留存率。</p>\r\n\
    <span id="data1Content">\r\n\
    <!--sonicdiff-data1-->\r\n\
    <p>示例：</p>\r\n\
    <img src="//mc.vip.qq.com/img/img-');
;_p(__e(data.dataImg));
;_p('.png?max_age=2592000" alt="">\r\n\
    <!--sonicdiff-data1-end-->\r\n\
    </span>\r\n\
    <span id="des0" class="sonic_des">\r\n\
        <h2>非Sonic模式 点击到页面打开耗时:<span id="pageTime0"></span></h2>\r\n\
        <p>普通直出的方式</p>\r\n\
    </span>\r\n\
    <span id="des1" class="sonic_des">\r\n\
        <h2>首次访问 点击到页面打开耗时:<span id="pageTime1"></span></h2>\r\n\
        <p>用户第一次访问，本地无缓存;使用直出的方式，终端生成缓存。</p>\r\n\
    </span>\r\n\
    <span id="des2" class="sonic_des">\r\n\
        <h2>模版更新 点击到页面打开耗时:<span id="pageTime2"></span></h2>\r\n\
        <p>本地模版跟服务器模版不一样;缓存失效，清除缓存，重新加载页面。</p>\r\n\
    </span>\r\n\
    <span id="des3" class="sonic_des">\r\n\
        <h2>数据更新 点击到页面打开耗时:<span id="pageTime3"></span></h2>\r\n\
        <p>模板一致，数据变更;针对页面局部数据变化的场景，Sonic会预先加载本地缓存再将变化部分的数据异步更新，提升用户体验。</p>\r\n\
    </span>\r\n\
    <span id="des4" class="sonic_des">\r\n\
        <h2>完全缓存 点击到页面打开耗时:<span id="pageTime4"></span></h2>\r\n\
        <p>本地数据与服务器数据完全一样;直接使用缓存，页面秒开。</p>\r\n\
    </span>');
if(data.templateFlag){;_p('    <h2>页面打开速度效果对比</h2>\r\n\
    <p>以手机QQ-VIP中心首页为例，在接入Sonic框架之后，页面打开速度在数据更新场景下优化提升42%，页面内容不变的场景下(完全cache模式)优化提升50%以上。</p>\r\n\
\r\n\
    <table>\r\n\
        <tr>\r\n\
            <td>原有直出页面：</td>\r\n\
            <td>Sonic改造页面：</td>\r\n\
        </tr>\r\n\
        <tr>\r\n\
            <td><img src="//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049810_nosonic.gif?max_age=2592000" alt=""></td>\r\n\
            <td><img src="//imgcache.gtimg.cn/ACT/svip_act/act_img/public/201707/1499049823_sonic.gif?max_age=2592000" alt=""></td>\r\n\
        </tr>\r\n\
    </table>');
 };_p('    <h2>Sonic实现原理简介</h2>\r\n\
    <p>Sonic框架使用终端应用层原生传输通道取代系统浏览器内核自身资源传输通道来请求页面主资源，在移动终端初始化的同时并行请求页面主资源并做到流式拦截，减少传统方案上终端初始化耗时长导致页面主资源发起请求时机慢或传统并行方案下必须等待主资源完成下载才能交给内核加载的影响。另外通过客户端和服务器端双方遵守Sonic格式规范(通过在html内增加注释代码区分模板和数据)，该框架能做到智能地对页面内容进行动态缓存和增量更新，减少对网络的依赖，节省用户流量，加快页面打开速度。</p>\r\n\
</div>\r\n\
<script>\r\n\
    _pageTime.jsendtTime = new Date();\r\n\
</script>\r\n\
\r\n\
<script src="http://open.mobile.qq.com/sdk/qqapi.js?_bid=152"></script>\r\n\
<script src="http://imgcache.gtimg.cn/club/platform/lib/seajs/sea-with-plugin-2.2.1.js?_bid=250&max_age=2592000" id="seajsnode"></script>\r\n\
<script>\r\n\
    seajs.config({\r\n\
        base: \'http://imgcache.gtimg.cn/club/platform/examples/\',\r\n\
        localcache: {\r\n\
            //浏览器缓存时间\r\n\
            maxAge: 2592000,\r\n\
            openLocalStorageCache: 0\r\n\
        },\r\n\
        maxFile: {},\r\n\
        debug: 1,\r\n\
        //别名\r\n\
        alias: {\r\n\
            \'zepto\': \'lib/zepto/zepto\'\r\n\
        },\r\n\
        paths: {\r\n\
            \'lib\': \'http://imgcache.gtimg.cn/club/platform/lib\'\r\n\
        },\r\n\
        manifest: {\r\n\
            "lib/zepto/zepto": "1.1.3",\r\n\
            "lib/sonic/sonic": "3-1"\r\n\
        }\r\n\
    });\r\n\
\r\n\
    seajs.use(["zepto", "lib/sonic/sonic"], function ($, sonic) {\r\n\
        /**\r\n\
         * 后置函数 sonic或普通模式 执行页面初始化等操作\r\n\
         */\r\n\
        function afterInit(sonicStatus){\r\n\
            $(\'.sonic_des\').css(\'display\', \'none\');\r\n\
            $(\'#des\'+sonicStatus).css(\'display\', \'block\');\r\n\
            //耗时分析(上报)\r\n\
            var performanceJson = JSON.parse(window.sonic.getPerformance());//clickTime;loadUrlTime\r\n\
            var pageTime = _pageTime.jsendtTime - performanceJson.clickTime;\r\n\
            $("#pageTime"+sonicStatus).text(pageTime+\'ms\');\r\n\
        }');
 if(data.sonicStatus != 0) {;_p('        /**\r\n\
         * sonic业务逻辑 diff数据处理，后置函数执行，状态上报\r\n\
         * @param sonicStatus\r\n\
         * @param reportSonicStatus\r\n\
         * @param sonicUpdateData\r\n\
         */\r\n\
        window.sonicStartTime = new Date;\r\n\
        //0-状态获取失败 1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache\r\n\
        sonic.getSonicData(function(sonicStatus, reportSonicStatus, sonicUpdateData){\r\n\
            if(sonicStatus == 1){\r\n\
                //首次没有特殊的逻辑处理，直接执行sonic完成后的逻辑，比如上报等\r\n\
            }else if(sonicStatus == 2){\r\n\
\r\n\
            }else if(sonicStatus == 3){\r\n\
                //局部刷新的时候需要更新页面的数据块和一些JS操作\r\n\
                var html = \'\';\r\n\
                var id = \'\';\r\n\
                var elementObj = \'\';\r\n\
                for(var key in sonicUpdateData){\r\n\
                    id = key.substring(1,key.length-1);\r\n\
                    html = sonicUpdateData[key];\r\n\
                    elementObj = document.getElementById(id+\'Content\');\r\n\
                    elementObj.innerHTML = html;\r\n\
                }\r\n\
\r\n\
            }else if(sonicStatus == 4){\r\n\
\r\n\
            }\r\n\
            afterInit(reportSonicStatus);\r\n\
        });');
 } else {;_p('            afterInit(0);');
 };_p('    });\r\n\
\r\n\
</script>\r\n\
</body>\r\n\
</html>');

return __p.join("");
};})();