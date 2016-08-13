package com.flashmusic;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.flashmusic.tool.LrcInfo;
import com.flashmusic.tool.LrcParse;
import com.flashmusic.tool.Mp3Info;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

        //Music的listview控件
        private ListView MusicList;

        // 存储数据的数组列表
        ArrayList<HashMap<String, Object>> MusiclistData = new ArrayList<HashMap<String, Object>>();

        // 适配器
        private SimpleAdapter MusicListAdapter;

        private LrcInfo lrcInfo =null;

        //存放mp3信息
        static  List<Mp3Info> mp3Infos;
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);

                MusicList = (ListView) findViewById(R.id.listviewmusic);
//                LrcParse a =new LrcParse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/幻听.lrc");
//
//                lrcInfo = a.readLrc();

                mp3Infos = getMp3Infos();
                GetData(mp3Infos);

                MusicListAdapter = new SimpleAdapter(
                        this,
                        MusiclistData,
                        R.layout.listmusic,
                        new String[]{"ID", "Title", "Artist", "Icon"},
                        new int[]{R.id.MusicID, R.id.Musictitle, R.id.MusicArtist, R.id.MusicIcon}
                );
                //赋予数据
                MusicList.setAdapter(MusicListAdapter);

                MusicList.setOnItemClickListener(MusiclistListen);
        }

        AdapterView.OnItemClickListener MusiclistListen = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                         //Toast.makeText(MainActivity.this, String.valueOf(l), Toast.LENGTH_SHORT).show();
//
//                        //判断当前服务是否已经开启
//                        if(isServiceRunning(getBaseContext(),"com.flashmusic.MusicService")){
//                                stopService(new Intent(MainActivity.this, MusicService.class));
//                        }
//                        Intent intent   = new Intent();
//                        intent.putExtra("url", mp3Infos.get(i).getUrl());
//                        intent.putExtra("position",String.valueOf(i));
//                        intent.setClass(MainActivity.this, MusicService.class);
//                        //启动服务
//                        startService(intent);

                        Intent musicintent      = new Intent(MainActivity.this, MusicActivity.class);
                        //当前播放的位置
                        musicintent.putExtra("position", String.valueOf(i));
                        MusicActivity.mp3Infos = mp3Infos;
                   //     MusicActivity.musicserviceintent = intent;
                        //启动音乐播放界面
                        startActivity(musicintent);
                }
        };

        //判断服务是否存在
        public static boolean isServiceRunning(Context mContext,String className) {
                boolean isRunning = false;
                ActivityManager activityManager = (ActivityManager)
                        mContext.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningServiceInfo> serviceList= activityManager.getRunningServices(50);
                if (!(serviceList.size()>0)) {
                        return false;
                }
                for (int i=0; i<serviceList.size(); i++) {
                        String a =serviceList.get(i).service.getClassName();
                        if (serviceList.get(i).service.getClassName().equals(className) == true) {
                                isRunning = true;
                                break;
                        }
                }
                return isRunning;
        }
        public void GetData(List<Mp3Info> mp3Infos) {
                for (int i = 0; i < mp3Infos.size(); i++) {
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("ID",mp3Infos.get(i).getID());
                        map.put("Title", mp3Infos.get(i).getTitle());
                        map.put("Artist", mp3Infos.get(i).getArtist());
                        map.put("Icon", R.drawable.musicicon);
                        MusiclistData.add(map);
                }
        }

        public List<Mp3Info> getMp3Infos() {
                //ContentProvider
                Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                int ID = 0;
                List<Mp3Info> mp3Infos = new ArrayList<Mp3Info>();
                for (int i = 0; i < cursor.getCount(); i++) {
                        Mp3Info mp3Info = new Mp3Info();
                        cursor.moveToNext();
                        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));   //音乐id,音乐ID不是播放的顺序
                        String title = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));//音乐标题
                        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));//艺术家
                        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));//时长
                        long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));  //文件大小
                        String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));              //文件路径
                        int isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));//是否为音乐
                        if (isMusic != 0) {
                                //只把音乐添加到集合当中
                                ID++;
                                mp3Info.setID(ID);
                                mp3Info.setTitle(title);
                                mp3Info.setArtist(artist);
                                mp3Info.setDuration(duration);
                                mp3Info.setSize(size);
                                mp3Info.setUrl(url);
                                mp3Infos.add(mp3Info);
                        }
                }
                return mp3Infos;
        }


        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        }
}



