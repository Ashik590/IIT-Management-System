package edu.du.iit.cms.pattern.chain;

import edu.du.iit.cms.service.ValidationException;

public final class TeacherCountValidator extends AbstractCompletionValidator {
    @Override
    protected void check(CompletionContext context) {
        if (context.actualTeacherCount() != context.requiredTeacherCount()) {
            throw new ValidationException("Course requires " + context.requiredTeacherCount()
                    + " Teacher(s), but " + context.actualTeacherCount() + " are assigned.");
        }
    }
}

