package com.example.serviceregistryexample.application;

import com.example.serviceregistryexample.domain.User;
import com.example.serviceregistryexample.infrastructure.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping()
    public ResponseEntity<List<User>> allUsers(){
        var users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> saveUser(@RequestBody SaveUserDTO saveUserDTO){
        var user = new User();
        user.setEmail(saveUserDTO.email());
        user.setName(saveUserDTO.name());
        user.setSecondName(saveUserDTO.secondName());
        user.setLastName(saveUserDTO.lastName());

        var savedUser = userRepository.saveAndFlush(user);
        return ResponseEntity.ok(savedUser);
    }

}
