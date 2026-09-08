package edu.du.iit.cms;

import edu.du.iit.cms.domain.Course;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertEquals(33.4, services.evaluation().calculateCe(course.id(), student.studentId()), 0.0001);
        assertEquals(100.0, services.attendance().count(course.id(), student.studentId()).percentage(), 0.0001);
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
                3.0, "2025-26", "5th");
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
        long componentId = services.evaluation().components(active.id()).getFirst().id();
        services.evaluation().deleteComponent(teacher.id(), active.id(), componentId);
        assertFalse(services.evaluation().components(active.id()).stream()
                .anyMatch(component -> component.id() == componentId));
        assertEquals(CeStatus.DRAFT, services.courses().get(active.id()).ceStatus());

        long studentId = services.users().search("student1").getFirst().id();
        services.users().setActive(studentId, false);
        assertThrows(ValidationException.class, () -> services.auth().login("student1", "student123"));
    }
}
