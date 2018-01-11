/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import React from 'react';
import Notifications, { notify } from 'react-notify-toast';
import { connect } from 'react-redux';

import GameHeader from '../components/GameHeader';
import GameArea from '../components/GameArea';
import GameFooter from '../components/GameFooter';

class GameContainer extends React.Component {
    constructor(props) {
        super(props);
        this.canPlay = true;
        this.state = {
            selImgIndex: props.selImgIndex,
            imgArr: props.imgArr
        };
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            imgArr: nextProps.imgArr,
            selImgIndex: nextProps.selImgIndex
        }, function() {
            this.canPlay = true;
        });
    }

    handleClickImg(index, i) {
        if (!this.canPlay) {
            return;
        }
        let selImgIndex = this.state.selImgIndex.slice(0);
        if (selImgIndex.length === 0) {
            this.setState({
                selImgIndex: [index, i]
            });
        } else {
            let imgArr = this.state.imgArr.slice(0);
            imgArr[selImgIndex[1]] = {
                src: '/static/img/jigsaw/' + index + '.jpg',
                index: index
            };
            imgArr[i] = {
                src: '/static/img/jigsaw/' + selImgIndex[0] + '.jpg',
                index: selImgIndex[0]
            };
            this.setState({
                imgArr,
                selImgIndex: []
            }, () => {
                let newSeqArr = imgArr.map((img) => {
                    return img.index;
                });
                if (newSeqArr.toString() === this.props.initSeqArr.toString()) {
                    this.canPlay = false;
                    notify.show('success!', 'success');
                }
            });
        }
    }

    render() {
        return (
            <div>
                <GameHeader />
                <GameArea
                    imgArr={this.state.imgArr}
                    selImgIndex={this.state.selImgIndex}
                    onClickImg={this.handleClickImg.bind(this)}
                />
                <GameFooter />
                <Notifications />
            </div>
        );
    }
}

const mapStateToProps = (state) => {
    return {
        selImgIndex: state.gameArea.selImgIndex,
        imgArr: state.gameArea.imgArr,
        initSeqArr: state.gameArea.initSeqArr
    };
};

export default connect(mapStateToProps, null)(GameContainer);
