package com.one211.application.model;

import java.time.LocalDateTime;

public record Group(Long id, String name, String description, LocalDateTime createdAt) {
    public static Group withId(Long id, Group original) {
        return new Group(id, original.name(), original.description(), original.createdAt);
    }
}