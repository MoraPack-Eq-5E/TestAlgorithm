package com.grupo5e.morapack.api.exception;

import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador global de excepciones para toda la aplicación
 * Proporciona respuestas de error estandarizadas
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones cuando un recurso no se encuentra
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja excepciones cuando se intenta crear un recurso duplicado
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de operaciones inválidas
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidOperation(
            InvalidOperationException ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones de capacidad insuficiente
     */
    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<ErrorResponseDTO> handleInsufficientCapacity(
            InsufficientCapacityException ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Insufficient Capacity")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones del algoritmo ALNS
     */
    @ExceptionHandler(AlnsExecutionException.class)
    public ResponseEntity<ErrorResponseDTO> handleAlnsExecution(
            AlnsExecutionException ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("ALNS Execution Error")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja excepciones de validación (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<String> detalles = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.add(error.getField() + ": " + error.getDefaultMessage());
        }
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .mensaje("Error de validación en los datos enviados")
                .detalles(detalles)
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja violaciones de integridad de datos (constraints de BD)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        String mensaje = "Violación de integridad de datos";
        
        // Intentar extraer información más específica del error
        if (ex.getMessage().contains("unique")) {
            mensaje = "El valor ingresado ya existe y debe ser único";
        } else if (ex.getMessage().contains("foreign key")) {
            mensaje = "Referencia inválida a otro registro";
        } else if (ex.getMessage().contains("not-null")) {
            mensaje = "Campo obligatorio no puede ser nulo";
        }
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Integrity Violation")
                .mensaje(mensaje)
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones genéricas no capturadas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .mensaje("Ha ocurrido un error inesperado en el servidor")
                .path(request.getRequestURI())
                .build();
        
        // Log del error para debugging (en producción usar un logger apropiado)
        ex.printStackTrace();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja excepciones de argumentos ilegales
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}

