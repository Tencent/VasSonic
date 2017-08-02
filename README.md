## VasSonic
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---
 VasSonic is a lightweight and high-performance Hybrid framework which is intended to speed up the first screen of websites working on Android and IOS platform.
 Not only does VasSonic supports the static or dynamic websites which are rendered by server, but it is also compatible with web offline resource perfectly.

 VasSonic uses custom url connection instead of original network connection to request the index html, so it can request resource in advance or parallel to avoid waiting for the view initialization.
 In this parallel case, VasSonic can read and render partial data by WebKit kernel without spending too much time waiting for the end of data stream.

 VasSonic can cache html cleverly according to VasSonic Specification obeyed by client and server.
 VasSonic Specification specify template and data by inserting different comment anchor, templates are bigger parts of html which stay the same or changed rarely , in contradiction data, which is the smaller and constantly change part of html.
 According to this, VasSonic request less data by incremental updating templates and data, the websites are faster and feel more like native application.
 In conclusion, VasSonic effectively enhance the user experience and increase click rate, retention rate and other indicators.

#### Pic 1: default mode  </br>
![default mode][1]    

#### Pic 2: VasSonic mode </br>
![VasSonic mode][2]

 
## Getting started
### How to use in Android
#### build.gradle:

Add VasSonic gradle plugin as a dependency in your module's build.gradle
```gradle
compile 'com.tencent.sonic:sdk:1.0.0'
```

Create a class that subclasses ```SonicRuntime```

> SonicRuntime is a class which interacts with the overall running information in the system, including Context, UA, ID (which is the unique identification for the saved data) and other information.

```Java
/**
* Here is a sample subclass of SonicRuntime
*/
public class HostSonicRuntime extends SonicRuntime {
    public HostSonicRuntime(Context context) {
        super(context);
    }
    /**
     * @return @return Returns User's UA
     */
    @Override
    public String getUserAgent() {
        return "";
    }
    /**
     * @return Returns the ID of user.
     */
    @Override
    public String getCurrentUserAccount() {
        return "";
    }
    /**
     * @return Returns the file path which is used to save Sonic caches.
     */
    @Override
    public File getSonicCacheDir() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator         + "sonic/";
        File file = new File(path.trim());
        if(!file.exists()){
            file.mkdir();
        }
        return file;
    }
}
```
Create a subclass of ```SonicSessionClinet```

