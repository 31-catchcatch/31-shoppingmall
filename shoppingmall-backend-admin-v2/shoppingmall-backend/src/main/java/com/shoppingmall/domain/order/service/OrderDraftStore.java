package com.shoppingmall.domain.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * [1-3 조치] 주문 초안 보관소.
 *
 * <p>기존에는 결제 요청에 productId·optionId·quantity 가 실려 왔기 때문에, 주문서 화면에
 * 표시된 내용과 실제로 생성되는 주문이 달라질 수 있었다. 서버는 그 값이 화면에 뜬 것과
 * 같은지 알 방법이 없어 검증 대상 자체가 없었다.
 *
 * <p>이제 주문서 진입 시점에 서버가 대상과 금액을 확정해 여기에 보관하고, 결제 요청에는
 * draftId 만 싣는다. <b>변조할 파라미터가 존재하지 않는다.</b>
 *
 * <p>consume() 은 조회와 동시에 제거하므로 같은 초안으로 두 번 주문할 수 없다.
 */
@Component
public class OrderDraftStore {

    /** 주문서에 머무를 수 있는 시간. 초과하면 상품을 다시 선택해야 한다. */
    private static final Duration TTL = Duration.ofMinutes(30);

    public record DraftItem(Long productId, Long optionId, int quantity, int unitPrice) {}

    public record Draft(Long userId, List<DraftItem> items,
                        int totalProductAmount, int shippingFee) {}

    private final Cache<String, Draft> drafts = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(50_000)
            .build();

    public String put(Draft draft) {
        String draftId = UUID.randomUUID().toString();
        drafts.put(draftId, draft);
        return draftId;
    }

    /** 화면 표시용 조회. 제거하지 않는다. */
    public Draft peek(String draftId, Long userId) {
        Draft draft = drafts.getIfPresent(draftId);
        return validate(draft, userId);
    }

    /** 주문 확정용 조회. 꺼내면서 제거하므로 재사용이 불가능하다. */
    public Draft consume(String draftId, Long userId) {
        Draft draft = drafts.asMap().remove(draftId);
        return validate(draft, userId);
    }

    private Draft validate(Draft draft, Long userId) {
        if (draft == null) {
            throw new CustomException(ErrorCode.ORDER_DRAFT_EXPIRED);
        }
        // 남의 draftId 를 알아내도 쓸 수 없다.
        if (!draft.userId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return draft;
    }
}

