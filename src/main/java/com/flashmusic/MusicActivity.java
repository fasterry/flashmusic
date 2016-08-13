package com.flashmusic;

import com.flashmusic.View.LrcView;
import com.flashmusic.tool.PlayerMSG;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ThemedSpinnerAdapter;
import android.widget.Toast;

import com.flashmusic.tool.Mp3Info;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;


public class MusicActivity extends Activity implements View.OnClickListener {

    //mp3需要播放的数据
    static List<Mp3Info> mp3Infos;
    static Intent musicserviceintent;

    //播放按钮
    private ImageView MusicPlay;
    //下一首
    private ImageView MusicNext;
    //上一首
    private ImageView MusicPrevious;
    //播放方式选择
    private ImageView MusicMOde;
    //播放菜单
    private ImageView MusicMenu;
    //显示总时间
    private TextView MusicTime;
    //显示当前时间
    private TextView MusicCurrentTime;
    //显示歌曲名
    private TextView MusicTitle;
    //显示歌曲艺术家
    private TextView MusicArtist;
    // 自定义歌词视图
    public static LrcView lrcView;
    //进度条
    SeekBar seekBar;
    //广播
    MusicPlayerReceiver musicPlayerReceiver;


    private final int isorderplay = 0;//顺序播放
    private final int islistloop = 1;//表示列表循环
    private final int isrepeatone = 2;//单曲循环
    private final int israndomplay = 3;//随机

    public static final String UPDATE_ACTION = "com.flashmusic.action.UPDATE_ACTION";  //更新动作
    public static final String CTL_ACTION = "com.flashmusic.action.CTL_ACTION";        //控制动作
    public static final String MUSIC_CURRENT = "com.flashmusic.action.MUSIC_CURRENT";  //音乐当前时间改变动作
    public static final String MUSIC_DURATION = "com.flashmusic.action.MUSIC_DURATION";//音乐播放长度改变动作
    public static final String MUSIC_PLAYING = "com.flashmusic.action.MUSIC_PLAYING";  //音乐正在播放动作
    public static final String REPEAT_ACTION = "com.flashmusic.action.REPEAT_ACTION";  //音乐重复播放动作
    public static final String SHUFFLE_ACTION = "com.flashmusic.action.RANDOM_ACTION";//音乐随机播放动作
    public static final String PLAY_STATUE = "com.flashmusic.action.PLAY_STATUE";      //更新播放状态

    //是否绑定了对应的service
    Boolean mBound = false;
    //播放方式表识0表示顺序播放，1表示列表循环，2表示单曲循环，3表示随机，初始为顺序播放
    int playmodeflag = 0;
    //歌曲播放的位置,就能够获取位置
    int position;
    int currentTime;
    int duration;//总时间

    //记录鼠标点击了几次，播放和暂停状态
    boolean playorpauseflag = false;
    //播放服务
    MusicService mService;
    // 正在播放
    private boolean isPlaying;
    // 暂停
    private boolean isPause;

    //多线程，后台更新UI
    Thread myThread;

    //控制后台线程退出
    boolean playStatus = true;


    //转换毫秒数为时间模式，一般都是分钟数，音乐文件
    public static String formatDuring(long mss) {
        long days = mss / (1000 * 60 * 60 * 24);
        long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musicplay);

        //初始化控件
        InitView();

        //获得传过来的值
        Intent intent = getIntent();
        position = Integer.parseInt(intent.getStringExtra("position"));

        MusicArtist.setText(mp3Infos.get(position).getArtist());
        MusicTitle.setText(mp3Infos.get(position).getTitle());

        //注册广播
        musicPlayerReceiver = new MusicPlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        filter.addAction(MUSIC_CURRENT);
        filter.addAction(MUSIC_DURATION);
        filter.addAction(PLAY_STATUE);

        registerReceiver(musicPlayerReceiver, filter);


