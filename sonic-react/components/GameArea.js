/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import React from 'react';
import PropTypes from 'prop-types';

class GameArea extends React.Component {
    handleClickImg(index, i) {
        this.props.onClickImg && this.props.onClickImg(index, i);
    }

    render() {
        return (
            <ul className="game-area">
                {this.props.imgArr.map((img, i) => {
                    return (
                        <li key={img.index} onClick={() => this.handleClickImg(img.index, i)}>
                            <img src={img.src} alt="" />
                            {(this.props.selImgIndex.length !== 0 && this.props.selImgIndex[1] === i) ? <div className="mask" ></div> : null}
                        </li>
                    );
                })}
                <style jsx>{`
                    .game-area {
                        position: absolute;
                        left: 50%;
                        top: 4.451rem;
                        width: 8.75rem;
                        margin-left: -4.7rem;
                        border: .3rem solid #fff;
                    }
                    .game-area li {
                        position: relative;
                        width: 4.375rem;
                        height: 4.375rem;
                        overflow: hidden;
                        float: left;
                    }
                    .game-area li img {
                        width: 100%;
                    }
                    .game-area .mask {
                        position: absolute;
                        left: 0;
                        top: 0;
                        width: 100%;
                        height: 100%;
                        background: rgba(0,0,0,.6);
                     }
                `}</style>
            </ul>
        );
    }
}

GameArea.propTypes = {
    onClickImg: PropTypes.func,
    imgArr: PropTypes.array
};

export default GameArea;
