package com.flashmusic.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by zhouchenglin on 2016/4/19.
 */
public class LrcInfo {


    private String title;//标题
    private String artist;//歌手
    private String album;//专辑名字
    private String bySomeBody;//歌词制作者
    private String offset;
    private String language;   //语言
    private String errorinfo;   //错误信息


    //保存歌词信息和时间点
    ArrayList<LrcList> lrcLists;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setErrorinfo(String errorinfo) {
        this.errorinfo = errorinfo;
    }

    public String getErrorinfo() {
        return errorinfo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getBySomeBody() {
        return bySomeBody;
    }

    public void setBySomeBody(String bySomeBody) {
        this.bySomeBody = bySomeBody;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public ArrayList<LrcList> getLrcLists() {
        return lrcLists;
    }

    public void setLrcLists(ArrayList<LrcList> lrcLists) {
        this.lrcLists = lrcLists;
    }
}

