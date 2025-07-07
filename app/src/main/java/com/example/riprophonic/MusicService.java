package com.example.riprophonic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.io.InputStream;

public class MusicService extends Service {

    public static Song currentSong;
    public static ExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionCompat mediaSession;

    private static final String CHANNEL_ID = "MUSIC_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        createNotificationChannel();

        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setActive(true);

        playerNotificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public CharSequence getCurrentContentTitle(Player player) {
                        return currentSong != null ? currentSong.title : "Unknown Title";
                    }

                    @Nullable
                    @Override
                    public CharSequence getCurrentContentText(Player player) {
                        return currentSong != null ? currentSong.artist : "Unknown Artist";
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(MusicService.this, PlayerActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        return PendingIntent.getActivity(MusicService.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        if (currentSong != null) {
                            Uri albumArtUri = Uri.parse("content://media/external/audio/albumart")
                                    .buildUpon().appendPath(String.valueOf(currentSong.albumId)).build();
                            try (InputStream inputStream = getContentResolver().openInputStream(albumArtUri)) {
                                if (inputStream != null) {
                                    return BitmapFactory.decodeStream(inputStream);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return BitmapFactory.decodeResource(getResources(), R.drawable.ic_music_note_24);
                    }
                })
                .build();

        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        playerNotificationManager.setPlayer(player);
        playerNotificationManager.setUsePlayPauseActions(true);
        playerNotificationManager.setUseNextAction(true);
        playerNotificationManager.setUsePreviousAction(true);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Service")
                .setContentText("Preparing Music...")
                .setSmallIcon(R.drawable.ic_music_note_24)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()))
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        if (mediaSession != null) mediaSession.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
