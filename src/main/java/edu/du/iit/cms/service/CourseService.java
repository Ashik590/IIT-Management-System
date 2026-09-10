package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStatus;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.domain.Teacher;
import edu.du.iit.cms.pattern.state.CourseLifecycle;
import edu.du.iit.cms.pattern.strategy.TeacherAllocationPolicies;
import edu.du.iit.cms.repository.CourseRepository;

import java.util.List;

public final class CourseService {
    private final CourseRepository courseRepository;
    private final TeacherAllocationPolicies allocationPolicies;

    public CourseService(CourseRepository courseRepository, TeacherAllocationPolicies allocationPolicies) {
        this.courseRepository = courseRepository;
        this.allocationPolicies = allocationPolicies;
    }

    public long createCourse(String code, String title, CourseType type, double credit,
                             String academicSession, String semester) {
        validateCourseFields(code, title, type, credit, academicSession, semester);
        return courseRepository.create(code.trim().toUpperCase(), title.trim(), type, credit,
                academicSession.trim(), semester.trim());
    }

    public void updateCourse(long courseId, String code, String title, CourseType type, double credit,
                             String academicSession, String semester) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanConfigure();
        validateCourseFields(code, title, type, credit, academicSession, semester);
        courseRepository.updateDraft(courseId, code.trim().toUpperCase(), title.trim(), type, credit,
                academicSession.trim(), semester.trim());
    }

    public void deleteCourse(long courseId) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanConfigure();
        courseRepository.deleteDraft(courseId);
    }

    public void resetFinishedCourse(long courseId) {
        requireFinished(courseId, "reset");
        courseRepository.resetFinished(courseId);
    }

    private void requireFinished(long courseId, String operation) {
        if (get(courseId).status() != CourseStatus.FINISHED) {
            throw new ValidationException("Only a Finished course can be " + operation + ".");
        }
    }

    private void validateCourseFields(String code, String title, CourseType type, double credit,
                                      String academicSession, String semester) {
        require(code, "Course code");
        require(title, "Course title");
        require(academicSession, "Academic session");
        require(semester, "Semester");
        if (type == null) {
            throw new ValidationException("Course type is required.");
        }
        if (credit <= 0 || credit > 6) {
            throw new ValidationException("Credit must be greater than 0 and at most 6.");
        }
    }

    public void assignTeacher(long courseId, long teacherId) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanConfigure();
        if (courseRepository.isTeacherAssigned(courseId, teacherId)) {
            throw new ValidationException("Teacher is already assigned to this course.");
        }
        int currentCount = courseRepository.countTeachers(courseId);
        allocationPolicies.forType(course.courseType()).ensureCanAdd(currentCount);
        courseRepository.assignTeacher(courseId, teacherId);
    }

    public void removeTeacher(long courseId, long teacherId) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanConfigure();
        courseRepository.removeTeacher(courseId, teacherId);
    }

    public void enrollStudent(long courseId, long studentId) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanConfigure();
        if (courseRepository.isStudentEnrolled(courseId, studentId)) {
            throw new ValidationException("Student is already enrolled in this course.");
        }
        courseRepository.enrollStudent(courseId, studentId);
    }

    public void removeStudent(long courseId, long studentId) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanConfigure();
        courseRepository.removeStudent(courseId, studentId);
    }

    public void activateCourse(long courseId) {
        Course course = get(courseId);
        require(course.academicSession(), "Academic session");
        CourseLifecycle lifecycle = new CourseLifecycle(course.status());
        int teachers = courseRepository.countTeachers(courseId);
        allocationPolicies.forType(course.courseType()).validateReady(teachers);
        if (courseRepository.countStudents(courseId) == 0) {
            throw new ValidationException("Enroll at least one Student before activation.");
        }
        lifecycle.activate();
        courseRepository.updateStatus(courseId, lifecycle.status());
    }

    public void saveFinalExamMark(long courseId, long studentId, double mark) {
        Course course = get(courseId);
        new CourseLifecycle(course.status()).ensureCanManageAcademics();
        int maximumMark = course.courseType().finalExamMarks();
        if (mark < 0 || mark > maximumMark) {
            throw new ValidationException("Final-exam mark must be between 0 and " + maximumMark + ".");
        }
        courseRepository.saveFinalExamMark(courseId, studentId, mark);
    }

    public Course get(long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("Course was not found."));
    }

    public List<Course> allCourses() {
        return courseRepository.findAll();
    }

    public List<Course> coursesForTeacher(long teacherId) {
        return courseRepository.findByTeacher(teacherId);
    }

    public List<Course> coursesForStudent(long studentId) {
        return courseRepository.findByStudent(studentId);
    }

    public List<CourseStudent> students(long courseId) {
        return courseRepository.findStudents(courseId);
    }

    public List<Teacher> teachers(long courseId) {
        return courseRepository.findTeachers(courseId);
    }

    public void ensureAssignedTeacher(long courseId, long teacherId) {
        if (!courseRepository.isTeacherAssigned(courseId, teacherId)) {
            throw new ValidationException("You are not assigned to this course.");
        }
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
    }
}
