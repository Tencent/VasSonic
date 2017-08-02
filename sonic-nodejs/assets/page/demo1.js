;(function(){var __a={'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'},
__b=/[&<>"']/g,
__e=function (s) {s = String(s);return s.replace(__b, function (m) {return __a[m]});};

module.exports = function (data, children) {
data=typeof data != "undefined"?data:{},children= typeof children != "undefined"?children:{};
var __p=[],_p=function(s){__p.push(s)};
;_p('<!doctype html>\r\n\
<html lang="en">\r\n\
<head>');
;_p( (require('./tpls/common_meta'))() );
;_p('    <title>demo1</title>');
;_p( (require('./tpls/common_header'))() );
;_p('</head>\r\n\
<body>');
;_p( (require('./tpls/common_body_header'))() );
;_p('<h1>\r\n\
    <!--sonicdiff-->服务端时间是：');
;_p(__e( (new Date()).toLocaleString() ));
;_p('    <!--sonicdiff-end-->\r\n\
</h1>\r\n\
\r\n\
</body>\r\n\
</html>');

return __p.join("");
};})();