//
//  RootViewController.m
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

#import "RootViewController.h"
#import "SonicWebViewController.h"
#import "SonicOfflineCacheConnection.h"

@import Sonic;

@interface RootViewController ()

@property (nonatomic,strong)NSString *url;

@end

@implementation RootViewController

- (instancetype)init
{
    if (self = [super init]) {
        
        self.title = @"Sonic";
        
        self.url = @"http://mc.vip.qq.com/demo/indexv3";
        
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    
    //Subclass the SonicConnection to return offline cache
    [SonicSession registerSonicConnection:[SonicOfflineCacheConnection class]];

    //header
    UIImageView *header = [[UIImageView alloc]initWithImage:[UIImage imageNamed:@"header.png"]];
    header.frame = CGRectMake(0, SizeFitHeightPlus(65), SizeFitWidthPlus(214), SizeFitHeightPlus(59));
    header.center = CGPointMake([UIScreen mainScreen].bounds.size.width/2, header.frame.origin.y+header.frame.size.height/2);
    [self.view addSubview:header];
    
    [self setupSubViews];
    
    [self setupBottomLabel];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:NO animated:NO];
    [super viewWillDisappear:animated];
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:YES animated:NO];
    [super viewWillAppear:animated];
}

- (void)setupSubViews
{
    [self createButtonWithIndex:0 withTitle:@"LOAD WITHOUT SONIC" withAction:@selector(normalRequestAction)];
    [self createButtonWithIndex:1 withTitle:@"LOAD WITH SONIC" withAction:@selector(sonicRequestAction)];
    [self createButtonWithIndex:2 withTitle:@"DO SONIC PRELOAD" withAction:@selector(sonicPreloadAction)];
    [self createButtonWithIndex:3 withTitle:@"LOAD SONIC WITH OFFLINE CACHE" withAction:@selector(loadWithOfflineFileAction)];
    [self createButtonWithIndex:4 withTitle:@"CLEAN UP CACHE" withAction:@selector(clearAllCacheAction)];
}

- (void)setupBottomLabel
{
    UILabel *bottomLabel = [[UILabel alloc]init];
    bottomLabel.text = @"腾讯增值技术团队出品";
    bottomLabel.textColor = [UIColor grayColor];
    [bottomLabel sizeToFit];
    bottomLabel.center = CGPointMake([UIScreen mainScreen].bounds.size.width/2, [UIScreen mainScreen].bounds.size.height - SizeFitHeightPlus(30.f) - bottomLabel.frame.size.height/2);
    
    [self.view addSubview:bottomLabel];
}

- (void)createButtonWithIndex:(NSInteger)index withTitle:(NSString *)title withAction:(SEL)action
{
    CGFloat offsetX = SizeFitWidthPlus(12.f);
    CGFloat rowMargin = SizeFitHeightPlus(20.f);
    CGFloat screenWidth = [UIScreen mainScreen].bounds.size.width;
    CGFloat buttonWidth = screenWidth - 2*offsetX;
    CGFloat buttonHeight = SizeFitHeightPlus(44.f);
    
    CGFloat offsetY = SizeFitHeightPlus(160) + (index+1) * rowMargin + index *buttonHeight;
    
    UIButton *button = [UIButton buttonWithType:UIButtonTypeCustom];
    button.frame = CGRectMake(offsetX, offsetY, buttonWidth, buttonHeight);
    button.layer.cornerRadius = SizeFitHeightPlus(5);
    button.layer.masksToBounds = YES;
    UIColor *color = [self colorWithRGBHexString:@"#049DFF"];
    UIImage *normalBack = [self imageFromColor:color];
    [button setBackgroundImage:normalBack forState:UIControlStateNormal];
    [button setTitle:title forState:UIControlStateNormal];
    [button setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    [button addTarget:self action:action forControlEvents:UIControlEventTouchUpInside];
    
    [self.view addSubview:button];
}

static CGFloat SizeFitWidthPlus(CGFloat value)
{
    CGFloat screenWidth = [UIScreen mainScreen].bounds.size.width;
    return screenWidth < 375? (value/375)*screenWidth:value;
}

static CGFloat SizeFitHeightPlus(CGFloat value)
{
    CGFloat screenHeight = [UIScreen mainScreen].bounds.size.height;
    return screenHeight < 667? (value/667)*screenHeight:value;
}

- (void)normalRequestAction
{
    SonicWebViewController *webVC = [[SonicWebViewController alloc]initWithUrl:self.url useSonicMode:NO];
    [self.navigationController pushViewController:webVC animated:YES];
}

- (void)sonicPreloadAction
{
    [[SonicClient sharedClient] createSessionWithUrl:self.url withWebDelegate:nil];
    [self alertMessage:@"Preload Start!"];
}

- (void)sonicRequestAction
{
    SonicWebViewController *webVC = [[SonicWebViewController alloc]initWithUrl:self.url useSonicMode:YES];
    [self.navigationController pushViewController:webVC animated:YES];
}

- (void)loadWithOfflineFileAction
{
    SonicWebViewController *webVC = [[SonicWebViewController alloc]initWithUrl:@"http://mc.vip.qq.com/demo/indexv3?offline=1" useSonicMode:YES];
    [self.navigationController pushViewController:webVC animated:YES];
}

- (void)clearAllCacheAction
{
    [[SonicClient sharedClient] clearAllCache];
    [self alertMessage:@"Clear Success!"];
}

- (void)alertMessage:(NSString *)message
{
    UIAlertView *alert = [[UIAlertView alloc]initWithTitle:@"" message:message delegate:nil cancelButtonTitle:@"Done" otherButtonTitles:nil, nil];
    [alert show];
}

#pragma mark - Uitil

- (UIColor *)colorWithRGBHexString:(NSString *)rgbString
{
    if ([rgbString length] == 0) {
        return nil;
    }
    
    NSScanner *scanner = [NSScanner scannerWithString:rgbString];
    if ([rgbString hasPrefix:@"#"]) {
        scanner.scanLocation = 1;
    }
    else if (rgbString.length >= 2 && [[[rgbString substringToIndex:2] lowercaseString] isEqualToString:@"0x"]) {
        scanner.scanLocation = 2;
    }
    
    unsigned int value = 0;
    [scanner scanHexInt:&value];
    
    return [self colorWithRGBHex:value];
}

- (UIColor *)colorWithRGBHex: (unsigned int)hex
{
    int r = (hex >> 16) & 0xFF;
    int g = (hex >> 8) & 0xFF;
    int b = (hex) & 0xFF;
    
    return [UIColor colorWithRed:r / 255.0f
                           green:g / 255.0f
                            blue:b / 255.0f
                           alpha:1.0f];
}

- (UIImage *)imageFromColor:(UIColor *)color
{
    CGRect rect = CGRectMake(0, 0, 1, 1);
    UIGraphicsBeginImageContextWithOptions(rect.size, NO, [UIScreen mainScreen].scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSetFillColorWithColor(context, color.CGColor);
    CGContextFillRect(context, rect);
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

@end
