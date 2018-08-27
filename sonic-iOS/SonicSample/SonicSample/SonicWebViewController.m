//
//  SonicWebViewController.m
//  SonicSample
//
//  Tencent is pleased to support the open source community by making VasSonic available.
//  Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//  Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
//  in compliance with the License. You may obtain a copy of the License at
//
//  https://opensource.org/licenses/BSD-3-Clause
//
//  Unless required by applicable law or agreed to in writing, software distributed under the
//  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//  either express or implied. See the License for the specific language governing permissions
//  and limitations under the License.
//
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicWebViewController.h"
#import "SonicJSContext.h"

@interface SonicWebViewController ()

@property (nonatomic,strong)SonicJSContext *sonicContext;

@property (nonatomic,assign)BOOL isStandSonic;

@end

@implementation SonicWebViewController

- (instancetype)initWithUrl:(NSString *)aUrl useSonicMode:(BOOL)isSonic unStrictMode:(BOOL)state
{
    if (self = [super init]) {
        
        self.url = aUrl;
        
        self.clickTime = (long long)([[NSDate date]timeIntervalSince1970]*1000);
        
        if (isSonic) {
            if (state) {
                SonicSessionConfiguration *configuration = [SonicSessionConfiguration new];
                NSString *linkValue = @"http://assets.kgc.cn/ff7f069b/css/common-min.www.kgc.css?v=e4ecfe82;http://assets.kgc.cn/ff7f069b/css/themes.www.kgc.css?v=612eb426;http://assets.kgc.cn/ff7f069b/css/style.www.kgc.css?v=05d94f84";
                configuration.customResponseHeaders = @{
                                                        SonicHeaderKeyCacheOffline:SonicHeaderValueCacheOfflineStore,
                                                        SonicHeaderKeyLink:linkValue
                                                        };
                configuration.enableLocalServer = YES;
                configuration.supportCacheControl = YES;
                [[SonicEngine sharedEngine] createSessionWithUrl:self.url withWebDelegate:self withConfiguration:configuration];
            }else{
                self.isStandSonic = YES;
                [[SonicEngine sharedEngine] createSessionWithUrl:self.url withWebDelegate:self];
            }
        }
    }
    return self;
}

- (void)dealloc
{
    self.sonicContext = nil;
    [[SonicEngine sharedEngine] removeSessionWithWebDelegate:self];
}

- (void)loadView
{
    [super loadView];
    
    self.webView = [[UIWebView alloc]initWithFrame:self.view.bounds];
    self.webView.delegate = self;
    self.webView.scrollView.decelerationRate = UIScrollViewDecelerationRateNormal;
    self.view = self.webView;
    
    if (self.isStandSonic) {
        UIBarButtonItem *reloadItem = [[UIBarButtonItem alloc]initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh target:self action:@selector(updateAction)];
        self.navigationItem.rightBarButtonItem = reloadItem;
    }
    
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.url]];
    
    SonicSession* session = [[SonicEngine sharedEngine] sessionWithWebDelegate:self];
    if (session) {
        [self.webView loadRequest:[SonicUtil sonicWebRequestWithSession:session withOrigin:request]];
    }else{
        [self.webView loadRequest:request];
    }
    
    self.sonicContext = [[SonicJSContext alloc]init];
    self.sonicContext.owner = self;
}

- (void)updateAction
{
    [[SonicEngine sharedEngine] reloadSessionWithWebDelegate:self completion:^(NSDictionary *result) {
        
    }];
}

#pragma mark - UIWebViewDelegate

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType
{
    self.jscontext = [self.webView valueForKeyPath:@"documentView.webView.mainFrame.javaScriptContext"];
    self.jscontext[@"sonic"] = self.sonicContext;
    
    return YES;
}

- (void)webViewDidFinishLoad:(UIWebView *)webView
{
    self.jscontext = [self.webView valueForKeyPath:@"documentView.webView.mainFrame.javaScriptContext"];
    self.jscontext[@"sonic"] = self.sonicContext;
}

#pragma mark - Sonic Session Delegate

- (void)sessionWillRequest:(SonicSession *)session
{
    //可以在请求发起前同步Cookie等信息
}

- (void)session:(SonicSession *)session requireWebViewReload:(NSURLRequest *)request
{
    [self.webView loadRequest:request];
}

@end
