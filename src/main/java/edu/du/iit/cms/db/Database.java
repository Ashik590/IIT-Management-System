package edu.du.iit.cms.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final Path databasePath;
    private final Path resourceDirectory;

    public Database(Path dataDirectory) {
        this.databasePath = dataDirectory.resolve("iit-course-management.db").toAbsolutePath();
        this.resourceDirectory = dataDirectory.resolve("resources").toAbsolutePath();
    }

    public void initialize() {
        try {
            Files.createDirectories(databasePath.getParent());
            Files.createDirectories(resourceDirectory);
            try (Connection connection = openConnection()) {
                executeSchema(connection);
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialize the database", exception);
        }
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public Path resourceDirectory() {
        return resourceDirectory;
    }

    public Path databasePath() {
        return databasePath;
    }

    private void executeSchema(Connection connection) throws IOException, SQLException {
        try (InputStream input = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (input == null) {
                throw new IOException("Missing /db/schema.sql");
            }
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String command : schema.split(";")) {
                String sql = command.trim();
                if (!sql.isEmpty()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }
}

