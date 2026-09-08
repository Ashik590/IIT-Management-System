package edu.du.iit.cms.pattern.chain;

public interface CompletionValidator {
    CompletionValidator setNext(CompletionValidator next);

    void validate(CompletionContext context);
}

