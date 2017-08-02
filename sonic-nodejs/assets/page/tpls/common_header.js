;(function(){var __a={'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'},
__b=/[&<>"']/g,
__e=function (s) {s = String(s);return s.replace(__b, function (m) {return __a[m]});};

module.exports = function (data, children) {
data=typeof data != "undefined"?data:{},children= typeof children != "undefined"?children:{};
var __p=[],_p=function(s){__p.push(s)};
;_p('<link rel="stylesheet" href="http://frozenui.github.io/frozenui/css/frozen.css">');

return __p.join("");
};})();