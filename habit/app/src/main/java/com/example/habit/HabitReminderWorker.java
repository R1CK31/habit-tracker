package com.example.habit;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.habit.network.ApiClient;
import com.example.habit.network.HabitApi;
import com.example.habit.network.dto.HabitDto;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Response;

public class HabitReminderWorker extends Worker {

    private static final String CHANNEL_ID = "habit_reminder_channel";
    private static final int NOTIFICATION_ID = 101;

    public HabitReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        createNotificationChannel(context);
        HabitApi habitApi = ApiClient.getRetrofit(context).create(HabitApi.class);
        try {
            Response<List<HabitDto>> response = habitApi.listHabits().execute();
            if (response.isSuccessful() && response.body() != null) {
                List<HabitDto> habits = response.body();
                List<HabitDto> incompleteHabits = habits.stream()
                        .filter(habit -> !habit.completedToday)
                        .collect(Collectors.toList());
                if (!incompleteHabits.isEmpty()) {
                    sendReminderNotification(context, incompleteHabits);
                }
                return Result.success();
            } else {
                if (response.code() >= 500) return Result.retry();
                return Result.failure();
            }
        } catch (IOException e) {
            return Result.retry();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private void sendReminderNotification(Context context, List<HabitDto> incompleteHabits) {
        Intent intent = new Intent(context, HabitListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = "Habit Reminder";
        String contentText;
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        int count = incompleteHabits.size();
        if (count == 1) {
            contentText = "Don't forget: " + incompleteHabits.get(0).name;
            inboxStyle.addLine(incompleteHabits.get(0).name);
        } else {
            contentText = count + " habits remaining today!";
            int maxLines = 5;
            for (int i = 0; i < Math.min(count, maxLines); i++) {
                inboxStyle.addLine("• " + incompleteHabits.get(i).name);
            }
            if (count > maxLines) {
                inboxStyle.setSummaryText("+" + (count - maxLines) + " more");
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(inboxStyle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Habit Reminders";
            String description = "Daily reminders for incomplete habits";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}