package edu.du.iit.cms;

import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.AttendanceStatus;
import edu.du.iit.cms.domain.CeStatus;
import edu.du.iit.cms.domain.CourseStatus;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.domain.EnrollmentStatus;
import edu.du.iit.cms.domain.Role;
import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.service.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppServicesIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void seedsAuthenticatesAndCalculatesAcademicData() {
        AppServices services = new AppServices(temporaryDirectory, true);

        User admin = services.auth().login("admin", "admin123");
        assertEquals(Role.ADMIN, admin.role());
        assertThrows(ValidationException.class, () -> services.auth().login("admin", "wrong"));

        Course course = services.courses().allCourses().stream()
                .filter(item -> item.courseCode().equals("SE-2215"))
                .findFirst().orElseThrow();
        CourseStudent student = services.courses().students(course.id()).stream()
                .filter(item -> item.rollNumber().equals("BSSE-1401"))
                .findFirst().orElseThrow();

        assertEquals(34.3, services.evaluation().calculateCe(course.id(), student.studentId()), 0.0001);
        assertEquals(100.0, services.attendance().count(course.id(), student.studentId()).percentage(), 0.0001);
    }

    @Test
    void hidesDraftCoursesFromTeachersAndStudentsButKeepsThemVisibleToAdministrators() {
        AppServices services = new AppServices(temporaryDirectory, true);
        User teacher = services.auth().login("teacher1", "teacher123");
        User student = services.auth().login("student1", "student123");

        assertTrue(services.courses().allCourses().stream()
                .anyMatch(course -> course.status() == CourseStatus.DRAFT));
        assertTrue(services.courses().coursesForTeacher(teacher.id()).stream()
                .noneMatch(course -> course.status() == CourseStatus.DRAFT));
        assertTrue(services.courses().coursesForStudent(student.id()).stream()
                .noneMatch(course -> course.status() == CourseStatus.DRAFT));
    }

    @Test
    void userManagementSearchNeverReturnsAdministratorAccounts() {
        AppServices services = new AppServices(temporaryDirectory, true);

        assertTrue(services.users().search("").stream()
                .noneMatch(result -> result.role() == Role.ADMIN));
        assertTrue(services.users().search("admin").isEmpty());
        assertFalse(services.users().search("student1").isEmpty());
        assertFalse(services.users().search("T-101").isEmpty());
    }

    @Test
    void appliesCourseTypeSpecificCeAndFinalExamMarks() {
        AppServices services = new AppServices(temporaryDirectory, true);
        User teacher = services.auth().login("teacher1", "teacher123");
        Course lab = services.courses().allCourses().stream()
                .filter(course -> course.courseCode().equals("SE-2216"))
                .findFirst().orElseThrow();

        services.courses().activateCourse(lab.id());
        var attendance = services.courses().students(lab.id()).stream().collect(Collectors.toMap(
                CourseStudent::studentId, student -> AttendanceStatus.PRESENT));
        services.attendance().createSession(teacher.id(), lab.id(), LocalDate.now(), "Lab 1", attendance);
        var attendanceComponent = services.evaluation().components(lab.id()).stream()
                .filter(component -> component.title().equals("Attendance"))
                .findFirst().orElseThrow();
        assertEquals(15.0, attendanceComponent.weightPercentage(), 0.0001);
        services.evaluation().updateWeight(teacher.id(), lab.id(), attendanceComponent.id(), 10);
        long componentId = services.evaluation().addComponent(
                teacher.id(), lab.id(), "Lab performance", 90, 100);
        services.evaluation().finalizeStructure(teacher.id(), lab.id());

        for (CourseStudent student : services.courses().students(lab.id())) {
            services.evaluation().saveMark(teacher.id(), lab.id(), componentId, student.studentId(), 100);
            services.courses().saveFinalExamMark(lab.id(), student.studentId(), 30);
            assertEquals(70.0, services.evaluation().calculateCe(lab.id(), student.studentId()), 0.0001);
        }
        CourseStudent firstStudent = services.courses().students(lab.id()).getFirst();
        assertThrows(ValidationException.class,
                () -> services.courses().saveFinalExamMark(lab.id(), firstStudent.studentId(), 30.01));

        services.completion().finish(lab.id());
        assertTrue(services.courses().students(lab.id()).stream()
                .allMatch(student -> student.ceMark() == 70.0 && student.totalMark() == 100.0));

        int componentCount = services.evaluation().components(lab.id()).size();
        services.courses().resetFinishedCourse(lab.id());
        Course reset = services.courses().get(lab.id());
        assertEquals(CourseStatus.DRAFT, reset.status());
        assertTrue(reset.academicSession().isEmpty());
        assertTrue(services.courses().students(lab.id()).isEmpty());
        assertEquals(2, services.courses().teachers(lab.id()).size());
        assertEquals(componentCount, services.evaluation().components(lab.id()).size());
        assertNull(services.evaluation().mark(componentId, firstStudent.studentId()));
        assertEquals(0, services.attendance().count(lab.id(), firstStudent.studentId()).total());
        assertThrows(ValidationException.class, () -> services.courses().activateCourse(lab.id()));
    }

    @Test
    void completesCourseAtomicallyAndStoresOutcomes() {
        AppServices services = new AppServices(temporaryDirectory, true);
        Course course = services.courses().allCourses().stream()
                .filter(item -> item.courseCode().equals("SE-2215"))
                .findFirst().orElseThrow();

        for (CourseStudent student : services.courses().students(course.id())) {
            double finalMark = student.rollNumber().equals("BSSE-1402") ? 5 : 30;
            services.courses().saveFinalExamMark(course.id(), student.studentId(), finalMark);
        }

        services.completion().finish(course.id());

        assertEquals(CourseStatus.FINISHED, services.courses().get(course.id()).status());
        var stored = services.courses().students(course.id());
        assertTrue(stored.stream().allMatch(student -> student.totalMark() != null));
        assertEquals(EnrollmentStatus.INCOMPLETE, stored.stream()
                .filter(student -> student.rollNumber().equals("BSSE-1402"))
                .findFirst().orElseThrow().enrollmentStatus());

    }

    @Test
    void supportsSafeCrudWithoutDeletingAcademicHistory() {
        AppServices services = new AppServices(temporaryDirectory, true);

        long courseId = services.courses().createCourse("SE-2299", "Temporary", CourseType.THEORY,
                3.0, "2026-27", "5th");
        services.courses().updateCourse(courseId, "SE-2299", "Updated course", CourseType.LAB,
                1.5, "2026-27", "6th");
        Course updated = services.courses().get(courseId);
        assertEquals("Updated course", updated.title());
        assertEquals(CourseType.LAB, updated.courseType());
        services.courses().deleteCourse(courseId);
        assertThrows(ValidationException.class, () -> services.courses().get(courseId));

        User teacher = services.auth().login("teacher1", "teacher123");
        Course active = services.courses().allCourses().stream()
                .filter(item -> item.courseCode().equals("SE-2215"))
                .findFirst().orElseThrow();
        long componentId = services.evaluation().components(active.id()).stream()
                .filter(component -> !component.title().equals("Attendance"))
                .findFirst().orElseThrow().id();
        services.evaluation().deleteComponent(teacher.id(), active.id(), componentId);
        assertFalse(services.evaluation().components(active.id()).stream()
                .anyMatch(component -> component.id() == componentId));
        assertEquals(CeStatus.DRAFT, services.courses().get(active.id()).ceStatus());

        long studentId = services.users().search("student1").getFirst().id();
        services.users().setActive(studentId, false);
        assertThrows(ValidationException.class, () -> services.auth().login("student1", "student123"));
    }
}
