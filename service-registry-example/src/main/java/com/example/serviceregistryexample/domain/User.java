package com.example.serviceregistryexample.domain;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @PrePersist
    public void generateUUIDv7(){
        id = Generators.timeBasedEpochGenerator().generate();
    }
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "second_name", nullable = true)
    private String secondName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


}
