package edu.du.iit.cms.pattern.chain;

import edu.du.iit.cms.domain.CeStatus;
import edu.du.iit.cms.service.ValidationException;

public final class CeStructureValidator extends AbstractCompletionValidator {
    @Override
    protected void check(CompletionContext context) {
        if (context.course().ceStatus() != CeStatus.FINALIZED) {
            throw new ValidationException("The CE structure must be finalized.");
        }
        if (Math.abs(context.totalCeWeight() - 100.0) > 0.0001) {
            throw new ValidationException("CE component weights must total exactly 100%.");
        }
    }
}