```Java
/**
 *
 * SonicSessionClient  is a thin API class that delegates its public API to a backend WebView class instance, such as loadUrl and loadDataWithBaseUrl.
 */
public class SonicSessionClientImpl extends SonicSessionClient {
    private WebView webView;
    public void bindWebView(WebView webView) {
        this.webView = webView;
    }
    
    @Override
    public void loadUrl(String url, Bundle extraData) {
        webView.loadUrl(url);
    }

    @Override
    public void loadDataWithBaseUrl(String baseUrl, String data, String mimeType, String encoding,                
                                    String historyUrl) {
        webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }
}
```
#### Android Demo
Here is a demo shows how to create an Android activity which uses the VasSonic Framework
```Java
public class SonicTestActivity extends Activity {
    WebView webView;
    SonicSessionClientImpl sessionClient;
    WebViewClient webViewClient;
    SonicSession session;
    String url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        int sonicMode = intent.getIntExtra("sonicMode", SonicSessionConfig.SESSION_MODE_DEFAULT);
        //step 1 Initialize SonicEngine before webview loadUrl
        SonicRuntime runtime = new HostSonicRuntime(this.getApplication());
        SonicEngine.createInstance(runtime, new SonicConfig());
        //step 2 Create SonicSession
        SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
        sessionConfigBuilder.setSessionMode(sonicMode);
        SonicSessionConfig config = sessionConfigBuilder.build();
        session = SonicEngine.getInstance().createSession(url, config);
        setContentView(R.layout.test);
        LinearLayout root = (LinearLayout) findViewById(R.id.root);
        webView = new WebView(this);
        LinearLayout.LayoutParams lp = new  LinearLayout.LayoutParams
                (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        root.addView(webView, lp);
        webView.setVisibility(View.VISIBLE);
        //step 3 BindWebView for sessionClient and bindClient for SonicSession
        if (SonicEngine.getInstance().getRuntime().isSonicUrl(url)) {
            if (session != null) {
                sessionClient = new SonicSessionClientImpl();
                sessionClient.bindWebView(webView);
                session.bindClient(sessionClient);
            }
        }
        webViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(sessionClient != null){
                    sessionClient.pageFinish(url);
                }
            }
            @TargetApi(21)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, 
                                             WebResourceRequest request) {
                                             
                return doIntercept(view, request.getUrl().toString());
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return doIntercept(view, url);
            }
        };
        webView.setWebViewClient(webViewClient);
        WebChromeClient chromeClient = new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        };
        webView.setWebChromeClient(chromeClient);
        String cookie = "testCookie=1";
        ArrayList<String> cookies = new ArrayList<>(1);
        cookies.add(cookie);
        runtime.setCookie(url, cookies);
        WebSettings webSettings = webView.getSettings();
        //step 4 set javascript
        webSettings.setJavaScriptEnabled(true);
        intent.putExtra("loadUrlTime", System.currentTimeMillis());
        webView.addJavascriptInterface(new SonicJavaScript(sessionClient, intent), "sonic");
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        //step 5 Notify sonicSession： webView ready，then it starts to load url.
        if (sessionClient != null) {
            sessionClient.clientReady();
        } else {
            webView.loadUrl(url);
        }
    }
    @Override
    protected void onDestroy() {
        if (session != null) {
            session.destroy();
        }
        super.onDestroy();
    }
    private WebResourceResponse doIntercept(WebView view, String url) {
        //step 6 Call sessionClient.requestResource when host allow the application 
        // to return the local data .
        if (sessionClient != null) {
            return (WebResourceResponse) sessionClient.requestResource(url);
        }
        return null;
    }
}
```

#### How to use in IOS
Step 1: import and declare 
Add Sonic.framework as a dependency in your main project. Then register ```SonicURLProtocol``` in AppDelegate and import ```Sonic.h```:
```Objective-C
[NSURLProtocol registerClass:[SonicURLProtocol class]];

@interface SonicWebViewController : UIViewController<SonicSessionDelegate,UIWebViewDelegate>
```
Step2: Implement custom ```SonicSessionDelegate```
```Objective-C
#pragma mark - Sonic Session Delegate
/*
 * Callback before Sonic send request
 */
- (void)sessionWillPerformRequest:(SonicSession *)session
{
    //This callback can be used to set some information, such as cookie and UA.
}
/*
 * Sonic will request website to reload to be intercepted on NSURLProtocol layer in Template-Update mode. 
 */
- (void)sessionRefreshReload:(SonicSession *)session
{
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.url]];
    [self.webView loadRequest:sonicWebRequest(request)];
}
/*
 * Sonic will use original network connection to send request again while failed.
 */
- (void)sessionRequireNormalReload:(SonicSession *)session
{
    NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:self.url]];
    [self.webView loadRequest:request];
}
```

Step3: Use Sonic in your own WebView ViewController
```Objective-C
/*
 * Send request when initialize ViewController 
 */
- (instancetype)initWithUrl:(NSString *)aUrl
{
    if (self = [super init]) {
        
        self.url = aUrl;
        
        self.clickTime = (long long)[[NSDate date]timeIntervalSince1970]*1000; 
        //Create a Sonic session with url.
        [[SonicClient sharedClient] createSessionWithUrl:self.url withWebDelegate:self];
    }
    return self;
}
/*
 * Send request with Sonic property immediately after the WebView initialization.
 */
- (void)loadView
{
    [super loadView];
    
    self.webView = [[UIWebView alloc]initWithFrame:self.view.bounds];
    self.webView.delegate = self;
    self.view = self.webView;
    
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.url]];
    
    /*
     * If SonicSession is not null, Sonic uses custom SonicWebRequest instead of original network request. 
     * Then SonicWebRequest will be intercepted on NSURLProtocol layer when host allow the application to return the local data.
     */
    if ([[SonicClient sharedClient] sessionWithWebDelegate:self]) {
        [self.webView loadRequest:sonicWebRequest(request)];
    }else{
        [self.webView loadRequest:request];
    }
}
```

