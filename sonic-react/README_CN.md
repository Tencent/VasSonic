基于 [React](https://reactjs.org/) 同构直出的 Sonic 使用示例。[React](https://reactjs.org/) 同构直出使用 [Redux](https://redux.js.org/)/[Next.js](https://github.com/zeit/next.js/)/[Koa2](http://koajs.com/) 实现。

[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)

<details>
<summary><strong>目录</strong></summary>

* [技术栈](#feature)
* [快速开始](#getting-start)
  + [安装](#installation)
  + [启动](#start)
  + [脚本](#script)
* [项目架构](#project-structure)
  + [目录结构](#file-tree)
  + [原理](#principle)
* [技术支持](#support)
* [License](#license)

</details>

## <a name="feature">&sect; 技术栈</a>

- [React](https://reactjs.org/)
- [Redux](https://redux.js.org/)
- [Next.js](https://github.com/zeit/next.js/)
- [Koa2](http://koajs.com/)
- [ES6](http://babeljs.io/learn-es2015/)

---

## <a name="getting-start">&sect; 快速开始</a>

> **推荐升级**到 node 8.x + npm 5.x 环境。

### <a name="installation">⊙ 安装</a>

```bash
git clone https://github.com/Tencent/VasSonic.git <my-project-name>
cd <my-project-name>/sonic-react
npm install  # 安装项目依赖
```

### <a name="start">⊙ 启动</a>

```bash
npm run build
npm start
```
![](http://pub.idqqimg.com/pc/misc/files/20171222/1f820357f8de420e8a267040522645d8.png)

手机端安装 Android 或 iOS 测试用应用程序。（[下载](https://github.com/Tencent/VasSonic/releases)）

然后将手机与服务器连接在同一局域网下，查看服务器 ip 配置手机代理，并设置测试链接地址为 http://服务器ip:3000/demo

1：设置手机代理 | 2：设置测试链接
:-------------------------:|:-------------------------:
<img width="285" height="500" src="http://pub.idqqimg.com/pc/misc/files/20180103/ef3a88fce92f41a18bacd5d335b4aeef.jpg" alt="设置手机代理" />  |  <img width="285" height="500" src="http://pub.idqqimg.com/pc/misc/files/20180103/c3f3d07093ca42be988ccc5dddce2be7.png" alt="设置测试链接" />

3：访问 demo | 4：效果演示
:-------------------------:|:-------------------------:
<img width="285" height="500" src="http://pub.idqqimg.com/pc/misc/files/20180103/26302761e2f9465e955b510a69ebd0a9.png" alt="访问demo" />  |  <img width="285" height="500" src="http://pub.idqqimg.com/pc/misc/files/20180103/04a44f93ad454ea5ae51d0e2f2bf319b.jpg" alt="demo" />

### <a name="script">⊙ 脚本</a>

| `npm run <script>`  | 描述                                         |
| ------------------- | ------------------------------------------- |
| `start`             | 启动服务(生产环境，需先执行 npm run build 命令) |
| `dev`               | 启动服务(开发环境，无需执行 npm run build 命令) |
| `build`             | 打包构建到目录 .next                        |

---

## <a name="project-structure">&sect; 项目架构</a>

### <a name="file-tree">⊙ 目录结构</a>

```bash
.
├── components               # demo 页面视图组件
├── containers               # demo 页面容器组件
├── pages                    # Next.js 用于存放每个页面入口组件的目录
│   └── demo.js              # demo 页面入口 js
├── redux                    # Redux 相关模块
│   └── duck.js              # ducks 模式组织 redux 模块
├── static                   # Next.js 用于存放静态资源的目录
└── server.js                # 服务入口 js
```

### <a name="principle">⊙ 原理</a>

我们不去深究 React 直出以及示例中拼图游戏逻辑的实现，主要来说明下示例中是如何在 React 项目中使用 Sonic 的，流程图如下所示：

![](http://pub.idqqimg.com/pc/misc/files/20180104/e6f7c4331ed441eb88d3c693617d0bf9.png)

- 服务端拦截 React 渲染出来的HTML字符串，添加 HTML 注释标签来帮助 Sonic 区分模板和数据块。数据块需要通过 `<!-- sonicdiff-moduleName -->` `<!-- sonicdiff-moduleName-end -->` 来标记，剩下的部分称为模版。示例中代码实现如下：
```js
/**
 * 添加 Sonic 所需的 HTML 注释标签
 *
 * 举例:
 * <!DOCTYPE html>                                                 <!DOCTYPE html>
 * <html>                                                          <html>
 * <head></head>                                                   <head></head>
 * <body>                                                          <body>
 * … …                                                             … …
 * <div id="root" data-sonicdiff="firstScreenHtml">      =>        <!-- sonicdiff-firstScreenHtml -->
 *     … …                                                         <div id="root" data-sonicdiff="firstScreenHtml">
 * </div>                                                              … …
 * … …                                                             </div>
 * <script>                                                        <!-- sonicdiff-firstScreenHtml-end -->
 *     __NEXT_DATA__=xxx                                           … …
 * </script>                                                       <!-- sonicdiff-initState -->
 * </body>                                                         <script>
 *                                                                     __NEXT_DATA__=xxx
 *                                                                 </script>
 *                                                                 <!-- sonicdiff-initState-end -->
 *                                                                 </body>
 *
 * @param html {string} 原始 HTML 字符串
 * @returns {string} 添加注释标签后的 HTML 字符串
 */
function formatHtml(html) {
    const $ = cheerio.load(html);
    $('*[data-sonicdiff]').each(function(index, element) {
        let tagName = $(this).data('sonicdiff');
        return $(this).replaceWith('<!--sonicdiff-' + tagName + '-->' + $(this).clone() + '<!--sonicdiff-' + tagName + '-end-->');
    });
    html = $.html();
    html = html.replace(/<script\s*>\s*__NEXT_DATA__\s*=([\s\S]+?)<\/script>/ig, function(data1) {
        return '<!--sonicdiff-initState-->' + data1 + '<!--sonicdiff-initState-end-->';
    });
    return html;
}
```

- 服务端使用 [sonic_differ](https://github.com/Tencent/VasSonic/blob/master/sonic-nodejs/common/diff.js) 模块对数据进行处理后输出给浏览器。
```js
server.use(async (ctx, next) => {
    await next();

    // 只拦截 html 请求
    if (!ctx.response.is('html')) {
        return;
    }

    // 非 sonic 模式不做特殊处理
    if (!ctx.request.header['accept-diff']) {
        ctx.body = ctx.state.resHtml;
        return;
    }

    // 使用 sonic_differ 模块对数据进行处理
    let sonicData = sonicDiff(ctx, formatHtml(ctx.state.resHtml));

    if (sonicData.cache) {
        // sonic 模式：完全缓存
        ctx.body = '';
    } else {
        // 其它 sonic 状态
        ctx.body = sonicData.data;
    }
});
```

- 前端在执行到 componentDidMount() 阶段时，通过 js 调用终端接口来获取 sonic 状态和数据，根据终端返回的不同状态，来决定如何渲染页面。
```js
componentDidMount() {
    // 获取客户端返回的 sonic 状态和数据，根据终端返回数据做出相应的处理
    this.getSonicData((status, sonicUpdateData) => {
        switch (status) {
            // sonic 状态：数据更新
            case 3:
                // 使用客户端返回的数据更新页面 Store
                let initState = sonicUpdateData['{initState}'] || '';
                initState.replace(/<!--sonicdiff-initState-->\s*<script>\s*__NEXT_DATA__\s*=([\s\S]+?)module=/ig, function(matched, $1) {
                    window.__NEXT_DATA__ = JSON.parse($1);
                });
                this.props.initImgArr(window.__NEXT_DATA__.props.initialState.gameArea);
                break;
            default:
                break
        }
        // 展示 sonic 状态
        this.props.setSonicStatus(status);
    });
}

getSonicData(callback) {
    let sonicHadExecute = 0;   // 判断回调是否触发过的标识
    const timeout = 3000;      // 终端接口 3s 内没有响应，触发超时逻辑

    // 调用终端接口通知客户端进行 sonic 处理逻辑
    window.sonic && window.sonic.getDiffData();

    function sonicCallback(data) {
        if (sonicHadExecute === 0) {
            sonicHadExecute = 1;
            callback(data['sonicStatus'], data['sonicUpdateData']);
        }
    }

    setTimeout(function() {
        if (sonicHadExecute === 0) {
            sonicHadExecute = 1;
            callback(0, {});
        }
    }, timeout);

    // 终端调用 getDiffDataCallback 方法将数据传递给页面
    window['getDiffDataCallback'] = function (sonicData) {
        /**
         * Sonic 状态:
         * 0: 异常
         * 1: 首次加载（首次和正常页面逻辑一样，前端无需特殊处理）
         * 2: 模板更新（当模版发生变化时，终端会自动刷新当前页面，前端也无需特殊处理）
         * 3: 数据更新（sonic页面模版没有变化，只有数据块发生变化，终端会返回变化的数据块名称和内容，前端只需要把变化的内容替换到页面即可）
         * 4: 完全缓存（sonic页面模版和数据都没有变化，页面无需任何处理）
         */
        let sonicStatus = 0;
        let sonicUpdateData = {};  // 数据更新时终端返回的数据
        sonicData = JSON.parse(sonicData);
        switch (parseInt(sonicData['srcCode'], 10)) {
            case 1000:
                sonicStatus = 1;
                break;
            case 2000:
                sonicStatus = 2;
                break;
            case 200:
                sonicStatus = 3;
                sonicUpdateData = JSON.parse(sonicData['result'] || '{}');
                break;
            case 304:
                sonicStatus = 4;
                break;
        }
        sonicCallback({ sonicStatus: sonicStatus, sonicUpdateData: sonicUpdateData });
    };
}
```

---

## <a name="support">&sect; 技术支持</a>
遇到其他问题，可以：

1. 通过demo来理解 [sample](https://github.com/Tencent/VasSonic/tree/master/sonic-react)。
2. 联系我们。

## <a name="license">&sect; License</a>
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.