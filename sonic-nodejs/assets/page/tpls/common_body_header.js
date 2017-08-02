;(function(){var __a={'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'},
__b=/[&<>"']/g,
__e=function (s) {s = String(s);return s.replace(__b, function (m) {return __a[m]});};

module.exports = function (data, children) {
data=typeof data != "undefined"?data:{},children= typeof children != "undefined"?children:{};
var __p=[],_p=function(s){__p.push(s)};
;_p('<style>\r\n\
    body{padding-top:45px;}\r\n\
</style>\r\n\
<header class="ui-header ui-header-positive ui-border-b">\r\n\
    <i class="ui-icon-return" onclick="history.back()"></i><h1>sonic demo</h1><button class="ui-btn" onclick="location.reload()">刷新</button>\r\n\
</header>');

return __p.join("");
};})();