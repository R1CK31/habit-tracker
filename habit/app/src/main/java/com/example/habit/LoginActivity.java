package com.example.habit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habit.databinding.ActivityLoginBinding;
import com.example.habit.network.ApiClient;
import com.example.habit.network.AuthApi;
import com.example.habit.network.dto.LoginRequest;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthApi authApi;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authApi = ApiClient.getRetrofit(this).create(AuthApi.class);
        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        binding.username.setError(null);
        binding.password.setError(null);
        String u = binding.username.getText().toString().trim();
        String p = binding.password.getText().toString().trim();
        boolean cancel = false;
        View focusView = null;
        if (TextUtils.isEmpty(p)) {
            binding.password.setError("Required");
            focusView = binding.password;
            cancel = true;
        }
        if (TextUtils.isEmpty(u)) {
            binding.username.setError("Required");
            focusView = binding.username;
            cancel = true;
        }
        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            performLogin(u, p);
        }
    }

    private void performLogin(String username, String password) {
        authApi.login(new LoginRequest(username, password)).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> res) {
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    Log.d(TAG, "Login successful");
                    String jwt = res.body();
                    Log.d(TAG, "Received token: " + jwt);
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    prefs.edit().putString("jwt", jwt)
                            .putString("username", username)
                            .apply();
                    startActivity(new Intent(LoginActivity.this, HabitListActivity.class));
                    finish();
                } else {
                    String errorMessage = "Login failed. Check credentials.";
                    if (res.errorBody() != null) {
                        try {
                            String errorBodyString = res.errorBody().string();
                            Log.e(TAG, "Login failed - Code: " + res.code() + " Body: " + errorBodyString);
                            errorMessage = "Login failed: " + res.code() + " (See Logcat for details)";
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                            errorMessage = "Login failed: " + res.code() + " (Error reading details)";
                        }
                    } else if (!res.isSuccessful()) {
                        Log.e(TAG, "Login failed - Code: " + res.code() + " No error body.");
                        errorMessage = "Login failed: Server responded with code " + res.code();
                    } else {
                        Log.e(TAG, "Login failed - Successful response but empty/null body.");
                        errorMessage = "Login failed: Received empty response from server.";
                    }
                    if (res.code() == 401) {
                        errorMessage = "Invalid username or password.";
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e(TAG, "Network error during login", t);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}