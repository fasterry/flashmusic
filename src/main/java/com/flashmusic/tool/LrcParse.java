package com.flashmusic.tool;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhouchenglin on 2016/4/19.
 */
public class LrcParse {

    private LrcInfo lrcInfo = new LrcInfo();
    public static String charSet = "gbk";

    //mp3歌词存放地地方
    private String Path;
    //mp3时间
    private long currentTime;
    //MP3对应时间的内容
    private String currentContent;

    //保存时间点和内容
    ArrayList<LrcList> lrcLists = new ArrayList<LrcList>();


    private InputStream inputStream;

    public LrcParse(String path) {

        this.Path = path.replace(".mp3", ".lrc");
    }

    public LrcInfo readLrc() {
        //定义一个StringBuilder对象，用来存放歌词内容
        StringBuilder stringBuilder = new StringBuilder();
        try {

            inputStream = new FileInputStream(this.Path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charSet));
            String str = null;
            //逐行解析
            while ((str = reader.readLine()) != null) {
                if (!str.equals("")) {
                    decodeLine(str);
                }
            }
            //全部解析完后，设置lrcLists
            lrcInfo.setLrcLists(lrcLists);
            return lrcInfo;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            lrcLists.clear();
            LrcList lrcList = new LrcList();
            //设置时间点和内容的映射
            lrcList.setContent("歌词文件没发现！");
            lrcLists.add(lrcList);
            lrcInfo.setLrcLists(lrcLists);
            return lrcInfo;
        } catch (IOException e) {
            e.printStackTrace();
            lrcLists.clear();
            LrcList lrcList = new LrcList();
            //设置时间点和内容的映射
            lrcList.setContent("木有读取到歌词！");
            lrcLists.add(lrcList);
            lrcInfo.setLrcLists(lrcLists);
            return lrcInfo;
        }

    }

    /**
     * 单行解析
     */
    private LrcInfo decodeLine(String str) {

        if (str.startsWith("[ti:")) {
            // 歌曲名
            lrcInfo.setTitle(str.substring(4, str.lastIndexOf("]")));
            //   lrcTable.put("ti", str.substring(4, str.lastIndexOf("]")));

        } else if (str.startsWith("[ar:")) {// 艺术家
            lrcInfo.setArtist(str.substring(4, str.lastIndexOf("]")));

        } else if (str.startsWith("[al:")) {// 专辑
            lrcInfo.setAlbum(str.substring(4, str.lastIndexOf("]")));

        } else if (str.startsWith("[by:")) {// 作词
            lrcInfo.setBySomeBody(str.substring(4, str.lastIndexOf("]")));

        } else if (str.startsWith("[la:")) {// 语言
            lrcInfo.setLanguage(str.substring(4, str.lastIndexOf("]")));
        } else {

            //设置正则表达式
            String timeflag = "\\[(\\d{1,2}:\\d{1,2}\\.\\d{1,2})\\]|\\[(\\d{1,2}:\\d{1,2})\\]";

            Pattern pattern = Pattern.compile(timeflag);
            Matcher matcher = pattern.matcher(str);
            //如果存在匹配项则执行如下操作
            while (matcher.find()) {
                //得到匹配的内容
                String msg = matcher.group();
                //得到这个匹配项开始的索引
                int start = matcher.start();
                //得到这个匹配项结束的索引
                int end = matcher.end();
                //得到这个匹配项中的数组
                int groupCount = matcher.groupCount();
                for (int index = 0; index < groupCount; index++) {
                    String timeStr = matcher.group(index);
                    Log.i("", "time[" + index + "]=" + timeStr);
                    if (index == 0) {
                        //将第二组中的内容设置为当前的一个时间点
                        currentTime = str2Long(timeStr.substring(1, timeStr.length() - 1));
                    }
                }

                //得到时间点后的内容
                String[] content = pattern.split(str);

                //将内容设置为当前内容
                if (content.length == 0) {
                    currentContent = "";
                } else {
                    currentContent = content[content.length - 1];
                }
                LrcList lrcList = new LrcList();
                //设置时间点和内容的映射
                lrcList.setCurrentTime(currentTime);
                lrcList.setContent(currentContent);
                lrcLists.add(lrcList);

            }

        }
        return this.lrcInfo;
    }

    private long str2Long(String timeStr) {
        //将时间格式为xx:xx.xx，返回的long要求以毫秒为单位
        Log.i("", "timeStr=" + timeStr);
        String[] s = timeStr.split("\\:");
        int min = Integer.parseInt(s[0]);
        int sec = 0;
        int mill = 0;
        if (s[1].contains(".")) {
            String[] ss = s[1].split("\\.");
            sec = Integer.parseInt(ss[0]);
            mill = Integer.parseInt(ss[1]);
            Log.i("", "s[0]=" + s[0] + "s[1]" + s[1] + "ss[0]=" + ss[0] + "ss[1]=" + ss[1]);
        } else {
            sec = Integer.parseInt(s[1]);
            Log.i("", "s[0]=" + s[0] + "s[1]" + s[1]);
        }
        return min * 60 * 1000 + sec * 1000 + mill * 10;
    }
}
