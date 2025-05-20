package com.example.habit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habit.databinding.ActivityRegisterBinding;
import com.example.habit.network.ApiClient;
import com.example.habit.network.AuthApi;
import com.example.habit.network.dto.RegisterRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthApi authApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authApi = ApiClient.getRetrofit(this).create(AuthApi.class);
        binding.registerButton.setOnClickListener(v -> attemptRegistration());
        binding.loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegistration() {
        binding.username.setError(null);
        binding.email.setError(null);
        binding.password.setError(null);
        String username = binding.username.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        boolean cancel = false;
        View focusView = null;
        if (TextUtils.isEmpty(password)) {
            binding.password.setError("Password is required");
            focusView = binding.password;
            cancel = true;
        } else if (password.length() < 4) {
            binding.password.setError("Password must be at least 4 characters");
            focusView = binding.password;
            cancel = true;
        }
        if (TextUtils.isEmpty(email)) {
            binding.email.setError("Email is required");
            focusView = binding.email;
            cancel = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError("Enter a valid email address");
            focusView = binding.email;
            cancel = true;
        }
        if (TextUtils.isEmpty(username)) {
            binding.username.setError("Username is required");
            focusView = binding.username;
            cancel = true;
        }
        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            performRegistration(username, email, password);
        }
    }

    private void performRegistration(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.username = username;
        registerRequest.email = email;
        registerRequest.password = password;
        authApi.register(registerRequest).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Registration failed. Please try again.";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = "Registration failed: " + response.code();
                        } catch (Exception e) {
                        }
                    } else {
                        errorMessage = "Registration failed: " + response.code();
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}