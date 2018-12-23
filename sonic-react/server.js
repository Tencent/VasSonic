/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

const Koa = require('koa');
const next = require('next');
const Router = require('koa-router');
const cheerio = require('cheerio');
const sonicDiff = require('sonic_differ');

const port = parseInt(process.env.PORT, 10) || 3000;
const dev = process.env.NODE_ENV !== 'production';

const app = next({ dev });
const handle = app.getRequestHandler();

/**
 * add comment tags to separate template and data blocks from the initial html
 *
 * Example:
 * <!DOCTYPE html>                                                         <!DOCTYPE html>
 * <html>                                                                  <html>
 * <head></head>                                                           <head></head>
 * <body>                                                                  <body>
 * … …                                                                     … …
 * <div id="root" data-sonicdiff="firstScreenHtml">          =>            <!-- sonicdiff-firstScreenHtml -->
 *     … …                                                                 <div id="root" data-sonicdiff="firstScreenHtml">
 * </div>                                                                      … …
 * … …                                                                     </div>
 * <script>                                                                <!-- sonicdiff-firstScreenHtml-end -->
 *     __NEXT_DATA__=xxx                                                   … …
 * </script>                                                               <!-- sonicdiff-initState -->
 * </body>                                                                 <script>
 *                                                                             __NEXT_DATA__=xxx
 *                                                                         </script>
 *                                                                         <!-- sonicdiff-initState-end -->
 *                                                                         </body>
 *
 * @param html {string} the initial html
 * @returns {string} html with comment tags which help sonic to splits the html to template and data
 */
function formatHtml(html) {
    const $ = cheerio.load(html);
    $('*[data-sonicdiff]').each(function(index, element) {
        let tagName = $(this).data('sonicdiff');
        return $(this).replaceWith('<!--sonicdiff-' + tagName + '-->' + $(this).clone() + '<!--sonicdiff-' + tagName + '-end-->');
    });
    html = $.html();
    html = html.replace(/<script\s*>\s*__NEXT_DATA__\s*=([\s\S]+?)<\/script>/ig, function(data1) {
        return '<!--sonicdiff-initState-->' + data1 + '<!--sonicdiff-initState-end-->';
    });
    return html;
}

app.prepare().then(() => {
    const server = new Koa();
    const router = new Router();

    // intercept request and render html string only if at a given URL
    router.get('/demo', async ctx => {
        ctx.set('Content-Type', 'text/html');
        ctx.state.resHtml = await app.renderToHTML(ctx.req, ctx.res, ctx.path, ctx.query);
    });

    router.get('*', async ctx => {
        ctx.set('Cache-Control', 'max-age:30');
        await handle(ctx.req, ctx.res);
    });

    server.use(async (ctx, next) => {
        await next();

        // only intercept html request
        if (!ctx.response.is('html')) {
            return;
        }

        // process non-sonic mode
        if (!ctx.request.header['accept-diff']) {
            ctx.body = ctx.state.resHtml;
            return;
        }

        // use sonic_differ module to process the data
        let sonicData = sonicDiff(ctx, formatHtml(ctx.state.resHtml));

        if (sonicData.cache) {
            // 304 Not Modified, return nothing.
            ctx.body = '';
        } else {
            // other Sonic status.
            ctx.body = sonicData.data;
        }
    });

    server.use(router.routes());

    server.listen(port, (err) => {
        if (err) {
            throw err;
        }
        console.log(`> Ready on http://localhost:${port}`);
    });
});
