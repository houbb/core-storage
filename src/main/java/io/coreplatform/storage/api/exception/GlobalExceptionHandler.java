package io.coreplatform.storage.api.exception;

import io.coreplatform.storage.application.service.StorageAccessService;
import io.coreplatform.storage.application.service.StorageAccessService.AccessDeniedException;
import io.coreplatform.storage.application.service.StorageImageService;
import io.coreplatform.storage.application.service.StorageMetadataService;
import io.coreplatform.storage.application.service.StorageProfileService;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.application.service.StorageService;
import io.coreplatform.storage.application.service.ImagePipeline;
import io.coreplatform.storage.infrastructure.driver.DriverRegistry;
import io.coreplatform.storage.infrastructure.driver.StorageDriverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StorageService.FileNotFoundException.class)
    public ProblemDetail handleFileNotFound(StorageService.FileNotFoundException ex) {
        log.warn("File not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("File not found");
        pd.setType(URI.create("https://core-platform.dev/problems/file-not-found"));
        pd.setProperty("errorCode", "STORAGE_FILE_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageMetadataService.MetadataNotFoundException.class)
    public ProblemDetail handleMetadataNotFound(StorageMetadataService.MetadataNotFoundException ex) {
        log.warn("Metadata not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Metadata not found");
        pd.setType(URI.create("https://core-platform.dev/problems/metadata-not-found"));
        pd.setProperty("errorCode", "STORAGE_METADATA_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageMetadataService.ReferenceNotFoundException.class)
    public ProblemDetail handleReferenceNotFound(StorageMetadataService.ReferenceNotFoundException ex) {
        log.warn("Reference not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Reference not found");
        pd.setType(URI.create("https://core-platform.dev/problems/reference-not-found"));
        pd.setProperty("errorCode", "STORAGE_REFERENCE_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: mode={}, operation={}", ex.getMode(), ex.getOperation());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Access denied");
        pd.setType(URI.create("https://core-platform.dev/problems/access-denied"));
        pd.setProperty("errorCode", "STORAGE_ACCESS_DENIED");
        pd.setProperty("accessMode", ex.getMode().name());
        pd.setProperty("operation", ex.getOperation());
        return pd;
    }

    @ExceptionHandler(StorageResourceService.ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(StorageResourceService.ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        pd.setType(URI.create("https://core-platform.dev/problems/resource-not-found"));
        pd.setProperty("errorCode", "STORAGE_RESOURCE_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageImageService.ImageNotFoundException.class)
    public ProblemDetail handleImageNotFound(StorageImageService.ImageNotFoundException ex) {
        log.warn("Image not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Image not found");
        pd.setType(URI.create("https://core-platform.dev/problems/image-not-found"));
        pd.setProperty("errorCode", "STORAGE_IMAGE_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageImageService.VariantNotFoundException.class)
    public ProblemDetail handleVariantNotFound(StorageImageService.VariantNotFoundException ex) {
        log.warn("Variant not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Variant not found");
        pd.setType(URI.create("https://core-platform.dev/problems/variant-not-found"));
        pd.setProperty("errorCode", "STORAGE_VARIANT_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(ImagePipeline.ImageTooLargeException.class)
    public ProblemDetail handleImageTooLarge(ImagePipeline.ImageTooLargeException ex) {
        log.warn("Image too large: {}x{}, max={}", ex.getWidth(), ex.getHeight(), ex.getMaxDimension());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "Image dimension " + ex.getWidth() + "x" + ex.getHeight()
                        + " exceeds maximum " + ex.getMaxDimension());
        pd.setTitle("Image too large");
        pd.setType(URI.create("https://core-platform.dev/problems/image-too-large"));
        pd.setProperty("errorCode", "STORAGE_IMAGE_TOO_LARGE");
        pd.setProperty("width", ex.getWidth());
        pd.setProperty("height", ex.getHeight());
        pd.setProperty("maxDimension", ex.getMaxDimension());
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit");
        pd.setTitle("Upload too large");
        pd.setType(URI.create("https://core-platform.dev/problems/upload-too-large"));
        pd.setProperty("errorCode", "STORAGE_UPLOAD_TOO_LARGE");
        return pd;
    }

    // ---- P5: Driver & Profile exceptions ----

    @ExceptionHandler(DriverRegistry.DriverNotFoundException.class)
    public ProblemDetail handleDriverNotFound(DriverRegistry.DriverNotFoundException ex) {
        log.warn("Driver not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Driver not found");
        pd.setType(URI.create("https://core-platform.dev/problems/driver-not-found"));
        pd.setProperty("errorCode", "STORAGE_DRIVER_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageDriverFactory.ProfileNotFoundException.class)
    public ProblemDetail handleProfileNotFound(StorageDriverFactory.ProfileNotFoundException ex) {
        log.warn("Profile not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Profile not found");
        pd.setType(URI.create("https://core-platform.dev/problems/profile-not-found"));
        pd.setProperty("errorCode", "STORAGE_PROFILE_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageProfileService.ProfileNotFoundException.class)
    public ProblemDetail handleProfileServiceNotFound(StorageProfileService.ProfileNotFoundException ex) {
        log.warn("Profile not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Profile not found");
        pd.setType(URI.create("https://core-platform.dev/problems/profile-not-found"));
        pd.setProperty("errorCode", "STORAGE_PROFILE_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageProfileService.ProfileAlreadyExistsException.class)
    public ProblemDetail handleProfileAlreadyExists(StorageProfileService.ProfileAlreadyExistsException ex) {
        log.warn("Profile already exists: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Profile already exists");
        pd.setType(URI.create("https://core-platform.dev/problems/profile-already-exists"));
        pd.setProperty("errorCode", "STORAGE_PROFILE_ALREADY_EXISTS");
        return pd;
    }

    @ExceptionHandler(StorageProfileService.InvalidDriverException.class)
    public ProblemDetail handleInvalidDriver(StorageProfileService.InvalidDriverException ex) {
        log.warn("Invalid driver: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid driver");
        pd.setType(URI.create("https://core-platform.dev/problems/invalid-driver"));
        pd.setProperty("errorCode", "STORAGE_INVALID_DRIVER");
        return pd;
    }

    @ExceptionHandler(StorageProfileService.CannotDeleteDefaultProfileException.class)
    public ProblemDetail handleCannotDeleteDefault(StorageProfileService.CannotDeleteDefaultProfileException ex) {
        log.warn("Cannot delete default profile: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Cannot delete default profile");
        pd.setType(URI.create("https://core-platform.dev/problems/cannot-delete-default-profile"));
        pd.setProperty("errorCode", "STORAGE_CANNOT_DELETE_DEFAULT_PROFILE");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("https://core-platform.dev/problems/internal-error"));
        pd.setProperty("errorCode", "STORAGE_INTERNAL_ERROR");
        return pd;
    }
}