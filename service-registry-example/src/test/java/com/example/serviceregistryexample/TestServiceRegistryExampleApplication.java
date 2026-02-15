package com.example.serviceregistryexample;

import org.springframework.boot.SpringApplication;

public class TestServiceRegistryExampleApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServiceRegistryExampleApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
