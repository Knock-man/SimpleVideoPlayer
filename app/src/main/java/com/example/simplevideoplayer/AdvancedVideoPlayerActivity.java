package com.example.simplevideoplayer;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class AdvancedVideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private boolean isFullScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_video_player);

        videoView = findViewById(R.id.videoView);

        // 设置媒体控制器（包含进度条、播放/暂停按钮等）
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // 获取传递的视频URI
        String videoUrl = getIntent().getStringExtra("VIDEO_URL");
        if (videoUrl != null) {
            videoView.setVideoPath(videoUrl);
            videoView.start();
        }

        // 全屏按钮点击监听
        videoView.setOnClickListener(v -> toggleFullScreen());
    }

    private void toggleFullScreen() {
        if (isFullScreen) {
            // 退出全屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isFullScreen = false;
        } else {
            // 进入全屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isFullScreen = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!videoView.isPlaying()) {
            videoView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.stopPlayback();
    }
}