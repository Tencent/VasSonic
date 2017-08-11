<?php
/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

 * sonic插件
 * 在所有controller中都可以调用sonic插件
 * @author shawnlzhang
 * @time 2017.03.24
 *
 */
if (!function_exists('getallheaders'))  {
    function getallheaders()
    {
        if (!is_array($_SERVER)) {
            return array();
        }
        $headers = array();
        foreach ($_SERVER as $name => $value) {
            if (substr($name, 0, 5) == 'HTTP_') {
                $headers[str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))))] = $value;
            }
        }
        return $headers;
    }
}
class TemplateReplace{
    public static $shotWnsDiffBodyReplace = false; //判断是否成功替换sonicdiffbody
    public static $diffIndex = 0; 
    public static $tagPrefix = 'auto';
    public static $diffTagNames = array(); //数据块

    public function callback($matches) {
        if(isset($matches) && isset($matches[0])) {
            self::$shotWnsDiffBodyReplace = true;
            if(isset($matches[1])) {
                $tagName = $matches[1];
            } else {
                $tagName = self::$tagPrefix.self::$diffIndex++;
            }
            self::$diffTagNames[$tagName] = $matches[0];

            return '{'.$tagName.'}';
        }
    }
}
class util_sonic {
	public static function start(){
		ob_start();
        $_SERVER['PHP_SONIC'] = '1';
	}

    public static function end() {
		$outContent = ob_get_clean();
    	$headers = getallheaders();

        if(isset($headers['accept-diff']) && $headers['accept-diff'] === 'true'){
            header('Cache-Control: no-cache');

            $offline = 'true';
            header("Cache-Offline: $offline");

            $Etag = NULL;
            if(isset($headers['If-None-Match'])){
                $Etag = $headers['If-None-Match'];
            }

            //offline值需要进入离线
            if(isset($offline) && $offline !== 'false'){
                //取响应内容的md5
                $md5 = sha1($outContent);
                //304命中离线缓存
                if($Etag === $md5) {
                    header('Cache-Offline: store');
                    header('Content-Length: 0');
                    header('HTTP/1.1 304 Not Modified');
                    exit;
                }
                header('Etag: '.$md5);
            }
            $outContent = self::wnsHtmlDiffDivision($outContent);
            header('Content-Length:'.strlen($outContent));

        }
        echo $outContent;
    }

    public static function wnsHtmlDiffDivision($htmlStr){
        $htmlMd5 = sha1($htmlStr);

        $headers = getallheaders();
        $clientTemplateTag = NULL;
        if(isset($headers['template-tag'])){
            $clientTemplateTag = $headers['template-tag'];
        }
        
        preg_match('/<title(.*?)<\/title>/i', $htmlStr, $titleMatchs);
        $title = '';
        if(isset($titleMatchs[0])) {
           $title = $titleMatchs[0];
        }
        $templateHtml = preg_replace('/<title(.*?)<\/title>/i', '{title}', $htmlStr);
        $templateReplace = new TemplateReplace();
        $templateHtml = preg_replace_callback('/<!--sonicdiff-?(\w*)-->[\s\S]+?<!--sonicdiff-?\w*-end-->/i', array($templateReplace, 'callback'),$templateHtml);

        //转换模板为buffer，再获取模板md5，作为template-tag
        $templateMd5 = sha1($templateHtml);
        //加个头，告诉客户端，我是一个diff结构的响应
        //var_dump($clientTemplateTag, $templateMd5); exit;
        header('template-tag: '.$templateMd5);

        //结构化数据
        $result = array(
            'data' =>array(),
            'template-tag' => $templateMd5
        );
        $result['data']['{title}'] = $title;
        // 设置数据块数据
        $diffTagNames = TemplateReplace::$diffTagNames;
        foreach($diffTagNames as $i => $diffTagName) {
            if(isset($diffTagNames[$i])) {
                $result['data']['{'.$i.'}'] = $diffTagNames[$i];
            }
        }
        $result['html-sha1'] = $htmlMd5;

        $resultStr = '';
        if(!TemplateReplace::$shotWnsDiffBodyReplace){
            $result['template'] = $templateHtml;
            $resultStr = json_encode($result);
        } else if($templateMd5 === $clientTemplateTag){
            header('template-change: false');
            //离线模板没有差异，不用更新
            $result['diff'] = '';
            $resultStr =  json_encode($result);
        } else {
            if($templateMd5 != $clientTemplateTag){
                header('template-change: true');
            }
            
            //客户端没有带离线版本，直接全量即可
            $resultStr = $htmlStr;
        }

        return $resultStr;
    }
	
}