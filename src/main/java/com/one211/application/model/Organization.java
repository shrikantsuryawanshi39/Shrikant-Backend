package com.one211.application.model;
import java.time.LocalDateTime;

public record Organization (Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt){}
