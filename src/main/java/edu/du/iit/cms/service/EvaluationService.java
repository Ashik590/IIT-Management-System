package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.AssessmentComponent;
import edu.du.iit.cms.domain.AssessmentComponentType;
import edu.du.iit.cms.domain.CeStatus;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.pattern.state.CourseLifecycle;
import edu.du.iit.cms.repository.CourseRepository;
import edu.du.iit.cms.repository.EvaluationRepository;

import java.util.List;

public final class EvaluationService {
    private static final double EPSILON = 0.0001;

    private final CourseRepository courseRepository;
    private final EvaluationRepository evaluationRepository;

    public EvaluationService(CourseRepository courseRepository, EvaluationRepository evaluationRepository) {
        this.courseRepository = courseRepository;
        this.evaluationRepository = evaluationRepository;
    }

    public long addComponent(long teacherId, long courseId, String title, double weight, double maximumMark) {
        Course course = requireManagedCourse(teacherId, courseId);
        requireText(title, "Assessment title");
        if (title.trim().equalsIgnoreCase("Attendance")) {
            throw new ValidationException("Attendance is already included as an automatic CE component.");
        }
        validateWeight(weight);
        if (maximumMark <= 0) {
            throw new ValidationException("Maximum mark must be greater than 0.");
        }
        if (evaluationRepository.totalWeight(courseId) + weight > 100 + EPSILON) {
            throw new ValidationException("Adding this component would make the total weight exceed 100%.");
        }
        new CourseLifecycle(course.status()).ensureCanManageAcademics();
        return evaluationRepository.addComponent(courseId, title.trim(), weight, maximumMark);
    }

    public void updateWeight(long teacherId, long courseId, long componentId, double newWeight) {
        requireManagedCourse(teacherId, courseId);
        validateWeight(newWeight);
        AssessmentComponent component = component(courseId, componentId);
        double adjustedTotal = evaluationRepository.totalWeight(courseId)
                - component.weightPercentage() + newWeight;
        if (adjustedTotal > 100 + EPSILON) {
            throw new ValidationException("The updated total weight would exceed 100%.");
        }
        evaluationRepository.updateWeight(courseId, componentId, newWeight);
    }

    public void deleteComponent(long teacherId, long courseId, long componentId) {
        requireManagedCourse(teacherId, courseId);
        AssessmentComponent component = component(courseId, componentId);
        if (component.type() == AssessmentComponentType.ATTENDANCE) {
            throw new ValidationException("The Attendance component cannot be deleted; its weight can be changed.");
        }
        evaluationRepository.deleteComponent(courseId, componentId);
    }

    public void finalizeStructure(long teacherId, long courseId) {
        requireManagedCourse(teacherId, courseId);
        double total = evaluationRepository.totalWeight(courseId);
        if (Math.abs(total - 100) > EPSILON) {
            throw new ValidationException("CE weights currently total " + String.format("%.2f", total)
                    + "%. They must total exactly 100%.");
        }
        if (evaluationRepository.findComponents(courseId).isEmpty()) {
            throw new ValidationException("Add at least one assessment component.");
        }
        evaluationRepository.finalizeStructure(courseId);
    }

    public void saveMark(long teacherId, long courseId, long componentId, long studentId, double mark) {
        Course course = requireManagedCourse(teacherId, courseId);
        if (course.ceStatus() != CeStatus.FINALIZED) {
            throw new ValidationException("Finalize the CE structure before entering marks.");
        }
        if (!courseRepository.isStudentEnrolled(courseId, studentId)) {
            throw new ValidationException("Student is not enrolled in this course.");
        }
        AssessmentComponent component = component(courseId, componentId);
        if (component.type() == AssessmentComponentType.ATTENDANCE) {
            throw new ValidationException("Attendance marks are calculated automatically from attendance records.");
        }
        if (mark < 0 || mark > component.maximumMark()) {
            throw new ValidationException("Obtained mark must be between 0 and " + component.maximumMark() + ".");
        }
        evaluationRepository.saveMark(componentId, studentId, mark);
    }

    public List<AssessmentComponent> components(long courseId) {
        return evaluationRepository.findComponents(courseId);
    }

    public Double mark(long componentId, long studentId) {
        return evaluationRepository.findMark(componentId, studentId);
    }

    public double calculateCe(long courseId, long studentId) {
        return evaluationRepository.calculateCe(courseId, studentId);
    }

    public double totalWeight(long courseId) {
        return evaluationRepository.totalWeight(courseId);
    }

    private Course requireManagedCourse(long teacherId, long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("Course was not found."));
        if (!courseRepository.isTeacherAssigned(courseId, teacherId)) {
            throw new ValidationException("You are not assigned to this course.");
        }
        new CourseLifecycle(course.status()).ensureCanManageAcademics();
        return course;
    }

    private AssessmentComponent component(long courseId, long componentId) {
        return evaluationRepository.findComponents(courseId).stream()
                .filter(item -> item.id() == componentId)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Assessment component was not found."));
    }

    private void validateWeight(double weight) {
        if (weight <= 0 || weight > 100) {
            throw new ValidationException("Weight must be greater than 0 and at most 100.");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
    }
}
