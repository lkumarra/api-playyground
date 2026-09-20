package com.bestbuy.api.exception;

import com.bestbuy.api.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse(
            "NotFound",
            ex.getMessage(),
            404,
            "not-found",
            List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> "'" + e.getField() + "' " + e.getDefaultMessage())
            .toList();
        return new ErrorResponse(
            "BadRequest",
            "Invalid Parameters",
            400,
            "bad-request",
            errors
        );
    }

    @ExceptionHandler(ReadOnlyException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleReadOnly(ReadOnlyException ex) {
        return new ErrorResponse(
            "MethodNotAllowed",
            ex.getMessage(),
            405,
            "method-not-allowed",
            List.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadArgument(IllegalArgumentException ex) {
        return new ErrorResponse(
            "BadRequest",
            ex.getMessage(),
            400,
            "bad-request",
            List.of()
        );
    }
}
