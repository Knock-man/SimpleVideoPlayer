package com.example.simplevideoplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button btnPlay, btnPause, btnStop, btnSelect, btnOnline;
    private TextView tvStatus;
    private TextView tvGestureInfo; // 显示手势操作信息

    private GestureDetector gestureDetector;
    private AudioManager audioManager;

    private static final int PICK_VIDEO_REQUEST = 1;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private boolean isAdjusting = false;
    private float initialBrightness;
    private int initialVolume;
    private int maxVolume;
    private int currentVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        setupGestureControl();

        // 初始化音频管理器
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    private void initViews() {
        videoView = findViewById(R.id.videoView);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnSelect = findViewById(R.id.btnSelect);
        btnOnline = findViewById(R.id.btnOnline);
        tvStatus = findViewById(R.id.tvStatus);
        tvGestureInfo = findViewById(R.id.tvGestureInfo);
    }

    private void setupClickListeners() {
        // 播放按钮
        btnPlay.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.resume();
            } else {
                videoView.start();
            }
            updateStatus("正在播放");
        });

        // 暂停按钮
        btnPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                updateStatus("已暂停");
            }
        });

        // 停止按钮
        btnStop.setOnClickListener(v -> {
            videoView.stopPlayback();
            updateStatus("已停止");
        });

        // 选择本地视频
        btnSelect.setOnClickListener(v -> selectLocalVideo());

        // 播放在线视频
        btnOnline.setOnClickListener(v -> playOnlineVideo());
    }

    private void setupGestureControl() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // 记录初始值
                initialBrightness = getWindow().getAttributes().screenBrightness;
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float deltaX = e2.getX() - e1.getX();
                float deltaY = e2.getY() - e1.getY();

                // 判断滑动方向和距离
                if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > SWIPE_THRESHOLD) {
                    // 垂直滑动 - 调节音量或亮度
                    float screenWidth = getWindowManager().getDefaultDisplay().getWidth();
                    float touchX = e1.getX();

                    if (touchX < screenWidth / 2) {
                        // 屏幕左侧 - 调节亮度
                        adjustBrightness(deltaY);
                    } else {
                        // 屏幕右侧 - 调节音量
                        adjustVolume(deltaY);
                    }
                    isAdjusting = true;
                    return true;
                } else if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                    // 水平滑动 - 调节进度
                    adjustProgress(deltaX);
                    isAdjusting = true;
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // 单击切换播放/暂停状态
                if (videoView.isPlaying()) {
                    videoView.pause();
                    updateStatus("已暂停");
                } else {
                    videoView.start();
                    updateStatus("正在播放");
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击切换全屏
                toggleFullScreen();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // 长按显示/隐藏控制按钮
                toggleControlsVisibility();
            }
        });

        // 设置VideoView的触摸监听
        videoView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);

            // 滑动结束时隐藏提示信息
            if (event.getAction() == MotionEvent.ACTION_UP && isAdjusting) {
                tvGestureInfo.setVisibility(View.GONE);
                isAdjusting = false;
            }
            return true;
        });
    }

    // 调节亮度
    private void adjustBrightness(float deltaY) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        float brightness = initialBrightness - (deltaY / getWindowManager().getDefaultDisplay().getHeight());

        // 亮度范围在0.1到1.0之间
        brightness = Math.max(0.1f, Math.min(1.0f, brightness));

        layoutParams.screenBrightness = brightness;
        getWindow().setAttributes(layoutParams);

        // 显示亮度信息
        int brightnessPercent = (int) (brightness * 100);
        tvGestureInfo.setText("亮度: " + brightnessPercent + "%");
        tvGestureInfo.setVisibility(View.VISIBLE);
    }

    // 调节音量
    private void adjustVolume(float deltaY) {
        int volume = initialVolume - (int) (deltaY / 10);

        // 音量范围在0到最大音量之间
        volume = Math.max(0, Math.min(maxVolume, volume));

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        currentVolume = volume;

        // 显示音量信息
        int volumePercent = (int) (((float) volume / maxVolume) * 100);
        tvGestureInfo.setText("音量: " + volumePercent + "%");
        tvGestureInfo.setVisibility(View.VISIBLE);
    }
    // 调节进度
    private void adjustProgress(float deltaX) {
        if (videoView.getDuration() <= 0) return;

        int currentPosition = videoView.getCurrentPosition();
        int duration = videoView.getDuration();
        int newPosition = currentPosition + (int) (deltaX * 10);

        // 进度范围在0到视频总时长之间
        newPosition = Math.max(0, Math.min(duration, newPosition));

        videoView.seekTo(newPosition);

        // 显示进度信息
        int progressPercent = (int) (((float) newPosition / duration) * 100);
        tvGestureInfo.setText("进度: " + progressPercent + "%");
        tvGestureInfo.setVisibility(View.VISIBLE);
    }

    // 切换全屏
    private void toggleFullScreen() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    // 切换控制按钮可见性
    private void toggleControlsVisibility() {
        int visibility = (btnPlay.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
        btnPlay.setVisibility(visibility);
        btnPause.setVisibility(visibility);
        btnStop.setVisibility(visibility);
        btnSelect.setVisibility(visibility);
        btnOnline.setVisibility(visibility);
        tvStatus.setVisibility(visibility);
    }

    private void selectLocalVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    private void playOnlineVideo() {
        // 使用示例视频（Big Buck Bunny）
        String sampleUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

        videoView.setVideoURI(Uri.parse(sampleUrl));
        videoView.start();
        updateStatus("正在加载在线视频...");

        // 设置准备完成监听
        videoView.setOnPreparedListener(mp -> {
            updateStatus("在线视频准备就绪，正在播放");
            mp.start();
        });

        // 设置错误监听
        videoView.setOnErrorListener((mp, what, extra) -> {
            updateStatus("播放错误: " + what);
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            playVideoFromUri(videoUri);
        }
    }

    private void playVideoFromUri(Uri videoUri) {
        try {
            videoView.setVideoURI(videoUri);
            updateStatus("视频加载中...");

            videoView.setOnPreparedListener(mp -> {
                updateStatus("视频准备就绪，点击播放");
                // 获取视频信息
                int duration = mp.getDuration();
                tvStatus.setText(String.format("视频准备就绪 (时长: %d秒)", duration / 1000));
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                updateStatus("播放错误，请选择其他视频");
                return true;
            });

        } catch (Exception e) {
            updateStatus("播放失败: " + e.getMessage());
        }
    }

    private void updateStatus(String message) {
        tvStatus.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
