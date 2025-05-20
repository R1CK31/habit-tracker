package com.example.habit.network;

import com.example.habit.network.dto.HabitDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface HabitApi {
    @GET("api/habits")
    Call<List<HabitDto>> listHabits();

    @POST("api/habits")
    Call<HabitDto> addHabit(@Body HabitDto habit);

    @DELETE("api/habits/{id}")
    Call<Void> deleteHabit(@Path("id") long id);

    @PUT("api/habits/{id}")
    Call<HabitDto> updateHabit(@Path("id") long id, @Body HabitDto habit);
}
