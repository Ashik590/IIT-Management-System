package edu.du.iit.cms.domain;

public record Student(
        long id,
        String fullName,
        String rollNumber,
        String academicSession,
        String bloodGroup
) {
    @Override
    public String toString() {
        return rollNumber + " - " + fullName;
    }
}

