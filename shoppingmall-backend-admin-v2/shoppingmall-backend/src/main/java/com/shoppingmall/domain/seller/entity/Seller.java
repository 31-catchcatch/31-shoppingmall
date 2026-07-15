package com.shoppingmall.domain.seller.entity;

import com.shoppingmall.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sellers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seller_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_seller_business_registration_number",
                        columnNames = "business_registration_number"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 사용자 한 명당 판매자 계정 하나를 가진다는 기준이다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(
            name = "business_name",
            nullable = false,
            length = 100
    )
    private String businessName;

    @Column(
            name = "business_registration_number",
            nullable = false,
            unique = true,
            length = 10
    )
    private String businessRegistrationNumber;

    @Column(
            name = "representative_name",
            nullable = false,
            length = 50
    )
    private String representativeName;

    @Column(
            name = "contact_number",
            nullable = false,
            length = 20
    )
    private String contactNumber;

    @Column(
            name = "business_address",
            nullable = false,
            length = 255
    )
    private String businessAddress;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private SellerStatus status = SellerStatus.ACTIVE;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = SellerStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = SellerStatus.ACTIVE;
    }

    public void suspend() {
        this.status = SellerStatus.SUSPENDED;
    }

    public void close() {
        this.status = SellerStatus.CLOSED;
    }

    public void forceClose() {
        this.status = SellerStatus.FORCED_CLOSED;
    }
}