        //设置响应事件
        MusicNext.setOnClickListener(this);
        MusicPrevious.setOnClickListener(this);
        MusicMenu.setOnClickListener(this);
        MusicMOde.setOnClickListener(this);
        MusicPlay.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        PlayMusic();
    }

    //初始化控件
    void InitView() {
        MusicPlay = (ImageView) findViewById(R.id.musicplay);
        MusicNext = (ImageView) findViewById(R.id.musicnext);
        MusicPrevious = (ImageView) findViewById(R.id.musicprevious);
        MusicMenu = (ImageView) findViewById(R.id.musicplaymenu);
        MusicMOde = (ImageView) findViewById(R.id.musicplaymode);
        MusicTime = (TextView) findViewById(R.id.playtime);
        MusicCurrentTime = (TextView) findViewById(R.id.playcurrenttime);
        MusicTitle = (TextView) findViewById(R.id.musictitle);
        MusicArtist = (TextView) findViewById(R.id.musicartist);
        seekBar = (SeekBar) findViewById(R.id.MusicProgress);
        lrcView = (LrcView) findViewById(R.id.lrcShowView);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.musicplay:
                if (isPlaying) {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause, null));
                    intent.setClass(MusicActivity.this, MusicService.class);
                    intent.putExtra("MSG", PlayerMSG.MSG.PAUSE_MSG);
                    startService(intent);
                    isPlaying = false;

                } else {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
                    intent.setClass(MusicActivity.this, MusicService.class);
                    intent.putExtra("MSG", PlayerMSG.MSG.CONTINUE_MSG);
                    startService(intent);
                    isPlaying = true;
                }
                break;
            case R.id.musicplaymode:
                setPlayMOde();
                break;
            case R.id.musicnext:
                PlayNextMusic();
                break;
            case R.id.musicprevious:
                PlayPreviousMusic();
                break;
            case R.id.musicplaymenu:
                break;
            default:
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void setPlayMOde() {
        playmodeflag++;
        Intent intent = new Intent(CTL_ACTION);
        intent.putExtra("control", playmodeflag);
        sendBroadcast(intent);
        switch (playmodeflag) {
            case isorderplay:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.orderplay, null));
                break;
            case islistloop:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.repeatplay, null));
                break;
            case isrepeatone:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.repeatoneplay, null));
                break;
            case israndomplay:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.randomplay, null));
                playmodeflag = -1;
                break;
            default:
                break;
        }

    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        //停止拖动事件
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //手动调节进度
            // TODO Auto-generated method stub
            //seekbar的拖动位置
            int progress = seekBar.getProgress();

            ChangeProgress(progress);
            //调用service调节播放进度
            //      mService.setProgress(max, dest);
        }

        //数值改变事件
        @Override
        public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
            // TODO Auto-generated method stub
        }

        //开始拖动事件
        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub
        }
    };

    public void ChangeProgress(int progress) {
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Infos.get(position).getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.PROGRESS_CHANGE);
        intent.putExtra("progress", progress);
        startService(intent);
    }

    //播放音乐
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void PlayMusic() {
        isPlaying = true;

        MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
        // 开始播放的时候为顺序播放
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Infos.get(position).getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.PLAY_MSG);
        MusicService.mp3Infos = mp3Infos;
        startService(intent);
    }

    //播放上一首音乐
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void PlayPreviousMusic() {
        position = position - 1;
        //   MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause,null));

        if (position < 0) {
            position = mp3Infos.size() - 1;
        }
        Mp3Info mp3Info = mp3Infos.get(position);
        MusicTitle.setText(mp3Info.getTitle());
        MusicArtist.setText(mp3Info.getArtist());
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Info.getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.PRIVIOUS_MSG);
        startService(intent);
        isPlaying = true;

        MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
    }

    //播放下一首音乐
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void PlayNextMusic() {
        //判断是否是随机播放，因为随机播放设置后，playmodeflag变为-1了
        if (playmodeflag == -1) {
            Random random = new Random();
            position = random.nextInt(mp3Infos.size());
        } else
            position = position + 1;
        if (position >= mp3Infos.size())
            position = 0;

        Mp3Info mp3Info = mp3Infos.get(position);
        MusicTitle.setText(mp3Info.getTitle());
        MusicArtist.setText(mp3Info.getArtist());
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Info.getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.NEXT_MSG);
        startService(intent);
        MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
        isPlaying = true;

    }


    public class MusicPlayerReceiver extends BroadcastReceiver {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //当前时间更新
            if (action.equals(MUSIC_CURRENT)) {
                currentTime = intent.getIntExtra("currentTime", -1);
                seekBar.setProgress(currentTime);
                MusicCurrentTime.setText(formatDuring(currentTime));


            } else if (action.equals(MUSIC_DURATION)) {
                //总时间更新
                duration = intent.getIntExtra("duration", -1);
                seekBar.setMax(duration);
                MusicTime.setText(formatDuring(duration));

            } else if (action.equals(UPDATE_ACTION)) {
                position = intent.getIntExtra("position", -1);
                String url = mp3Infos.get(position).getUrl();

                MusicTitle.setText(mp3Infos.get(position).getTitle());
                MusicArtist.setText(mp3Infos.get(position).getArtist());
                MusicTime.setText(formatDuring(mp3Infos.get(position).getDuration()));

            } else if (action.equals(PLAY_STATUE)) {
                boolean playstatue = intent.getBooleanExtra("playstatue", true);
                if (playstatue) {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
                    isPlaying = true;
                } else {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause, null));
                    isPlaying = false;
                }
            }

        }
    }

    public void onDestroy() {
        //销毁activity时，要记得销毁线程
        playStatus = false;
        super.onDestroy();
    }
}

