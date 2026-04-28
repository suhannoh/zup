package com.noh.zup.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noh.zup.domain.admin.AdminRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationSeconds;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(String email, AdminRole role) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", email);
        payload.put("role", role.name());
        payload.put("iat", now);
        payload.put("exp", now + expirationSeconds);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public JwtAdminPrincipal validateAndParse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]),
                    new TypeReference<>() {
                    }
            );

            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Expired token");
            }

            String email = String.valueOf(payload.get("sub"));
            AdminRole role = AdminRole.valueOf(String.valueOf(payload.get("role")));
            return new JwtAdminPrincipal(email, role);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid token", exception);
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode JWT", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
