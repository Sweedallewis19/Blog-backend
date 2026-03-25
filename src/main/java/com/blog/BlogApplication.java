package com.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@SpringBootApplication
public class BlogApplication {

    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(BlogApplication.class, args);
    }

    private static void loadEnvFile() {
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            Properties props = new Properties();
            try {
                props.load(Files.newInputStream(envPath));
                props.forEach((k, v) -> System.setProperty(k.toString(), v.toString()));
            } catch (IOException e) {
                System.err.println("Failed to load .env file: " + e.getMessage());
            }
        }
    }
}
