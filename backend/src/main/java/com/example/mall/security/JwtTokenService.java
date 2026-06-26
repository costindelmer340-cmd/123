package com.example.mall.security;

import com.example.mall.common.auth.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenService(
        ObjectMapper objectMapper,
        @Value("${app.auth.jwt-secret}") String secret,
        @Value("${app.auth.access-token-ttl-seconds}") long accessTokenTtlSeconds,
        @Value("${app.auth.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public String createAccessToken(LoginUser loginUser) {
        return createToken(loginUser, "access", accessTokenTtlSeconds);
    }

    public String createRefreshToken(LoginUser loginUser) {
        return createToken(loginUser, "refresh", refreshTokenTtlSeconds);
    }

    public JwtClaims parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            String payloadJson = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<>() {});
            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Token expired");
            }

            return new JwtClaims(claims);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token", ex);
        }
    }

    public LoginUser toLoginUser(JwtClaims claims) {
        Long userId = longValue(claims.get("uid"));
        String username = stringValue(claims.get("uname"));
        String nickname = stringValue(claims.get("nick"));
        Long merchantId = claims.containsKey("mid") ? longValue(claims.get("mid")) : null;
        String password = stringValue(claims.get("pwd"));
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        return new LoginUser(userId, username, password, nickname, merchantId, Set.copyOf(roles));
    }

    public record JwtClaims(Map<String, Object> claims) {
        public Object get(String key) {
            return claims.get(key);
        }

        public boolean containsKey(String key) {
            return claims.containsKey(key);
        }
    }

    private String createToken(LoginUser loginUser, String type, long ttlSeconds) {
        Map<String, Object> payload = new HashMap<>();
        long now = Instant.now().getEpochSecond();
        payload.put("jti", java.util.UUID.randomUUID().toString());
        payload.put("type", type);
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);
        payload.put("uid", loginUser.getUserId());
        payload.put("uname", loginUser.getUsername());
        payload.put("nick", loginUser.getNickname());
        payload.put("mid", loginUser.getMerchantId());
        payload.put("roles", loginUser.getRoles().stream().sorted().toList());

        try {
            String header = ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String body = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = sign(header + "." + body);
            return header + "." + body + "." + signature;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to create jwt", ex);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return ENCODER.encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign jwt", ex);
        }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
