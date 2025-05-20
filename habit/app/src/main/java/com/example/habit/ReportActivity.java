package com.example.habit;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.habit.databinding.ActivityReportBinding;
import com.example.habit.network.ApiClient;
import com.example.habit.network.HabitApi;
import com.example.habit.network.dto.HabitDto;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportActivity extends AppCompatActivity {

    private ActivityReportBinding binding;
    private HabitApi habitApi;
    private SharedPreferences userPrefs;
    private static final String TAG = "ReportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: Activity started.");

        Toolbar toolbar = binding.toolbarReport;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        habitApi = ApiClient.getRetrofit(this).create(HabitApi.class);
        userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String username = userPrefs.getString("username", "User");
        binding.reportUserGreeting.setText(String.format(Locale.getDefault(), "Hello, %s!", username));

        fetchReportData();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchReportData() {
        Log.d(TAG, "fetchReportData: Attempting to fetch habits for report...");
        showLoading(true);

        habitApi.listHabits().enqueue(new Callback<List<HabitDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<HabitDto>> call, @NonNull Response<List<HabitDto>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "fetchReportData: Successfully fetched " + response.body().size() + " habits.");
                    processHabitData(response.body());
                    binding.reportErrorText.setVisibility(View.GONE);
                } else {
                    handleApiError("fetchReportData", response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HabitDto>> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "fetchReportData: Network error or parsing issue.", t);
                binding.reportErrorText.setText("Network Error: " + t.getMessage());
                binding.reportErrorText.setVisibility(View.VISIBLE);
                clearReportFields();
            }
        });
    }

    private void processHabitData(List<HabitDto> habits) {
        if (habits == null) {
            Log.w(TAG, "processHabitData: Received null habit list.");
            clearReportFields();
            binding.reportErrorText.setText("No habit data received.");
            binding.reportErrorText.setVisibility(View.VISIBLE);
            return;
        }

        int totalHabits = habits.size();
        long completedTodayCount = habits.stream()
                .filter(h -> h.completedToday)
                .count();
        int longestStreakOverall = habits.stream()
                .mapToInt(h -> h.longestStreak)
                .max()
                .orElse(0);

        OptionalDouble averageStreakOptional = habits.stream()
                .mapToInt(h -> h.currentStreak)
                .average();
        double averageStreak = averageStreakOptional.isPresent() ? averageStreakOptional.getAsDouble() : 0.0;


        Log.d(TAG, "Calculated Stats: Total=" + totalHabits + ", CompletedToday=" + completedTodayCount +
                ", LongestOverall=" + longestStreakOverall + ", AvgStreak=" + averageStreak);

        binding.reportTotalHabits.setText(String.format(Locale.getDefault(), "%d", totalHabits));
        binding.reportCompletedToday.setText(String.format(Locale.getDefault(), "%d", completedTodayCount));
        binding.reportLongestStreakOverall.setText(String.format(Locale.getDefault(), "%d days", longestStreakOverall));
        binding.reportAverageStreak.setText(String.format(Locale.US, "%.1f days", averageStreak));
    }

    private void showLoading(boolean isLoading) {
        binding.reportProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        View userGreetingCardParent = binding.reportUserGreeting.getParent() instanceof View ? (View) binding.reportUserGreeting.getParent() : null;
        View statsGridParent = binding.reportTotalHabits.getParent() instanceof View ? (View) ((View) binding.reportTotalHabits.getParent()).getParent() : null;

        if (isLoading) {
            if(userGreetingCardParent != null) userGreetingCardParent.setVisibility(View.INVISIBLE);
            if (statsGridParent != null) statsGridParent.setVisibility(View.INVISIBLE);
        } else {
            if(userGreetingCardParent != null) userGreetingCardParent.setVisibility(View.VISIBLE);
            if (statsGridParent != null) statsGridParent.setVisibility(View.VISIBLE);
        }
        binding.reportErrorText.setVisibility(View.GONE);
    }


    private void clearReportFields() {
        binding.reportTotalHabits.setText("-");
        binding.reportCompletedToday.setText("-");
        binding.reportLongestStreakOverall.setText("- days");
        binding.reportAverageStreak.setText("- days");
    }

    private <T> void handleApiError(String methodTag, Response<T> response) {
        String errorMsg = methodTag + " onResponse: Failed. Code: " + response.code();
        if (response.errorBody() != null) {
            try {
                errorMsg += ", Body: " + response.errorBody().string();
            } catch (IOException e) { /* ignore */ }
        }
        Log.e(TAG, errorMsg);
        String displayMessage = "Error loading report (Code: " + response.code() + ")";
        if (response.code() == 401 || response.code() == 403) {
            displayMessage = "Authentication error. Please re-login.";
        }
        binding.reportErrorText.setText(displayMessage);
        binding.reportErrorText.setVisibility(View.VISIBLE);
        clearReportFields();
    }
}