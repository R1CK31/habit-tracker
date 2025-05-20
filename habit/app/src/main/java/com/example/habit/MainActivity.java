package com.example.habit;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String REMINDER_WORK_TAG = "habitReminderWork_OneTime";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    scheduleReminderWorker();
                } else {
                    scheduleReminderWorker();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askNotificationPermissionAndScheduleWork();
    }

    private void askNotificationPermissionAndScheduleWork() {
        boolean permissionFlowTriggered = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                scheduleReminderWorker();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(this, "Notification permission is needed for reminders.", Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                permissionFlowTriggered = true;
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                permissionFlowTriggered = true;
            }
        } else {
            scheduleReminderWorker();
        }

        if (!permissionFlowTriggered) {
            navigateToNextScreen();
        }
        navigateToNextScreen();
    }

    private void navigateToNextScreen() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        Intent intent;
        if (token != null && !token.isEmpty()) {
            intent = new Intent(MainActivity.this, HabitListActivity.class);
        } else {
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void scheduleReminderWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest reminderRequest =
                new OneTimeWorkRequest.Builder(HabitReminderWorker.class)
                        .setConstraints(constraints)
                        .addTag(REMINDER_WORK_TAG)
                        .build();
        WorkManager.getInstance(this).enqueue(reminderRequest);
    }
}