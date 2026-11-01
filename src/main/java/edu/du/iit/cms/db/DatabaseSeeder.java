package edu.du.iit.cms.db;

import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseSeeder {
    private static final String ACADEMIC_SESSION = "2024-25";
    private static final String SEMESTER = "1st";

    private static final String[] STUDENT_NAMES = {
            "M. HASEBUR RAHMAN SINHA", "MD. SIMUL HAQUE RIHAN", "OMER MAHBOOB", "AKAID MOONJIN",
            "MD. SHOUROV", "SADMAN SAKIB", "PARVEJ SHAH", "MD. MOSADDIK SARKER SOURAV",
            "DARUN NAYEEM", "NAFIS AHMED", "MD. NAIMUL ISLAM NAIM", "MD. AZIZUL HAKIM",
            "A. M. TOUFIQ ALAM", "RUBAIYAT ISLAM", "MD. ASIM ALAM CHOWDHURY",
            "MD. MAHMUDUL HASAN ANIK", "SHARIAR ISLAM MAHI", "MD. IRFANUR RAHMAN BHUYAN",
            "MD. TAMIM HOSSEN", "ABDULLAH AL MAHMUD", "MD. TAWRAT HOSSEN NIBIR",
            "MD. SAIKAT ISLAM", "EKRAMUL HOQUE", "MD. INZAMAMUL LOHANI", "SHOHIDUR RAHMAN",
            "ADNAN ALAM PRANTO", "MD. ABU RAYHAN", "MD. ROKIB IKBAL", "MD. AFIF HASAN",
            "MD. RASHIK RAIHAN", "ASADUZZAMAN RIFAT", "OASI MAHMUD", "ASHISH ROY SHAYON",
            "ARITHRO CHOUDHURY", "CHOYON BONIK PROTIK", "SAJIB SARKAR"
    };

    private static final SeedCourse[] COURSES = {
            new SeedCourse("CSE 1101", "THEORY", 3.0),
            new SeedCourse("CSE 1102", "THEORY", 3.0),
            new SeedCourse("STAT 1103", "THEORY", 3.0),
            new SeedCourse("GE 1105", "THEORY", 3.0),
            new SeedCourse("SE 1106", "THEORY", 3.0),
            new SeedCourse("MATH 1107", "THEORY", 3.0),
            new SeedCourse("CSE 1101L", "LAB", 1.5)
    };

    private static final SeedTeacher[] TEACHERS = {
            new SeedTeacher("Dr. Naushin Nower", "Director and Professor"),
            new SeedTeacher("Dr. Zerina Begum", "Professor"),
            new SeedTeacher("Mohd. Zulfiquar Hafiz", "Professor"),
            new SeedTeacher("Dr. Kazi Muheymin-Us-Sakib", "Professor"),
            new SeedTeacher("Dr. Md. Shariful Islam", "Professor"),
            new SeedTeacher("Dr. Mohammad Shoyaib", "Professor"),
            new SeedTeacher("Dr. B M Mainul Hossain", "Professor")
    };

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
                insertUser(connection, "admin", "admin123", "System Administrator",
                        "admin@iit.du.ac.bd", Role.ADMIN);
                long[] teachers = new long[TEACHERS.length];
                for (int index = 0; index < TEACHERS.length; index++) {
                    int number = index + 1;
                    SeedTeacher seedTeacher = TEACHERS[index];
                    teachers[index] = insertUser(connection, "teacher" + number, "teacher123",
                            seedTeacher.name(), "teacher" + number + "@iit.du.ac.bd", Role.TEACHER);
                    insertTeacherProfile(connection, teachers[index], "T-" + String.format("%03d", number),
                            seedTeacher.designation());
                }

                long[] students = new long[STUDENT_NAMES.length];
                for (int index = 0; index < STUDENT_NAMES.length; index++) {
                    int number = index + 1;
                    students[index] = insertUser(connection, "student" + number, "student123",
                            STUDENT_NAMES[index], "student" + number + "@iit.du.ac.bd", Role.STUDENT);
                    insertStudentProfile(connection, students[index], "BSSE-14" + String.format("%02d", number),
                            ACADEMIC_SESSION, "Unknown");
                }

                for (int index = 0; index < COURSES.length; index++) {
                    SeedCourse seedCourse = COURSES[index];
                    long course = insertCourse(connection, seedCourse.code(), seedCourse.code(), seedCourse.type(),
                            seedCourse.credit(), ACADEMIC_SESSION, SEMESTER, "ACTIVE", "DRAFT");
                    assignTeacher(connection, course, teachers[index]);
                    if ("LAB".equals(seedCourse.type())) {
                        assignTeacher(connection, course, teachers[0]);
                    }
                    for (long student : students) {
                        enroll(connection, course, student);
                    }
                    insertComponent(connection, course, "Attendance", 15, 100, "ATTENDANCE");
                }
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

    private void insertComponent(Connection connection, long course, String title, double weight,
                                 double maximum, String componentType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO assessment_components(course_id,title,weight_percentage,maximum_mark,component_type) VALUES(?,?,?,?,?)")) {
            statement.setLong(1, course);
            statement.setString(2, title);
            statement.setDouble(3, weight);
            statement.setDouble(4, maximum);
            statement.setString(5, componentType);
            statement.executeUpdate();
        }
    }

    private record SeedCourse(String code, String type, double credit) {
    }

    private record SeedTeacher(String name, String designation) {
    }
}
