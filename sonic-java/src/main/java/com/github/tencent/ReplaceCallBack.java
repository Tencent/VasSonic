package com.github.tencent;

import java.util.regex.Matcher;

public interface ReplaceCallBack {
    /**
     * 将text转化为特定的字符串返回
     * @param text 指定的字符串
     * @param index 替换的次序
     * @param matcher Matcher对象
     * @return
     */
    public String replace(String text, int index, Matcher matcher);
}
