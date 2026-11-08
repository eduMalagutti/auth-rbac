package com.esmalagutti.authrbac;

import org.springframework.boot.SpringApplication;

public class TestAuthRbacApplication {

    public static void main(String[] args) {
        SpringApplication.from(AuthRbacApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
