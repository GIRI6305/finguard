package com.finguard.service;

import com.finguard.dto.AuthRequest;
import com.finguard.dto.AuthResponse;
import com.finguard.dto.RegisterRequest;
import com.finguard.exception.AccountLockedException;
import com.finguard.exception.UsernameTakenException;
import com.finguard.model.AppUser;
import com.finguard.model.Role;
import com.finguard.repository.UserRepository;
import com.finguard.security.JwtUtil;
import com.finguard.security.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginAttemptService loginAttemptService;

    public AuthResponse login(AuthRequest request) {
        if (loginAttemptService.isLocked(request.getUsername())) {
            throw new AccountLockedException(
                "Too many failed login attempts. Please wait 60 seconds and try again.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(request.getUsername());
            throw e;
        }

        loginAttemptService.recordSuccess(request.getUsername());

        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    /**
     * Self-service signup. Every new account is created with the ANALYST role --
     * ADMIN accounts are never created through this public endpoint, only via the
     * seeded default admin user, so privileged access can't be self-granted.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UsernameTakenException("Username already taken: " + request.getUsername());
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ANALYST);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}
