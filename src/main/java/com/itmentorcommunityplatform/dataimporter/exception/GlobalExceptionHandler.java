package com.itmentorcommunityplatform.dataimporter.exception;

import com.itmentorcommunityplatform.dataimporter.dto.response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> base = super.handleExceptionInternal(ex, body, headers, statusCode, request);

        String message = (base != null && base.getBody() instanceof ProblemDetail pd && pd.getDetail() != null)
                ? pd.getDetail() : "Request failed";

        return ResponseEntity
                .status(statusCode)
                .body(new ErrorResponseDto(message));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(status).body(new ErrorResponseDto(message));
    }

    @ExceptionHandler(UnknownImportTypeException.class)
    public ResponseEntity<ErrorResponseDto> handleUnknownType(UnknownImportTypeException ex) {
        log.warn(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(ImportNotImplementedException.class)
    public ResponseEntity<ErrorResponseDto> handleNotImplemented(ImportNotImplementedException ex) {
        log.error("Import command is not registered: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto("An unexpected error occurred. Please try again later."));
    }

    @ExceptionHandler(ImportAlreadyRunningException.class)
    public ResponseEntity<ErrorResponseDto> handleImportConflict(ImportAlreadyRunningException ex) {
        log.warn(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllUncaughtExceptions(Exception ex) {
        log.error("Unhandled exception occurred: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto("An unexpected error occurred. Please try again later."));
    }
}
