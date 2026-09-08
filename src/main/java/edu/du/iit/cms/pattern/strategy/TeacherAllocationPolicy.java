package edu.du.iit.cms.pattern.strategy;

import edu.du.iit.cms.domain.CourseType;
import edu.du.iit.cms.service.ValidationException;

public interface TeacherAllocationPolicy {
    CourseType courseType();

    int requiredTeachers();

    default void ensureCanAdd(int currentTeacherCount) {
        if (currentTeacherCount >= requiredTeachers()) {
            throw new ValidationException(courseType() + " courses require exactly "
                    + requiredTeachers() + " Teacher(s).");
        }
    }

    default void validateReady(int currentTeacherCount) {
        if (currentTeacherCount != requiredTeachers()) {
            throw new ValidationException(courseType() + " courses require exactly "
                    + requiredTeachers() + " Teacher(s), but " + currentTeacherCount + " are assigned.");
        }
    }
}

