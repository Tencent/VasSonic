/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

class GameHeader extends React.Component {
    render() {
        return (
            <div className="game-header">
                <div className="thumbnail">
                    <img src="/static/img/jigsaw/0.jpg" alt="" />
                </div>
                <div className="tag">
                    Sonic状态：{this.props.sonicStatus}
                </div>
                <style jsx>{`
                    .game-header .thumbnail{
                        position: absolute;
                        left: .969rem;
                        top: .61rem;
                        width: 2.734rem;
                        height: 2.734rem;
                        border: .2rem solid #fff;
                    }
                    .game-header .thumbnail img {
                        width: 100%;
                    }
                    .game-header .tag{
                        position: absolute;
                        right: .844rem;
                        top: 1.456rem;
                        width: 4.719rem;
                        height: 1.2rem;
                        padding-left: .2rem;
                        font-size: .47rem;
                        font-weight: bold;
                        line-height: 1.2rem;
                        color: #fff;
                    }
                `}</style>
            </div>
        );
    }
}

GameHeader.propTypes = {
    sonicStatus: PropTypes.string
};

const mapStateToProps = ({ sonicStatus }) => ({ sonicStatus });

export default connect(mapStateToProps, null)(GameHeader);
