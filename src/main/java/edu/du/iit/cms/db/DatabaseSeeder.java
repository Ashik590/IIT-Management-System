package edu.du.iit.cms.db;

import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseSeeder {
    private final Database database;
    private final PasswordHasher passwordHasher;

    public DatabaseSeeder(Database database, PasswordHasher passwordHasher) {
        this.database = database;
        this.passwordHasher = passwordHasher;
    }

    public void seedIfEmpty() {
        try (Connection connection = database.openConnection()) {
            if (hasUsers(connection)) {
                return;
            }
            connection.setAutoCommit(false);
            try {
                long admin = insertUser(connection, "admin", "admin123", "System Administrator",
                        "admin@iit.du.ac.bd", Role.ADMIN);
                long teacherOne = insertUser(connection, "teacher1", "teacher123", "Dr. Nusrat Rahman",
                        "nusrat@iit.du.ac.bd", Role.TEACHER);
                long teacherTwo = insertUser(connection, "teacher2", "teacher123", "Md. Tanvir Ahmed",
                        "tanvir@iit.du.ac.bd", Role.TEACHER);
                long studentOne = insertUser(connection, "student1", "student123", "Afiya Islam",
                        "afiya@iit.du.ac.bd", Role.STUDENT);
                long studentTwo = insertUser(connection, "student2", "student123", "Rafi Hasan",
                        "rafi@iit.du.ac.bd", Role.STUDENT);
                long studentThree = insertUser(connection, "student3", "student123", "Nabil Karim",
                        "nabil@iit.du.ac.bd", Role.STUDENT);

                insertTeacherProfile(connection, teacherOne, "T-101", "Associate Professor");
                insertTeacherProfile(connection, teacherTwo, "T-102", "Lecturer");
                insertStudentProfile(connection, studentOne, "BSSE-1401", "2023-24", "A+");
                insertStudentProfile(connection, studentTwo, "BSSE-1402", "2023-24", "B+");
                insertStudentProfile(connection, studentThree, "BSSE-1403", "2023-24", "O+");

                long course = insertCourse(connection, "SE-2215", "Design Patterns", "THEORY",
                        3.0, "2025-26", "5th", "ACTIVE", "FINALIZED");
                long lab = insertCourse(connection, "SE-2216", "Design Patterns Lab", "LAB",
                        1.5, "2025-26", "5th", "DRAFT", "DRAFT");

                assignTeacher(connection, course, teacherOne);
                assignTeacher(connection, lab, teacherOne);
                assignTeacher(connection, lab, teacherTwo);
                enroll(connection, course, studentOne);
                enroll(connection, course, studentTwo);
                enroll(connection, course, studentThree);
                enroll(connection, lab, studentOne);
                enroll(connection, lab, studentTwo);

                insertComponent(connection, course, "Attendance", 15, 100, "ATTENDANCE");
                insertComponent(connection, lab, "Attendance", 15, 100, "ATTENDANCE");
                long quiz = insertComponent(connection, course, "Quizzes", 25, 20, "MANUAL");
                long assignment = insertComponent(connection, course, "Assignments", 15, 20, "MANUAL");
                long midterm = insertComponent(connection, course, "Midterm", 45, 30, "MANUAL");
                insertMark(connection, quiz, studentOne, 17);
                insertMark(connection, assignment, studentOne, 18);
                insertMark(connection, midterm, studentOne, 24);
                insertMark(connection, quiz, studentTwo, 14);
                insertMark(connection, assignment, studentTwo, 16);
                insertMark(connection, midterm, studentTwo, 22);
                insertMark(connection, quiz, studentThree, 18);
                insertMark(connection, assignment, studentThree, 17);
                insertMark(connection, midterm, studentThree, 26);

                insertAttendance(connection, course, teacherOne, studentOne, studentTwo, studentThree);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not seed the database", exception);
        }
    }

    private boolean hasUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return result.next() && result.getInt(1) > 0;
        }
    }

    private long insertUser(Connection connection, String username, String password, String fullName,
                            String email, Role role) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash, full_name, email, role) VALUES(?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHasher.hash(password));
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

    private void insertStudentProfile(Connection connection, long id, String roll, String session,
                                      String bloodGroup) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO student_profiles(user_id, roll_number, academic_session, blood_group) VALUES(?,?,?,?)")) {
            statement.setLong(1, id);
            statement.setString(2, roll);
            statement.setString(3, session);
            statement.setString(4, bloodGroup);
            statement.executeUpdate();
        }
    }

    private void insertTeacherProfile(Connection connection, long id, String employeeId,
                                      String designation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO teacher_profiles(user_id, employee_id, designation) VALUES(?,?,?)")) {
            statement.setLong(1, id);
            statement.setString(2, employeeId);
            statement.setString(3, designation);
            statement.executeUpdate();
        }
    }

    private long insertCourse(Connection connection, String code, String title, String type, double credit,
                              String session, String semester, String status, String ceStatus) throws SQLException {
        String sql = "INSERT INTO courses(course_code,title,course_type,credit,academic_session,semester,status,ce_status) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, code);
            statement.setString(2, title);
            statement.setString(3, type);
            statement.setDouble(4, credit);
            statement.setString(5, session);
            statement.setString(6, semester);
            statement.setString(7, status);
            statement.setString(8, ceStatus);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void assignTeacher(Connection connection, long course, long teacher) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO course_teachers(course_id, teacher_id) VALUES(?,?)")) {
            statement.setLong(1, course);
            statement.setLong(2, teacher);
            statement.executeUpdate();
        }
    }

    private void enroll(Connection connection, long course, long student) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO enrollments(course_id, student_id) VALUES(?,?)")) {
            statement.setLong(1, course);
            statement.setLong(2, student);
            statement.executeUpdate();
        }
    }

    private long insertComponent(Connection connection, long course, String title, double weight,
                                 double maximum, String componentType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO assessment_components(course_id,title,weight_percentage,maximum_mark,component_type) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, course);
            statement.setString(2, title);
            statement.setDouble(3, weight);
            statement.setDouble(4, maximum);
            statement.setString(5, componentType);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertMark(Connection connection, long component, long student, double mark) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO assessment_marks(component_id,student_id,obtained_mark) VALUES(?,?,?)")) {
            statement.setLong(1, component);
            statement.setLong(2, student);
            statement.setDouble(3, mark);
            statement.executeUpdate();
        }
    }

    private void insertAttendance(Connection connection, long course, long teacher,
                                  long studentOne, long studentTwo, long studentThree) throws SQLException {
        long sessionId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO attendance_sessions(course_id,class_date,title,created_by) VALUES(?,date('now'),'Orientation',?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, course);
            statement.setLong(2, teacher);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                sessionId = keys.getLong(1);
            }
        }
        long[] students = {studentOne, studentTwo, studentThree};
        for (int index = 0; index < students.length; index++) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO attendance_records(session_id,student_id,status) VALUES(?,?,?)")) {
                statement.setLong(1, sessionId);
                statement.setLong(2, students[index]);
                statement.setString(3, index == 1 ? "ABSENT" : "PRESENT");
                statement.executeUpdate();
            }
        }
    }
}
