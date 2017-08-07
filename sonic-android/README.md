## Getting started with Android
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---

## Getting started

## build.gradle:

Add VasSonic gradle plugin as a dependency in your module's build.gradle
```gradle
compile 'com.tencent.sonic:sdk:1.0.0'
```

## Implement sonic interface:
1. Implement a class which extends from ```SonicRuntime```

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
2. Implement a subclass which extends from ```SonicSessionClient```

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
## Android Demo
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


