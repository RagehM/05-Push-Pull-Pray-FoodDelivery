package com.team05.fooddelivery.user.dto;

public record RegisterRequest(String name, String email, String password, String phone) {
}