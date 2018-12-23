/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import { createStore, applyMiddleware } from 'redux';
import { composeWithDevTools } from 'redux-devtools-extension';
import thunkMiddleware from 'redux-thunk';

const defaultInitialState = {
    sonicStatus: '--',
    gameArea: {
        initSeqArr: [],
        imgArr: [],
        selImgIndex: []
    }
};

// ------------------------------------
// Constants
// ------------------------------------
export const actionTypes = {
    GET_SONIC_STATUS: 'GET_SONIC_STATUS',
    INIT_IMG_ARR: 'INIT_IMG_ARR'
};

// ------------------------------------
// Actions
// ------------------------------------
const setSonicStatusAct = (res) => ({
    type: actionTypes.GET_SONIC_STATUS,
    sonicStatus: res
});
const initImgArrAct = (res) => ({
    type: actionTypes.INIT_IMG_ARR,
    gameArea: res
});

// ------------------------------------
// Specialized Action Creator
// ------------------------------------
export const setSonicStatus = (sonicStatus) => async (dispatch, getState) => {
    try {
        let statusWording = '';
        switch (sonicStatus) {
            case 0:
                statusWording = '异常';
                break;
            case 1:
                statusWording = '首次加载';
                break;
            case 2:
                statusWording = '模板更新';
                break;
            case 3:
                statusWording = '数据更新';
                break;
            case 4:
                statusWording = '完全缓存';
                break;
            default:
                statusWording = '异常';
                break;
        }
        await dispatch(setSonicStatusAct(statusWording));
    } catch (error) {
        console.log('error: ', error);
    }
};

export const initImgArr = (imgArrState) => async (dispatch, getState) => {
    try {
        if (imgArrState) {
            await dispatch(initImgArrAct(imgArrState));
            return;
        }
        let initSeqArr = [1, 2, 3, 4];
        let initSeqArrTmp = initSeqArr.slice(0);
        let initImgArr = [];

        while (initSeqArr.toString() === initSeqArrTmp.toString()) {
            initSeqArrTmp.sort(() => {
                return Math.random() - 0.5;
            });
        }
        initSeqArrTmp.forEach((i) => {
            initImgArr.push({
                'src': '/static/img/jigsaw/' + i + '.jpg',
                'index': i
            });
        });
        await dispatch(initImgArrAct({
            initSeqArr,
            imgArr: initImgArr,
            selImgIndex: []
        }));
    } catch (error) {
        console.log('error: ', error);
    }
};

// ------------------------------------
// Reducer
// ------------------------------------
export const reducer = (state = defaultInitialState, action) => {
    switch (action.type) {
        case actionTypes.GET_SONIC_STATUS:
            return {
                ...state,
                sonicStatus: action.sonicStatus
            };
        case actionTypes.INIT_IMG_ARR:
            return {
                ...state,
                gameArea: action.gameArea
            };
        default: return state;
    }
};

export const initStore = (initialState = defaultInitialState) => {
    return createStore(reducer, initialState, composeWithDevTools(applyMiddleware(thunkMiddleware)));
};