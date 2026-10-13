package id.ruanghari.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        // Alarms are stored by the app. The main screen re-schedules upcoming alarms
        // when opened after a device restart; Android may restrict exact alarms until access is granted.
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.getSharedPreferences("ruanghari", Context.MODE_PRIVATE)
                .edit().putBoolean("needs_reschedule", true).apply();
        }
    }
}
