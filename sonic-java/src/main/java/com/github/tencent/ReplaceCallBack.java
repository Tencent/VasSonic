package com.github.tencent;

import java.util.regex.Matcher;

public interface ReplaceCallBack {
    /**
     * replace string using matcher
     * @param text 
     * @param index 
     * @param matcher 
     * @return
     */
    public String replace(String text, int index, Matcher matcher);
}
