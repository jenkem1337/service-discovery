package com.example.serviceregistryexample.application;

public record SaveUserDTO(
        String name,
        String secondName,
        String lastName,
        String email) {
}
