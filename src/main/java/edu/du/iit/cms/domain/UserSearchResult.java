package edu.du.iit.cms.domain;

public record UserSearchResult(
        long id,
        String username,
        String fullName,
        String email,
        Role role,
        String identifier,
        String details,
        boolean active
) {
    @Override
    public String toString() {
        return fullName + " | " + identifier + " | " + role + (active ? "" : " | INACTIVE");
    }
}

