;(function(){var __a={'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'},
__b=/[&<>"']/g,
__e=function (s) {s = String(s);return s.replace(__b, function (m) {return __a[m]});};

module.exports = function (data, children) {
data=typeof data != "undefined"?data:{},children= typeof children != "undefined"?children:{};
var __p=[],_p=function(s){__p.push(s)};
;_p('<meta charset="utf-8">\r\n\
<meta name="format-detection" content="telephone=no"/>\r\n\
<meta http-equiv="x-dns-prefetch-control" content="on">\r\n\
<meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"/>\r\n\
<meta name="apple-mobile-web-app-capable" content="yes"/>\r\n\
<meta name="apple-mobile-web-app-status-bar-style" content="black"/>');

return __p.join("");
};})();