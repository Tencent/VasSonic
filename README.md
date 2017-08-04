## VasSonic
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---
 VasSonic is a lightweight and high-performance Hybrid framework which is intended to speed up the first screen of websites working on Android and iOS platform.
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

[Getting started with Android](https://github.com/Tencent/VasSonic/blob/master/sonic-android/README.md)

[Getting started with iOS](https://github.com/Tencent/VasSonic/blob/master/sonic-iOS/README.md)

[Getting started with Node.js](https://github.com/Tencent/VasSonic/blob/master/sonic-nodejs/README.md)

[Getting started with PHP](https://github.com/Tencent/VasSonic/blob/master/sonic-php/README.md)

## Support
Any problem?

1. Learn more from [Android sample](https://github.com/Tencent/VasSonic/tree/master/sonic-android/sample)   [iOS sample](https://github.com/Tencent/VasSonic/tree/master/sonic-iOS/SonicSample) or [PHP sample](https://github.com/Tencent/VasSonic/tree/master/sonic-php/sample).
2. Read the [Android source code](https://github.com/Tencent/VasSonic/tree/master/sonic-android/sdk) and [iOS source code](https://github.com/Tencent/VasSonic/tree/master/sonic-iOS/Sonic) or [PHP source code](https://github.com/Tencent/VasSonic/tree/master/sonic-php/sdk).
3. Read the [wiki](https://github.com/Tencent/VasSonic/wiki) for help.
4. Contact [us](https://jq.qq.com/?_wv=1027&k=4EaxB4K) or scan QR code for help.</br>
![QR code][3]

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.

[1]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120005424.gif
[2]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120029897.gif
[3]: https://github.com/Tencent/VasSonic/blob/master/article/QR.JPG

