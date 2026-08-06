package com.shoppingmall.domain.qna.service;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.qna.dto.request.QnaCreateRequest;
import com.shoppingmall.domain.qna.dto.response.QnaListResponse;
import com.shoppingmall.domain.qna.dto.response.QnaResponse;
import com.shoppingmall.domain.qna.entity.Qna;
import com.shoppingmall.domain.qna.repository.QnaRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일반 사용자 상품 문의 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * 특정 상품의 문의 목록 조회.
     * currentUserId가 null(비로그인)이거나 본인 글이 아니면 비밀글은 마스킹 처리된다.
     */
    public QnaListResponse getProductQnaList(Long productId, Long currentUserId, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        Page<Qna> qnaPage = qnaRepository
                .findAllByProduct_IdAndDeletedFalseOrderByCreatedAtDesc(productId, pageable);

        Page<QnaResponse> masked = qnaPage.map(qna -> QnaResponse.from(qna, currentUserId));
        return new QnaListResponse(
                masked.getContent(),
                masked.getNumber(),
                masked.getSize(),
                masked.getTotalElements(),
                masked.getTotalPages()
        );
    }

    /** 상품 문의 등록 */
    @Transactional
    public QnaResponse createQna(Long userId, Long productId, QnaCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 판매중지 상품에는 신규 문의를 막는다. (기존 문의 조회는 그대로 허용)
        if (!product.isOnSale()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }

        Qna qna = Qna.builder()
                .user(user)
                .product(product)
                .title(request.title())
                .content(request.content())
                .secret(request.secret())
                .build();

        Qna savedQna = qnaRepository.save(qna);
        return QnaResponse.from(savedQna); // 작성 직후 응답은 본인 글이므로 마스킹 불필요
    }
}
