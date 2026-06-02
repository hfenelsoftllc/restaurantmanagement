package com.hfenelsoftllc.restaurantlisting.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JwtTestTokenFactory {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtTestTokenFactory() {
    }

    public static String createToken(String secret, Long userId, String email, String sessionVersion) {
        try {
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plusSeconds(3600);

            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("email", email);
            payload.put("sessionVersion", sessionVersion);
            payload.put("iat", issuedAt.getEpochSecond());
            payload.put("exp", expiresAt.getEpochSecond());

            String body = encodeJson(payload);
            String unsignedToken = header + "." + body;
            String signature = sign(secret, unsignedToken);
            return unsignedToken + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create test JWT", ex);
        }
    }

    private static String encodeJson(Map<String, Object> value) throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String secret, String unsignedToken) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
}

