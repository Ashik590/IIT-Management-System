package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.Student;
import edu.du.iit.cms.domain.Teacher;
import edu.du.iit.cms.domain.UserSearchResult;
import edu.du.iit.cms.repository.UserRepository;
import edu.du.iit.cms.security.PasswordHasher;

import java.util.List;
import java.util.regex.Pattern;

public final class UserService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public long createStudent(String username, String password, String fullName, String email,
                              String rollNumber, String session, String bloodGroup) {
        validateCommon(username, fullName, email);
        require(rollNumber, "Roll number");
        require(session, "Academic session");
        require(bloodGroup, "Blood group");
        return userRepository.createStudent(username.trim(), passwordHasher.hash(password), fullName.trim(),
                email.trim(), rollNumber.trim(), session.trim(), bloodGroup.trim().toUpperCase());
    }

    public long createTeacher(String username, String password, String fullName, String email,
                              String employeeId, String designation) {
        validateCommon(username, fullName, email);
        require(employeeId, "Employee ID");
        require(designation, "Designation");
        return userRepository.createTeacher(username.trim(), passwordHasher.hash(password), fullName.trim(),
                email.trim(), employeeId.trim(), designation.trim());
    }

    public List<UserSearchResult> searchStudents(String query) {
        return userRepository.search(query, edu.du.iit.cms.domain.Role.STUDENT);
    }

    public List<UserSearchResult> searchTeachers(String query) {
        return userRepository.search(query, edu.du.iit.cms.domain.Role.TEACHER);
    }

    public List<Student> activeStudents() {
        return userRepository.findActiveStudents();
    }

    public List<Teacher> activeTeachers() {
        return userRepository.findActiveTeachers();
    }

    public void setActive(long userId, boolean active) {
        userRepository.setActive(userId, active);
    }

    private void validateCommon(String username, String fullName, String email) {
        require(username, "Username");
        require(fullName, "Full name");
        require(email, "Email");
        if (username.trim().contains(" ")) {
            throw new ValidationException("Username cannot contain spaces.");
        }
        if (!EMAIL.matcher(email.trim()).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
    }
}
