//
//  SonicConnection.m
//  sonic
//
//  Created by zyvincenthu on 17/4/1.
//  Copyright © 2017年 Tencent. All rights reserved.
//

#import "SonicConnection.h"

@interface SonicConnection ()<NSURLSessionDelegate,NSURLSessionDataDelegate>

@property (nonatomic,retain)NSURLSession *dataSession;
@property (nonatomic,retain)NSURLSessionDataTask *dataTask;

@end

@implementation SonicConnection

+ (BOOL)canInitWithRequest:(NSURLRequest *)request
{
    return YES;
}

- (instancetype)initWithRequest:(NSURLRequest *)aRequest
{
    if (self = [super init]) {
        _request = [aRequest retain];
    }
    return self;
}

- (void)dealloc
{
    if (self.session) {
        self.session = nil;
    }
    
    [self stopLoading];
    
    [_request release];
    _request = nil;
    
    self.dataTask = nil;
    self.dataSession = nil;
    
    [super dealloc];
}

- (void)startLoading
{
    NSURLSessionConfiguration *sessionCfg = [NSURLSessionConfiguration defaultSessionConfiguration];
    sessionCfg.requestCachePolicy = NSURLRequestReloadIgnoringCacheData;
    /**
     * NSURLSession will retain it's delegate,so you must remember do cancel action to avoid memory leak
     */
    self.dataSession = [NSURLSession sessionWithConfiguration:sessionCfg delegate:self delegateQueue:[SonicSession sonicSessionQueue]];
    self.dataTask = [self.dataSession dataTaskWithRequest:self.request];
    [self.dataTask resume];
}

- (void)stopLoading
{
    if (self.dataTask && self.dataTask.state == NSURLSessionTaskStateRunning) {
        [self.dataTask cancel];
        [self.dataSession finishTasksAndInvalidate];
    }else{
        [self.dataSession invalidateAndCancel];
    }
}

#pragma mark - NSURLSessionDelegate

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error
{
    if (error) {
        [self.session session:self.session didFaild:error];
    }else{
        [self.session sessionDidFinish:self.session];
    }
}

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task
didReceiveChallenge:(NSURLAuthenticationChallenge *)challenge
 completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential * _Nullable credential))completionHandler
{
    SecTrustRef trust = challenge.protectionSpace.serverTrust;
    SecTrustResultType result;
    NSString *host = [[task currentRequest] valueForHTTPHeaderField:@"host"];
    
    SecPolicyRef policyOverride = SecPolicyCreateSSL(true, (CFStringRef)host);
    NSMutableArray *policies = [NSMutableArray array];
    [policies addObject:(__bridge id)policyOverride];
    SecTrustSetPolicies(trust, (__bridge CFArrayRef)policies);
    
    OSStatus status = SecTrustEvaluate(trust, &result);
    
    if (status == errSecSuccess && (result == kSecTrustResultProceed || result == kSecTrustResultUnspecified)) {
        
        NSURLCredential *cred = [NSURLCredential credentialForTrust:trust];
        completionHandler(NSURLSessionAuthChallengeUseCredential, cred);
        
    } else {
        
        completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, nil);
        
    }
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask
didReceiveResponse:(NSURLResponse *)response
 completionHandler:(void (^)(NSURLSessionResponseDisposition disposition))completionHandler
{
    completionHandler(NSURLSessionResponseAllow);
    [self.session session:self.session didRecieveResponse:(NSHTTPURLResponse *)response];
}

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task willPerformHTTPRedirection:(NSHTTPURLResponse *)response newRequest:(NSURLRequest *)request completionHandler:(void (^)(NSURLRequest * _Nullable))completionHandler
{
    completionHandler(request);
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask
    didReceiveData:(NSData *)data
{
    [self.session session:self.session didLoadData:data];
}

- (void)URLSession:(NSURLSession *)session didBecomeInvalidWithError:(nullable NSError *)error
{
    if (error) {
        [self.session session:self.session didFaild:error];
    }
}

@end
