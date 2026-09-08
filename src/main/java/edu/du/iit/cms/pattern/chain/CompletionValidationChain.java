package edu.du.iit.cms.pattern.chain;

public final class CompletionValidationChain {
    private final CompletionValidator first;

    public CompletionValidationChain() {
        CompletionValidator teacherCount = new TeacherCountValidator();
        CompletionValidator enrollment = new EnrollmentValidator();
        CompletionValidator ceStructure = new CeStructureValidator();
        CompletionValidator marks = new MarksCompleteValidator();
        teacherCount.setNext(enrollment).setNext(ceStructure).setNext(marks);
        first = teacherCount;
    }

    public void validate(CompletionContext context) {
        first.validate(context);
    }
}
