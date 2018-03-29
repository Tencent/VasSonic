package com.github.tencent;

import java.util.regex.Matcher;

public abstract class AbstractReplaceCallBack implements ReplaceCallBack {

    protected Matcher matcher;

    final public String replace(String text, int index, Matcher matcher) {
        this.matcher = matcher;
        try {
            return doReplace(text, index, matcher);
        } finally {
            this.matcher = null;
        }
    }

    /**
     * 将text转化为特定的字符串返回
     * @param text
     * @param index
     * @param matcher
     * @return
     */
    public abstract String doReplace(String text, int index, Matcher matcher);

}
