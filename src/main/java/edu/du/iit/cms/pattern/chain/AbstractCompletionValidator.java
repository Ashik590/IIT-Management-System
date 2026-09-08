package edu.du.iit.cms.pattern.chain;

public abstract class AbstractCompletionValidator implements CompletionValidator {
    private CompletionValidator next;

    @Override
    public CompletionValidator setNext(CompletionValidator next) {
        this.next = next;
        return next;
    }

    @Override
    public final void validate(CompletionContext context) {
        check(context);
        if (next != null) {
            next.validate(context);
        }
    }

    protected abstract void check(CompletionContext context);
}

