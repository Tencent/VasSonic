package com.github.tencent;

import java.util.regex.Matcher;

public abstract class AbstractReplaceCallBack implements ReplaceCallBack {

    protected Matcher matcher;

    /*
     * (non-Javadoc)
     *
     * @see utils.ReplaceCallBack#replace(java.lang.String, int,
     * java.util.regex.Matcher)
     */
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
     *
     * @param text
     *            指定的字符串
     * @param index
     *            替换的次序
     * @param matcher
     *            Matcher对象
     * @return
     */
    public abstract String doReplace(String text, int index, Matcher matcher);

}
