## Getting started with Java
[![license](http://img.shields.io/badge/license-BSD3-brightgreen.svg?style=flat)](https://github.com/Tencent/VasSonic/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/VasSonic/pulls)
[![wiki](https://img.shields.io/badge/Wiki-open-brightgreen.svg)](https://github.com/Tencent/VasSonic/wiki)
---

## How to use for Server
### Dependencies

1)Apache Tomcat

2)Download VasSonic-1.1.jar and put it in your Java project. If you are familiar with Maven, you can get VasSonic-1.1.jar from [Maven repository](http://search.maven.org/#search%7Cga%7C1%7CVasSonic).

3)Configure the Application Deployment Descriptor - "web.xml" in your project. put the following code in web.xml before any other filter or servlet config. 

```xml
  <filter>
    <filter-name>SonicFilter</filter-name>
    <filter-class>com.github.tencent.SonicFilter</filter-class>
  </filter>
  <filter-mapping>
    <filter-name>SonicFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
```
4) Redeploy your web apps, and restart server.


## How to use for front-end
Here is a simple demo shows how to use Sonic for front-end.
```Html
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <title>demo</title>
    <script type="text/javascript">
            
            // Interacts with mobile client by JavaScript interface to get Sonic diff data.
            function getDiffData(){
                window.sonic.getDiffData();
            }
            // step 3: Handle the response from mobile client which include Sonic response code and diff data.   
           function getDiffDataCallback(result){
                var sonicStatus = 0; 
                /**
                * The Sonic status:
                * 0: It fails to get any data from mobile client.
                * 1: It is first time for mobile client to use Sonic.
                * 2: Mobile client reload the whole websites.
                * 3: Websites will be updated dynamically with local refresh.
                * 4: The Sonic request of mobile client receives a 304 response code and nothing has been modified.
                */
                sonicUpdateData = {}; //sonic diff data
                var result = JSON.parse(result);
                if(result['code'] == 200){
                    sonicStatus = 3;
                    sonicUpdateData = JSON.parse(result['result']);
                } else if (result['code'] == 1000) {
                    sonicStatus = 1;
                } else if (result['code'] == 2000) {
                    sonicStatus = 2;
                } else if(result['code'] == 304) {
                    sonicStatus = 4;
                }
                handleSonicDiffData(sonicStatus, sonicUpdateData);
            }
            // step 3: Handle the response from mobile client which include Sonic response code and diff data.  
            function handleSonicDiffData(sonicStatus, sonicUpdateData){
                if(sonicStatus == 3){
                    //Websites will be updated dynamically and run some JavaScript while in local refresh mode. 
                    var html = '';
                    var id = '';
                    var elementObj = '';
                    for(var key in sonicUpdateData){
                        id = key.substring(1,key.length-1);
                        html = sonicUpdateData[key];
                        elementObj = document.getElementById(id+'Content');
                        elementObj.innerHTML = html;
                    }
                }
            }
    </script>
</head>
<body>
    // step 1: specify template and data by inserting different comment anchor.
    <div id="data1Content">
        <!--sonicdiff-data1-->
        <p id="partialRefresh"></p>
        <!--sonicdiff-data1-end-->
    </div>
    <div id="data2Content">
        <!--sonicdiff-data2-->
        <p id="data2">here is the data</p>
        <!--sonicdiff-data2-end-->
        <p id="pageRefresh"></p>
    </div>
    <div id = "data3">data3</div>
    
    // step 2: Receives diff data from mobile client through Javascript interface.
    <script type="text/javascript">
        window.onload = function(){
            getDiffData();
        }
    </script>
</body>
</html>
```
### Step 1:
Specify template and data by inserting different comment anchor. The data will be wrapped with anchor ```<!-- sonicdiff-moduleName -->```  ```<!-- sonicdiff-moduleName-end -->```. The other part of html is template.
```Html
    <div id="data1Content">
        <!--sonicdiff-data1-->
        <p id="partialRefresh"></p>
        <!--sonicdiff-data1-end-->
    </div>
    <div id="data2Content">
        <!--sonicdiff-data2-->
        <p id="data2">here is the data</p>
        <!--sonicdiff-data2-end-->
        <p id="pageRefresh"></p>
    </div>
    <div id = "data3">data3</div>
```

### Step 2:
Receives diff data from mobile client through JavaScript interface. The JavaScript interface of demo was involved when websites are finish. But the time when inferface was involved is not immutable, websites can decide whenever they want.
```Html
<script type="text/javascript">
    window.onload = function(){
        getDiffData();
    }
</script>
```

### Step 3:
Handle different status received from mobile client. The demo shows how to find and replace the data of specified anchor according to the diff data come from mobile client, then the website is updated.
```Html
//step 3 Handle the response from mobile client which include Sonic response code and diff data.  
function getDiffDataCallback(result){
｝
//step 3 Handle the response from mobile client which include Sonic response code and diff data.  
function handleSonicDiffData(sonicStatus, sonicUpdateData){
｝
```
## Demo
Copy the 'sample' directory into Tomcat 'webapps' directory, Then restart tomcat server, make a request [http://localhost:8080/sample/webapp/index.jsp](http://localhost:8080/sample/webapp/index.jsp).

## Support
Any problem?

1. Learn more from [sample](https://github.com/Tencent/VasSonic/tree/master/sonic-java/sample).
2. Contact us for help.

## License
VasSonic is under the BSD license. See the [LICENSE](https://github.com/Tencent/VasSonic/blob/master/LICENSE) file for details.

[1]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120005424.gif
[2]: https://github.com/Tencent/VasSonic/blob/master/article/20170705120029897.gif
