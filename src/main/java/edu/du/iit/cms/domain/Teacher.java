package edu.du.iit.cms.domain;

public record Teacher(
        long id,
        String fullName,
        String employeeId,
        String designation
) {
    @Override
    public String toString() {
        return employeeId + " - " + fullName;
    }
}

