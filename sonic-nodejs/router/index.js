/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

const routerConfig = require('./config');
const through2 = require('through2');
module.exports = () => {
	return async function (ctx, next) {
		let pathname = (ctx.request.url || '').split('?')[0];
		if (routerConfig[pathname]) {
			console.log(routerConfig[pathname]);
			try {
				let tpl = require(routerConfig[pathname].template), data = {};
				if (routerConfig[pathname].data) {
					let dataFn = require(routerConfig[pathname].data);
					data = dataFn(ctx) || {};
				}
				ctx.body = through2();
				ctx.type = 'html';
				ctx.body.end(tpl(data));
			} catch (e) {
				console.error(e);
			}
		} else {
			console.log('路由未配置', pathname);
		}
	}
};