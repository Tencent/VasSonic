//gulp
let gulp = require('gulp');
let gutil = require('gulp-util');
let sequence = require('gulp-sequence');
let through2 = require('through2');
// let babel = require('gulp-babel');
// let plumber = require('gulp-plumber');

//路径相关
// let glob = require('glob');
let path = require('path');
let fs = require('fs');

//配置
let projectPath = '../project.config.js';
let projectConfig = require(projectPath);
let paths = projectConfig.build;

// //webpack配置及插件
// let webpackConfigDev = require('./webpack.dev.config');
// let webpackConfigProd = require('./webpack.production.config');
//
// let webpack = require('webpack');
// let webpackMerge = require('webpack-merge');
// let CleanWebpackPlugin = require('clean-webpack-plugin');


// let async = require('async');


/**----------ejs2js--------------**/
let ejs2js = require('@tencent/gulp-ejs2js');
gulp.task("ejs2js", function (callback) {
	console.info('执行ejs2js');
	buildEjs2js(paths.ejs2js + '/**/*.ejs', paths.ejs2js, callback);
});

/**
 * 构建ejs -> jsc
 * @param src 源文件地址
 * @param dest 生成地址
 * @param callback
 */
function buildEjs2js(src, dest, callback) {
	gulp.src(src)
		.on('error', function handleError(err) {
			console.error(err.toString());
			this.emit('end');
		})
		.on('end', function () {
			console.log('ejsEnd');
			callback && callback();
		})
		.pipe(ejs2js())
		.pipe(lf2crlf()) //LF2CRLF
		.pipe(gulp.dest(dest));
}


// /**-----------uglify-------------**/
// let uglify = require('gulp-uglify');
// function buildUglify(src, dist, callback) {
//     return gulp.src(src)
//         .pipe(uglify())
//         .pipe(gulp.dest(dist))
//         .on('end', function () {
//             console.log('uglify END');
//             callback();
//         })
// }
// gulp.task('libs', callback => {
//     let buildPath = projectConfig.build;
//     buildUglify(buildPath.srcLib + '/**/*.js', buildPath.distLib, callback);
// });

/**----------辅助方法-------------**/
//简易gulp插件
function modify(fn) {
	return through2.obj(function (file, encoding, done) {
		let content = String(file.contents);
		//具体业务
		content = fn(content);

		file.contents = new Buffer(content);
		this.push(file);
		done();
	})
}

/**
 * gulp lf2crlf
 * @returns {*}
 */
function lf2crlf() {
	return modify(function (content) {
		return content.replace(/\r?\n/g, '\r\n');
	});
}


