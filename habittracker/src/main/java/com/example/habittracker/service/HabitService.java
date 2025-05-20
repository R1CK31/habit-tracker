package com.example.habittracker.service;

import com.example.habittracker.model.Habit;
import com.example.habittracker.model.User;
import com.example.habittracker.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HabitService {

    @Autowired
    private HabitRepository habitRepository;

    @Transactional
    public Habit addHabit(Habit habit) {
        if (habit.getUser() == null) {
            throw new IllegalArgumentException("Habit must be associated with a user.");
        }
        habit.setCompletedToday(false);
        habit.setCurrentStreak(0);
        habit.setLongestStreak(0);
        habit.setLastCompletedDate(null);
        habit.setDaysCompleted(0);
        if (habit.getTargetDays() != null && habit.getTargetDays() <= 0) {
            habit.setTargetDays(null);
        }
        return habitRepository.save(habit);
    }

    public List<Habit> getAllHabitsForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        List<Habit> habits = habitRepository.findAllByUser(user);
        LocalDate today = LocalDate.now();

        return habits.stream().map(habit -> {
            if (habit.getLastCompletedDate() != null && habit.getLastCompletedDate().isEqual(today)) {
                habit.setCompletedToday(true);
            } else if (habit.getLastCompletedDate() != null && habit.getLastCompletedDate().isBefore(today.minusDays(1))) {
                habit.setCompletedToday(false);
            } else {
                habit.setCompletedToday(false);
            }
            return habit;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Habit updateHabit(Long id, Habit updatedHabitData, User currentUser)
            throws NoSuchElementException, SecurityException {

        Habit existingHabit = habitRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Habit not found with id: " + id));

        if (existingHabit.getUser() == null || !Objects.equals(existingHabit.getUser().getId(), currentUser.getId())) {
            throw new SecurityException("User (ID: " + currentUser.getId() + ") does not have permission to update habit (ID: " + id + ")");
        }

        boolean nameChanged = updatedHabitData.getName() != null && !updatedHabitData.getName().equals(existingHabit.getName());
        LocalDate today = LocalDate.now();
        boolean wasCompletedTodayBasedOnDate = existingHabit.getLastCompletedDate() != null && existingHabit.getLastCompletedDate().isEqual(today);
        boolean completionRequested = updatedHabitData.isCompletedToday();

        if (nameChanged) {
            existingHabit.setName(updatedHabitData.getName());
        }

        if (updatedHabitData.getTargetDays() != null) {
            Integer newTargetDays = updatedHabitData.getTargetDays();
            if (newTargetDays <= 0) {
                newTargetDays = null;
            }
            if (!Objects.equals(existingHabit.getTargetDays(), newTargetDays)) {
                existingHabit.setTargetDays(newTargetDays);
            }
        }

        if (completionRequested && !wasCompletedTodayBasedOnDate) {
            LocalDate lastCompleted = existingHabit.getLastCompletedDate();
            int oldStreak = existingHabit.getCurrentStreak();

            if (lastCompleted == null) {
                existingHabit.setCurrentStreak(1);
            } else if (lastCompleted.isEqual(today.minusDays(1))) {
                existingHabit.setCurrentStreak(oldStreak + 1);
            } else if (lastCompleted.isBefore(today.minusDays(1))) {
                existingHabit.setCurrentStreak(1);
            }

            if (existingHabit.getCurrentStreak() > existingHabit.getLongestStreak()) {
                existingHabit.setLongestStreak(existingHabit.getCurrentStreak());
            }
            existingHabit.setLastCompletedDate(today);
            existingHabit.setCompletedToday(true);

            if (existingHabit.getTargetDays() != null) {
                if (existingHabit.getDaysCompleted() < existingHabit.getTargetDays()) {
                    existingHabit.setDaysCompleted(existingHabit.getDaysCompleted() + 1);
                }
            }

        } else if (!completionRequested && wasCompletedTodayBasedOnDate) {
            existingHabit.setCompletedToday(false);
            if (existingHabit.getLastCompletedDate() != null && existingHabit.getLastCompletedDate().isEqual(today)) {
                if (existingHabit.getDaysCompleted() > 0) {
                    existingHabit.setDaysCompleted(existingHabit.getDaysCompleted() - 1);
                }
                existingHabit.setLastCompletedDate(null);
                existingHabit.setCurrentStreak(0);
            }
        } else {
            existingHabit.setCompletedToday(updatedHabitData.isCompletedToday());
        }

        return habitRepository.save(existingHabit);
    }

    @Transactional
    public void deleteHabit(Long id, User currentUser)
            throws NoSuchElementException, SecurityException {

        Habit habitToDelete = habitRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Habit not found with id: " + id));

        if (habitToDelete.getUser() == null || !Objects.equals(habitToDelete.getUser().getId(), currentUser.getId())) {
            throw new SecurityException("User (ID: " + currentUser.getId() + ") does not have permission to delete habit (ID: " + id + ")");
        }

        habitRepository.delete(habitToDelete);
    }
}