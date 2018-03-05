package com.owen.wechat;


import java.util.List;

import com.owen.wechat.models.Topic;
import com.owen.wechat.util.WechatUtil;

public class WechatSpider extends WechatUtil {

    /**
     * <pre>
     *  new WechatSpider("123").getPageDocs(1); 获取第一页的的全部文章
     * </pre>
     * 
     * @param id
     *            微信公共号的openId
     */
    public WechatSpider(String id) {
        super.setId(id);
        //super.excute();
    }


    public static void main(String[] args) {
        WechatSpider spider = new WechatSpider("CCB_4008200588");//小米xiaomigongsi0406
        String listUrl = spider.getListUrl();
        System.out.println(listUrl);
        List<String> list = spider.getTopicUrls(listUrl);
        int i = 0;
        for (String url : list) {
			Topic topic = spider.getTopicByUrl(url);
//			if(i++==0) System.out.println(topic.getContent());
			System.out.println("标题是:"+topic.getTitle());
			System.out.println("时间是:"+topic.getDate());
			System.out.println("Url是:"+url);
        }
    }

}
