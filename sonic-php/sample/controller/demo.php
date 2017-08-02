<?php
/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

 * sonic demo控制器
 * @author owenlai
 *
 */
class controller_demo
{
    public function actionIndex(){
        /**
         * 构造随机sonic状态 begin
         */
        $headers = getallheaders();
        $dataImg = 1;
        $templateFlag = 1;
        $sonicStatusMap = array(
            'templateChange' => 2,
            'dataUpdate' => 3,
            'cache' => 4
        );
        $sonicStatus = 0; //1-sonic首次 2-页面刷新 3-局部刷新 4-完全cache
        if (isset($headers['accept-diff']) && $headers['accept-diff'] == 'true') {
            if (isset($headers['template-tag']) && !empty($headers['template-tag'])) { //有缓存的情况随机局部刷新、模板变更、完全缓存
                $sonicStatusRand = array(3,3,3,3,3,4,4,2,4,4,4);
                $sonicStatus = isset($_GET['sonicStatus']) && in_array($_GET['sonicStatus'], array_keys($sonicStatusMap))
                    ? $sonicStatusMap[$_GET['sonicStatus']] : $sonicStatusRand[array_rand($sonicStatusRand)];
                switch($sonicStatus) {
                    case 2: //模板变更 数据不变 改模板
                        if (isset($_COOKIE['dataImg'])) {
                            $dataImg = $_COOKIE['dataImg'];
                        }
                        if (isset($_COOKIE['templateFlag'])) {
                            $templateFlag = !$_COOKIE['templateFlag'];
                        }
                        break;
                    case 3://局部刷新 数据变 模板不变
                        if (isset($_COOKIE['dataImg'])) {
                            $dataImg = !$_COOKIE['dataImg'];
                        }
                        if (isset($_COOKIE['templateFlag'])) {
                            $templateFlag = $templateFlag;
                        }
                        break;
                    case 4:
                        if (isset($_COOKIE['dataImg'])) {
                            $dataImg = $_COOKIE['dataImg'];
                        }
                        if (isset($_COOKIE['templateFlag'])) {
                            $templateFlag = $_COOKIE['templateFlag'];
                        }
                        break;
                }
            } else { //首次
                $sonicStatus = 1;
            }
        }
        setcookie('dataImg', intval($dataImg));
        setcookie('templateFlag',intval($templateFlag));
        /**
         * 构造随机sonic状态 end
         */
        /**
         * 模拟后台耗时操作 begin
         */
        sleep(1);
        /**
         * 模拟后台耗时操作 end
         */
        require_once 'util/sonic.php';
        util_sonic::start();
        require 'view/demo_template.php';
        util_sonic::end();
    }
}
