package com.flashmusic;

import com.flashmusic.tool.LrcInfo;
import com.flashmusic.tool.LrcList;
import com.flashmusic.tool.LrcParse;
import com.flashmusic.tool.PlayerMSG;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.Toast;


import com.flashmusic.tool.Mp3Info;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    // mp3的绝对路径
    private String path;
    //当前播放位置
    private int position;
    //当前播放进度
    private int currentTime;

    private int msg;                //播放信息

    private boolean isPause;

    static List<Mp3Info> mp3Infos;    //存放Mp3Info对象的集合

    private int duration;            //播放长度

    private LrcInfo lrcInfo;        //歌词信息
    private ArrayList<LrcList> lrcLists;    //存放歌词的结点信息

    int palyflag = 0;               //播放的标志

    int index;                      //歌词索引


    //服务要发送的一些Action
    public static final String UPDATE_ACTION = "com.flashmusic.action.UPDATE_ACTION";  //更新音乐播放曲目
    public static final String CTL_ACTION = "com.flashmusic.action.CTL_ACTION";        //控制播放模式
    public static final String MUSIC_CURRENT = "com.flashmusic.action.MUSIC_CURRENT";  //当前音乐播放时间更新
    public static final String MUSIC_DURATION = "com.flashmusic.action.MUSIC_DURATION";//播放音乐长度更新
    public static final String PLAY_STATUE = "com.flashmusic.action.PLAY_STATUE";      //更新播放状态
    public static final String SHOW_LRC = "com.flashmusic.action.SHOW_LRC";			//通知显示歌词
    //播放音乐的媒体类
    MediaPlayer mediaPlayer;
    //广播接收器，接收来自MusicActivity的广播
    private MusicReceiver musicReceiver;


    IBinder musicBinder = new MyBinder();

    //handler用来接收消息，来发送广播更新播放时间
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (mediaPlayer != null) {
                        currentTime = mediaPlayer.getCurrentPosition();
                        Intent intent = new Intent();
                        intent.setAction(MUSIC_CURRENT);
                        intent.putExtra("currentTime", currentTime);
                        sendBroadcast(intent); // 给PlayerActivity发送广播
                        mHandler.sendEmptyMessageDelayed(1, 1000);
                    }
                    break;
                default:
                    break;
            }
        }

        ;
    };

    class MyBinder extends Binder {
        public Service getService() {
            return MusicService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();

        /**
         * 设置音乐播放完成时的监听器
         */
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Intent intent = new Intent(PLAY_STATUE);
                // 发送播放完毕的信号，更新播放状态
                intent.putExtra("playstatue", false);
                sendBroadcast(intent);

                if (palyflag == 2) {
                    Intent loopintent = new Intent(PLAY_STATUE);
                    // 发送播放完毕的信号，更新播放状态
                    intent.putExtra("playstatue", true);
                    sendBroadcast(loopintent);
                    // 单曲循环
                    mediaPlayer.start();

                } else if (palyflag == 1) {
                    // 列表循环
                    position++;
                    if (position > mp3Infos.size() - 1) {
                        //变为第一首的位置继续播放
                        position = 0;
                    }
                    Intent sendIntent = new Intent(UPDATE_ACTION);
                    sendIntent.putExtra("position", position);
                    // 发送广播，将被MusicActivity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    path = mp3Infos.get(position).getUrl();
                    play(0);
                } else if (palyflag == 0) { // 顺序播放
                    position++;    //下一首位置
                    if (position <= mp3Infos.size() - 1) {
                        Intent sendIntent = new Intent(UPDATE_ACTION);
                        sendIntent.putExtra("position", position);
                        // 发送广播，将被MusicActivity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent);
                        path = mp3Infos.get(position).getUrl();
                        play(0);
                    } else {
                        mediaPlayer.seekTo(0);
                        position = 0;
                        Intent sendIntent = new Intent(UPDATE_ACTION);
                        sendIntent.putExtra("position", position);
                        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent);
                    }
                } else if (palyflag == 3) {    //随机播放
                    position = getRandomIndex(mp3Infos.size() - 1);
                    Intent sendIntent = new Intent(UPDATE_ACTION);
                    sendIntent.putExtra("position", position);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    path = mp3Infos.get(position).getUrl();
                    play(0);
                }
            }
        });

        musicReceiver = new MusicReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CTL_ACTION);
        filter.addAction(SHOW_LRC);
        registerReceiver(musicReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        path = intent.getStringExtra("url");        //歌曲路径
        position = intent.getIntExtra("position", -1);    //当前播放歌曲的在mp3Infos的位置
        msg = intent.getIntExtra("MSG", 0);            //播放信息
        if (msg == PlayerMSG.MSG.PLAY_MSG) {    //直接播放音乐
            play(0);
        } else if (msg == PlayerMSG.MSG.PAUSE_MSG) {    //暂停
            pause();
        } else if (msg == PlayerMSG.MSG.STOP_MSG) {        //停止
            stop();
        } else if (msg == PlayerMSG.MSG.CONTINUE_MSG) {    //继续播放
            resume();
        } else if (msg == PlayerMSG.MSG.PRIVIOUS_MSG) {    //上一首
            previous();
        } else if (msg == PlayerMSG.MSG.NEXT_MSG) {        //下一首
            next();
        } else if (msg == PlayerMSG.MSG.PROGRESS_CHANGE) {    //进度更新
            currentTime = intent.getIntExtra("progress", -1);
            play(currentTime);
        } else if (msg == PlayerMSG.MSG.PLAYING_MSG) {
            mHandler.sendEmptyMessage(1);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void play(int currentTime) {
        try {

            mediaPlayer.reset();// 把各项参数恢复到初始状态
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare(); // 进行缓冲
            mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// 注册一个监听器

            initLrc();
            //更新播放状态
            Intent intent = new Intent(PLAY_STATUE);
            // 发送播放完毕的信号，更新播放状态
            intent.putExtra("playstatue", true);
            sendBroadcast(intent);
            mHandler.sendEmptyMessage(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //继续播放
    private void resume() {
        if (isPause) {
            mediaPlayer.start();
            isPause = false;
        }
    }

    /**
     * 上一首
     */
    private void previous() {
        Intent sendIntent = new Intent(UPDATE_ACTION);
        sendIntent.putExtra("position", position);
        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
        sendBroadcast(sendIntent);
        play(0);
    }

    /**
     * 下一首
     */
    private void next() {
        Intent sendIntent = new Intent(UPDATE_ACTION);
        sendIntent.putExtra("position", position);
        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
        sendBroadcast(sendIntent);
        play(0);
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPause = true;
        }
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
     */
    private final class PreparedListener implements MediaPlayer.OnPreparedListener {
        private int currentTime;

        public PreparedListener(int currentTime) {
            this.currentTime = currentTime;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start(); // 开始播放
            if (currentTime > 0) { // 如果音乐不是从头播放
                mediaPlayer.seekTo(currentTime);
            }
            Intent intent = new Intent();
            intent.setAction(MUSIC_DURATION);
            duration = mediaPlayer.getDuration();
            intent.putExtra("duration", duration);    //通过Intent来传递歌曲的总长度
            sendBroadcast(intent);
        }
    }

    protected int getRandomIndex(int end) {
        int index = (int) (Math.random() * end);
        return index;
    }

    public void initLrc() {

        //建立歌词对象
       LrcParse lrcParser = new LrcParse(path);
        //读歌词，并将数据传给歌词信息类
        lrcInfo = lrcParser.readLrc();
        //获得歌词中的结点
        lrcLists = lrcInfo.getLrcLists();
        //在musicActivity里面设置静态来共享数据
        MusicActivity.lrcView.setmLrcList(lrcInfo);
        //切换带动画显示歌词
        MusicActivity.lrcView.setAnimation(AnimationUtils.loadAnimation(MusicService.this, R.anim.alpha_z));
        mHandler.post(mRunnable);

    }

    Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            MusicActivity.lrcView.setIndex(lrcIndex());
            MusicActivity.lrcView.invalidate();
            mHandler.postDelayed(mRunnable, 100);
        }
    };

    public int lrcIndex() {
        if (mediaPlayer.isPlaying()) {
            currentTime = mediaPlayer.getCurrentPosition();
            duration = mediaPlayer.getDuration();
        }
        if (currentTime < duration) {
            for (int i = 0; i < lrcLists.size(); i++) {
                if (i < lrcLists.size() - 1) {
                    if (currentTime < lrcLists.get(i).getCurrentTime() && i == 0) {
                        index = i;
                    }
                    if ((currentTime > lrcLists.get(i).getCurrentTime())&& currentTime < lrcLists.get(i+1).getCurrentTime()) {
                        index = i;
                    }
                }
                if ((i == lrcLists.size() - 1)&& currentTime > lrcLists.get(i).getCurrentTime()) {
                    index = i;
                }
            }
        }
        return index;
    }

    public class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra("control", -1);
            switch (control) {
                case 0:
                    palyflag = 0; // 顺序播放
                    break;
                case 1:
                    palyflag = 1;    //列表循环
                    break;
                case 2:
                    palyflag = 2;    //单曲循环
                    break;
                case 3:
                    palyflag = 3;  //随机
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        //当绑定后，返回一个musicBinder
        return musicBinder;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mHandler.removeCallbacks(mRunnable);
    }
}
//     mHandler.removeCallbacks(mRunnable);
//    //初始化音乐播放
//    void init() {
//        //进入Idle
//
//        //    mediaPlayer.release();
//        mediaPlayer = new MediaPlayer();
//        try {
//            //初始化
//            mediaPlayer.setDataSource(path);
//
//            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//
//            // prepare 通过异步的方式装载媒体资源
//            mediaPlayer.prepareAsync();
//
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    //获得全部时间
//    public int getTime() {
//        return mediaPlayer.getDuration();
//    }
//
//    //获得当前时间
//    public int getCurrentTime() {
//        return mediaPlayer.getCurrentPosition();
//    }
//
//
//    //返回当前的播放进度，是double类型，即播放的百分比
//    public double getProgress() {
//        //当前播放位置
//        int position = mediaPlayer.getCurrentPosition();
//
//        //总位置
//        int time = mediaPlayer.getDuration();
//
//        double progress = (double) position / (double) time;
//
//        return progress;
//    }
//
//
//    //通过activity调节播放进度
//    public void setProgress(int max, int dest) {
//        int time = mediaPlayer.getDuration();
//        mediaPlayer.seekTo(time * dest / max);
//    }


//    //测试播放音乐
//    public void play() {
//        if (mediaPlayer != null) {
//            mediaPlayer.start();
//        }
//
//    }
//
//    //暂停音乐
//    public void pause() {
//        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//            mediaPlayer.pause();
//        }
//    }

//service 销毁时，停止播放音乐，释放资源
//    @Override
//    public void onDestroy() {
//        // 在activity结束的时候回收资源
//        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//        super.onDestroy();
//    }

