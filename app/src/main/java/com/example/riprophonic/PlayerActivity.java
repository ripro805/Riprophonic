package com.example.riprophonic;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.riprophonic.databinding.ActivityPlayerBinding;
import com.frolo.waveformseekbar.WaveformSeekBar;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jp.wasabeef.glide.transformations.BlurTransformation;
import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private Handler handler = new Handler();
    private List<Song> songList = new ArrayList<>();
    private List<Song> activeList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShuffle = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;
    private boolean isFavorite = false;
    private boolean isPlayerReady = false;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (MusicService.player != null && isPlayerReady) {
                long currentPosition = MusicService.player.getCurrentPosition();
                long duration = MusicService.player.getDuration();
                if (duration > 0) {
                    float progressPercent = ((float) currentPosition / duration);
                    binding.waveformSeekBar.setProgressInPercentage(progressPercent);
                    binding.textElapsed.setText(formatTime((int) (currentPosition / 1000)));
                    binding.textDuration.setText(formatTime((int) (duration / 1000)));
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent serviceIntent = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        songList = getIntent().getParcelableArrayListExtra("songList");
        currentIndex = getIntent().getIntExtra("position", 0);

        if (songList == null || songList.isEmpty()) {
            Toast.makeText(this, "No Songs Found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        activeList = new ArrayList<>(songList);
        binding.waveformSeekBar.setWaveform(createWaveForm(), true);
        initPlayerWithSong(currentIndex);
        setupControls();
        handler.post(updateRunnable);

        binding.backBtn.setOnClickListener(v -> finish());
        binding.favBtn.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            binding.favBtn.setImageResource(isFavorite ? R.drawable.ic_favorite_filled_24 : R.drawable.ic_favorite_24);
            Toast.makeText(PlayerActivity.this, isFavorite ? "Added to Favourite" : "Removed from Favourite", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupControls() {
        binding.buttonPlayPause.setOnClickListener(v -> {
            if (isPlayerReady) {
                togglePlayPause();
                updatePlayPauseButtonIcon();
            }
        });

        binding.buttonNext.setOnClickListener(v -> playNext());
        binding.buttonPrev.setOnClickListener(v -> playPrevious());
        binding.buttonShuffle.setOnClickListener(v -> toggleShuffle());
        binding.buttonRepeat.setOnClickListener(v -> toggleRepeat());

        binding.waveformSeekBar.setCallback(new WaveformSeekBar.Callback() {
            @Override
            public void onProgressChanged(WaveformSeekBar seekBar, float percent, boolean fromUser) {
                if (fromUser && MusicService.player != null && isPlayerReady) {
                    long duration = MusicService.player.getDuration();
                    long seekPos = (long) (percent * duration);
                    MusicService.player.seekTo(seekPos);
                    binding.textElapsed.setText(formatTime((int) (seekPos / 1000)));
                }
            }
            @Override public void onStartTrackingTouch(WaveformSeekBar seekBar) { handler.removeCallbacks(updateRunnable); }
            @Override public void onStopTrackingTouch(WaveformSeekBar seekBar) { handler.postDelayed(updateRunnable, 0); }
        });

        MusicService.player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) isPlayerReady = true;
                if (state == Player.STATE_ENDED) {
                    if (repeatMode == Player.REPEAT_MODE_ONE) {
                        MusicService.player.seekTo(0);
                        MusicService.player.play();
                    } else {
                        playNext();
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButtonIcon();
            }
        });
    }

    private void toggleRepeat() {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                repeatMode = Player.REPEAT_MODE_ALL;
                binding.buttonRepeat.setImageResource(R.drawable.ic_repeat_24);
                binding.buttonRepeat.setColorFilter(getResources().getColor(R.color.purple));
                binding.buttonRepeat.setAlpha(1f);
                break;
            case Player.REPEAT_MODE_ALL:
                repeatMode = Player.REPEAT_MODE_ONE;
                binding.buttonRepeat.setImageResource(R.drawable.ic_repeat_one_24);
                binding.buttonRepeat.setColorFilter(getResources().getColor(R.color.purple));
                binding.buttonRepeat.setAlpha(1f);
                break;
            case Player.REPEAT_MODE_ONE:
                repeatMode = Player.REPEAT_MODE_OFF;
                binding.buttonRepeat.setImageResource(R.drawable.ic_repeat_24);
                binding.buttonRepeat.setColorFilter(null);
                binding.buttonRepeat.setAlpha(0.3f);
                break;
        }
        MusicService.player.setRepeatMode(repeatMode);
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        if (isShuffle) {
            activeList = new ArrayList<>(songList);
            Collections.shuffle(activeList);
            binding.buttonShuffle.setColorFilter(getResources().getColor(R.color.purple));
        } else {
            activeList = new ArrayList<>(songList);
            binding.buttonShuffle.clearColorFilter();
        }
        currentIndex = 0;
        initPlayerWithSong(currentIndex);
    }

    private void playPrevious() {
        currentIndex = (currentIndex - 1 + activeList.size()) % activeList.size();
        initPlayerWithSong(currentIndex);
    }

    private void playNext() {
        currentIndex = (currentIndex + 1) % activeList.size();
        initPlayerWithSong(currentIndex);
    }

    private void togglePlayPause() {
        if (MusicService.player.isPlaying()) {
            MusicService.player.pause();
        } else {
            MusicService.player.play();
        }
    }

    private void initPlayerWithSong(int index) {
        Song song = activeList.get(index);
        MusicService.currentSong = song;
        MusicService.player.setRepeatMode(repeatMode);
        MusicService.player.clearMediaItems();

        Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId);

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(song.data)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(albumArtUri)
                        .build())
                .build();

        MusicService.player.addMediaItem(mediaItem);
        MusicService.player.prepare();
        MusicService.player.play();
        updateUI(song);
        updatePlayPauseButtonIcon();
    }

    private void updateUI(Song song) {
        binding.textTitle.setText(song.title != null ? song.title : "");
        binding.textArtist.setText(song.artist != null ? song.artist : "");
        setTitle(song.title);

        Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId);
        if (hasAlbumArt(albumArtUri)) {
            Glide.with(this).asBitmap().load(albumArtUri).circleCrop().placeholder(R.drawable.ic_music_note_24).into(binding.imageAlbumArtPlayer);
            Glide.with(this).asBitmap().load(albumArtUri).apply(bitmapTransform(new BlurTransformation(25, 3))).into(binding.bgAlbumart);
        } else {
            binding.imageAlbumArtPlayer.setImageResource(R.drawable.ic_music_note_24);
            binding.bgAlbumart.setImageResource(R.drawable.ic_music_note_24);
        }
    }

    private boolean hasAlbumArt(Uri albumArtUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(albumArtUri)) {
            return inputStream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private int[] createWaveForm() {
        Random random = new Random(System.currentTimeMillis());
        int[] values = new int[50];
        for (int i = 0; i < values.length; i++) values[i] = 5 + random.nextInt(50);
        return values;
    }

    private void updatePlayPauseButtonIcon() {
        if (MusicService.player != null && MusicService.player.isPlaying()) {
            binding.buttonPlayPause.setImageResource(R.drawable.ic_pause_24);
        } else {
            binding.buttonPlayPause.setImageResource(R.drawable.ic_play_arrow_24);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
}