package com.one211.application.model;

public record ClusterAssignmentRequest(
        String sourceType,
        String sourceName,
        String name,
        String action
) {
    public ClusterAssignmentRequest(String sourceType, String sourceName) {
        this(sourceType, sourceName, null, null);
    }
}