Step4: Interacts with websites by JavaScript callback.
```Objective-C

- (void)getDiffData:(NSDictionary *)option withCallBack:(JSValue *)jscallback
{
    /*
     * ViewController which sends the Sonic request queries and send back result 
     */
    [[SonicClient sharedClient] sonicUpdateDiffDataByWebDelegate:self.owner completion:^(NSDictionary *result) {
       
        /*
         * Send back the result
         */
        NSData *json = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:nil];
        NSString *jsonStr = [[NSString alloc]initWithData:json encoding:NSUTF8StringEncoding];
        
        JSValue *callback = self.owner.jscontext.globalObject;
        [callback invokeMethod:@"getDiffDataCallback" withArguments:@[jsonStr]];
        
    }];
}
```
#### How to use for front-end
Here is a simple demo shows how to use Sonic for front-end.
```Html
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <title>demo</title>
    <script type="text/javascript">
            
            //Interacts with mobile client by JavaScript interface to get Sonic diff data.
            function getDiffData(){
                window.sonic.getDiffData();
            }
            //step 3 Handle the response from mobile client which include Sonic response code and diff data.   
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
            //step 3 Handle the response from mobile client which include Sonic response code and diff data.  
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
    //step 1 specify template and data by inserting different comment anchor.
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
    
    //step 2 Receives diff data from mobile client through Javascript interface.
    <script type="text/javascript">
         window.function(){
                getDiffData();
    }
    </script>
</body>
</html>
```
##### Step 1:
Specify template and data by inserting different comment anchor. The data will be wrapped with anchor ```<!-- sonicdiff-moduleName -->```  ```<!-- sonicdiff-moduleName-end -->```. The other part of html is template.
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

##### Step 2:
Receives diff data from mobile client through JavaScript interface. The JavaScript interface of demo was involved when websites are finish. But the time when inferface was involved is not immutable, websites can decide whenever they want.
```Html
<script type="text/javascript">
     window.function(){
            getDiffData();
}
</script>
```

##### Step 3:
Handle different status received from mobile client. The demo shows how to find and replace the data of specified anchor according to the diff data come from mobile client, then the website is updated.
```Html
//step 3 Handle the response from mobile client which include Sonic response code and diff data.  
function getDiffDataCallback(result){
｝
//step 3 Handle the response from mobile client which include Sonic response code and diff data.  
function handleSonicDiffData(sonicStatus, sonicUpdateData){
｝
```

#### How to use for Server
##### PHP Version
Download and import ```sonic.php```. Then add following code.
```PHP
require_once(PATH."/sonic.php");


if (isset($_GET['sonic']) && $_GET['sonic'] == '1') {
// Check if Sonic is needed or not 
    util_sonic::start();
    $this->_index_v5($uin);
    util_sonic::end();
}
```

##### Node.js Version

1、Introduction

This is the server part of Sonic Project.

2、Step

1）Node Version > 7.0

2）install **sonic_differ** module


```Node.js
npm install sonic_differ --save
```

3）import **sonic_differ** module

```Node.js
const sonic_differ = require('sonic_differ');
```

4）Intercept and process data from server in Sonic mode.

i）First, create a Sonic cache struct like following code.

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

ii）Second, Intercept the data from server and use `sonic_differ` module to process

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

1. Learn more from sample.
2. Read the source code.
3. Read the [wiki](https://github.com/Tencent/VasSonic/wiki) for help.
4. Contact us for help.

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.

[1]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120005424.gif
[2]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120029897.gif


