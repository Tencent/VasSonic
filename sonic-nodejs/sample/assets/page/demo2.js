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
;_p('    <title>demo2</title>');
;_p( (require('./tpls/common_header'))() );
;_p('    <style>\r\n\
        body {\r\n\
            background-color: #ddd;\r\n\
            text-align: center;\r\n\
        }\r\n\
\r\n\
    </style>\r\n\
    <script type="application/javascript">\r\n\
        var _pageTime = {};\r\n\
        _pageTime.startTime = new Date;\r\n\
    </script>\r\n\
</head>\r\n\
<body>');
var urlParams = data && data.urlParams? data.urlParams : {};_p( (require('./tpls/common_body_header'))() );
;_p('<div id="data1Content" data-test="');
;_p(__e( urlParams.testmode=='1'?Date.now():'' ));
;_p('">\r\n\
    <!--sonicdiff-data1-->\r\n\
    <p id="partialRefresh">');
if(urlParams.testmode=='2'){;_p('局部刷新:');
;_p(__e( (new Date()).toLocaleString() ));
};_p('</p>\r\n\
    <!--sonicdiff-data1-end-->\r\n\
</div>\r\n\
<div id="data2Content">\r\n\
    <!--sonicdiff-data2-->\r\n\
    <p id="data2">数据块</p>\r\n\
    <!--sonicdiff-data2-end-->\r\n\
\r\n\
    <p id="pageRefresh">');
