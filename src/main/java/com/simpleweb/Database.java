package com.simpleweb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    // Get a fresh connection to the DB
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
