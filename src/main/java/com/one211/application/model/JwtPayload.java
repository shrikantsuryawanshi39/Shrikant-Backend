package com.one211.application.model;

import java.util.Date;

public record JwtPayload(Long orgId, String role, String subject, Date expiration) {}
