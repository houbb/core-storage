package io.coreplatform.storage.api.exception;

import io.coreplatform.storage.application.service.ReplicationService;
import io.coreplatform.storage.application.service.StorageAccessService;
import io.coreplatform.storage.application.service.StorageAccessService.AccessDeniedException;
import io.coreplatform.storage.application.service.StorageImageService;
import io.coreplatform.storage.application.service.StorageMetadataService;
import io.coreplatform.storage.application.service.StorageProfileService;
import io.coreplatform.storage.application.service.StorageResourceService;
import io.coreplatform.storage.application.service.StorageService;
import io.coreplatform.storage.application.service.StorageVersionService;
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

    // ---- P6: Replication exceptions ----

    @ExceptionHandler(ReplicationService.ReplicaNotFoundException.class)
    public ProblemDetail handleReplicaNotFound(ReplicationService.ReplicaNotFoundException ex) {
        log.warn("Replica not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Replica not found");
        pd.setType(URI.create("https://core-platform.dev/problems/replica-not-found"));
        pd.setProperty("errorCode", "STORAGE_REPLICA_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(ReplicationService.ReplicaAlreadyExistsException.class)
    public ProblemDetail handleReplicaAlreadyExists(ReplicationService.ReplicaAlreadyExistsException ex) {
        log.warn("Replica already exists: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Replica already exists");
        pd.setType(URI.create("https://core-platform.dev/problems/replica-already-exists"));
        pd.setProperty("errorCode", "STORAGE_REPLICA_ALREADY_EXISTS");
        return pd;
    }

    @ExceptionHandler(ReplicationService.CannotDeletePrimaryReplicaException.class)
    public ProblemDetail handleCannotDeletePrimary(ReplicationService.CannotDeletePrimaryReplicaException ex) {
        log.warn("Cannot delete primary replica: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Cannot delete primary replica");
        pd.setType(URI.create("https://core-platform.dev/problems/cannot-delete-primary-replica"));
        pd.setProperty("errorCode", "STORAGE_CANNOT_DELETE_PRIMARY_REPLICA");
        return pd;
    }

    @ExceptionHandler(ReplicationService.SyncTaskNotFoundException.class)
    public ProblemDetail handleSyncTaskNotFound(ReplicationService.SyncTaskNotFoundException ex) {
        log.warn("Sync task not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Sync task not found");
        pd.setType(URI.create("https://core-platform.dev/problems/sync-task-not-found"));
        pd.setProperty("errorCode", "STORAGE_SYNC_TASK_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(ReplicationService.SyncTaskAlreadyRunningException.class)
    public ProblemDetail handleSyncTaskAlreadyRunning(ReplicationService.SyncTaskAlreadyRunningException ex) {
        log.warn("Sync task already running: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Sync task conflict");
        pd.setType(URI.create("https://core-platform.dev/problems/sync-task-conflict"));
        pd.setProperty("errorCode", "STORAGE_SYNC_TASK_CONFLICT");
        return pd;
    }

    @ExceptionHandler(ReplicationService.InvalidReplicationTargetException.class)
    public ProblemDetail handleInvalidReplicationTarget(ReplicationService.InvalidReplicationTargetException ex) {
        log.warn("Invalid replication target: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid replication target");
        pd.setType(URI.create("https://core-platform.dev/problems/invalid-replication-target"));
        pd.setProperty("errorCode", "STORAGE_INVALID_REPLICATION_TARGET");
        return pd;
    }

    // ---- P7: Version exceptions ----

    @ExceptionHandler(StorageVersionService.VersionNotFoundException.class)
    public ProblemDetail handleVersionNotFound(StorageVersionService.VersionNotFoundException ex) {
        log.warn("Version not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Version not found");
        pd.setType(URI.create("https://core-platform.dev/problems/version-not-found"));
        pd.setProperty("errorCode", "STORAGE_VERSION_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageVersionService.InvalidVersionStateException.class)
    public ProblemDetail handleInvalidVersionState(StorageVersionService.InvalidVersionStateException ex) {
        log.warn("Invalid version state: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Invalid version state");
        pd.setType(URI.create("https://core-platform.dev/problems/invalid-version-state"));
        pd.setProperty("errorCode", "STORAGE_INVALID_VERSION_STATE");
        return pd;
    }

    @ExceptionHandler(StorageVersionService.VersionAlreadyPublishedException.class)
    public ProblemDetail handleVersionAlreadyPublished(StorageVersionService.VersionAlreadyPublishedException ex) {
        log.warn("Version already published: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Version already published");
        pd.setType(URI.create("https://core-platform.dev/problems/version-already-published"));
        pd.setProperty("errorCode", "STORAGE_VERSION_ALREADY_PUBLISHED");
        return pd;
    }

    @ExceptionHandler(StorageVersionService.AliasNotFoundException.class)
    public ProblemDetail handleAliasNotFound(StorageVersionService.AliasNotFoundException ex) {
        log.warn("Alias not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Alias not found");
        pd.setType(URI.create("https://core-platform.dev/problems/alias-not-found"));
        pd.setProperty("errorCode", "STORAGE_ALIAS_NOT_FOUND");
        return pd;
    }

    @ExceptionHandler(StorageVersionService.AliasAlreadyExistsException.class)
    public ProblemDetail handleAliasAlreadyExists(StorageVersionService.AliasAlreadyExistsException ex) {
        log.warn("Alias already exists: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Alias already exists");
        pd.setType(URI.create("https://core-platform.dev/problems/alias-already-exists"));
        pd.setProperty("errorCode", "STORAGE_ALIAS_ALREADY_EXISTS");
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