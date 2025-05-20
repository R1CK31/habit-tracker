package com.example.habit.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habit.R;
import com.example.habit.network.dto.HabitDto;
import java.util.List;
import java.util.Locale;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.ViewHolder> {

    private final List<HabitDto> habits;
    private final Context context;
    private int lastContextPosition = -1;
    private final HabitInteractionListener listener;

    public interface HabitInteractionListener {
        void onHabitCompletionToggle(int position, HabitDto habit, boolean markComplete);
        void onHabitLongPress(int position, View view);
    }

    public HabitAdapter(Context ctx, List<HabitDto> list, HabitInteractionListener interactionListener) {
        context = ctx;
        habits = list;
        listener = interactionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HabitDto habit = habits.get(position);
        if (habit == null) return;

        holder.text.setText(habit.name);
        holder.streakText.setText(String.format(Locale.getDefault(), "%d", habit.currentStreak));
        holder.streakIcon.setVisibility(habit.currentStreak > 0 ? View.VISIBLE : View.INVISIBLE);

        holder.applyCompletionStyle(habit.completedToday);

        if (habit.targetDays != null && habit.targetDays > 0) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressText.setVisibility(View.VISIBLE);
            holder.progressBar.setMax(habit.targetDays);
            holder.progressBar.setProgress(habit.daysCompleted);
            holder.progressText.setText(String.format(Locale.getDefault(), "%d/%d", habit.daysCompleted, habit.targetDays));
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.progressText.setVisibility(View.GONE);
        }

        holder.completionIndicator.setOnClickListener(v -> {
            if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                boolean newCompletionState = !habits.get(holder.getAdapterPosition()).completedToday;
                listener.onHabitCompletionToggle(holder.getAdapterPosition(), habits.get(holder.getAdapterPosition()), newCompletionState);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                lastContextPosition = holder.getAdapterPosition();
                listener.onHabitLongPress(lastContextPosition, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return habits != null ? habits.size() : 0;
    }

    public int getLastContextPosition() {
        return lastContextPosition;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        TextView text;
        ImageView completionIndicator;
        TextView streakText;
        ImageView streakIcon;
        View itemRootLayout;
        ProgressBar progressBar;
        TextView progressText;

        ViewHolder(View item) {
            super(item);
            text = item.findViewById(R.id.habitText);
            completionIndicator = item.findViewById(R.id.habitCompletionIndicator);
            streakText = item.findViewById(R.id.habitStreakText);
            streakIcon = item.findViewById(R.id.habitStreakIcon);
            itemRootLayout = item.findViewById(R.id.itemHabitRootLayout);
            progressBar = item.findViewById(R.id.habitProgressBar);
            progressText = item.findViewById(R.id.habitProgressText);
            item.setOnCreateContextMenuListener(this);
        }

        void applyCompletionStyle(boolean completed) {
            if (completed) {
                completionIndicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_habit_circle_complete));
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                text.setAlpha(0.7f);
                streakIcon.setAlpha(0.7f);
                streakText.setAlpha(0.7f);
                progressBar.setAlpha(0.7f);
                progressText.setAlpha(0.7f);
                itemView.setAlpha(0.9f);
            } else {
                completionIndicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_habit_circle_incomplete));
                text.setPaintFlags(text.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                text.setAlpha(1.0f);
                streakIcon.setAlpha(1.0f);
                streakText.setAlpha(1.0f);
                progressBar.setAlpha(1.0f);
                progressText.setAlpha(1.0f);
                itemView.setAlpha(1.0f);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        }
    }
}