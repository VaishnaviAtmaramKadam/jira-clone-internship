package com.example.jira.controller;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class Healthcontoller {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${spring.mongodb.uri}")
    private String mongoUri;

    @GetMapping("/health")
    public String healthcheck() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));

            return "MongoDB connected Successfully";

        } catch (Exception e) {
            return "MongoDB connected failed: " + e.getMessage();
        }
    }
}