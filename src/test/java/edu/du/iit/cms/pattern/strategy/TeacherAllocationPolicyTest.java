package edu.du.iit.cms.pattern.strategy;

import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.service.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeacherAllocationPolicyTest {
    private final TeacherAllocationPolicies policies = new TeacherAllocationPolicies();

    @Test
    void theoryRequiresOneTeacher() {
        TeacherAllocationPolicy policy = policies.forType(CourseType.THEORY);
        assertEquals(1, policy.requiredTeachers());
        assertDoesNotThrow(() -> policy.validateReady(1));
        assertThrows(ValidationException.class, () -> policy.ensureCanAdd(1));
    }

    @Test
    void labRequiresTwoTeachers() {
        TeacherAllocationPolicy policy = policies.forType(CourseType.LAB);
        assertEquals(2, policy.requiredTeachers());
        assertDoesNotThrow(() -> policy.ensureCanAdd(1));
        assertDoesNotThrow(() -> policy.validateReady(2));
        assertThrows(ValidationException.class, () -> policy.validateReady(1));
    }
}

