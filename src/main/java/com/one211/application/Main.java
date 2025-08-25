package com.one211.application;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

@SpringBootApplication
public class Main {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @PostConstruct
    public void printDatabaseInfo() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            String dbName = connection.getCatalog(); // or metaData.getDatabaseProductName()

            System.out.println("🔌 Connected to DB:");
            System.out.println("▶ URL: " + url);
            System.out.println("▶ Database Name: " + dbName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}