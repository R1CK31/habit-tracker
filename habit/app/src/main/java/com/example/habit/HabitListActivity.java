package com.example.habit;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.habit.adapter.HabitAdapter;
import com.example.habit.adapter.HabitAdapter.HabitInteractionListener;
import com.example.habit.databinding.ActivityHabitListBinding;
import com.example.habit.network.ApiClient;
import com.example.habit.network.HabitApi;
import com.example.habit.network.dto.HabitDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HabitListActivity extends AppCompatActivity implements HabitInteractionListener {

    private ActivityHabitListBinding binding;
    private List<HabitDto> habits = new ArrayList<>();
    private HabitAdapter adapter;
    private HabitApi habitApi;
    private int currentContextMenuPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHabitListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Habits");
        }

        habitApi = ApiClient.getRetrofit(this).create(HabitApi.class);
        adapter = new HabitAdapter(this, habits, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        registerForContextMenu(binding.recyclerView);

        fetchHabits();
    }

    @Override
    public void onHabitCompletionToggle(int position, HabitDto habit, boolean markComplete) {
        if (position < 0 || position >= habits.size()) {
            return;
        }
        HabitDto habitToUpdate = habits.get(position);
        habitToUpdate.completedToday = markComplete;
        habitToUpdate.targetDays = habits.get(position).targetDays;
        habitToUpdate.daysCompleted = habits.get(position).daysCompleted;
        updateHabitApiCall(position, habitToUpdate);
    }

    @Override
    public void onHabitLongPress(int position, View view) {
        if (position >= 0 && position < habits.size()) {
            currentContextMenuPosition = position;
            view.showContextMenu();
        }
    }

    private void showAddEditHabitDialog(final HabitDto habitToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingDp = 20;
        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int) (paddingDp * density);
        dialogLayout.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);

        final EditText inputName = new EditText(this);
        inputName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        inputName.setHint("Enter habit name");
        dialogLayout.addView(inputName);

        final EditText inputTargetDays = new EditText(this);
        inputTargetDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputTargetDays.setHint("Target days (optional)");
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        etParams.topMargin = (int) (8 * density);
        inputTargetDays.setLayoutParams(etParams);

        String dialogTitle;
        String positiveButtonText;

        if (habitToEdit != null) {
            dialogTitle = "Edit Habit";
            positiveButtonText = "Save";
            inputName.setText(habitToEdit.name);
            inputName.setSelection(inputName.getText().length());
            inputTargetDays.setVisibility(View.GONE);
        } else {
            dialogTitle = "Add New Habit";
            positiveButtonText = "Add";
            dialogLayout.addView(inputTargetDays);
        }

        builder.setTitle(dialogTitle);
        builder.setView(dialogLayout);

        builder.setPositiveButton(positiveButtonText, (dialog, which) -> {
            String habitName = inputName.getText().toString().trim();
            if (TextUtils.isEmpty(habitName)) {
                Toast.makeText(HabitListActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Integer targetDays = null;
            if (inputTargetDays.getVisibility() == View.VISIBLE && !TextUtils.isEmpty(inputTargetDays.getText().toString())) {
                try {
                    targetDays = Integer.parseInt(inputTargetDays.getText().toString().trim());
                    if (targetDays <= 0) targetDays = null;
                } catch (NumberFormatException e) {
                    Toast.makeText(HabitListActivity.this, "Invalid number for target days", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (habitToEdit != null) {
                if (!habitName.equals(habitToEdit.name)) {
                    habitToEdit.name = habitName;
                    int currentPosition = habits.indexOf(habitToEdit);
                    if(currentPosition != -1) {
                        updateHabitApiCall(currentPosition, habitToEdit);
                    } else {
                        fetchHabits();
                    }
                }
            } else {
                HabitDto newHabit = new HabitDto();
                newHabit.name = habitName;
                newHabit.targetDays = targetDays;
                addHabitApiCall(newHabit);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void fetchHabits() {
        habitApi.listHabits().enqueue(new Callback<List<HabitDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<HabitDto>> call, @NonNull Response<List<HabitDto>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    habits.clear();
                    habits.addAll(res.body());
                    adapter.notifyDataSetChanged();
                } else {
                    handleApiError("fetchHabits", res);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<HabitDto>> call, @NonNull Throwable t) {
                Toast.makeText(HabitListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addHabitApiCall(HabitDto habitToAdd) {
        habitApi.addHabit(habitToAdd).enqueue(new Callback<HabitDto>() {
            @Override
            public void onResponse(@NonNull Call<HabitDto> call, @NonNull Response<HabitDto> r) {
                if (r.isSuccessful() && r.body() != null) {
                    habits.add(r.body());
                    adapter.notifyItemInserted(habits.size() - 1);
                    binding.recyclerView.scrollToPosition(habits.size() - 1);
                } else {
                    handleApiError("addHabitApiCall", r);
                }
            }
            @Override
            public void onFailure(@NonNull Call<HabitDto> call, @NonNull Throwable t) {
                Toast.makeText(HabitListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHabitApiCall(final int position, HabitDto habitToUpdate) {
        if (habitToUpdate == null || habitToUpdate.id == null) return;
        habitApi.updateHabit(habitToUpdate.id, habitToUpdate).enqueue(new Callback<HabitDto>() {
            @Override
            public void onResponse(@NonNull Call<HabitDto> call, @NonNull Response<HabitDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (position >= 0 && position < habits.size()) {
                        habits.set(position, response.body());
                        adapter.notifyItemChanged(position);
                    } else { fetchHabits(); }
                } else {
                    handleApiError("updateHabitApiCall", response);
                    if (position >= 0 && position < habits.size()) {
                        adapter.notifyItemChanged(position);
                    } else { fetchHabits(); }
                }
            }
            @Override
            public void onFailure(@NonNull Call<HabitDto> call, @NonNull Throwable t) {
                Toast.makeText(HabitListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (position >= 0 && position < habits.size()) {
                    adapter.notifyItemChanged(position);
                } else { fetchHabits(); }
            }
        });
    }

    private void deleteHabitApiCall(long id, final int position) {
        habitApi.deleteHabit(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> r) {
                if (r.isSuccessful()) {
                    if (position >= 0 && position < habits.size() && habits.get(position).id != null && habits.get(position).id == id) {
                        habits.remove(position);
                        adapter.notifyItemRemoved(position);
                    } else { fetchHabits(); }
                } else {
                    handleApiError("deleteHabitApiCall", r);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(HabitListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private <T> void handleApiError(String methodTag, Response<T> response) {
        String errorMsg = methodTag + " failed. Code: " + response.code();
        if (response.errorBody() != null) {
            try { errorMsg += ", Body: " + response.errorBody().string(); }
            catch (IOException e) { System.out.println("Error reading error body" + e); }
        }
        String toastMsg = (response.code() == 401 || response.code() == 403) ? "Authentication error." : "API Error: " + response.code();
        Toast.makeText(HabitListActivity.this, toastMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, R.id.action_add_habit, Menu.NONE, "Add Habit");
        menu.add(Menu.NONE, R.id.action_refresh, Menu.NONE, "Refresh");
        menu.add(Menu.NONE, R.id.action_profile, Menu.NONE, "Profile");
        menu.add(Menu.NONE, R.id.action_logout, Menu.NONE, "Logout");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_habit) {
            showAddEditHabitDialog(null);
            return true;
        } else if (id == R.id.action_refresh) {
            fetchHabits();
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().remove("jwt").remove("username").apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, R.id.context_edit, Menu.NONE, "Edit");
        menu.add(Menu.NONE, R.id.context_delete, Menu.NONE, "Delete");
        if (currentContextMenuPosition >= 0 && currentContextMenuPosition < habits.size()) {
            menu.setHeaderTitle(habits.get(currentContextMenuPosition).name);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = currentContextMenuPosition;
        currentContextMenuPosition = -1;
        if (position < 0 || position >= habits.size()) {
            return super.onContextItemSelected(item);
        }
        HabitDto selectedHabit = habits.get(position);
        int itemId = item.getItemId();

        if (itemId == R.id.context_edit) {
            showAddEditHabitDialog(selectedHabit);
            return true;
        } else if (itemId == R.id.context_delete) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Habit")
                    .setMessage("Are you sure you want to delete '" + selectedHabit.name + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (selectedHabit.id != null) deleteHabitApiCall(selectedHabit.id, position);
                        else Toast.makeText(this, "Error: Cannot delete habit.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }
}