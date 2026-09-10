package edu.du.iit.cms.pattern.chain;

import edu.du.iit.cms.domain.CeStatus;
import edu.du.iit.cms.domain.Course;
import edu.du.iit.cms.domain.CourseStatus;
import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.service.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompletionValidationChainTest {
    private final CompletionValidationChain chain = new CompletionValidationChain();

    @Test
    void acceptsCompleteCourseContext() {
        assertDoesNotThrow(() -> chain.validate(context(1, 1, 3, 100, 0, 0)));
    }

    @Test
    void rejectsEachIncompleteRequirement() {
        assertThrows(ValidationException.class, () -> chain.validate(context(1, 0, 3, 100, 0, 0)));
        assertThrows(ValidationException.class, () -> chain.validate(context(1, 1, 0, 100, 0, 0)));
        assertThrows(ValidationException.class, () -> chain.validate(context(1, 1, 3, 90, 0, 0)));
        assertThrows(ValidationException.class, () -> chain.validate(context(1, 1, 3, 100, 1, 0)));
        assertThrows(ValidationException.class, () -> chain.validate(context(1, 1, 3, 100, 0, 1)));
    }

    private CompletionContext context(int requiredTeachers, int actualTeachers, int students,
                                      double weight, int missingAssessment, int missingFinal) {
        Course course = new Course(1, "SE-2215", "Design Patterns", CourseType.THEORY, 3,
                "2026-27", "5th", CourseStatus.ACTIVE, CeStatus.FINALIZED, null);
        return new CompletionContext(course, requiredTeachers, actualTeachers, students,
                weight, missingAssessment, missingFinal);
    }
}
