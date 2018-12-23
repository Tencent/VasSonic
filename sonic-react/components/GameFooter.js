/**
 * Tencent is pleased to support the open source community by making VasSonic available.
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * https://opensource.org/licenses/BSD-3-Clause
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

const GameFooter = (props) => {
    return (
        <div className="game-footer">
            <p>
                重新打开页面以观察 sonic 状态和效果
            </p>
            <style jsx>{`
                .game-footer {
                    position: absolute;
                    top: 14.5rem;
                    width: 100%;
                    height: 1.2rem;
                    margin-left: -0.3rem;
                }
                .game-footer p {
                    margin: 0;
                    padding: 0;
                    font-size: .47rem;
                    font-weight: bold;
                    line-height: 1.2rem;
                    color: #fff;
                    text-align: center;
                }
            `}</style>
        </div>
    );
};

export default GameFooter;
