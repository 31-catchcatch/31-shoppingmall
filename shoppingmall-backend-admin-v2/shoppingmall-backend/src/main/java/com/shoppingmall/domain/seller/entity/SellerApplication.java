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
        name = "seller_applications",
        indexes = {
                @Index(
                        name = "idx_seller_application_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_seller_application_status",
                        columnList = "status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 한 사용자가 반려 후 다시 신청할 수 있으므로
     * SellerApplication 입장에서는 ManyToOne이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
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

    @Column(
            name = "business_registration_file_url",
            nullable = false,
            length = 500
    )
    private String businessRegistrationFileUrl;

    @Column(
            name = "mail_order_report_file_url",
            length = 500
    )
    private String mailOrderReportFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20)"
    )
    @Builder.Default
    private SellerApplicationStatus status =
            SellerApplicationStatus.PENDING;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = SellerApplicationStatus.PENDING;
        }
    }

    /**
     * 관리자가 입점 신청을 승인한다.
     */
    public void approve() {
        this.status = SellerApplicationStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 관리자가 입점 신청을 반려한다.
     */
    public void reject(String rejectionReason) {
        this.status = SellerApplicationStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 사용자가 대기 중인 신청을 취소한다.
     */
    public void cancel() {
        this.status = SellerApplicationStatus.CANCELED;
    }

    /** PUT /api/v1/seller/me - 판매자 마이페이지에서 수정 가능한 사업자 정보 갱신 */
    public void updateProfile(String businessName, String representativeName,
                               String contactNumber, String businessAddress) {
        this.businessName = businessName;
        this.representativeName = representativeName;
        this.contactNumber = contactNumber;
        if (businessAddress != null && !businessAddress.isBlank()) {
            this.businessAddress = businessAddress;
        }
    }
}