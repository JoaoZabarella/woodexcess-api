package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    Optional<Address> findByIdAndActiveTrue(UUID id);

    List<Address> findByUserIdAndActiveTrue(UUID userId);

    Optional<Address> findByUserIdAndIsPrimaryTrueAndActiveTrue(UUID userId);

    @Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId AND a.active = true")
    long countActiveAddressesByUser(@Param("userId") UUID userId);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.zipCode = :zipCode AND a.number = :number AND a.active = true")
    Optional<Address> findDuplicateAddress(
            @Param("userId") UUID userId,
            @Param("zipCode") String zipCode,
            @Param("number") String number
    );

    @Modifying
    @Query("UPDATE Address a SET a.isPrimary = false WHERE a.user.id = :userId")
    void removePrimaryFromAllUserAddresses(@Param("userId") UUID userId);
}
