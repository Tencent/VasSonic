## Getting started with iOS
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---

### Step 1: import and declare 
Build Sonic.framework;
Add Sonic.framework to dependency in your main project. Then ```@import Sonic``` and register ```SonicURLProtocol``` :
```Objective-C
[NSURLProtocol registerClass:[SonicURLProtocol class]];

@interface SonicWebViewController : UIViewController<SonicSessionDelegate,UIWebViewDelegate>
```

### Step2: Implement ```SonicSessionDelegate```
```Objective-C
#pragma mark - Sonic Session Delegate
/*
 * Call back when Sonic will send request.
 */
- (void)sessionWillRequest:(SonicSession *)session
{
    //This callback can be used to set some information, such as cookie and UA.
}
/*
 * Call back when Sonic require WebView to reload, e.g template changed or error occurred. 
 */
- (void)session:(SonicSession *)session requireWebViewReload:(NSURLRequest *)request
{
    [self.webView loadRequest:request];
}
```

### Step3: Use Sonic in WebView ViewController
```Objective-C
- (instancetype)initWithUrl:(NSString *)aUrl
{
    if (self = [super init]) {
        
        self.url = aUrl;
        
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
     */
    if ([[SonicClient sharedClient] sessionWithWebDelegate:self]) {
        [self.webView loadRequest:sonicWebRequest(request)];
    }else{
        [self.webView loadRequest:request];
    }
}
```

### Step4: Interacts with websites by JavaScript callback.
```Objective-C

- (void)getDiffData:(NSDictionary *)option withCallBack:(JSValue *)jscallback
{
    /*
     * ViewController which sends the Sonic request and return result through callback. 
     */
    [[SonicClient sharedClient] sonicUpdateDiffDataByWebDelegate:self.owner completion:^(NSDictionary *result) {
       
        /*
         * Return the result.
         */
        NSData *json = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:nil];
        NSString *jsonStr = [[NSString alloc]initWithData:json encoding:NSUTF8StringEncoding];
        
        JSValue *callback = self.owner.jscontext.globalObject;
        [callback invokeMethod:@"getDiffDataCallback" withArguments:@[jsonStr]];
        
    }];
}
```
### Step5: Remove sonic session.
```Objective-C

- (void)dealloc
{
[[SonicClient sharedClient] removeSessionWithWebDelegate:self];
}
```

## Support
Any problem?

1. Learn more from [sample](https://github.com/Tencent/VasSonic/tree/master/sonic-iOS/SonicSample).
2. Contact us for help.

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.

[1]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120005424.gif
[2]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120029897.gif


