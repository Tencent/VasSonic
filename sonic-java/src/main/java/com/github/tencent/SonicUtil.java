package com.github.tencent;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

public class SonicUtil {

    /**
     * 返回十六进制字符串
     *
     * @param arr
     * @return
     */
    public static String hex(byte[] arr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; ++i) {
            sb.append(Integer.toHexString((arr[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    /**
     *
     * @param inputText
     * @param algorithmName
     * @return
     */
    public static String encrypt(String inputText, String algorithmName) {
        if (inputText == null || "".equals(inputText.trim())) {
            return "";
        }
        if (algorithmName == null || "".equals(algorithmName.trim())) {
            algorithmName = "md5";
        }
        String encryptText = null;
        try {
            MessageDigest m = MessageDigest.getInstance(algorithmName);
            m.update(inputText.getBytes("UTF8"));
            byte s[] = m.digest();
            // m.digest(inputText.getBytes("UTF8"));
            return hex(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encryptText;
    }

    /**
     * 将String中的所有pattern匹配的字符串替换掉
     *
     * @param string
     *            待替换的字符串
     * @param pattern
     *            替换查找的正则表达式对象
     * @param replacement
     *            替换函数
     * @return
     */
    public static String replaceAllCallBack(String string, Pattern pattern, ReplaceCallBack replacement) {
        if (string == null) {
            return null;
        }
        Matcher m = pattern.matcher(string);
        if (m.find()) {
            StringBuffer sb = new StringBuffer();
            int index = 0;
            while (true) {
                m.appendReplacement(sb, replacement.replace(m.group(0), index++, m));
                if (!m.find()) {
                    break;
                }
            }
            m.appendTail(sb);
            return sb.toString();
        }
        return string;
    }

    public static String pregMatch(String strContent, String strPattern)
    {
        Pattern titlePattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        Matcher titleMatcher = titlePattern.matcher(strContent);
        if(titleMatcher.find()){
            return titleMatcher.group(0);
        }
        return "";
    }

    /**
     * 获取HTTP所有的请求头
     * @param httpRequest
     * @return
     */
    public static Map<String,String> getAllHttpHeaders(HttpServletRequest httpRequest ){
        Map<String, String> headerMap = new HashMap<String, String>();
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = httpRequest.getHeader(key);
                headerMap.put(key, value);
            }
        }
        return headerMap;
    }
}
