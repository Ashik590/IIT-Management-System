package edu.du.iit.cms.pattern.chain;

import edu.du.iit.cms.service.ValidationException;

public final class MarksCompleteValidator extends AbstractCompletionValidator {
    @Override
    protected void check(CompletionContext context) {
        if (context.missingAssessmentMarks() > 0) {
            throw new ValidationException(context.missingAssessmentMarks()
                    + " assessment mark(s) are missing.");
        }
        if (context.missingFinalExamMarks() > 0) {
            throw new ValidationException(context.missingFinalExamMarks()
                    + " final-exam mark(s) are missing.");
        }
    }
}

