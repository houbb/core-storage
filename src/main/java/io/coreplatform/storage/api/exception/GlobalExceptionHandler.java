package io.coreplatform.storage.api.exception;

import io.coreplatform.storage.application.service.StorageService;
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