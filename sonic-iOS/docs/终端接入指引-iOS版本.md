
## 终端接入指引-iOS版本

### 1. 引入头文件，声明协议

### (1.1)将Sonic.framework或者Sonic源码拖入工程

      在AppDelegate中注册SonicURLProtocol:

      [NSURLProtocol registerClass:[SonicURLProtocol class]];

### (1.2)引入 @import Sonic;

      @interface SonicWebViewController : UIViewController<SonicSessionDelegate,UIWebViewDelegate>


## 2. 实现SonicSessionDelegate

#pragma mark - Sonic Session Delegate

/*
 * sonic请求发起前回调
 */
- (void)sessionWillRequest:(SonicSession *)session
{
    //可以在请求发起前同步Cookie等信息
}

/*
 * sonic要求webView重新load指定request
 */
- (void)session:(SonicSession *)session requireWebViewReload:(NSURLRequest *)request
{
    [self.webView loadRequest:request];
}

## 3. 在WebView的ViewController中接入Sonic使用 (Sample:SonicWebViewController)

/*
 * 在初始化ViewController的时候发起sonic的请求
 */
- (instancetype)initWithUrl:(NSString *)aUrl
{
    if (self = [super init]) {
        
        self.url = aUrl;
        
        self.clickTime = (long long)[[NSDate date]timeIntervalSince1970]*1000; 

        //使用sonic链接创建一个会话
        [[SonicClient sharedClient] createSessionWithUrl:self.url withWebDelegate:self];
    }
    return self;
}

/*
 * 在初始化WebView之后立即发起带有sonic信息的请求
 */
- (void)loadView
{
    [super loadView];
    
    self.webView = [[UIWebView alloc]initWithFrame:self.view.bounds];
    self.webView.delegate = self;
    self.view = self.webView;
    
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.url]];
    
    /*
     * 查询当前ViewController是否成功创建sonic会话，如果已经创建，那么包装request成sonic请求，以便在NSURLProtocol层拦截
     * 否则走正常模式加载请求，不会在NSURLProtocol层拦截
     */
    if ([[SonicClient sharedClient] sessionWithWebDelegate:self]) {
        [self.webView loadRequest:sonicWebRequest(request)];
    }else{
        [self.webView loadRequest:request];
    }
}

## 4. 调用获取差异的接口，传递sonic会话的结果信息

/*
 * 此接口由页面驱动，由前端sonic组件向终端发起请求获取会话结果
 */
- (void)getDiffData:(NSDictionary *)option withCallBack:(JSValue *)jscallback
{
	/*
	 * 根据发起sonic会话的ViewController来查询需要的结果
	 */
    [[SonicClient sharedClient] sonicUpdateDiffDataByWebDelegate:self.owner completion:^(NSDictionary *result) {
       
        /*
         * 这里将result传递回页面即可
         */
        NSData *json = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:nil];
        NSString *jsonStr = [[NSString alloc]initWithData:json encoding:NSUTF8StringEncoding];
        
        JSValue *callback = self.owner.jscontext.globalObject;
        [callback invokeMethod:@"getDiffDataCallback" withArguments:@[jsonStr]];
        
    }];
}
