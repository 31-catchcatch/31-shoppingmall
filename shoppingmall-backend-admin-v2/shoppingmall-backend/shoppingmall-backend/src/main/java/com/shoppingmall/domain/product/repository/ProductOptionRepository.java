package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    /**
     * 재고를 원자적으로 차감한다. (주문 생성 시 오버셀링 방지)
     *
     * 조건부 UPDATE(stock >= qty)라 동시 요청에도 재고 미만으로는 절대 내려가지 않는다.
     * 영향받은 행 수를 반환하므로, 0이면 "재고 부족"으로 판정한다 (별도 SELECT 검증 불필요).
     *
     * 벌크 UPDATE라 영속성 컨텍스트를 우회한다. 호출 측(placeOrder)은 차감 후 이 옵션의
     * stockQuantity 값을 다시 읽지 않으므로 clearAutomatically 는 두지 않는다
     * (두면 같은 트랜잭션에서 이미 로드한 다른 엔티티까지 detach 되어 주문 저장이 깨질 수 있다).
     */
    @Modifying
    @Query("""
            update ProductOption o
               set o.stockQuantity = o.stockQuantity - :quantity
             where o.id = :optionId
               and o.stockQuantity >= :quantity
            """)
    int decreaseStock(@Param("optionId") Long optionId, @Param("quantity") int quantity);

    /**
     * 재고를 복원한다. (환불 완료 시 반품된 수량만큼 되돌림)
     * 증가 연산이라 조건은 없다.
     */
    @Modifying
    @Query("""
            update ProductOption o
               set o.stockQuantity = o.stockQuantity + :quantity
             where o.id = :optionId
            """)
    int restoreStock(@Param("optionId") Long optionId, @Param("quantity") int quantity);
}
