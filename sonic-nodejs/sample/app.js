/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

'use strict';
const koa = require('koa');
const compress = require('./middleware/compress');
const through2 = require('through2');
const router = require('./router');
const app = new koa();

app.use(compress());

app.use(router());

// app.use(async (ctx, next) => {
//     ctx.body = through2();
//     ctx.type = 'html';
//     ctx.body.write('<html><head><meta charset="utf-8"/><title>sonic demo</title></head><body>')
//     await next();
// });
//
// app.use(async (ctx, next) => {
//     ctx.body.write(`<h1 id="time"><!--sonicdiff-data1-->现在时间是：${(new Date()).toLocaleString()}<!--sonicdiff-data1-end--></h1>`);
//     await next();
// });
//
// app.use(async (ctx, next) => {
//     ctx.body.end(`<body></html>`);
//     await next();
// });

app.listen(8080);