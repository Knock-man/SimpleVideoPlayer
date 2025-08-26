package com.example.simplevideoplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button btnPlay, btnPause, btnStop, btnSelect, btnOnline;
    private TextView tvStatus;

    private static final int PICK_VIDEO_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        videoView = findViewById(R.id.videoView);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnSelect = findViewById(R.id.btnSelect);
        btnOnline = findViewById(R.id.btnOnline);
        tvStatus = findViewById(R.id.tvStatus);
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

    private void selectLocalVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    private void playOnlineVideo() {
        // 这里可以替换为任何在线视频URL
        String videoUrl = "https://www.example.com/sample.mp4";
        // 实际使用时请替换为真实的视频URL

        // 使用一个示例视频（Big Buck Bunny）
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