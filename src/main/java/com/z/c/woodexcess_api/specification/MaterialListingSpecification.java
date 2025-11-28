package com.z.c.woodexcess_api.specification;


import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.enums.MaterialType;
import com.z.c.woodexcess_api.model.MaterialListing;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MaterialListingSpecification{

    public static Specification<MaterialListing> withFilters(
            ListingStatus status,
            MaterialType materialType,
            String city,
            String state,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Condition condition
    ){

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if(materialType != null) {
                predicates.add(criteriaBuilder.equal(root.get("materialType"), materialType));
            }

            if(city != null &&  !city.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("city")),
                        "%" + city.toLowerCase() + "%"));
            }

            if(state != null &&  !state.isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("state")),
                        state.toLowerCase()
                ));
            }

            if(minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if(maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if(condition != null) {
                predicates.add(criteriaBuilder.equal(root.get("condition"), condition));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    }
}
