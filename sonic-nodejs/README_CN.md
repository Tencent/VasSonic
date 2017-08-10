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
 ## Support
Any problem?

1. Learn more from [sample](https://github.com/Tencent/VasSonic/tree/master/sonic-nodejs).
2. Contact us for help.

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.

[1]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120005424.gif
[2]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120029897.gif