if(urlParams.testmode=='1'){;_p('页面刷新:');
;_p(__e( (new Date()).toLocaleString() ));
};_p('</p>\r\n\
\r\n\
\r\n\
</div>\r\n\
<div>\r\n\
    <p>sonicStatus:<span id="sonicStatus"></span></p>\r\n\
    <p>reportSonicStatus:<span id="reportSonicStatus"></span></p>\r\n\
    <p>jsbrigeTime:<span id="jsbrigeTime"></span></p>\r\n\
    <p>pageTime:<span id="pageTime"></span></p>\r\n\
</div>\r\n\
<hr/>\r\n\
<div>\r\n\
    <p>sonic状态含义：</p>\r\n\
    <p>0-非sonic</p>\r\n\
    <p>2-页面刷新</p>\r\n\
    <p>3-局部刷新</p>\r\n\
    <p>4-完全缓存</p>\r\n\
</div>\r\n\
<script>\r\n\
    _pageTime.contentLoadedTime = new Date;\r\n\
</script>\r\n\
\r\n\
<script src="http://open.mobile.qq.com/sdk/qqapi.js?_bid=152"></script>\r\n\
<script src="http://imgcache.gtimg.cn/club/platform/lib/seajs/sea-with-plugin-2.2.1.js?_bid=250&max_age=2592000"\r\n\
        id="seajsnode"></script>\r\n\
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
    seajs.use(["zepto"], function ($) {\r\n\
        /**\r\n\
         * sonic后置函数\r\n\
         */\r\n\
        function afterSonicInit() {\r\n\
            console.debug(\'afterSonicInit\');\r\n\
        }\r\n\
\r\n\
        var sonicStatus = 0, //sonic状态 0-状态获取失败 1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache\r\n\
            reportSonicStatus = 0, //sonic上报状态\r\n\
            sonicHadExecute = 0, //sonic执行标志位\r\n\
            sonicUpdateData = {}; //sonic diff数据\r\n\
\r\n\
        var sonicStartTime = new Date();\r\n\
        window.sonic.getDiffData(); //执行sonicdiff\r\n\
        window[\'getDiffDataCallback\'] = function (result) {\r\n\
            alert(JSON.stringify(result));\r\n\
            if (result[\'code\'] == 200) {\r\n\
                reportSonicStatus = sonicStatus = 3;\r\n\
                sonicUpdateData = JSON.parse(result[\'result\']);\r\n\
                //页面完全没有变化\r\n\
            } else if (result[\'code\'] == 1000) {\r\n\
                reportSonicStatus = sonicStatus = 1;\r\n\
            } else if (result[\'code\'] == 2000) {\r\n\
                reportSonicStatus = sonicStatus = 2;\r\n\
            } else if (result[\'code\'] == 304) {\r\n\
                sonicStatus = 4;\r\n\
                switch (parseInt(result[\'srcCode\'])) { //上报状态处理\r\n\
                    case 304:\r\n\
                        reportSonicStatus = 4;\r\n\
                        break;\r\n\
                    case 200:\r\n\
                        reportSonicStatus = 3;\r\n\
                        break;\r\n\
                    case 1000:\r\n\
                        reportSonicStatus = 1;\r\n\
                        break;\r\n\
                    case 2000:\r\n\
                        reportSonicStatus = 2;\r\n\
                        break;\r\n\
                    default:\r\n\
                        reportSonicStatus = sonicStatus;\r\n\
                }\r\n\
            }\r\n\
            if (sonicHadExecute == 0) {\r\n\
                sonicCallback(sonicStatus, reportSonicStatus, sonicUpdateData);\r\n\
                sonicHadExecute = 1;\r\n\
            }\r\n\
        }\r\n\
        /**\r\n\
         * sonic业务逻辑 diff数据处理，后置函数执行，状态上报\r\n\
         * @param sonicStatus\r\n\
         * @param reportSonicStatus\r\n\
         * @param sonicUpdateData\r\n\
         */\r\n\
        var sonicCallback = function (sonicStatus, reportSonicStatus, sonicUpdateData) {\r\n\
            if (sonicStatus == 1) {\r\n\
                //首次没有特殊的逻辑处理，直接执行sonic完成后的逻辑，比如上报等\r\n\
                afterSonicInit();\r\n\
            } else if (sonicStatus == 2) {\r\n\
                afterSonicInit();\r\n\
            } else if (sonicStatus == 3) {\r\n\
                mqq && mqq.debug && mqq.debug.detailLog({\r\n\
                    id: "pingtai",\r\n\
                    subid: "vipcenter",\r\n\
                    content: \'sonic H5 debug data\' + JSON.stringify(sonicUpdateData),\r\n\
                    level: "info"\r\n\
                });\r\n\
                //局部刷新的时候需要更新页面的数据块和一些JS操作\r\n\
                var html = \'\';\r\n\
                var id = \'\';\r\n\
                var elementObj = \'\';\r\n\
                for (var key in sonicUpdateData) {\r\n\
                    id = key.substring(1, key.length - 1);\r\n\
                    html = sonicUpdateData[key];\r\n\
                    elementObj = document.getElementById(id + \'Content\');\r\n\
                    elementObj.innerHTML = html;\r\n\
                }\r\n\
                afterSonicInit();\r\n\
            } else if (sonicStatus == 4) {\r\n\
                afterSonicInit();\r\n\
            }\r\n\
            alert(\'sonic数据：\' + sonicStatus + JSON.stringify(sonicUpdateData));\r\n\
            $("#sonicStatus").text(sonicStatus);\r\n\
            $("#reportSonicStatus").text(reportSonicStatus);\r\n\
            $("#jsbrigeTime").text((new Date) - sonicStartTime);\r\n\
            _pageTime.jsendtTime = new Date();\r\n\
            mqq.data.getPerformance(function (json) {\r\n\
                alert(JSON.stringify(json, null, 2));\r\n\
                if (json && 0 == json.result && json.data) {\r\n\
                    var clickStart = json.data.clickStart || 0;\r\n\
                    var webviewStart = json.data.webviewStart || 0;\r\n\
                    var loadUrl = json.data.pageStart || 0;\r\n\
                    var speedPoints = [];\r\n\
                    speedPoints[0] = webviewStart - clickStart;      //第一个上报点，从click到webviewstart\r\n\
                    speedPoints[1] = loadUrl - clickStart;      //第二上上报时间点，从webviewstart开始到loadurl之间的时间\r\n\
                    speedPoints[2] = _pageTime.startTime - clickStart;      //从开始loadurl，到html头获取到得时间点\r\n\
                    speedPoints[3] = _pageTime.contentLoadedTime - clickStart;      //从html开始时间点到domready时间点\r\n\
                    speedPoints[4] = _pageTime.jsendtTime - clickStart;      //从domready到可以交互的时间点\r\n\
                    //alert(JSON.stringify(speedPoints))\r\n\
                    $(\'#pageTime\').text(JSON.stringify(speedPoints));\r\n\
                    //alert(JSON.stringify(speedPoints))\r\n\
\r\n\
                }\r\n\
            });\r\n\
        }\r\n\
        /**\r\n\
         * sonic超时处理\r\n\
         */\r\n\
//        setTimeout(function () {\r\n\
//            if (sonicHadExecute == 0) {\r\n\
//                sonicHadExecute = 1;\r\n\
//                sonicCallback(sonicStatus, reportSonicStatus, sonicUpdateData);\r\n\
//            }\r\n\
//        }, 5000);\r\n\
    });\r\n\
\r\n\
</script>\r\n\
</body>\r\n\
</html>');

return __p.join("");
};})();