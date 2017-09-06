/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

const crypto = require('crypto');

module.exports = function (ctx, buffer) {
	let etag = ctx.get('if-none-match');
	let now = Date.now();
	let md5 = crypto.createHash('sha1').update(buffer).digest('hex');

	console.info(`请求头etag = ${etag}`);
	console.info(`页面md5 = ${md5}`);

	//sonicMode含义
	// 0-非sonic（没有sonicdiff标签）
	// 1-首次加载（本地无模板和数据）
	// 2-页面刷新（模板有变）
	// 3-局部数据刷新（模板不变，数据有变）
	// 4-完成缓存304（模板不变，数据不变）
	if (etag && md5 == etag) {
		ctx.set('Cache-Offline', 'store');
		ctx.set('Content-Length', 0);
		ctx.status = 304;
		ctx.sonicMode = 4;
		return {
			cache: true
		}
	} else {
		let htmlStr = buffer.toString('utf8');
		//替换 &&提取　title，body
		let title = "";

		let now2 = Date.now();
		let templateHtml = htmlStr.replace(/<title(.*?)<\/title>/i, function (titleHtml) {
			title = titleHtml;
			return "{title}";
		});
		//判断是否成功替换wnsdiffbody
		let flag = false;

		let tagIndex = 0, tagPrefix = 'auto';

		let diffTagNames = {};

		templateHtml = templateHtml.replace(/<!--sonicdiff-?(\w*)-->([\s\S]+?)<!--sonicdiff-?(\w*)-end-->/ig, function (diffhtml, tagName) {
			flag = true;
			if (!tagName) {
				tagName = tagPrefix + tagIndex++;
			}

			diffTagNames[tagName] = diffhtml;

			return '{' + tagName + '}';
		});

		console.info(`获取sonic diff耗时${Date.now() - now2}`);

		let now3 = Date.now();

		let templateMd5 = crypto.createHash('sha1').update(new Buffer(templateHtml)).digest('hex');

		console.info(`获取sonic diff耗时${Date.now() - now3}`);

		ctx.set('Etag', md5);
		ctx.set('template-tag', templateMd5);

		ctx.set('Cache-Offline', true);

		if (flag) {
			let templateTag = ctx.get('template-tag');
			if (templateMd5 == templateTag) {
				ctx.set('template-change', 'false');

				let result = {
					'data': {
						'{title}': title
					},
					'diff': '',
					'html-sha1': md5,
					"template-tag": templateMd5
				};

				Object.keys(diffTagNames).forEach(v => {
					result['data']['{' + v + '}'] = diffTagNames[v];
				});

				console.info(`数据更新耗时${Date.now() - now}`);

				ctx.sonicMode = 3;

				return {
					data: new Buffer(JSON.stringify(result))
				}

			} else {
				ctx.set('template-change', 'true');

				ctx.sonicMode = templateTag ? 2 : 1;

				console.info(`模版更新耗时${Date.now() - now}`);

				return {
					data: new Buffer(htmlStr)
				}
			}

		} else {

			ctx.set('template-change', 'true');

			ctx.sonicMode = 0;

			return {
				data: new Buffer(htmlStr)
			}
		}
	}
};