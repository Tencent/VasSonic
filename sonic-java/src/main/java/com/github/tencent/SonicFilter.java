package com.github.tencent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.*;

class TemplateReplace extends AbstractReplaceCallBack{
    public static boolean shoudSonicDiffBodyReplace = false; // 判断是否成功替换sonicdiffbody
    public static int diffIndex = 0;
    public static String tagPrefix = "auto";
    public static HashMap<String, String> diffTagNames = new HashMap<String, String>(); // 数据块

    public String doReplace(String text, int index, Matcher matcher)
    {
        StringBuilder tagBuilder = new StringBuilder();
        String tagName;
        shoudSonicDiffBodyReplace = true;
        if(matcher.groupCount() == 1)
        {
            tagName = matcher.group(1);
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            sb.append(tagPrefix).append(diffIndex++);
            tagName = sb.toString();
        }
        diffTagNames.put(tagName, matcher.group(0));
        return tagBuilder.append("{").append(tagName).append("}").toString();
    }

    public static void reset()
    {
        shoudSonicDiffBodyReplace = false;
        diffIndex = 0;
        diffTagNames.clear();
    }

}

public class SonicFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void destroy() {
        this.filterConfig = null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Map<String,String> headerMap = SonicUtil.getAllHttpHeaders(httpRequest);
        //客户端本地缓存的HTML摘要
        String etag = "";
        //服务端最新内容
        String htmlContent;
        //服务端最新内容摘要
        String htmlContentSha1 ="";
        //如果客户端支持diff能力，不使用缓存
        String value = headerMap.get("accept-diff");
        if (headerMap.containsKey("accept-diff") && value.equals("true")) {
            httpResponse.addHeader("Cache-Control", "no-cache");
            httpResponse.addHeader("Cache-Offline", "true");
            if (headerMap.containsKey("if-none-match") || headerMap.containsKey("If-None-Match")) {
                etag = (headerMap.get("if-none-match") != null ? headerMap.get("if-none-match")
                        : headerMap.get("If-None-Match"));
            }
        }
        HttpServletResponseCopier responseCopier = new HttpServletResponseCopier((HttpServletResponse) response);
        try {
            chain.doFilter(request, responseCopier);
            responseCopier.flushBuffer();
        } finally {
            String contentType = responseCopier.getContentType();
            byte[] copy = responseCopier.getCopy();
            if(contentType == null || !contentType.contains("text"))
            {
                ServletOutputStream out =  httpResponse.getOutputStream();
                out.write(copy);
                out.close();
                return;
            }
            htmlContent = new String(copy, "UTF-8");
            htmlContentSha1 = SonicUtil.encrypt(htmlContent, "sha-1");
        }
        // if html not change,return 304
        if(etag.equalsIgnoreCase(htmlContentSha1)){
            httpResponse.addHeader("Cache-Offline", "store");
            httpResponse.addHeader("Content-Length", "0");
            httpResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        httpResponse.setHeader("Etag", htmlContentSha1);
        String htmlTitle;
        String clientTemplateTag ="";
        if(headerMap.containsKey("template-tag")){
            clientTemplateTag = headerMap.get("template-tag");
        }
        String stringTitlePattern = "<title(.*?)<\\/title>";
        htmlTitle = SonicUtil.pregMatch(htmlContent, stringTitlePattern);
        String htmlTemplate= htmlContent.replaceAll(stringTitlePattern,"{title}");
        String stringTemplatePattern = "<!--sonicdiff-?(\\w*)-->[\\s\\S]+?<!--sonicdiff-?\\w*-end-->";
        TemplateReplace templateReplace = new TemplateReplace();
        Pattern pattern = Pattern.compile(stringTemplatePattern, Pattern.CASE_INSENSITIVE);
        htmlTemplate = SonicUtil.replaceAllCallBack(htmlTemplate, pattern, templateReplace);
        String templateMd5 = SonicUtil.encrypt(htmlTemplate, "sha-1");
        httpResponse.addHeader("template-tag", templateMd5);
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("{title}", htmlTitle);
        for (Map.Entry<String, String> entry : TemplateReplace.diffTagNames.entrySet()) {
            StringBuilder strKey = new StringBuilder();
            strKey.append("{").append(entry.getKey()).append("}");
            dataMap.put(strKey.toString(), entry.getValue());
        }
        TemplateReplace.reset();
        result.put("data", dataMap);
        result.put("template-tag", templateMd5);
        result.put("html-sha1", htmlContentSha1);
        String resultStr="";
        if(templateMd5.equals(clientTemplateTag))
        {
            httpResponse.addHeader("template-change", "false");
            //离线模板没有差异，不用更新
            result.put("diff", "");
            Gson gson = new Gson();
            resultStr = gson.toJson(result);
        }
        else
        {
            httpResponse.addHeader("template-change", "true");
            //客户端没有带离线版本，直接全量即可
            resultStr = htmlContent;
        }
        ServletOutputStream out =  httpResponse.getOutputStream();
        out.write(resultStr.getBytes("UTF-8"));
        httpResponse.addHeader("Content-Length", String.valueOf(resultStr.getBytes("UTF-8").length));
        out.close();
    }
    @Override
    public void init(FilterConfig config) throws ServletException {
        this.filterConfig = config;
    }

}
