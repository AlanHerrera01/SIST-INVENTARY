package com.veris.inventory.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Problemas RFC 9457 con `code` estable; nunca expone mensajes internos. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handle(ApiException exception, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(exception.status()), exception.detail());
        problem.setTitle(HttpStatus.valueOf(exception.status()).getReasonPhrase());
        problem.setProperty("code", exception.code());
        problem.setProperty("instance", request.getRequestURI());
        return problem;
    }
}
