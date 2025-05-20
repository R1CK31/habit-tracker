package com.example.habit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText nameField, emailField;
    private Button saveButton, reportsButton;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", null);

        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(this, "Error: User session not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        nameField = findViewById(R.id.profileName);
        emailField = findViewById(R.id.profileEmail);
        saveButton = findViewById(R.id.saveProfileButton);
        reportsButton = findViewById(R.id.viewReportsButton);

        nameField.setText(prefs.getString(currentUsername + "_name", ""));
        emailField.setText(prefs.getString(currentUsername + "_email", ""));

        saveButton.setOnClickListener(v -> {
            String newName = nameField.getText().toString();
            String newEmail = emailField.getText().toString();

            prefs.edit()
                    .putString(currentUsername + "_name", newName)
                    .putString(currentUsername + "_email", newEmail)
                    .apply();
            Toast.makeText(ProfileActivity.this, "Profile Saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        reportsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ReportActivity.class);
            startActivity(intent);
        });
    }
}