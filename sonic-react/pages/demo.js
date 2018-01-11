/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import React from 'react';
import Head from 'next/head';
import withRedux from 'next-redux-wrapper';
import { bindActionCreators } from 'redux';

import GameContainer from '../containers/GameContainer';
import { initStore, setSonicStatus, initImgArr } from '../redux/duck';

class App extends React.Component {
    static async getInitialProps({ store }) {
        store.dispatch(initImgArr());
    }

    componentDidMount() {
        // handle the response from mobile client which include Sonic response code and diff data.
        this.getSonicData((status, sonicUpdateData) => {
            switch (status) {
                // only data updates
                case 3: {
                    // update the Redux store based on changes coming from the mobile client
                    let initState = sonicUpdateData['{initState}'] || '';
                    initState.replace(/<!--sonicdiff-initState-->\s*<script>\s*__NEXT_DATA__\s*=([\s\S]+?)module=/ig, function(matched, $1) {
                        window.__NEXT_DATA__ = JSON.parse($1);
                    });
                    this.props.initImgArr(window.__NEXT_DATA__.props.initialState.gameArea);
                    break;
                }
                default:
                    break;
            }
            // display sonic status
            this.props.setSonicStatus(status);
        });
    }

    /**
     * Handle the response from mobile client which include Sonic response code and diff data
     *
     * @param callback {function} It executes until it receives an asynchronous callback from the mobile client.
     */
    getSonicData(callback) {
        let sonicHadExecute = 0;   // whether the callback is triggered
        const timeout = 3000;      // a timeout value of 3 seconds is specified to trigger callback

        // Interacts with mobile client by JavaScript interface to get Sonic diff data.
        window.sonic && window.sonic.getDiffData();

        function sonicCallback(data) {
            if (sonicHadExecute === 0) {
                sonicHadExecute = 1;
                callback(data['sonicStatus'], data['sonicUpdateData']);
            }
        }

        setTimeout(function() {
            if (sonicHadExecute === 0) {
                sonicHadExecute = 1;
                callback(0, {});
            }
        }, timeout);

        // the mobile client will invoke method getDiffDataCallback which can send Sonic response code and diff data to websites.
        window['getDiffDataCallback'] = function(sonicData) {
            /**
             * Sonic status:
             * 0: It fails to get any data from mobile client.
             * 1: It is first time for mobile client to use Sonic.
             * 2: Mobile client reload the whole websites.
             * 3: Websites will be updated dynamically with local refresh.
             * 4: The Sonic request of mobile client receives a 304 response code and nothing has been modified.
             */
            let sonicStatus = 0;
            let sonicUpdateData = {};  // sonic diff data
            sonicData = JSON.parse(sonicData);
            switch (parseInt(sonicData['srcCode'], 10)) {
                case 1000:
                    sonicStatus = 1;
                    break;
                case 2000:
                    sonicStatus = 2;
                    break;
                case 200:
                    sonicStatus = 3;
                    sonicUpdateData = JSON.parse(sonicData['result'] || '{}');
                    break;
                case 304:
                    sonicStatus = 4;
                    break;
            }
            sonicCallback({ sonicStatus: sonicStatus, sonicUpdateData: sonicUpdateData });
        };
    }

    render() {
        return (
            <div>
                <Head>
                    <meta httpEquiv="cache-control" content="no-cache" />
                    <link rel="shortcut icon" href="/static/favicon.ico" />
                    <title>react-sonic demo</title>
                    <script type="text/javascript" src="/static/js/flexible.js"></script>
                </Head>
                <div id="root" data-sonicdiff="firstScreenHtml">
                    <GameContainer {...this.props} />
                </div>
                <style jsx global>{`
                    body {
                        background: #059eff;
                    }
                    ul, li {
                        margin: 0;
                        padding: 0;
                        list-style: none;
                    }
                `}</style>
            </div>
        );
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        setSonicStatus: bindActionCreators(setSonicStatus, dispatch), // set sonic status
        initImgArr: bindActionCreators(initImgArr, dispatch) // initialize image array
    };
};

export default withRedux(initStore, null, mapDispatchToProps)(App);
