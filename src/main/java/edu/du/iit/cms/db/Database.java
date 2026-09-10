package edu.du.iit.cms.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
                migrateEnrollmentMarkLimits(connection);
                migrateAttendanceComponents(connection);
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

    private void migrateEnrollmentMarkLimits(Connection connection) throws SQLException {
        String tableSql = null;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type='table' AND name='enrollments'")) {
            if (result.next()) {
                tableSql = result.getString(1);
            }
        }
        if (tableSql == null || !tableSql.contains("ce_mark BETWEEN 0 AND 40")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE enrollments_new (
                        course_id INTEGER NOT NULL,
                        student_id INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED', 'COMPLETED', 'INCOMPLETE')),
                        final_exam_mark REAL CHECK (final_exam_mark BETWEEN 0 AND 60),
                        ce_mark REAL CHECK (ce_mark BETWEEN 0 AND 70),
                        total_mark REAL CHECK (total_mark BETWEEN 0 AND 100),
                        enrolled_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (course_id, student_id),
                        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
                        FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT
                    )
                    """);
            statement.execute("""
                    INSERT INTO enrollments_new(course_id,student_id,status,final_exam_mark,ce_mark,total_mark,enrolled_at)
                    SELECT course_id,student_id,status,final_exam_mark,ce_mark,total_mark,enrolled_at FROM enrollments
                    """);
            statement.execute("DROP TABLE enrollments");
            statement.execute("ALTER TABLE enrollments_new RENAME TO enrollments");
            statement.execute("CREATE INDEX idx_enrollments_student ON enrollments(student_id)");
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    private void migrateAttendanceComponents(Connection connection) throws SQLException {
        boolean hasComponentType = false;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(assessment_components)")) {
            while (result.next()) {
                if ("component_type".equals(result.getString("name"))) {
                    hasComponentType = true;
                    break;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            if (!hasComponentType) {
                statement.execute("ALTER TABLE assessment_components ADD COLUMN component_type TEXT NOT NULL DEFAULT 'MANUAL'");
            }
            statement.execute("""
                    UPDATE courses SET ce_status='DRAFT'
                    WHERE status='ACTIVE' AND NOT EXISTS (
                        SELECT 1 FROM assessment_components c
                        WHERE c.course_id=courses.id AND c.component_type='ATTENDANCE'
                    )
                    """);
            statement.execute("""
                    UPDATE assessment_components SET weight_percentage=weight_percentage * 0.85
                    WHERE course_id IN (
                        SELECT id FROM courses
                        WHERE status<>'FINISHED' AND NOT EXISTS (
                            SELECT 1 FROM assessment_components c
                            WHERE c.course_id=courses.id AND c.component_type='ATTENDANCE'
                        )
                    )
                    """);
            statement.execute("""
                    INSERT INTO assessment_components(course_id,title,weight_percentage,maximum_mark,component_type)
                    SELECT id,'Attendance',15,100,'ATTENDANCE' FROM courses
                    WHERE status<>'FINISHED' AND NOT EXISTS (
                        SELECT 1 FROM assessment_components c
                        WHERE c.course_id=courses.id AND c.component_type='ATTENDANCE'
                    )
                    """);
        }
    }
}
