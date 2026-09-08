package edu.du.iit.cms.pattern.state;

import edu.du.iit.cms.domain.CourseStatus;
import edu.du.iit.cms.service.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseLifecycleTest {
    @Test
    void followsDraftActiveFinishedLifecycle() {
        CourseLifecycle lifecycle = new CourseLifecycle(CourseStatus.DRAFT);

        assertDoesNotThrow(lifecycle::ensureCanConfigure);
        assertThrows(ValidationException.class, lifecycle::ensureCanManageAcademics);

        lifecycle.activate();
        assertEquals(CourseStatus.ACTIVE, lifecycle.status());
        assertDoesNotThrow(lifecycle::ensureCanManageAcademics);
        assertThrows(ValidationException.class, lifecycle::ensureCanConfigure);

        lifecycle.finish();
        assertEquals(CourseStatus.FINISHED, lifecycle.status());
        assertThrows(ValidationException.class, lifecycle::ensureCanManageAcademics);
    }

    @Test
    void rejectsInvalidTransitions() {
        assertThrows(ValidationException.class,
                () -> new CourseLifecycle(CourseStatus.DRAFT).finish());
        assertThrows(ValidationException.class,
                () -> new CourseLifecycle(CourseStatus.FINISHED).activate());
    }
}

