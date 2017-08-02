## VasSonic For IOS
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---

## Getting started

## How to use in IOS
### Step 1: import and declare 
Build Sonic.framework for different platform,such as simulator or device;
Add Sonic.framework as a dependency in your main project. Then ```@import Sonic``` and register ```SonicURLProtocol``` in the ```AppDelegate```  :
```Objective-C
[NSURLProtocol registerClass:[SonicURLProtocol class]];

@interface SonicWebViewController : UIViewController<SonicSessionDelegate,UIWebViewDelegate>
```

### Step2: Implement custom ```SonicSessionDelegate```
```Objective-C
#pragma mark - Sonic Session Delegate
/*
 * Callback before Sonic send request
 */
- (void)sessionWillRequest:(SonicSession *)session
{
    //This callback can be used to set some information, such as cookie and UA.
}
/*
 * Sonic will request website to reload to be intercepted on NSURLProtocol layer. 
 */
- (void)session:(SonicSession *)session requireWebViewReload:(NSURLRequest *)request
{
    [self.webView loadRequest:request];
}
```

### Step3: Use Sonic in your own WebView ViewController
```Objective-C
/*
 * Send request when initialize ViewController 
 */
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
     * Then SonicWebRequest will be intercepted on NSURLProtocol layer when host allow the application to return the local data.
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
### Step5: Remove sonic session for the webDelegate.
```Objective-C

- (void)dealloc
{
[[SonicClient sharedClient] removeSessionWithWebDelegate:self];
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


