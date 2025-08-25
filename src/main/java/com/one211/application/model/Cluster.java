package com.one211.application.model;

import java.time.LocalDateTime;

public record Cluster(Long id, Long orgId, String name, String description, Boolean status, LocalDateTime createdAt) {}
