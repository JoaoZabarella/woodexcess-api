package com.z.c.woodexcess_api.model;

import com.z.c.woodexcess_api.model.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private MaterialListing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private  User seller;

    @Column(name = "offer_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal offeredPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name= "status", nullable = false, columnDefinition = "offer_status")
    private OfferStatus status = OfferStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_offer_id")
    private Offer parentOffer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        if(expiresAt == null){
            expiresAt = LocalDateTime.now().plusHours(48);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == OfferStatus.PENDING;
    }

    public boolean canBeAccepted(){
        return status == OfferStatus.PENDING && !isExpired();
    }

    public boolean canBeRejected(){
        return status == OfferStatus.PENDING && !isExpired();
    }

}
