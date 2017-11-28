package org.jsoup.autohome;
import java.io.IOException;  
import java.net.URISyntaxException;  
public class AppTest {  
    public static void main(String[] args) throws IOException, URISyntaxException {  
        DecodeAutoHome d = new DecodeAutoHome();  
        String url = "https://club.autohome.com.cn/bbs/thread-c-3980-65944945-1.html";  
        String article = d.getArticle(url);  
        System.out.println(article);  
    }  
} 
