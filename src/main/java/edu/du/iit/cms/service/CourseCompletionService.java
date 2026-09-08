package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStudent;
import edu.du.iit.cms.domain.EnrollmentStatus;
import edu.du.iit.cms.domain.FinalResult;
import edu.du.iit.cms.pattern.chain.CompletionContext;
import edu.du.iit.cms.pattern.chain.CompletionValidationChain;
import edu.du.iit.cms.pattern.state.CourseLifecycle;
import edu.du.iit.cms.pattern.strategy.TeacherAllocationPolicies;
import edu.du.iit.cms.repository.CourseRepository;
import edu.du.iit.cms.repository.EvaluationRepository;

import java.util.ArrayList;
import java.util.List;

public final class CourseCompletionService {
    private final CourseRepository courseRepository;
    private final EvaluationRepository evaluationRepository;
    private final TeacherAllocationPolicies allocationPolicies;
    private final CompletionValidationChain validationChain;

    public CourseCompletionService(CourseRepository courseRepository,
                                   EvaluationRepository evaluationRepository,
                                   TeacherAllocationPolicies allocationPolicies,
                                   CompletionValidationChain validationChain) {
        this.courseRepository = courseRepository;
        this.evaluationRepository = evaluationRepository;
        this.allocationPolicies = allocationPolicies;
        this.validationChain = validationChain;
    }

    public List<FinalResult> finish(long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("Course was not found."));
        CourseLifecycle lifecycle = new CourseLifecycle(course.status());
        lifecycle.ensureCanManageAcademics();

        List<CourseStudent> students = courseRepository.findStudents(courseId);
        int missingFinalMarks = (int) students.stream().filter(student -> student.finalExamMark() == null).count();
        int requiredTeachers = allocationPolicies.forType(course.courseType()).requiredTeachers();
        CompletionContext context = new CompletionContext(course, requiredTeachers,
                courseRepository.countTeachers(courseId), students.size(), evaluationRepository.totalWeight(courseId),
                evaluationRepository.countMissingMarks(courseId), missingFinalMarks);
        validationChain.validate(context);

        List<FinalResult> results = new ArrayList<>();
        for (CourseStudent student : students) {
            double ceMark = round(evaluationRepository.calculateCe(courseId, student.studentId()));
            double finalExamMark = student.finalExamMark();
            double total = round(ceMark + finalExamMark);
            EnrollmentStatus status = total >= 40 ? EnrollmentStatus.COMPLETED : EnrollmentStatus.INCOMPLETE;
            results.add(new FinalResult(student.studentId(), ceMark, finalExamMark, total, status));
        }

        lifecycle.finish();
        courseRepository.finishCourse(courseId, results);
        return results;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

