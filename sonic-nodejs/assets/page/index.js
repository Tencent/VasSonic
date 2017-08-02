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
;_p('    <title>sonic demo</title>');
;_p( (require('./tpls/common_header'))() );
;_p('</head>\r\n\
<body>');
;_p( (require('./tpls/common_body_header'))() );
;_p('<ul class="ui-list ui-list-text">\r\n\
	<a href="/demo3?sonicStatus=1"><li class="ui-arrowlink">首次进入</li></a>\r\n\
	<a href="/demo3?sonicStatus=2"><li class="ui-arrowlink">页面刷新</li></a>\r\n\
    <a href="/demo3?sonicStatus=3"><li class="ui-arrowlink">局部刷新</li></a>\r\n\
    <a href="/demo3?sonicStatus=4"><li class="ui-arrowlink">完全缓存</li></a>\r\n\
</ul>\r\n\
</body>\r\n\
</html>');

return __p.join("");
};})();