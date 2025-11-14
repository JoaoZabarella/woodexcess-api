package com.z.c.woodexcess_api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "addresses", indexes = {
        @Index(name = "idx_addresses_user_id", columnList = "user_id"),
        @Index(name = "idx_addresses_zip_code", columnList = "zip_code"),
        @Index(name = "idx_addresses_active", columnList = "active")
})
@ToString(exclude = "user")
@EqualsAndHashCode(of = "id")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_addresses_user"))
    private User user;

    @NotBlank(message = "Street is required")
    @Size(min = 3, max = 255, message = "Street must be between 3 and 255 characters")
    @Column(name = "street", nullable = false)
    private String street;

    @NotBlank(message = "Number is required")
    @Size(max = 20, message = "Number must be at most 20 characters")
    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Size(max = 255, message = "Complement must be at most 255 characters")
    @Column(name = "complement")
    private String complement;

    @NotBlank(message = "District is required")
    @Size(min = 2, max = 100, message = "District must be between 2 and 100 characters")
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 2, message = "State must be exactly 2 characters (UF)")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State must be a valid Brazilian UF (e.g., SP, RJ)")
    @Column(name = "state", nullable = false, length = 2, columnDefinition = "VARCHAR(2)")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "ZIP code must be in format 12345-678 or 12345678")
    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
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
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getNormalizedZipCode(){
        return zipCode != null ? zipCode.replaceAll("[^0-9]", "") : null;
    }

    public String getFormatedZipCode(){
        String normalized = getNormalizedZipCode();
        if(normalized != null && normalized.length() == 8){
            return normalized.substring(0,5) + "-"  + normalized.substring(5);
        }
        return zipCode;
    }

    public String getFullAddress(){
        StringBuilder sb = new StringBuilder();
        sb.append(street).append(", ").append(number);

        if(complement != null && !complement.isBlank()){
            sb.append(" - ").append(complement);
        }

        sb.append(", ").append(district)
                .append(", ").append(city)
                .append(", ").append(state)
                .append(", ").append(getFormatedZipCode())
                .append(", ").append(country);
        return sb.toString();
    }


    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public void desactivate() {
        this.active = false;
        this.isPrimary = false;
    }

    public void isPrimary() {
        this.isPrimary = true;
    }

    public void removePrimary() {
        this.isPrimary = false;
    }


    @PrePersist
    protected void onCreate() {
        if (this.active == null) {
            this.active = true;
        }
        if (country == null || country.isBlank()) {
            country = "Brasil";
        }
        if (zipCode != null) {
            zipCode = getNormalizedZipCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (zipCode != null) {
            zipCode = getNormalizedZipCode();
        }
    }
}
