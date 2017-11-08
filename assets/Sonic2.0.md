Sonic 2.0相比1.0，主要新增了以下几个特性：
* 支持纯终端模式（Local Server模式），在该模式下无需后台配合亦可完成秒开；
* 支持自定义请求头和自定义响应头；
* 支持Cache-Control来控制缓存生命周期；
* 支持非utf-8编码。

# Local Server模式

## Local Server模式介绍
Local Server模式是Sonic 2.0新增的纯终端模式，相比于Sonic 1.0需要终端、前端、后台全部改造接入Sonic，Local Server模式可以在业务后台无法及时支持时，通过终端模拟Server实现Sonic逻辑，从而降低接入成本。

Local Server的开启方式如下：
    
    SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
    sessionConfigBuilder.setSupportLocalServer(true);
    sessionConfigBuilder.build();

开启Local Server模式后，对于一般页面的请求，Sonic会模拟Sonic后台，对返回的页面数据进行eTag的计算，以及模板和数据的拆分，对比本地缓存的数据，添加sonic响应头（eTag、template-change、template-tag）。最后根据Server数据和本地缓存数据的对比结果，来决定此次请求模式是完全缓存、局部刷新还是模板更新，从而执行不同的刷新逻辑。后台无需接入sonic，也能体验页面秒开。

## Local Server执行流程
### 1、首次请求
首次请求也就是无缓存情况下发起的请求，与普通Sonic无缓存模式流程一致。具体细节可参考QuickSonicSession和StandardSonicSession的无缓存模式流程。

### 2、非首次请求
非首次请求也就是有缓存情况下发起的请求，分为完全缓存、局部刷新、模板变更三种情况。

#### (1) 完全缓存

完全缓存就是本地的数据跟服务器的数据是完全一样的。以Quick模式为例，Local Server的执行流程如下图：

![LocalServer流程](https://github.com/Tencent/VasSonic/blob/master/assets/LocalServerModeCache.png)

上图主要展示了两条并行线，左边是在主线程执行的Webview流程，右边是在子线程执行Sonic流程。

**Sonic线程：**
Sonic会话创建完成后，首先获取url对应的本地缓存数据，并通知主线程Webview加载该数据。接着Sonic会与Server建立连接，如果Server返回304，则Server数据没有变更，直接使用本地缓存，Sonic流程结束；否则，Sonic拉取到完整的Server数据，计算其SHA1作为eTag，如果与请求头中的eTag相同，就确定本次请求是完全缓存模式，Sonic流程结束。 

**主线程：**
主线程在收到Sonic通知后，加载本地缓存数据，交给Webview渲染。

#### (2) 局部刷新

局部刷新就是本地的数据跟服务器的数据相比，只有data部分有变化，模板与服务器一样。

![LocalServer流程](https://github.com/Tencent/VasSonic/blob/master/assets/LocalServerModeDataUpdate.png)

**Sonic线程：**
Sonic会话创建完成后，首先获取url对应的本地缓存数据，并通知主线程Webview加载该数据。接着Sonic与Server建立连接，读取到完整的Server数据，计算其SHA1作为eTag，如果与请求头中的eTag不同，Sonic将Server数据拆分为template和data，计算template的SHA1作为template-tag，如果与请求头中的template-tag相同，则说明模板没有变更，此时确定本次请求是局部刷新，将拆分得到的data与本地保存的data做对比计算，得到增量数据。最后通知Webview进行局部刷新，并更新本地缓存。

**主线程：**
局部刷新模式下主线程会先收到加载本地缓存数据的通知，而Sonic线程通知主线程刷新data时，主线程可能还未开始加载本地缓存，也可能已经开始渲染本地缓存。如果WebView还未开始加载本地缓存，就会直接加载最新的数据（拆分得到的data跟本地模版合成的数据）；如果主线程已经加载本地缓存，就会直接通过Js接口让WebView用增量数据刷新页面。

#### (3) 模板更新

模板更新是本地的模板跟服务器的模板不一致。

![LocalServer流程](https://github.com/Tencent/VasSonic/blob/master/assets/LocalServerModeTemplateChange.png)

**Sonic线程：**
Sonic会话创建完成后，首先获取url对应的本地缓存数据，并通知主线程Webview加载该数据。接着Sonic与Server建立连接，读取到完整的Server数据，计算其SHA1作为eTag，如果与请求头中的eTag不同，Sonic将Server数据拆分为template和data，计算template的SHA1作为template-tag，如果与请求头中的template-tag不同则说明模板发生了变更，此时确定本次请求是模板刷新模式，通知主线程Webview进行模板刷新，并更新本地缓存。 

**主线程：**
主线程会先收到加载本地缓存数据的通知，之后Sonic线程通知主线程进行模板刷新时，无论WebView是否已经开始加载本地缓存数据，都会直接重新加载最新的Server数据，完成模板刷新。

# 其他新增特性
### 1、支持自定义请求头和自定义响应头
Sonic 2.0支持添加自定义请求头和自定义响应头，添加方式如下：

    SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
    sessionConfigBuilder.setCustomRequestHeaders(requestHeaderMap);
	sessionConfigBuilder.setCustomResponseHeaders(responseHeaderMap);
    sessionConfigBuilder.build();

### 2、支持Cache-Control来控制缓存生命周期
Sonic 2.0支持在Http响应头部添加Cache-Control字段来控制缓存生命周期，目前支持max-age、private、public三个可选值。


### 3、支持非UTF-8编码
Sonic 2.0优化了字符编码的使用。如果http响应头中包含"Content-Type"字段，则优先使用该字段的值作为字符编码，否则默认使用UTF-8编码。

# FAQ

### 1、Sonic后台返回的数据与非sonic后台返回的数据的区别？
     
接入了Sonic的Server返回的数据，http响应头会包含"cache-offline"、"template-change"或者"template-tag"字段，而未接入Sonic的Server返回的数据，响应头中不会包含以上三个字段。

### 2、Local Server模式的优缺点？

**优点：**
Local Server模式下，简化了终端执行逻辑；而且无需后台接入Sonic，大大减少了接入成本。  
**缺点：**
Local Server模式相比后台接入，损失了一定的性能。因为终端模拟后台的话，非首次请求场景需要等Server数据全部返回才能计算eTag，template-tag，template-change，从而判断是哪种模式（完全缓存、局部刷新还是模板更新）。
