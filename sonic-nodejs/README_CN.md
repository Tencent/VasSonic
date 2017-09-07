## 开始使用
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---

## Node端的使用

### 依赖关系

1）Node版本必须大于7，因为代码中使用了async/await语法

2）安装`sonic_differ`模块

```Node.js
npm install sonic_differ --save
```

简单情况下直接npm install 即可

3）后端代码中引用`sonic_differ`模块

```Node.js
const differ = require('sonic_differ');
```

### Sonic模式中，需要对数据进行拦截与加工处理

1）第一步，新建一个sonic结构体，主要是方便操作，大家理解之后可以自行修改

```Node.js
let sonic = {
    buffer: [],
    write: function (chunk, encoding) {
        let buffer = chunk;
        let ecode = encoding || 'utf8';
        if (!Buffer.isBuffer(chunk)) {
            buffer = new Buffer(chunk, ecode);
        }
        sonic.buffer.push(buffer);
    }
};
```

2）第二步，拦截服务端的数据，用`sonic_differ`模块对数据进行处理，这里大家理解之后可以根据自己的后端改造，本质就是直出的内容用`sonic_differ`模块进行一次二次加工，再输出给前端		
 	
 ```Node.js		
 response.on('data', (chunk, encoding) => {		
     sonic.write(chunk, encoding)		
 });		
 response.on('end', () => {		
     let result = differ(ctx, Buffer.concat(sonic.buffer));		
     sonic.buffer = [];		
     if (result.cache) {		
         //304 Not Modified, return nothing.		
         return ''		
     } else {		
         //other Sonic status.		
         return result.data		
     }		
 });		
 ```
 
 ## 前端的使用

通过一个简单的demo来说明一下前端使用方法。
```Html
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <title>demo</title>
    <script type="text/javascript">
            
            // 调用终端接口触发终端去获取diff data
            function getDiffData(){
                window.sonic.getDiffData();
            }
            
            // step 3: 添加响应方法，用于获取终端返回的diff data
           function getDiffDataCallback(result){
                var sonicStatus = 0; 
                /**
                * The Sonic status:
                * 0: It fails to get any data from mobile client.
                * 1: It is first time for mobile client to use Sonic.
                * 2: Mobile client reload the whole websites.
                * 3: Websites will be updated dynamically with local refresh.
                * 4: The Sonic request of mobile client receives a 304 response code and nothing has been modified.
                */
                sonicUpdateData = {}; //sonic diff data
                var result = JSON.parse(result);
                if(result['code'] == 200){
                    sonicStatus = 3;
                    sonicUpdateData = JSON.parse(result['result']);
                } else if (result['code'] == 1000) {
                    sonicStatus = 1;
                } else if (result['code'] == 2000) {
                    sonicStatus = 2;
                } else if(result['code'] == 304) {
                    sonicStatus = 4;
                }
                handleSonicDiffData(sonicStatus, sonicUpdateData);
            }
            
            // step 3: 业务上根据diff data来做渲染处理
            function handleSonicDiffData(sonicStatus, sonicUpdateData){
                if(sonicStatus == 3){
                    //Websites will be updated dynamically and run some JavaScript while in local refresh mode. 
                    var html = '';
                    var id = '';
                    var elementObj = '';
                    for(var key in sonicUpdateData){
                        id = key.substring(1,key.length-1);
                        html = sonicUpdateData[key];
                        elementObj = document.getElementById(id+'Content');
                        elementObj.innerHTML = html;
                    }
                }
            }
    </script>
</head>

<body>
    // step 1: 通过html注释标记模版和数据
    <div id="data1Content">
        <!--sonicdiff-data1-->
        <p id="partialRefresh"></p>
        <!--sonicdiff-data1-end-->
    </div>
    <div id="data2Content">
        <!--sonicdiff-data2-->
        <p id="data2">here is the data</p>
        <!--sonicdiff-data2-end-->
        <p id="pageRefresh"></p>
    </div>
    <div id = "data3">data3</div>
    
    // step 2: 调用终端接口获取diff data
    <script type="text/javascript">
        window.onload = function(){
            getDiffData();
        }
    </script>
</body>
</html>
```

### Step 1:
通过注释来标记模版和数据部分。 数据快需要通过 ```<!-- sonicdiff-moduleName -->```  ```<!-- sonicdiff-moduleName-end -->```来标记。其他部分则为模版。
```Html
    <div id="data1Content">
        <!--sonicdiff-data1-->
        <p id="partialRefresh"></p>
        <!--sonicdiff-data1-end-->
    </div>
    <div id="data2Content">
        <!--sonicdiff-data2-->
        <p id="data2">here is the data</p>
        <!--sonicdiff-data2-end-->
        <p id="pageRefresh"></p>
    </div>
    <div id = "data3">data3</div>
```

### Step 2:
js调用终端接口来获取diff data。 本demo中是在页面加载完成后调用js接口，实际调用js接口的时机可以由各个业务自定。
```Html
<script type="text/javascript">
     window.onload = function(){
         getDiffData();
     }
</script>
```

### Step 3:
通过终端返回的状态来决定是否重新渲染。 在本demo中，根据终端返回的状态，来获取对应的数据块，继而重新渲染页面。
```Html
//step 3 Handle the response from mobile client which include Sonic response code and diff data.  
function getDiffDataCallback(result){
｝
//step 3 Handle the response from mobile client which include Sonic response code and diff data.  
function handleSonicDiffData(sonicStatus, sonicUpdateData){
｝
```

## 技术支持
遇到其他问题，可以：

1. 通过demo来理解 [sample](https://github.com/Tencent/VasSonic/tree/master/sonic-nodejs)。
2. 联系我们。

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.

[1]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120005424.gif
[2]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120029897.gif
