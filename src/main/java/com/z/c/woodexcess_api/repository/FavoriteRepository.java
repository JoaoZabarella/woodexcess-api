package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUserAndListing(User user, MaterialListing listing);

    Optional<Favorite> findByUserAndListing(User user, MaterialListing listing);

    @Query("""
            SELECT f FROM Favorite f
            JOIN FETCH f.listing l 
            LEFT JOIN FETCH l.images img
            WHERE f.user = :user
            AND l.status = 'ACTIVE'
            ORDER BY f.createdAt DESC            
            """)
    Page<Favorite> finndByUserOrderByCreatedAtDesc(@Param("user") User user,
                                                   Pageable page);

    long countByListing(MaterialListing listing);

    long countByUser(User user);

    void deleteByUserAndListing(User user, MaterialListing listing);
}
