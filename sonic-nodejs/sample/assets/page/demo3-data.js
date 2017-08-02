
let dataFn = function(ctx){
	var request = ctx.request;
	var headers = request.header;
	var query = request.query || {};
	var dataImg = 1;
    var templateFlag = 1;
    var sonicStatusArr = [
        2, //templateChange
        3, //dataUpdate
        4 //cache
    ];
    var sonicStatus = 0; //1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache

    if (headers['accept-diff']=='true') {
    	if (headers['template-tag']) { //有缓存的情况随机局部刷新、模板变更、完全缓存
    		var getRandomStatus = function(){
    			var sonicStatusRand = [3,3,3,3,3,4,4,2,4,4,4];
    			var index = Math.floor(Math.random() * sonicStatusRand.length);
    			return sonicStatusRand[index];
    		}
    		
            sonicStatus = sonicStatusArr[parseInt(query['sonicStatus'], 10)]? parseInt(query['sonicStatus'], 10) : getRandomStatus();
            switch(sonicStatus) {
                case 2: //模板变更 数据不变 改模板
                    if (ctx.cookies.get('dataImg')) {
                        dataImg = parseInt(ctx.cookies.get('dataImg'), 10);
                    }
                    if (ctx.cookies.get('templateFlag')) {
                        templateFlag = +!parseInt(ctx.cookies.get('templateFlag'), 10);
                    }
                    break;
                case 3://局部刷新 数据变 模板不变
                    if (ctx.cookies.get('dataImg')) {
                        dataImg = +!parseInt(ctx.cookies.get('dataImg'), 10);
                    }
                    // if (ctx.cookies.get('templateFlag')) {
                    //     templateFlag = templateFlag;
                    // }
                    break;
                case 4:
                    if (ctx.cookies.get('dataImg')) {
                        dataImg = parseInt(ctx.cookies.get('dataImg'), 10);
                    }
                    if (ctx.cookies.get('templateFlag')) {
                        templateFlag = parseInt(ctx.cookies.get('templateFlag'), 10);
                    }
                    break;
            }
    	} else { //首次
            sonicStatus = 1;
        }
    }

    ctx.cookies.set('dataImg', parseInt(dataImg, 10));
    ctx.cookies.set('templateFlag',parseInt(templateFlag, 10));

    return {
    	dataImg: dataImg,
    	templateFlag: templateFlag,
    	sonicStatus: sonicStatus
    };
};
module.exports = dataFn; 