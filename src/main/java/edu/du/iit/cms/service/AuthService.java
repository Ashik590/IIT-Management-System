package edu.du.iit.cms.service;

import edu.du.iit.cms.domain.User;
import edu.du.iit.cms.repository.UserRepository;
import edu.du.iit.cms.security.PasswordHasher;

public final class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Enter both username and password.");
        }
        UserRepository.CredentialRow credentials = userRepository.findCredentials(username.trim())
                .orElseThrow(() -> new ValidationException("Incorrect username or password."));
        if (!credentials.user().active()) {
            throw new ValidationException("This account is inactive.");
        }
        if (!passwordHasher.matches(password, credentials.passwordHash())) {
            throw new ValidationException("Incorrect username or password.");
        }
        return credentials.user();
    }
}

