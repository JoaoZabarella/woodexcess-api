package com.z.c.woodexcess_api.exception;

import com.z.c.woodexcess_api.dto.error.ErrorResponse;
import com.z.c.woodexcess_api.exception.address.AddressNotFoundException;
import com.z.c.woodexcess_api.exception.auth.RefreshTokenException;
import com.z.c.woodexcess_api.exception.auth.TokenReuseDetectedException;
import com.z.c.woodexcess_api.exception.listing.*;
import com.z.c.woodexcess_api.exception.message.MessageNotFoundException;
import com.z.c.woodexcess_api.exception.storage.FileStorageException;
import com.z.c.woodexcess_api.exception.users.UserNotFoundException;
import com.z.c.woodexcess_api.exception.users.EmailAlreadyExistException;
import com.z.c.woodexcess_api.exception.users.PasswordIncorrectException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.warn("Validation error on path: {}: {}", request.getRemoteAddr());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad request data: " + ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.error("Resource not found on path {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not Found:",
                request.getRequestURI()
        );

    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        log.warn("Business rule violation on path {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );

    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Bad credentials attempt from IP: {}", request.getRemoteAddr());

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(PasswordIncorrectException.class)
    public ResponseEntity<ErrorResponse> handlePasswordIncorrect(
            PasswordIncorrectException ex,
            HttpServletRequest request) {

        log.warn("Password incorrect attempt from IP: {}", request.getRemoteAddr());

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenException(
            RefreshTokenException ex,
            HttpServletRequest request) {

        log.warn("Refresh token error: {} from IP: {}", ex.getMessage(), request.getRemoteAddr());

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TokenReuseDetectedException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuseDetected(
            TokenReuseDetectedException ex,
            HttpServletRequest request) {

        log.error("SECURITY ALERT: Token reuse detected from IP: {} - Details: {}",
                request.getRemoteAddr(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Security violation detected. All sessions have been revoked.",
                request.getRequestURI()
        );
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String parameterName = ex.getParameterName();
        String parameterType = ex.getParameterType();

        log.warn("Missing required request parameter '{}' of type {} on path: {}",
                parameterName, parameterType, request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(String.format("Required parameter '%s' is not present", parameterName))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed for {} fields on path: {}",
                validationErrors.size(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed. Check 'validationErrors' for details.")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument: {} on path: {}", ex.getMessage(), request.getRequestURI());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidImageFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImageFormat(
            InvalidImageFormatException ex,
            HttpServletRequest request) {

        log.warn("Invalid image format: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid image format. Supported formats: JPG, JPEG, PNG, WEBP",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MaxImagesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxImagesExceeded(
            MaxImagesExceededException ex,
            HttpServletRequest request) {

        log.warn("Max images exceeded: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Maximum number of images (5) exceeded for this listing",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ListingImageException.class)
    public ResponseEntity<ErrorResponse> handleListingImageException(
            ListingImageException ex,
            HttpServletRequest request) {

        log.warn("Listing image validation error: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Entity not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        log.warn("User not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAddressNotFound(
            AddressNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Address not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Address not found. Please verify the CEP and try again.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleListingNotFound(
            ListingNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Listing not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotFound(
            MessageNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Message not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistException ex,
            HttpServletRequest request) {

        log.warn("Email already exists: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.warn("File size exceeded maximum allowed limit from IP: {}", request.getRemoteAddr());

        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds maximum allowed limit (10MB per file)",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ErrorResponse> handleImageUploadException(
            ImageUploadException ex,
            HttpServletRequest request) {

        log.error("Image upload error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to upload image. Please try again later.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(
            FileStorageException ex,
            HttpServletRequest request) {

        log.error("File storage error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "File storage system error. Please contact support.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred on path: {}", request.getRequestURI(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}
