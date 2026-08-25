package com.shoppingmall.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    private SecretKey key;

    /** [4-2][5-1 조치] 소스에 공개된 기본값이 서명키가 되는 것을 막기 위한 최소 길이(HS256 = 256bit). */
    private static final int MIN_SECRET_LENGTH = 32;

    /**
     * [4-2][5-1 조치] 서명키를 검증하고 기동한다.
     *
     * <p>기존에는 application.yml 의 기본값(플레이스홀더)이 그대로 서명키가 될 수 있었다.
     * 환경변수를 빠뜨리면 <b>소스에 공개된 문자열로 토큰을 위조</b>할 수 있으므로,
     * 조용히 취약해지는 대신 기동을 실패시킨다.
     */
    @PostConstruct
    protected void init() {
        if (secretKey == null || secretKey.isBlank() || secretKey.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "jwt.secret 이 설정되지 않았거나 너무 짧습니다(최소 " + MIN_SECRET_LENGTH + "자). "
                            + "환경변수 JWT_SECRET 을 설정해 주세요. 예: openssl rand -base64 48");
        }
        if (secretKey.startsWith("CHANGE_ME")) {
            throw new IllegalStateException(
                    "jwt.secret 이 소스에 공개된 기본 플레이스홀더입니다. 환경변수 JWT_SECRET 을 설정해 주세요.");
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /** 인증/인가에 필요한 최소 정보(userId, role)만 클레임에 담는다. */
    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, accessTokenValidityMs, null);
    }

    /**
     * Refresh Token 에는 <b>jti(고유 식별자)</b> 를 넣어 항상 다른 문자열이 되게 한다.
     *
     * <p>넣지 않으면 클레임이 {@code sub·role·iat·exp} 뿐이고 iat 가 <b>초 단위</b>라,
     * 같은 계정이 <b>같은 초에</b> 두 번 발급받으면 서명까지 동일한
     * <b>완전히 같은 토큰 문자열</b>이 만들어진다. {@code refresh_tokens.token} 은 UNIQUE 라
     * 두 번째 발급이 409(DataIntegrityViolation)로 실패한다.
     *
     * <p>실제로 재현된다 — 로그인 버튼 연타, 여러 탭 동시 로그인, 로그인 직후 자동 갱신.
     *
     * <p>Access Token 에는 넣지 않는다. 저장하지도 UNIQUE 로 묶지도 않아 충돌할 일이 없고,
     * 클레임을 늘리면 매 요청 실려 가는 쿠키만 커진다.
     */
    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, refreshTokenValidityMs, UUID.randomUUID().toString());
    }

    /**
     * [4-1 조치] 인증 쿠키의 Max-Age 계산용.
     *
     * <p>쿠키 수명을 별도 상수로 두면 application.yml 의 토큰 수명과 반드시 어긋나므로
     * 항상 이 값에서 파생시킨다.
     */
    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    /** RefreshToken 엔티티의 expires_at 계산용. (쿠키 Max-Age 에도 사용) */
    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    /** jti 가 null 이면 붙이지 않는다(Access Token). */
    private String createToken(Long userId, String role, long validityMs, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry);

        if (jti != null) {
            builder.id(jti);
        }

        return builder
                .signWith(key) // 0.12.x부터는 key 타입으로 알고리즘이 추론되어 별도 SignatureAlgorithm 지정 불필요
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * [4-2 조치] 토큰 발급시각(iat). 로그아웃 이후 발급분인지 판정하는 데 쓴다.
     *
     * <p>JWT 의 iat 는 초 단위라 밀리초 이하는 버려진다. 경계값 처리는
     * {@code TokenInvalidationRegistry} 에서 무효화 기준 시각도 초 단위로 내려
     * "같은 초에 발급된 토큰은 살린다"로 맞춘다.
     */
    public LocalDateTime getIssuedAt(String token) {
        Date issuedAt = parseClaims(token).getIssuedAt();
        return issuedAt == null
                ? null
                : LocalDateTime.ofInstant(issuedAt.toInstant(), ZoneId.systemDefault());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
