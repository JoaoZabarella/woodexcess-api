package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.offer.*;
import com.z.c.woodexcess_api.model.enums.OfferStatus;
import com.z.c.woodexcess_api.security.CustomUserDetails;
import com.z.c.woodexcess_api.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Offers", description = "Offer management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create offer",
            description = "Buyer creates an offer for a listing",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Offer created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation"),
                    @ApiResponse(responseCode = "404", description = "Listing not found")
            }
    )

    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody CreateOfferRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("POST /api/offers - Creating offer for listing: {}, buyer: {}", request.listingId(), userDetails.getUsername());

        OfferResponse response = offerService.createOffer(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{offerId}/counter")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create counter-offer",
            description = "Seller creates a counter-offer for an existing offer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Counter-offer created successfully"),
                    @ApiResponse(responseCode = "400", description = "Offer cannot be countered"),
                    @ApiResponse(responseCode = "403", description = "Only seller can create counter-offer"),
                    @ApiResponse(responseCode = "404", description = "Offer not found")
            }
    )
    public ResponseEntity<OfferResponse> createCounterOffer(
            @Parameter(description = "Original offer ID")
            @PathVariable UUID offerId,
            @Valid @RequestBody CounterOfferRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        log.info("POST /api/offers/{}/counter - Creating counter-offer by seller: {}", offerId, userDetails.getUsername());

        OfferResponse response = offerService.createCounterOffer(offerId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }
    @PostMapping("/{offerId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Accept offer",
            description = "Seller accepts an offer (rejects all other pending offers for the listing)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offer accepted successfully"),
                    @ApiResponse(responseCode = "400", description = "Offer cannot be accepted"),
                    @ApiResponse(responseCode = "403", description = "Only seller can accept offer"),
                    @ApiResponse(responseCode = "404", description = "Offer not found")
            }
    )
    public ResponseEntity<OfferResponse> acceptOffer(
            @Parameter(description = "Offer ID to accept")
            @PathVariable UUID offerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/offers/{}/accept - Seller: {}", offerId, userDetails.getUsername());

        OfferResponse response = offerService.acceptOffer(offerId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{offerId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Reject offer",
            description = "Seller rejects an offer with optional reason",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offer rejected successfully"),
                    @ApiResponse(responseCode = "400", description = "Only pending offers can be rejected"),
                    @ApiResponse(responseCode = "403", description = "Only seller can reject offer"),
                    @ApiResponse(responseCode = "404", description = "Offer not found")
            }
    )
    public ResponseEntity<OfferResponse> rejectOffer(
            @Parameter(description = "Offer ID to reject")
            @PathVariable UUID offerId,
            @Valid @RequestBody RejectOfferRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/offers/{}/reject - Seller: {}", offerId, userDetails.getUsername());

        OfferResponse response = offerService.rejectOffer(offerId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{offerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cancel offer",
            description = "Buyer cancels their pending offer",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Offer cancelled successfully"),
                    @ApiResponse(responseCode = "400", description = "Only pending offers can be cancelled"),
                    @ApiResponse(responseCode = "403", description = "Only buyer can cancel offer"),
                    @ApiResponse(responseCode = "404", description = "Offer not found")
            }
    )
    public ResponseEntity<Void> cancelOffer(
            @Parameter(description = "Offer ID to cancel")
            @PathVariable UUID offerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/offers/{} - Buyer: {}", offerId, userDetails.getUsername());

        offerService.cancelOffer(offerId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{offerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get offer details",
            description = "Get complete offer details (only buyer or seller can access)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offer retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "User not authorized to view this offer"),
                    @ApiResponse(responseCode = "404", description = "Offer not found")
            }
    )
    public ResponseEntity<OfferResponse> getOffer(
            @Parameter(description = "Offer ID")
            @PathVariable UUID offerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/offers/{} - User: {}", offerId, userDetails.getUsername());

        OfferResponse response = offerService.getOfferById(offerId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sent")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get sent offers",
            description = "Get paginated list of offers sent by buyer (filterable by status)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offers retrieved successfully")
            }
    )
    public ResponseEntity<Page<OfferSummaryResponse>> getSentOffers(
            @Parameter(description = "Filter by offer status")
            @RequestParam(required = false) OfferStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/offers/sent - Buyer: {}, Status: {}", userDetails.getUsername(), status);

        Page<OfferSummaryResponse> response = offerService.getOffersSent(userDetails.getId(), status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get received offers",
            description = "Get paginated list of offers received by seller (filterable by status)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offers retrieved successfully")
            }
    )
    public ResponseEntity<Page<OfferSummaryResponse>> getReceivedOffers(
            @Parameter(description = "Filter by offer status")
            @RequestParam(required = false) OfferStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/offers/received - Seller: {}, Status: {}", userDetails.getUsername(), status);

        Page<OfferSummaryResponse> response = offerService.getOffersReceived(userDetails.getId(), status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{offerId}/chain")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get offer chain",
            description = "Get complete negotiation history (original offer + counter-offers)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offer chain retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "User not authorized to view this offer chain"),
                    @ApiResponse(responseCode = "404", description = "Offer not found")
            }
    )
    public ResponseEntity<List<OfferResponse>> getOfferChain(
            @Parameter(description = "Offer ID")
            @PathVariable UUID offerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET  /api/offers/{}/chain - User: {}", offerId, userDetails.getUsername());

        List<OfferResponse> response = offerService.getOfferChain(offerId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get pending offers count",
            description = "Get count of pending offers sent and received by user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stats retrieved successfully")
            }
    )
    public ResponseEntity<PendingOffersStats> getPendingStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/offers/stats/pending - User: {}", userDetails.getUsername());

        long sent = offerService.countPendingOffersSent(userDetails.getId());
        long received = offerService.countPendingOffersReceived(userDetails.getId());

        PendingOffersStats stats = new PendingOffersStats(sent, received);
        return ResponseEntity.ok(stats);
    }

    public record PendingOffersStats(
            @Parameter(description = "Number of pending offers sent by user")
            long sent,

            @Parameter(description = "Number of pending offers received by user")
            long received
    ) {}
}
