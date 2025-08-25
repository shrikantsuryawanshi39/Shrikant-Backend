package com.one211.application.security;

import com.one211.application.model.JwtPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtHelper {
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;
    private static final String SECRET = "0eP3wX7yT-p6eWZ9zSDoXDJdkEZwYGgVSk6Tz3Iz6S9jU8rK9Gm-fjsyGQJp7Jqzv3-8Vw8xltV9L7Adb2oC8A";

    public static final String CLAIM_USER_ID = "user_id";
    public static final String CLAIM_ORG_ID = "org_id";
    public static final String CLAIM_ROLE = "role";

    private final Key signingKey;
    private final JwtParser jwtParser;

    public JwtHelper() {
        byte[] decodedKey = Base64.getUrlDecoder().decode(SECRET);
        this.signingKey = Keys.hmacShaKeyFor(decodedKey);
        this.jwtParser = Jwts.parserBuilder().setSigningKey(signingKey).build();
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        if (userDetails instanceof CustomUserDetails customUserDetails) {
            claims.put(CLAIM_ORG_ID, customUserDetails.user().orgId());
            claims.put(CLAIM_ROLE, customUserDetails.user().role());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public JwtPayload getPayload(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        return new JwtPayload(
                claims.get(CLAIM_ORG_ID, Long.class),
                claims.get(CLAIM_ROLE, String.class),
                claims.getSubject(),
                claims.getExpiration()
        );
    }

    public Long getOrgId(String token) {
        return getPayload(token).orgId();
    }

    public String getRole(String token) {
        return getPayload(token).role();
    }

    public String getUsernameFromToken(String token) {
        return getPayload(token).subject();
    }

    public Date getExpirationDateFromToken(String token) {
        return getPayload(token).expiration();
    }

    private boolean isTokenExpired(JwtPayload payload) {
        return payload.expiration().before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        JwtPayload payload = getPayload(token);
        return payload.subject().equals(userDetails.getUsername()) && !isTokenExpired(payload);
    }
}
