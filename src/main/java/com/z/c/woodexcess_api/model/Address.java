package com.z.c.woodexcess_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_address_user_id", columnList = "user_id"),
                @Index(name = "idx_address_zip_code", columnList = "zip_code"),
                @Index(name = "idx_address_active", columnList = "active"),
                @Index(name = "idx_address_primary", columnList = "is_primary")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_zip_number",
                        columnNames = {"user_id", "zip_code", "number"}
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_address_user"))
    private User user;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "complement", length = 255)
    private String complement;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 9)
    private String zipCode;

    @Column(name = "country", nullable = false, length = 50)
    @Builder.Default
    private String country = "Brasil";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Transient
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();

        sb.append(street).append(", ").append(number);

        if (complement != null && !complement.isBlank()) {
            sb.append(" - ").append(complement);
        }

        sb.append(", ").append(district)
                .append(", ").append(city)
                .append(" - ").append(state)
                .append(", ").append(zipCode)
                .append(", ").append(country);

        return sb.toString();
    }

    @Transient
    public String getFormatedZipCode() {
        if (zipCode == null || zipCode.length() != 9) {
            return zipCode;
        }
        return zipCode.substring(0, 5) + "-" + zipCode.substring(5);
    }


    public void setPrimary(Boolean primary) {
        this.isPrimary = primary;
    }


    public void desactivate() {
        this.active = false;
        this.isPrimary = false;
    }


    public void activate() {
        this.active = true;
    }


    @Transient
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }


    @Transient
    public boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimary);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return id != null && id.equals(address.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", street='" + street + '\'' +
                ", number='" + number + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", active=" + active +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
