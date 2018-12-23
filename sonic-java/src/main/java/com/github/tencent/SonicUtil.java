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
     * get hex string
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
     * encrypt string
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
            return hex(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encryptText;
    }

    /**
     * replace string which match the pattern with callback
     * @param string
     * @param pattern
     * @param replacement
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

    /**
     * get matched string
     * @param strContent
     * @param strPattern
     * @return
     */
    public static String pregMatch(String strContent, String strPattern) {
        Pattern titlePattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        Matcher titleMatcher = titlePattern.matcher(strContent);
        if(titleMatcher.find()) {
            return titleMatcher.group(0);
        }
        return "";
    }

    /**
     * get http headers
     * @param httpRequest
     * @return
     */
    public static Map<String,String> getAllHttpHeaders(HttpServletRequest httpRequest) {
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
