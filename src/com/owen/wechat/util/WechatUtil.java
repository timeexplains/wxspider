package com.owen.wechat.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.owen.wechat.exception.WechatException;
import com.owen.wechat.models.Topic;

public abstract class WechatUtil {

    private String id;
    protected Topic model;
    private int totalpages = 0;
    private String sogouParam = "";

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return "1.0";
    }

    public String getId() {
        return this.id;
    }

    private int getTotalPage(String str) {
        if (0 != totalpages) {
            return totalpages;
        }
        Pattern pattern = Pattern.compile("totalPages\":([0-9]*)");
        Matcher matcher = pattern.matcher(str);

        while (matcher.find()) {
            totalpages = Integer.parseInt(matcher.group(1));
        }
        return totalpages;
    }

    /**
     * 获取第一页的doc对象
     * 
     * @return
     */
    protected Document getDoc() {

        String url = makeUrl();
        try {
            return Jsoup
                    .connect(url)
                    .timeout(10000)
                    .ignoreContentType(true)
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36")
                    .header("Cookie",
                            "ABTEST=8|1430710665|v1; SUID=F55370722708930A000000005546E989; PHPSESSID=0hk2d8cl4128niajvb4f4asfq6; SUIR=1430710665; SUID=F55370724FC80D0A000000005546E989; SNUID=D47250532024351871AD39CB21F3D59C; SUV=00EA70CE727053F55546F1207367B700; weixinIndexVisited=1; wuid=AAGjZr7TCQAAAAqUKHWrjwEAkwA=; ld=nAVZ9yllll2qSs4glllllVqpDNtllllltXxFdyllll9lllllxllll5@@@@@@@@@@; usid=pz2gIdtBRiERY8lB; sct=2; wapsogou_qq_nickname=; IPLOC=CN3200")
                    .get();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取指定url的doc对象
     * 
     * @param url
     * @return
     */
    protected Document getDoc(String url) {
        try {
            return Jsoup
                    .connect(url)
                    .ignoreContentType(true)
                    .timeout(10000)
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36")
                    .header("Cookie",
                            "ABTEST=8|1430710665|v1; SUID=F55370722708930A000000005546E989; PHPSESSID=0hk2d8cl4128niajvb4f4asfq6; SUIR=1430710665; SUID=F55370724FC80D0A000000005546E989; SNUID=D47250532024351871AD39CB21F3D59C; SUV=00EA70CE727053F55546F1207367B700; weixinIndexVisited=1; wuid=AAGjZr7TCQAAAAqUKHWrjwEAkwA=; ld=nAVZ9yllll2qSs4glllllVqpDNtllllltXxFdyllll9lllllxllll5@@@@@@@@@@; usid=pz2gIdtBRiERY8lB; sct=2; wapsogou_qq_nickname=; IPLOC=CN3200")
                    .get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取搜狗url的参数
     */
    public String getSogouParam() {
        if (!"".equals(this.sogouParam)) {
            return this.sogouParam;
        }
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("js");
        try {
            se.eval(new FileReader(this.getClass().getResource("/url.js")
                    .getPath()));
            se.eval("eval(\"window.SogouEncrypt.setKv('8d11ae022be','1')\")");
            this.sogouParam = (String) se
                    .eval("eval(\"window.SogouEncrypt.encryptquery('"
                            + this.getId() + "','sogou')\")");
        } catch (FileNotFoundException | ScriptException e) {
            System.out.println(e);
        }
        return this.sogouParam;
    }

    protected String makeUrl() {
        if (null == id || "".equals(id)) {
            throw new WechatException("must set id first");
        }
        String urlParams = this.getSogouParam();
        return "http://weixin.sogou.com/gzhjs?cb=sogou.weixin.gzhcb&" + urlParams;
    }

    protected String makeUrl(int page) {
        if (null == id || "".equals(id)) {
            throw new WechatException("must set id first");
        }
        String urlParams = this.getSogouParam();
        return "http://weixin.sogou.com/gzhjs?cb=sogou.weixin.gzhcb&" + urlParams + "&page=" + page;
    }

    protected void excute() {
        Document doc = getDoc();
        if (null == doc) {
            throw new WechatException("unknown error");
        }

        Element topicUrl = doc.select("url").first();
        if (null == topicUrl) {
            throw new WechatException(
                    "make sure the openId is right, otherwise no topcs in this wechat account");
        }
        topicUrl.select("title1").remove();
        String url = topicUrl.text();
        fetchContent(url);
    }

    protected void fetchContent(String url) {
        Document doc = getDoc(url);
        if (null == doc) {
            return;
        }
        model = new Topic();
        String title = doc.select("#activity-name").first().text();
        Elements imagesDom = doc.select("#js_content img[data-src]");
        String content = doc.select("#js_content").first().html();
        String date = doc.select("#post-date").first().text();
        String user = doc.select("#post-user").first().text();
        List<String> images = new ArrayList<>();
        for (Element img : imagesDom) {
            images.add(img.attr("data-src"));
        }

        model.setContent(content);
        model.setImages(images);
        model.setUrl(url);
        model.setTitle(title);
        model.setDate(date);
        model.setUser(user);
    }

    public Topic getTopicByUrl(String url) {
        Document doc = getDoc(url);
        if (null == doc) {
            return null;
        }
        Topic topic = new Topic();
        String title = doc.select("#activity-name").first().text();
        Elements imagesDom = doc.select("#js_content img[data-src]");
        String content = doc.select("#js_content").first().html();
        String date = doc.select("#post-date").first().text();
        String user = doc.select("#post-user").first().text();
        List<String> images = new ArrayList<>();
        for (Element img : imagesDom) {
            images.add(img.attr("data-src"));
        }

        topic.setContent(content);
        topic.setImages(images);
        topic.setUrl(url);
        topic.setTitle(title);
        topic.setDate(date);
        topic.setUser(user);
        return topic;
    }


    /**
     * 获取指定页的全部话题
     * 
     * @param limit
     * @return
     */
    protected List<String> getTopicUrls(String url) {

    	List<String> result = new ArrayList<String>();
    	if(url.equals("") || null == url)
    	{
    		return result;
    	}
    	Document doc = getDoc(url);
    	String jsonStr = doc.html().split("var msgList = ")[1].split("seajs.use")[0].trim();
    	String[] tempList = jsonStr.split("content_url\":\"");
    	for (int i = 0; i < tempList.length; i++) {
    		if(tempList[i].startsWith("/s"))
    		{
    			result.add("http://mp.weixin.qq.com" +  tempList[i].split("\",\"copyright_stat")[0].replaceAll("amp;", ""));
    		}
			
		}
    	
    	return result;
    }
    
    /**
     *页面url路径 $("[name=em_weixinhao]:contains(letvwallpapers)").parents(".txt-box").find(".tit a").attr("href")
     *
     */
    public String getListUrl(){
    	String baseUrl = "http://weixin.sogou.com/weixin?type=1&ie=utf8&query=";
    	String searchUrl = baseUrl+this.id;
    	Document doc = getDoc(searchUrl);
    	return doc.select("[name=em_weixinhao]:contains("+this.id+")").parents().select(".tit a").attr("href");
    }

    /**
     * 获取指定页的文章doc对象
     * 
     * @param page
     *            当前页数
     * @return
     */
    public List<Document> getPageDocuments(int page) {
        String url = makeUrl(page);
        Document doc = getDoc(url);
        System.out.println(url);
        if (null == doc) {
            throw new WechatException("unknown error");
        }
        List<Document> docs = new ArrayList<Document>();
        if (0 != totalpages && page > totalpages) {
            return docs;
        }

        if (0 != totalpages) {
            getTotalPage(doc.select("pagesize").last().html().toString());
            if (page > totalpages) {
                return docs;
            }
        }

        ListIterator<Element> topicUrls = doc.select("url").listIterator();
        if (!topicUrls.hasNext()) {
            throw new WechatException(
                    "make sure the openId is right, otherwise no topics in this wechat account");
        }

        while (topicUrls.hasNext()) {
            Element topicUrl = topicUrls.next();
            topicUrl.select("title1").remove();
            Document topicDoc = getDoc(topicUrl.text());
            if (null != topicDoc) {
                docs.add(topicDoc);
            }
            topicDoc.attr("originUrl", url);
        }
        return docs;
    }

}
