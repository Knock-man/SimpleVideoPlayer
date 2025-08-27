package com.example.simplevideoplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "VideoPlayer";
    private VideoView videoView;
    private Button btnPlay, btnPause, btnStop, btnSelect, btnOnline;
    private TextView tvStatus;
    private TextView tvGestureInfo;

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
    private boolean isFullscreen = false;

    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key pressed: " + keyCode);

        // 视频播放状态下的按键处理
        if (videoView != null && videoView.isPlaying()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    increaseVolume();//加大音量
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    decreaseVolume();//降低音量
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    seekBackward();//后退五秒
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    seekForward();//前进五秒
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    togglePlayPause();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (isFullscreen) {
                        toggleFullScreen();
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_MENU:
                    toggleControlsVisibility();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    increaseVolume();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    decreaseVolume();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    togglePlayPause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    videoView.stopPlayback();
                    updateStatus("已停止");
                    return true;
            }
        }

        // 布局文件已定义焦点顺序，这里仅处理特殊情况
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                performClickOnFocusedView();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (isFullscreen) {
                    toggleFullScreen();
                } else {
                    finish();
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        setupGestureControl();

        // 初始化音频管理器
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // 设置初始焦点
        btnPlay.requestFocus();
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
            videoView.requestFocus(); // 切换焦点到视频区域
        });

        // 暂停按钮
        btnPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                updateStatus("已暂停");
            }
            videoView.requestFocus();
        });

        // 停止按钮
        btnStop.setOnClickListener(v -> {
            videoView.stopPlayback();
            updateStatus("已停止");
            btnPlay.requestFocus(); // 停止后焦点回到播放按钮
        });

        // 选择本地视频
        btnSelect.setOnClickListener(v -> {
            selectLocalVideo();
            btnSelect.requestFocus();
        });

        // 播放在线视频
        btnOnline.setOnClickListener(v -> {
            playOnlineVideo();
            videoView.requestFocus();
        });

        // 视频区域点击
        videoView.setOnClickListener(v -> togglePlayPause());
    }

    // 其他方法保持不变...

    private void setupGestureControl() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                initialBrightness = getWindow().getAttributes().screenBrightness;
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float deltaX = e2.getX() - e1.getX();
                float deltaY = e2.getY() - e1.getY();

                if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > SWIPE_THRESHOLD) {
                    float screenWidth = getWindowManager().getDefaultDisplay().getWidth();
                    float touchX = e1.getX();

                    if (touchX < screenWidth / 2) {
                        adjustBrightness(deltaY);
                    } else {
                        adjustVolume(deltaY);
                    }
                    isAdjusting = true;
                    return true;
                } else if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                    adjustProgress(deltaX);
                    isAdjusting = true;
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                togglePlayPause();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                toggleFullScreen();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                toggleControlsVisibility();
            }
        });

        videoView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_UP && isAdjusting) {
                tvGestureInfo.postDelayed(() -> tvGestureInfo.setVisibility(View.GONE), 1000);
                isAdjusting = false;
            }
            return true;
        });
    }

    //播放和暂停
    private void togglePlayPause() {
        if (videoView.isPlaying()) {
            videoView.pause();
            updateStatus("已暂停");
        } else {
            if (videoView.getCurrentPosition() > 0) {
                videoView.start();
                updateStatus("继续播放");
            }
        }
    }

    private void increaseVolume() {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (currentVolume < maxVolume) {
            currentVolume++;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_SHOW_UI);
            int volumePercent = (int) (((float) currentVolume / maxVolume) * 100);
            showTempMessage("音量: " + volumePercent + "%");
        }
    }

    private void decreaseVolume() {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (currentVolume > 0) {
            currentVolume--;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_SHOW_UI);
            int volumePercent = (int) (((float) currentVolume / maxVolume) * 100);
            showTempMessage("音量: " + volumePercent + "%");
        }
    }

    //后退五秒
    private void seekBackward() {
        int currentPos = videoView.getCurrentPosition();
        int newPos = Math.max(0, currentPos - 5000);
        videoView.seekTo(newPos);
        showTempMessage("后退 5 秒");
    }

    //前进五秒
    private void seekForward() {
        int currentPos = videoView.getCurrentPosition();
        int duration = videoView.getDuration();
        int newPos = Math.min(duration, currentPos + 5000);
        videoView.seekTo(newPos);
        showTempMessage("前进 5 秒");
    }

    private void showTempMessage(String message) {
        tvGestureInfo.setText(message);
        tvGestureInfo.setVisibility(View.VISIBLE);
        tvGestureInfo.postDelayed(() -> tvGestureInfo.setVisibility(View.GONE), 1000);
    }

    private void adjustBrightness(float deltaY) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        float brightness = initialBrightness - (deltaY / getWindowManager().getDefaultDisplay().getHeight());
        brightness = Math.max(0.1f, Math.min(1.0f, brightness));
        layoutParams.screenBrightness = brightness;
        getWindow().setAttributes(layoutParams);

        int brightnessPercent = (int) (brightness * 100);
        tvGestureInfo.setText("亮度: " + brightnessPercent + "%");
        tvGestureInfo.setVisibility(View.VISIBLE);
    }

    private void adjustVolume(float deltaY) {
        int volume = initialVolume - (int) (deltaY / 10);
        volume = Math.max(0, Math.min(maxVolume, volume));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        currentVolume = volume;

        int volumePercent = (int) (((float) volume / maxVolume) * 100);
        tvGestureInfo.setText("音量: " + volumePercent + "%");
        tvGestureInfo.setVisibility(View.VISIBLE);
    }

    private void adjustProgress(float deltaX) {
        if (videoView.getDuration() <= 0) return;

        int currentPosition = videoView.getCurrentPosition();
        int duration = videoView.getDuration();
        int newPosition = currentPosition + (int) (deltaX * 10);
        newPosition = Math.max(0, Math.min(duration, newPosition));
        videoView.seekTo(newPosition);

        int progressPercent = (int) (((float) newPosition / duration) * 100);
        tvGestureInfo.setText("进度: " + progressPercent + "%");
        tvGestureInfo.setVisibility(View.VISIBLE);
    }

    private void toggleFullScreen() {
        if (isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isFullscreen = false;
            updateStatus("已退出全屏");
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isFullscreen = true;
            updateStatus("已进入全屏");
        }
    }

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
        String sampleUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        videoView.setVideoURI(Uri.parse(sampleUrl));
        videoView.start();
        updateStatus("正在加载在线视频...");

        videoView.setOnPreparedListener(mp -> {
            updateStatus("在线视频准备就绪，正在播放");
            mp.start();
        });

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

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullScreen();
        } else {
            super.onBackPressed();
        }
    }

    // 触发当前焦点视图的点击事件
    private void performClickOnFocusedView() {
        View current = getCurrentFocus();
        if (current != null) {
            current.performClick();
        }
    }
}
