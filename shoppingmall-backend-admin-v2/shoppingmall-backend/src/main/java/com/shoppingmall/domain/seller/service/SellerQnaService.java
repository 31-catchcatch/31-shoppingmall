package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.qna.entity.Qna;
import com.shoppingmall.domain.qna.repository.QnaRepository;
import com.shoppingmall.domain.seller.dto.request.SellerQnaAnswerRequest;
import com.shoppingmall.domain.seller.dto.request.SellerQnaSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerQnaAnswerResponse;
import com.shoppingmall.domain.seller.dto.response.SellerQnaResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
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

/**
 * 판매자 상품 문의(Q&A) 관리 서비스
 *
 * 담당 API
 * GET    /api/v1/seller/qna
 * POST   /api/v1/seller/qna/{qnaId}/answers
 * DELETE /api/v1/seller/qna/{qnaId}
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerQnaService {

    private final QnaRepository qnaRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final UserRepository userRepository;

    /**
     * 판매자 상품에 등록된 Q&A 목록을 조회한다.
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 페이지 번호와 크기 보정
     * 3. 상품 ID 및 답변 여부에 따라 Q&A 조회
     * 4. 응답 DTO로 변환
     */
    public Page<SellerQnaResponse> getQnaList(
            Long userId,
            SellerQnaSearchRequest request
    ) {
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        int page = request.page() == null
                ? 0
                : Math.max(request.page(), 0);

        int size = request.size() == null
                ? 20
                : Math.min(Math.max(request.size(), 1), 100);

        Pageable pageable = PageRequest.of(page, size);

        Page<Qna> qnaPage;

        /*
         * 상품 ID와 답변 여부가 모두 전달된 경우
         */
        if (request.productId() != null
                && request.answered() != null) {

            qnaPage = qnaRepository
                    .findAllByProduct_Seller_IdAndProduct_IdAndAnsweredAndDeletedFalseOrderByCreatedAtDesc(
                            seller.getId(),
                            request.productId(),
                            request.answered(),
                            pageable
                    );

            /*
             * 상품 ID만 전달된 경우
             */
        } else if (request.productId() != null) {

            qnaPage = qnaRepository
                    .findAllByProduct_Seller_IdAndProduct_IdAndDeletedFalseOrderByCreatedAtDesc(
                            seller.getId(),
                            request.productId(),
                            pageable
                    );

            /*
             * 답변 여부만 전달된 경우
             */
        } else if (request.answered() != null) {

            qnaPage = qnaRepository
                    .findAllByProduct_Seller_IdAndAnsweredAndDeletedFalseOrderByCreatedAtDesc(
                            seller.getId(),
                            request.answered(),
                            pageable
                    );

            /*
             * 필터가 없는 경우 판매자의 전체 Q&A 조회
             */
        } else {

            qnaPage = qnaRepository
                    .findAllByProduct_Seller_IdAndDeletedFalseOrderByCreatedAtDesc(
                            seller.getId(),
                            pageable
                    );
        }

        return qnaPage.map(SellerQnaResponse::from);
    }

    /**
     * 고객 문의에 답변을 등록하거나 기존 답변을 수정한다.
     *
     * 답변이 없으면 새 답변을 생성하고,
     * 답변이 이미 있으면 내용을 수정한다.
     */
    @Transactional
    public SellerQnaAnswerResponse registerAnswer(
            Long userId,
            Long qnaId,
            SellerQnaAnswerRequest request
    ) {
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        Qna qna = qnaRepository.findByIdAndDeletedFalse(qnaId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.QNA_NOT_FOUND)
                );

        validateQnaOwnership(qna, seller);

        User answerer = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        qna.registerAnswer(request.content(), answerer);

        return SellerQnaAnswerResponse.from(qna);
    }

    /**
     * 부적절한 Q&A를 논리 삭제한다.
     *
     * 실제 DB 행을 삭제하지 않고 deleted 값을 true로 변경한다.
     */
    @Transactional
    public void deleteQna(
            Long userId,
            Long qnaId
    ) {
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        Qna qna = qnaRepository.findByIdAndDeletedFalse(qnaId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.QNA_NOT_FOUND)
                );

        validateQnaOwnership(qna, seller);

        qna.softDelete();
    }

    /**
     * 사용자의 가장 최근 입점 신청이 승인 상태인지 확인한다.
     */
    private SellerApplication getApprovedSellerApplication(
            Long userId
    ) {
        SellerApplication application =
                sellerApplicationRepository
                        .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SELLER_NOT_APPROVED
                                )
                        );

        if (application.getStatus()
                != SellerApplicationStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.SELLER_NOT_APPROVED
            );
        }

        return application;
    }

    /**
     * Q&A가 현재 판매자의 상품에 등록된 문의인지 확인한다.
     */
    private void validateQnaOwnership(
            Qna qna,
            SellerApplication seller
    ) {
        Long qnaSellerId = qna
                .getProduct()
                .getSeller()
                .getId();

        if (!qnaSellerId.equals(seller.getId())) {
            throw new CustomException(
                    ErrorCode.QNA_ACCESS_DENIED
            );
        }
    }
}