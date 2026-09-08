package edu.du.iit.cms.repository;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.domain.Student;
import edu.du.iit.cms.domain.Teacher;
import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.domain.UserSearchResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UserRepository {
    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public Optional<CredentialRow> findCredentials(String username) {
        String sql = "SELECT id,username,password_hash,full_name,email,role,active FROM users WHERE username=?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                User user = readUser(result);
                return Optional.of(new CredentialRow(user, result.getString("password_hash")));
            }
        } catch (SQLException exception) {
            throw persistenceFailure("load credentials", exception);
        }
    }

    public long createStudent(String username, String passwordHash, String fullName, String email,
                              String rollNumber, String session, String bloodGroup) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId = insertUser(connection, username, passwordHash, fullName, email, Role.STUDENT);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO student_profiles(user_id,roll_number,academic_session,blood_group) VALUES(?,?,?,?)")) {
                    statement.setLong(1, userId);
                    statement.setString(2, rollNumber);
                    statement.setString(3, session);
                    statement.setString(4, bloodGroup);
                    statement.executeUpdate();
                }
                connection.commit();
                return userId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceFailure("create Student", exception);
        }
    }

    public long createTeacher(String username, String passwordHash, String fullName, String email,
                              String employeeId, String designation) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId = insertUser(connection, username, passwordHash, fullName, email, Role.TEACHER);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO teacher_profiles(user_id,employee_id,designation) VALUES(?,?,?)")) {
                    statement.setLong(1, userId);
                    statement.setString(2, employeeId);
                    statement.setString(3, designation);
                    statement.executeUpdate();
                }
                connection.commit();
                return userId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceFailure("create Teacher", exception);
        }
    }

    public List<UserSearchResult> search(String query) {
        String sql = """
                SELECT u.id,u.username,u.full_name,u.email,u.role,u.active,
                       COALESCE(s.roll_number,t.employee_id,'ADMIN') AS identifier,
                       CASE WHEN u.role='STUDENT'
                            THEN s.academic_session || ' | ' || s.blood_group
                            WHEN u.role='TEACHER' THEN t.designation
                            ELSE 'System account' END AS details
                FROM users u
                LEFT JOIN student_profiles s ON s.user_id=u.id
                LEFT JOIN teacher_profiles t ON t.user_id=u.id
                WHERE u.full_name LIKE ? COLLATE NOCASE
                   OR u.username LIKE ? COLLATE NOCASE
                   OR s.roll_number LIKE ? COLLATE NOCASE
                   OR s.blood_group LIKE ? COLLATE NOCASE
                   OR t.employee_id LIKE ? COLLATE NOCASE
                ORDER BY u.role,u.full_name
                """;
        String term = "%" + (query == null ? "" : query.trim()) + "%";
        List<UserSearchResult> users = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 5; index++) {
                statement.setString(index, term);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    users.add(new UserSearchResult(
                            result.getLong("id"), result.getString("username"),
                            result.getString("full_name"), result.getString("email"),
                            Role.valueOf(result.getString("role")), result.getString("identifier"),
                            result.getString("details"), result.getInt("active") == 1));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw persistenceFailure("search users", exception);
        }
    }

    public List<Student> findActiveStudents() {
        String sql = """
                SELECT u.id,u.full_name,s.roll_number,s.academic_session,s.blood_group
                FROM users u JOIN student_profiles s ON s.user_id=u.id
                WHERE u.active=1 ORDER BY s.roll_number
                """;
        List<Student> students = new ArrayList<>();
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                students.add(new Student(result.getLong("id"), result.getString("full_name"),
                        result.getString("roll_number"), result.getString("academic_session"),
                        result.getString("blood_group")));
            }
            return students;
        } catch (SQLException exception) {
            throw persistenceFailure("load Students", exception);
        }
    }

    public List<Teacher> findActiveTeachers() {
        String sql = """
                SELECT u.id,u.full_name,t.employee_id,t.designation
                FROM users u JOIN teacher_profiles t ON t.user_id=u.id
                WHERE u.active=1 ORDER BY u.full_name
                """;
        List<Teacher> teachers = new ArrayList<>();
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                teachers.add(new Teacher(result.getLong("id"), result.getString("full_name"),
                        result.getString("employee_id"), result.getString("designation")));
            }
            return teachers;
        } catch (SQLException exception) {
            throw persistenceFailure("load Teachers", exception);
        }
    }

    public void setActive(long userId, boolean active) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET active=? WHERE id=?")) {
            statement.setInt(1, active ? 1 : 0);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("change account status", exception);
        }
    }

    private long insertUser(Connection connection, String username, String passwordHash, String fullName,
                            String email, Role role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users(username,password_hash,full_name,email,role) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            statement.setString(4, email);
            statement.setString(5, role.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private User readUser(ResultSet result) throws SQLException {
        return new User(result.getLong("id"), result.getString("username"),
                result.getString("full_name"), result.getString("email"),
                Role.valueOf(result.getString("role")), result.getInt("active") == 1);
    }

    private IllegalStateException persistenceFailure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + ": " + exception.getMessage(), exception);
    }

    public record CredentialRow(User user, String passwordHash) {
    }
}
