package edu.du.iit.cms.pattern.chain;

import edu.du.iit.cms.service.ValidationException;

public final class EnrollmentValidator extends AbstractCompletionValidator {
    @Override
    protected void check(CompletionContext context) {
        if (context.enrolledStudentCount() == 0) {
            throw new ValidationException("At least one Student must be enrolled.");
        }
    }
}

