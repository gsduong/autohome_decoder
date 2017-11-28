package org.jsoup.autohome;  
  
import java.io.IOException;  
import java.net.URISyntaxException;  
import java.util.ArrayList;
import java.util.List;  
import java.util.regex.Matcher;  
import java.util.regex.Pattern;  
  
import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  
import org.jsoup.nodes.Element;  
  
public class DecodeAutoHome {  
    public String getArticle(String url) throws IOException, URISyntaxException {  
        // 文章的文本内容 - văn bản của bài viết  
        String article = null;  
  
        // 获取网页的Document对象 (Object Document)  
        Document doc = Jsoup.connect(url).get();  
        /**
         * Chú ý: Trang autohome có cách antispy/anticrawl nên phải kiểm tra doc = cách in ra xem 
         * đã lấy được đúng bài viết đó chưa hay bị redirect sang trang điền captcha hoặc
         * trang check security. Nếu bị redirect thì sẽ không lấy được element chứa nội dung và 
         * xuất hiện lỗi indexOutOfBound phần lấy element chứa nội dung
         * Đã test trường hợp này, code chạy được nhưng chỉ chạy lần đầu thành công, lầu sau bị chặn IP
         * System.out.println(doc);
         */
        
        // 获取文章的内容 - Lấy element chứa nội dung của bài viết qua tên class của thẻ  
        Element content = doc.getElementsByClass("rconten").get(0);  
  
        String articleHtml = content.html();  
  
        // #############################################################  
        // 处理js混淆部分  
  
        // 获取混淆的js代码 - lấy script đầu tiên - cái đoạn script trang autohome dùng để giấu nội dung text  
        String script = content.getElementsByTag("script").get(0).html();  
  
        /* 
         * 1.判断混淆 无参数 返回常量 函数 function Ad_() { function _A() { return 'Ad__'; }; 
         * if (_A() == 'Ad__') { return '8'; } else { return _A(); } } 
         *  
         * 需要将Ad_()替换为8 
         */  
        // 取出每个"混淆 无参数 返回常量 函数" 格式的函数  
        String regex1 = "function\\s*(\\w+)\\(\\)\\s*\\{\\s*" + "function\\s+\\w+\\(\\)\\s*\\{\\s*"  
                + "return\\s+[\'\"]([^\'\"]+)[\'\"];\\s*" + "\\};\\s*"  
                + "if\\s*\\(\\w+\\(\\)\\s*==\\s*[\'\"]([^\'\"]+)[\'\"]\\)\\s*\\{\\s*"  
                + "return\\s*[\'\"]([^\'\"]+)[\'\"];\\s*" + "\\}\\s*else\\s*\\{\\s*" + "return\\s*\\w+\\(\\);\\s*"  
                + "\\}\\s*" + "\\}";  
        Pattern p1 = Pattern.compile(regex1);  
        Matcher m1 = p1.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l1 = new ArrayList<String>();  
        // 遍历每一条函数  
        while (m1.find()) {  
            // Matcher.group(int i) 此方法可以获取正则表达式中第i个括号里的内容.  
            l1.add(m1.group());  
        }  
        for (String str : l1) {  
            Pattern p11 = Pattern.compile(regex1);  
            Matcher m11 = p11.matcher(str);  
            while (m11.find()) {  
                String name = m11.group(1);// 获取第一个括号里的内容  
                String b = m11.group(2);// 获取第二个括号里的内容  
                String c = m11.group(3);// 获取第三个括号里的内容  
                String value = "";  
                if (b.equals(c)) {  
                    value = m11.group(4);  
                } else {  
                    value = m11.group(2);  
                }  
                script = script.replaceAll(name + "\\(\\)", value);// 给name的正则加上括号  
            }  
  
        }  
  
        // 2.判断混淆 无参数 返回函数 常量  
        /* 
         * function wu_() { function _w() { return 'wu_'; }; if (_w() == 'wu__') 
         * { return _w(); } else { return '5%'; } } 需要将wu_()替换为5% 
         */  
  
        // 取出每一个"混淆 无参数 返回函数 常量" 格式的函数  
  
        String regex2 = "function\\s*(\\w+)\\(\\)\\s*\\{\\s*" + "function\\s*\\w+\\(\\)\\s*\\{\\s*"  
                + "return\\s*[\'\"]([^\'\"]+)[\'\"];\\s*" + "\\};\\s*"  
                + "if\\s*\\(\\w+\\(\\)\\s*==\\s*[\'\"]([^\'\"]+)[\'\"]\\)\\s*\\{\\s*" + "return\\s*\\w+\\(\\);\\s*"  
                + "\\}\\s*else\\s*\\{\\s*" + "return\\s*[\'\"]([^\'\"]+)[\'\"];\\s*" + "\\}\\s*" + "\\}";  
        Pattern p2 = Pattern.compile(regex2);  
        Matcher m2 = p2.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l2 = new ArrayList<String>();  
        while (m2.find()) {  
            l2.add(m2.group());  
        }  
        for (String str : l2) {  
            Pattern p21 = Pattern.compile(regex2);  
            Matcher m21 = p21.matcher(str);  
            while (m21.find()) {  
                String name = m21.group(1);// 获取第一个括号里的内容  
                String b = m21.group(2);  
                String c = m21.group(3);  
                String value = "";  
                if (!b.equals(c)) {  
                    value = m21.group(4);  
                } else {  
                    value = m21.group(2);  
                }  
                script = script.replaceAll(name + "\\(\\)", value);  
            }  
        }  
  
        // 3.var 参数等于返回值函数  
        /* 
         * 类似于此的混淆 var ZA_ = function(ZA__) { 'return ZA_'; return ZA__; }; 
         */  
        // 需要替换ZA__(',5_198')为,5_198  
  
        // 从js中,利用正则,提取上面这种格式的变量  
        String regex3 = "var\\s*([^=]+)\\s*=\\s*function\\(\\w+\\)\\{\\s*" + "[\'\"]return\\s*\\w+\\s*[\'\"];\\s*"  
                + "return\\s+\\w+;\\s*" + "\\};";  
        Pattern p3 = Pattern.compile(regex3);  
        Matcher m3 = p3.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l3 = new ArrayList<String>();  
        while (m3.find()) {  
            l3.add(m3.group());  
        }  
        for (String str : l3) {  
            Pattern p31 = Pattern.compile(regex3);  
            Matcher m31 = p31.matcher(str);  
            while (m31.find()) {  
                String name31 = m31.group(1);  
                // 再次利用正则,在js中截取ZA__(',5_198')  
                String regex32 = name31 + "\\(([^\\)]+)\\)";  
                Pattern p32 = Pattern.compile(regex32);  
                Matcher m32 = p32.matcher(script);  
                while (m32.find()) {  
                    String value32 = m32.group(1);  
                    script = script.replaceAll(regex32, value32);  
                }  
            }  
  
        }  
        // System.out.println(script);  
        // 4.var 无参数 返回常量 函数  
        /* 
         * 类似于此结构 var jW_ = function() { 'return jW_'; return '34;'; }; 
         */  
        // 需要替换jW_()为34;  
        String regex4 = "var\\s*([^=]+)=\\s*function\\(\\)\\s*\\{\\s*" + "[\'\"]return\\s*\\w+\\s*[\'\"];\\s*"  
                + "return\\s*[\'\"]([^\'\"]+)[\'\"];\\s*" + "\\};";  
        Pattern p4 = Pattern.compile(regex4);  
        Matcher m4 = p4.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l4 = new ArrayList<String>();  
        while (m4.find()) {  
            l4.add(m4.group());  
            /* 
             * String name4 = m4.group(1);//获取第一个括号里的内容 String value4 = 
             * m4.group(2);//获取第一个括号里的内容 script = 
             * script.replaceAll(name4+"\\(\\)", value4); 
             */  
        }  
        for (String str4 : l4) {  
            Pattern p41 = Pattern.compile(regex4);  
            Matcher m41 = p41.matcher(str4);  
            while (m41.find()) {  
                String name41 = m41.group(1);// 获取第一个括号里的内容  
                String value41 = m41.group(2);// 获取第一个括号里的内容  
                script = script.replaceAll(name41 + "\\(\\)", value41);  
            }  
        }  
        // 5.无参数 返回常量 函数  
        /* 
         * 类似于此结构 function HB_() { 'return HB_'; return '_;'; } 
         */  
        // 需要将HB_()替换为_;  
        String regex5 = "function\\s*(\\w+)\\(\\)\\s*\\{\\s*" + "[\'\"]return\\s*[^\'\"]+[\'\"];\\s*"  
                + "return\\s*[\'\"]([^\'\"]+)[\'\"];\\s*" + "\\}\\s*";  
        Pattern p5 = Pattern.compile(regex5);  
        Matcher m5 = p5.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l5 = new ArrayList<String>();  
        while (m5.find()) {  
            l5.add(m5.group());  
  
        }  
        // 遍历每一条匹配到的字符串  
        for (String str5 : l5) {  
            Pattern p51 = Pattern.compile(regex5);  
            Matcher m51 = p51.matcher(str5);  
            while (m51.find()) {  
                String name51 = m51.group(1);// 获取第一个括号里的内容,也就是HB_  
                String value51 = m51.group(2);// 获取第二个括号里的内容,也就是-;  
                String regex51 = name51 + "\\(\\)";// 组合HB_()的正则表达式  
                script = script.replaceAll(regex51, value51);  
            }  
  
        }  
        // 6.无参数 返回常量 函数 中间无混淆代码  
        /* 
         * 类似于此结构的 function do_() { return ''; } 需要将do_()替换为 
         */  
        String regex6 = "function\\s*(\\w+)\\(\\)\\s*\\{\\s*" + "return\\s*[\'\"]([^\'\"]*)[\'\"];\\s*" + "\\}\\s*";  
        Pattern p6 = Pattern.compile(regex6);  
        Matcher m6 = p6.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l6 = new ArrayList<String>();  
        while (m6.find()) {  
            l6.add(m6.group());  
        }  
        for (String str6 : l6) {  
            Pattern p61 = Pattern.compile(regex6);  
            Matcher m61 = p61.matcher(str6);  
            while (m61.find()) {  
                String name61 = m61.group(1);  
                String value6 = m61.group(2);  
                String regex61 = name61 + "\\(\\)";  
                script = script.replaceAll(regex61, value6);  
            }  
        }  
  
        // System.out.println(script);  
        // 7.字符串拼接时使无参常量函数  
        /* 
         * 类似于下面这种结构 (function() { 'return rv_'; return '%' })() 将上面这个替换为% 
         */  
  
        String regex7 = "\\(function\\(\\)\\s*\\{\\s*" + "[\'\"]return[^\'\"]+[\'\"];\\s*"  
                + "return\\s*([\'\"][^\'\"]*[\'\"]);?\\s*" + "\\}\\)\\(\\)";  
        Pattern p7 = Pattern.compile(regex7);  
        Matcher m7 = p7.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l7 = new ArrayList<String>();  
        while (m7.find()) {  
            l7.add(m7.group());  
  
        }  
        // 遍历每一个匹配的字符串,并替换为实际的值  
        for (String str7 : l7) {  
            Pattern p71 = Pattern.compile(regex7);  
            Matcher m71 = p71.matcher(str7);  
            while (m71.find()) {  
                String value7 = m71.group(1);  
                script = script.replace(str7, value7);  
  
            }  
        }  
  
        // 8.字符串拼接时使用返回参数的函数  
        /* 
         * 类似于以下这种结构 (function(eR__) { 'return eR_'; return eR__; })('%9') 
         * 将整体替换为%9 
         */  
  
        String regex8 = "\\(function\\(\\w+\\)\\s*\\{\\s*" + "[\'\"]return[^\'\"]+[\'\"];\\s*" + "return\\s*\\w+;\\s*"  
                + "\\}\\)\\(([\'\"][^\'\"]*[\'\"])\\)";  
        Pattern p8 = Pattern.compile(regex8);  
        Matcher m8 = p8.matcher(script);  
        // 找出所有匹配的字符串,存入List中  
        List<String> l8 = new ArrayList<String>();  
        while (m8.find()) {  
            l8.add(m8.group());  
  
        }  
        // 遍历每一个匹配的字符串,并替换为实际的值  
        for (String str8 : l8) {  
            Pattern p81 = Pattern.compile(regex8);  
            Matcher m81 = p81.matcher(str8);  
            while (m81.find()) {  
                String value8 = m81.group(1);  
                script = script.replace(str8, value8);  
  
            }  
        }  
  
        // .获取所有var pz_='8'格式的变量  
        Pattern p = Pattern.compile("var \\w+=\'.*?\'");  
        Matcher m = p.matcher(script);  
        while (m.find()) {  
            if (m.group().contains("<")) {  
                continue;  
            }  
            // System.out.println(m.group());  
            String varName = m.group().split(" ")[1].split("=")[0];  
            String varValue = m.group().split(" ")[1].split("=")[1].replaceAll("'", "");  
            script = script.replaceAll(varName, varValue);  
        }  
  
        // 将js中所有的空格,+,',都去掉  
        script = script.replaceAll("[\\s+']", "");  
        // ((?:%\w\w|[A-Za-z\d])+)  
        Pattern p10 = Pattern.compile("((?:%\\w\\w)+)");  
        Matcher m10 = p10.matcher(script);  
        String[] words = {};  
        while (m10.find()) {  
            //System.out.println(m10.group());  
            // 将在js端decodeURIComponent编码的字符串,用下面这种方法解码.  
            String result = new java.net.URI(m10.group()).getPath();  
            words = result.split("");  
            // System.out.println(Arrays.toString(words));  
        }  
        // 从 字符串密集区域后面开始寻找索引区域,连续匹配十次以上,确保是索引  
        Pattern p11 = Pattern.compile("([\\d,]+(;[\\d,]+)+){10,}");  
        Matcher m11 = p11.matcher(script);  
        String[] indexs = {};  
        while (m11.find()) {  
            indexs = m11.group().split("[;,]");  
        }  
  
        //System.out.println(Arrays.toString(indexs));  
  
        // js中的混淆解决完成  
        // #########################################################################  
        // 开始替换<span>标签  
        // 获取<span>里class属性的数字  
        Pattern p12 = Pattern.compile("<span\\s*class=[\'\"]hs_kw(\\d+)_([^\'\"]+)[\'\"]></span>");  
        Matcher m12 = p12.matcher(articleHtml);  
        while (m12.find()) {  
            // 将String类型的数字转为int类型  
            int num = Integer.parseInt(m12.group(1));  
            // 以class属性中的数字为下标,寻找对应的索引号.  
            String index = indexs[num];  
            // 将String类型的索引号转为int类型  
            int indexWord = Integer.parseInt(index);  
            // 获取每个<span>对应的文字  
            String word = words[indexWord];  
            articleHtml = articleHtml.replace(m12.group(), word);  
        }  
        Document artcileDoc = Jsoup.parse(articleHtml);  
        article = artcileDoc.text().replaceAll(" ", "");// 获取文字,去空格  
        return article;  
    }
}  