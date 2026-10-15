package com.UVGgo.backend.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ErroresApi {
    @ExceptionHandler(ApiException.class)
    ProblemDetail negocio(ApiException e) { return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    ProblemDetail invalido(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos: revise el formato, los campos obligatorios y sus límites"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicado() { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "La operación viola una restricción de datos o un registro ya existe"); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrencia() { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El recurso cambió; consulte su versión actual"); }
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail prohibido() { return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación"); }
}
