package id.ruanghari.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "ruanghari_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String note = intent.getStringExtra("note");
        int id = intent.getIntExtra("reminderId", (int) System.currentTimeMillis());

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Pengingat kegiatan", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Pengingat jadwal dan alarm Ruang Hari");
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            manager.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title == null ? "Pengingat kegiatan" : title)
            .setContentText(note == null ? "Waktunya menjalankan kegiatanmu." : note)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(note == null ? "Waktunya menjalankan kegiatanmu." : note))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(content)
            .build();
        manager.notify(id, notification);
    }
}
