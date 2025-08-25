package com.one211.application.config;

import com.one211.application.model.User;
import com.one211.application.model.UserOrg;
import com.one211.application.security.CustomUserDetails;
import com.one211.application.security.JwtAuthenticationEntryPoint;
import com.one211.application.security.JwtAuthenticationFilter;
import com.one211.application.security.JwtHelper;
import com.one211.application.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Configuration
@EnableTransactionManagement
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint point;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtHelper jwtHelper, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtHelper, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return email -> {
            User user = userService.getUserByEmail(email);
            if (user == null) {
                throw new UsernameNotFoundException("User not found: " + email);
            }
            return new CustomUserDetails(new UserOrg(
                    user.name(),
                    user.email(),
                    user.password(),
                    user.role(),
                    null,
                    null
            ));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(CORS -> CORS
                        .configurationSource(request -> {
                            var cors = new org.springframework.web.cors.CorsConfiguration();
                            cors.setAllowedOrigins(List.of("http://localhost:5173"));
                            cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                            cors.setAllowedHeaders(List.of("*"));
                            cors.setAllowCredentials(true);
                            return cors;
                        })
                )
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/signup").permitAll()
                        .requestMatchers("/api/login").permitAll()
                        .requestMatchers("/api/login/org/*").permitAll()
                        .requestMatchers("/api/org/*/user/*").permitAll()
                        .requestMatchers("/api/org/*/user").hasRole("ADMIN")
                        .requestMatchers("/api/org/*/cluster").hasRole("ADMIN")
                        .requestMatchers("/api/org/*/cluster/*").hasRole("ADMIN")
                        .requestMatchers("/api/orgs/*/cluster-assignments").hasRole("ADMIN")
                        .requestMatchers("/api/orgs/*/groups").permitAll()
                        .requestMatchers("/api/orgs/*/groups/*").permitAll()
                        .requestMatchers("/api/orgs/*/user/*").permitAll()
                        .requestMatchers("/api/orgs/*/groups").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(point))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
