package com.workforceos.authentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final PasswordEncoder passwordEncoder;
    private final UserAccountLookupService userAccountLookupService;
    private final byte[] signingKey;

    public AuthService(PasswordEncoder passwordEncoder,
            UserAccountRepository userAccountRepository,
            UserAccountLookupService userAccountLookupService,
            @Value("${workforce.auth.signing-key:workforce-os-development-signing-key-change-me}") String signingKey,
            @Value("${workforce.auth.admin-username:admin}") String adminUsername,
            @Value("${workforce.auth.admin-password:admin}") String adminPassword) {
        this.passwordEncoder = passwordEncoder;
        this.userAccountLookupService = userAccountLookupService;
        this.signingKey = signingKey.getBytes(StandardCharsets.UTF_8);
        if (userAccountRepository.count() == 0) {
            userAccountRepository.save(new UserAccountEntity(
                    adminUsername, passwordEncoder.encode(adminPassword), Set.of("ADMIN"), true));
        }
    }

    public String login(AuthLoginRequest request) {
        UserAccount user = request == null || request.username() == null
                ? null
                : userAccountLookupService.findUser(request.username());
        if (user == null || !user.active() || request.password() == null
                || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        long expiresAt = (System.currentTimeMillis() / 1000) + 3600;
        String payload = user.username() + ":" + expiresAt;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + sign(payload);
    }

    public UserAccount authenticate(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException();
            }
            String[] values = payload.split(":", 2);
            if (Long.parseLong(values[1]) < System.currentTimeMillis() / 1000) {
                throw new IllegalArgumentException();
            }
            UserAccount user = userAccountLookupService.findUser(values[0]);
            if (user == null || !user.active()) {
                throw new IllegalArgumentException();
            }
            return user;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign token", exception);
        }
    }
}
