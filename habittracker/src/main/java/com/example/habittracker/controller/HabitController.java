package com.example.habittracker.controller;

import com.example.habittracker.model.Habit;
import com.example.habittracker.model.User;
import com.example.habittracker.repository.UserRepository;
import com.example.habittracker.service.HabitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    @Autowired
    private HabitService habitService;

    @Autowired
    private UserRepository userRepo;

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found in database: " + userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<Habit>> listHabits(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getAuthenticatedUser(userDetails);
        List<Habit> habits = habitService.getAllHabitsForUser(currentUser);
        return ResponseEntity.ok(habits);
    }

    @PostMapping
    public ResponseEntity<Habit> addHabit(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody Habit habit) {
        User currentUser = getAuthenticatedUser(userDetails);
        habit.setUser(currentUser);
        try {
            Habit savedHabit = habitService.addHabit(habit);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedHabit);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error adding habit: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Habit> updateHabit(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody Habit updatedHabitData) {
        User currentUser = getAuthenticatedUser(userDetails);
        try {
            Habit savedHabit = habitService.updateHabit(id, updatedHabitData, currentUser);
            return ResponseEntity.ok(savedHabit);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found with id: " + id, e);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to update this habit", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data for habit update: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating habit", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getAuthenticatedUser(userDetails);
        try {
            habitService.deleteHabit(id, currentUser);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found with id: " + id, e);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to delete this habit", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting habit", e);
        }
    }
}