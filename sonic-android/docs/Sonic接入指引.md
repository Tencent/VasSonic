# 终端接入指引-Android版本
----
## 1.Sdk引入配置
在模块的build.gradle文件里面加入

```
compile 'com.tencent.sonic:sdk:1.1.1'
```

## 2.代码接入
### (1).创建一个类继承SonicRuntime
SonicRuntime类主要提供sonic运行时环境，包括Context、用户UA、ID(用户唯一标识，存放数据时唯一标识对应用户)等等信息。以下代码展示了SonicRuntime的几个方法。
```java
public class HostSonicRuntime extends SonicRuntime {
    public HostSonicRuntime(Context context) {
        super(context);
    }
    /**
     * 获取用户UA信息
     * @return
     */
    @Override
    public String getUserAgent() {
        return "";
    }
    /**
     * 获取用户ID信息
     * @return
     */
    @Override
    public String getCurrentUserAccount() {
        return "";
    }
    /**
     * 创建sonic文件存放的路径
     * @return
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

### (2).创建一个类继承SonicSessionClient

SonicSessionClient主要负责跟webView的通信，比如调用webView的loadUrl、loadDataWithBaseUrl等方法。

```java
public class SonicSessionClientImpl extends SonicSessionClient {
    private WebView webView;
    public void bindWebView(WebView webView) {
        this.webView = webView;
    }
    /**
     * 调用webView的loadUrl
     */
    @Override
    public void loadUrl(String url, Bundle extraData) {
        webView.loadUrl(url);
    }
    /**
     * 调用webView的loadDataWithBaseUrl方法
     */
    @Override
    public void loadDataWithBaseUrl(String baseUrl, String data, String mimeType, String encoding,                
                                    String historyUrl) {
        webView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }
}
```
### (3).新建包含webView的Activity(或者Fragment等)，在activity中完成sonic的接入。这里通过简单的demo展示如何接入

```java
public class SonicTestActivity extends Activity {


    public final static String PARAM_URL = "param_url";

    public final static String PARAM_MODE = "param_mode";

    private SonicSession sonicSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String url = intent.getStringExtra(PARAM_URL);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // init sonic engine if necessary, or maybe u can do this when application created
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
        }

        SonicSessionClientImpl sonicSessionClient = null;

        // if it's sonic mode , startup sonic session at first time
        SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
        // create sonic session and run sonic flow
        sonicSession = SonicEngine.getInstance().createSession(url, sessionConfigBuilder.build());
        if (null != sonicSession) {
            sonicSession.bindClient(sonicSessionClient = new SonicSessionClientImpl());
        } else {
            // this only happen when a same sonic session is already running,
            // u can comment following code to feedback for default mode to
            throw new UnknownError("create session fail!");
        }

        // start init flow ... in the real world, the init flow may cost a long time as startup
        // runtime、init configs....
        setContentView(R.layout.activity_browser);

        // init webview
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (sonicSession != null) {
                    sonicSession.getSessionClient().pageFinish(url);
                }
            }

            @TargetApi(21)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldInterceptRequest(view, request.getUrl().toString());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (sonicSession != null) {
                    return (WebResourceResponse) sonicSession.getSessionClient().requestResource(url);
                }
                return null;
            }
        });

        WebSettings webSettings = webView.getSettings();

        // add java script interface
        // note:if api level if lower than 17(android 4.2), addJavascriptInterface has security
        // issue, please use x5 or see https://developer.android.com/reference/android/webkit/
        // WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String)
        webSettings.setJavaScriptEnabled(true);
        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        intent.putExtra(SonicJavaScriptInterface.PARAM_LOAD_URL_TIME, System.currentTimeMillis());
        webView.addJavascriptInterface(new SonicJavaScriptInterface(sonicSessionClient, intent), "sonic");

        // init webview settings
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);


        // webview is ready now, just tell session client to bind
        if (sonicSessionClient != null) {
            sonicSessionClient.bindWebView(webView);
            sonicSessionClient.clientReady();
        } else { // default mode
            webView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (null != sonicSession) {
            sonicSession.destroy();
            sonicSession = null;
        }
        super.onDestroy();
    }
}
```

SonicTestActivity是一个含有webView的demo代码，里面展示了sonic的整体流程。主要分为6个步骤：

**Step1**：在activity onCreate的时候创建SonicRuntime并且初始化SonicEngine。为sonic初始化运行时需要的环境
```java
if (!SonicEngine.isGetInstanceAllowed()) {
    SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
}
```

**Setp2**：通过SonicEngine.getInstance().createSession来为要加载的url创建一个SonicSession对象，同时为session绑定client。session创建之后sonic就会异步加载数据了。
```java
SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
// create sonic session and run sonic flow
sonicSession = SonicEngine.getInstance().createSession(url, sessionConfigBuilder.build());
if (null != sonicSession) {
    sonicSession.bindClient(sonicSessionClient = new SonicSessionClientImpl());
}
```

**Step3**：设置javascript，这个主要是设置页面跟终端的js交互方式。
按照sonic的规范，webView打开页面之后页面会通过js来获取sonic提供的一些数据(比如页面需要刷新的数据)。Demo里使用的是标准的js交互代码，第三方可以替换为自己的js交互实现方式(比如提供jsbridge伪协议等)。
```java
webSettings.setJavaScriptEnabled(true);
webView.removeJavascriptInterface("searchBoxJavaBridge_");
webView.addJavascriptInterface(new SonicJavaScriptInterface(sonicSessionClient, intent), "sonic");
```

**Step4**：为clinet绑定webview，在webView准备发起loadUrl的时候通过SonicSession的onClientReady方法通知sonicSession： webView ready可以开始loadUrl了。这时sonic内部就会根据本地的数据情况执行webView相应的逻辑（执行loadUrl或者loadData等）。
```java
if (sonicSessionClient != null) {
    sonicSessionClient.bindWebView(webView);
    sonicSessionClient.clientReady();
}
```

**Step5**：在webView资源拦截的回调中调用session.onClientRequestResource(url)。通过这个方法向sonic获取url对应的WebResourceResponse数据。这样内核就可以根据这个返回的response的内容进行渲染了。(如果sonic在webView ready的时候执行的是loadData的话，是不会走到资源拦截这里的)
```java
public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
    if (sonicSession != null) {
        return (WebResourceResponse) sonicSession.getSessionClient().requestResource(url);
    }
    return null;
}
```






