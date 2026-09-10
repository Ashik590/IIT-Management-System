package edu.du.iit.cms.domain;

public enum CourseType {
    THEORY(40, 60),
    LAB(70, 30);

    private final int ceMarks;
    private final int finalExamMarks;

    CourseType(int ceMarks, int finalExamMarks) {
        this.ceMarks = ceMarks;
        this.finalExamMarks = finalExamMarks;
    }

    public int ceMarks() {
        return ceMarks;
    }

    public int finalExamMarks() {
        return finalExamMarks;
    }
}
