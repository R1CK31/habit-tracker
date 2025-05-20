package com.example.habit.network;

import com.example.habit.network.dto.LoginRequest;
import com.example.habit.network.dto.RegisterRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest body);

    @POST("api/auth/login")
    Call<String> login(@Body LoginRequest body);

}
