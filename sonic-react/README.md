## Getting started with React
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---

## How to use the demo

>This demo will give you a quick start for using Sonic with React.

### Dependencies
Node Version > 7.0
### Installation
```bash
git clone https://github.com/Tencent/VasSonic.git <my-project-name>
cd <my-project-name>/sonic-react
npm install  # Install project dependencies
```
### Usage
```bash
npm run build  # Builds the application to ./.next
npm start      # Start the development server
```
Now you can visit http://localhost:3000/demo to view this demo using Mobile Emulation Mode in Chrome dev tools.

## How to use Sonic on server-side

>NOTE: This demo using Server Side Rendering (SSR) with [Redux](https://redux.js.org/), [Next.js](https://github.com/zeit/next.js/) and [Koa2](http://koajs.com/).

1. Add comment tags to separate **template** and **data blocks** in html files which will be published from the server. The **data blocks** should begin with a html comment like `<!-- sonicdiff-moduleName -->` and close with `<!-- sonicdiff-moduleName-end -->`(the moduleName is custom). And the other part of the html is called **template** in Sonic. In this demo, it is implemented like the code shown below.

- Below is the origin html, we will generate comment tags according to the `data-sonicdiff` attribute and the script block including `__NEXT_DATA__`:

```HTML
<!-- add comment tags to separate template and data blocks from the initial html -->
<!DOCTYPE html>
<html>
  <head></head>
  <body>
    … …
    <div id="root" data-sonicdiff="firstScreenHtml">
    … …
    </div>
    … …
    <script>
      __NEXT_DATA__=xxx
    </script>
  </body>
</html>
```

- Then, we have a transform function at server side:

```js
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
- After the transform, the latest code user will received will be like:

```HTML
<!-- add comment tags to separate template and data blocks from the initial html -->
<!DOCTYPE html>
<html>
  <head></head>
  <body>
    … …
+   <!-- sonicdiff-firstScreenHtml -->
    <div id="root" data-sonicdiff="firstScreenHtml">
    … …
    </div>
+   <!-- sonicdiff-firstScreenHtml-end -->
    … …
+   <!-- sonicdiff-initState -->
    <script>
      __NEXT_DATA__=xxx
    </script>
+   <!-- sonicdiff-initState-end -->
  </body>
</html>
```

2. Intercept the html response from server and use [sonic_differ](https://github.com/Tencent/VasSonic/blob/master/sonic-nodejs/common/diff.js) module to process the response.

```js
server.use(async (ctx, next) => {
    await next();
    // only intercept html request
    if (!ctx.response.is('html')) {
        return;
    }
    // process non-sonic mode
    if (!ctx.request.header['accept-diff']) {
        ctx.body = ctx.state.resHtml;
        return;
    }
    // use sonic_differ module to process the response
    let sonicData = sonicDiff(ctx, formatHtml(ctx.state.resHtml));
    if (sonicData.cache) {
        // 304 Not Modified, return nothing.
        ctx.body = '';
    } else {
        // other Sonic status.
        ctx.body = sonicData.data;
    }
});
```

  For more details please refer to [server.js](https://github.com/Tencent/VasSonic/blob/master/sonic-react/server.js).

## How to use Sonic on client-side

Handle the response from mobile client which include Sonic response code and diff data in `componentDidMount()`.

```js
componentDidMount() {
    // handle the response from mobile client which include Sonic response code and diff data.
    this.getSonicData((status, sonicUpdateData) => {
        switch (status) {
            // here, we only process the case when data updates
            case 3:
                // update the Redux store based on changes from the mobile client
                let initState = sonicUpdateData['{initState}'] || '';
                initState.replace(/<!--sonicdiff-initState-->\s*<script>\s*__NEXT_DATA__\s*=([\s\S]+?)module=/ig, function(matched, $1) {
                    window.__NEXT_DATA__ = JSON.parse($1);
                });
                this.props.initImgArr(window.__NEXT_DATA__.props.initialState.gameArea);
                break;
            default:
                break
        }
        // display sonic status
        this.props.setSonicStatus(status);
    });
}

getSonicData(callback) {
    let sonicHadExecute = 0;   // whether the callback is triggered
    const timeout = 3000;      // a timeout to trigger callback

    // Interacts with mobile client by JavaScript interface to get Sonic diff data.
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

    // the mobile client will invoke method getDiffDataCallback which can send Sonic response code and diff data to websites.
    window['getDiffDataCallback'] = function (sonicData) {
        /**
         * Sonic status:
         * 0: It fails to get any data from mobile client.
         * 1: It is first time for mobile client to use Sonic.
         * 2: Mobile client reload the whole websites.
         * 3: Websites will be updated dynamically with local refresh.
         * 4: The Sonic request of mobile client receives a 304 response code and nothing has been modified.
         */
        let sonicStatus = 0;
        let sonicUpdateData = {};  // sonic diff data
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
For more details please refer to [demo.js](https://github.com/Tencent/VasSonic/blob/master/sonic-react/pages/demo.js)

## Support
Any problem?

1. Learn more from [sample](https://github.com/Tencent/VasSonic/tree/master/sonic-react).
2. Contact us for help.

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.