//    //实现runnable接口，多线程实时更新进度条
//    public class UpdateProgress implements Runnable {
//        //通知UI更新的消息
//        //用来向UI线程传递进度的值
//        Bundle data = new Bundle();
//        //更新UI间隔时间
//        int milliseconds = 100;
//        double progress;
//        int currenttime;
//        int time;
//
//        @Override
//        public void run() {
//            // TODO Auto-generated method stub
//            //用来标识是否还在播放状态，用来控制线程退出
//            while (playStatus) {
//                try {
//                    //绑定成功才能开始更新UI
//                    if (mBound) {
//                        //发送消息，要求更新UI
//                        Message msg = new Message();
//                        data.clear();
//                        progress = mService.getProgress();
//                        time = mService.getTime();
//                        currenttime = mService.getCurrentTime();
//                        msg.what = 0;
//                        data.putDouble("progress", progress);
//                        data.putInt("currenttime", currenttime);
//                        data.putInt("time", time);
//                        msg.setData(data);
//                        mHandler.sendMessage(msg);
//                    }
//                    Thread.sleep(milliseconds);
//                    //Thread.currentThread().sleep(milliseconds);
//                    //每隔100ms更新一次UI
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
//    private ServiceConnection mConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName className, IBinder binder) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
//            MusicService.MyBinder myBinder = (MusicService.MyBinder) binder;
//            //获取service
//            mService = (MusicService) myBinder.getService();
//            //绑定成功
//            mBound = true;
//            //开启线程，更新UI
//            myThread.start();
//            MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay));
//            mService.play();
//            playorpauseflag = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            mBound = false;
//        }
//    };
//
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        //      getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }    //处理进度条更新
//    Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case 0:
//                    //从bundle中获取进度，是double类型，播放的百分比
//                    double progress = msg.getData().getDouble("progress");
//                    int time = msg.getData().getInt("time");
//                    int currenttime = msg.getData().getInt("currenttime");
//
//                    //根据播放百分比，计算seekbar的实际位置
//                    int max = seekBar.getMax();
//                    int position = (int) (max * progress);
//                    //设置seekbar的实际位置
//                    seekBar.setProgress(position);
//                    //减一的目的是为了让时间在最后一秒钟不再上涨
//                    MusicCurrentTime.setText(formatDuring(currenttime - 1));
//                    MusicTime.setText(formatDuring(time));
//                    break;
//                default:
//                    break;
//            }
//        }
//    };
    //定义一个新线程，用来发送消息，通知更新UI
    //    myThread = new Thread(new UpdateProgress());
    //绑定service;
    //     Intent serviceIntent = new Intent(MusicActivity.this, MusicService.class);

    //如果未绑定，则进行绑定,第三个参数是一个标志，它表明绑定中的操作．它一般应是BIND_AUTO_CREATE，这样就会在service不存在时创建一个
//        if (!mBound) {
//            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
//        }
//        MusicPlay.setOnClickListener(new View.OnClickListener() {
//
//            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//            @Override
//            public void onClick(View arg0) {
//                if (mBound && playorpauseflag) {
//                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause, null));
//                    mService.pause();
//                    playorpauseflag = false;
//                } else {
//                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
//                    mService.play();
//                    playorpauseflag = true;
//                }
//            }
//        });

