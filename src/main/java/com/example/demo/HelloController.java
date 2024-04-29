package com.example.demo;

import jakarta.servlet.ServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index(ServletRequest request) {
        return "Greetings from Spring Boot using " + request.getProtocol() + "!";
    }
    @GetMapping("/counter")
    public String counter(ServletRequest request) {
        return Instant.now().toString();
    }

}
