package com.example.habit.network.dto;

public class HabitDto {
    public Long id;
    public String name;
    public boolean completedToday;
    public String lastCompletedDate;
    public int currentStreak;
    public int longestStreak;
    public Integer targetDays;
    public int daysCompleted;

    public HabitDto() {}
}