/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

const stream = require('stream');
const isJSON = require('koa-is-json');
const differ = require('sonic_differ');
const zlib = require('zlib');
const method = {
	gzip: zlib.createGzip,
	deflate: zlib.createDeflate
};

module.exports = () => {
	return async function (ctx, next) {
		await next();
		let body = ctx.body;

		if (!body) return;

		if (isJSON(body)) body = ctx.body = JSON.stringify(body);
		let encoding = ctx.acceptsEncodings('gzip', 'deflate', 'identity');
		if (!encoding) ctx.throw(406, 'supported encodings: gzip, deflate, identity');
		if (encoding === 'identity') return;
		if (ctx.response.length < 2048) return;

		ctx.set('Content-Encoding', encoding);
		ctx.res.removeHeader('Content-Length');

		let zip = ctx.body = method[encoding]({
			flush: require('zlib').Z_SYNC_FLUSH
		});

		zip.on('error', err => {
			console.info(err)
		});

		zip.on('finish', () => {
			console.info('zip succ')
		});

		let sonic = {
			buffer: [],
			write: function (chunk, encoding) {
				let buffer = chunk;
				let ecode = encoding || 'utf8';
				if (!Buffer.isBuffer(chunk)) {
					buffer = new Buffer(chunk, ecode);
				}
				sonic.buffer.push(buffer);
			}
		};

		function getDiff() {
			console.info('sonic 开始拦截返回数据');
			body.on('data', (chunk, encoding) => {
				sonic.write(chunk, encoding)
			});

			body.on('end', () => {
				let result = differ(ctx, Buffer.concat(sonic.buffer));
				sonic.buffer = [];
				if (result.cache) {
					console.info('sonic 完全cache');
					zip.end()
				} else {
					console.info('sonic 非缓存模式');
					zip.end(result.data)
				}
			})
		}

		try {
			body instanceof stream ? ctx.get('accept-diff') ? getDiff() : body.pipe(zip) : zip.end(body);
		} catch (e) {
			console.info('zip error');
			console.error(e)
		}
	}
};