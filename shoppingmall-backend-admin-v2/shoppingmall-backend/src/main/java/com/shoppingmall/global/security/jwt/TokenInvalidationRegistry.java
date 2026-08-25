package com.shoppingmall.global.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * [4-2 조치] 로그아웃한 사용자의 Access Token 을 무효로 만든다.
 *
 * <p>JWT 는 stateless 라 발급 후 서버가 관여하지 않는다. 로그아웃이 Refresh Token 만 지우면
 * 이미 발급된 Access Token 은 남은 유효기간 동안 그대로 통해서, 토큰이 탈취된 사용자가
 * 스스로 끊어낼 방법이 없었다. 무효화 기준 시각을 남겨 두고 토큰의 발급시각(iat)과 비교해
 * "기준 시각 이전에 발급된 토큰"을 거부한다.
 *
 * <p><b>왜 DB 컬럼이 아니라 메모리인가.</b> 이 기록의 수명은 <b>Access Token 유효기간이면
 * 충분하다</b>. 기준 시각 T 에 무효화하면 대상은 {@code iat <= T} 인 토큰뿐이고, 그 토큰들은
 * 늦어도 {@code T + 유효기간} 에 스스로 만료한다. 그 뒤로는 기록을 지워도 판정 결과가
 * 달라지지 않는다. 즉 영구 보관할 이유가 없는 임시 정보라서, 스키마를 늘리는 대신
 * 만료 캐시로 들고 있는다. (같은 이유로 {@code OrderDraftStore}·{@code IpRateLimitFilter} 도
 * 이 방식을 쓴다)
 *
 * <p><b>한계.</b> WAS 재기동 시 기록이 사라져, 재기동 직전 유효기간 이내에 로그아웃한
 * 사용자의 옛 토큰이 남은 기간 동안 다시 통한다(최대 15분). 기동 시각 이후 발급분만
 * 인정하면 이 창을 없앨 수 있지만 배포마다 전 사용자가 로그아웃되므로 택하지 않았다.
 * 또한 인스턴스별로 기록이 갈리므로 WAS 를 늘리면 이 클래스를 공유 저장소 기반으로
 * 바꿔야 한다 — 판정이 이 한 곳에 모여 있어 교체 지점은 여기뿐이다.
 */
@Component
public class TokenInvalidationRegistry {

    private final Cache<Long, LocalDateTime> invalidatedAt;

    public TokenInvalidationRegistry(
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs) {
        // 유효기간을 상수로 박지 않고 설정에서 읽는다. 토큰 수명을 조정해도 따라온다.
        this.invalidatedAt = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(accessTokenValidityMs))
                .maximumSize(100_000)
                .build();
    }

    /**
     * 지금까지 발급된 이 사용자의 Access Token 을 전부 무효로 만든다.
     *
     * <p>기준 시각을 <b>초 단위로 내림</b>해서 기록한다. JWT 의 iat 는 초 단위로 잘려 오므로,
     * 밀리초까지 기록해 두면 로그아웃과 같은 초에 재로그인한 <b>새 토큰</b>이 곧바로
     * 무효로 잡힌다(발급시각이 절삭되어 기준보다 앞서 보인다).
     */
    public void invalidate(Long userId) {
        if (userId == null) {
            return;
        }
        invalidatedAt.put(userId, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * 전달된 발급시각(iat)의 토큰이 무효화 대상인지 판정한다.
     *
     * <p>같은 초에 발급된 토큰은 살린다. 위 절삭과 짝이 되는 처리로, 로그아웃 직후
     * 재로그인이 즉시 막히는 것을 피하기 위해서다. 대신 로그아웃과 같은 초에 발급된
     * 토큰이 살아남는 1초 미만의 창이 생긴다.
     */
    public boolean isInvalidated(Long userId, LocalDateTime issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }

        LocalDateTime at = invalidatedAt.getIfPresent(userId);
        return at != null && issuedAt.isBefore(at);
    }
}
