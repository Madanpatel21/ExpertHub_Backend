package exception;

import dto.apiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandling {

    @ExceptionHandler(badRequestException.class)
    public ResponseEntity<apiResponse<Void>> handleBadRequestException(badRequestException ex) {
        return ResponseEntity.badRequest()
                .body(new apiResponse<>(ex.getMessage(), false, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<apiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        apiResponse<Map<String, String>> response = new apiResponse<>("Validation failed", false, errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<apiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new apiResponse<>("Access denied: You don't have permission to access this resource", false,
                        null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<apiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new apiResponse<>("Invalid credentials", false, null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<apiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new apiResponse<>("File size exceeds the maximum allowed limit", false, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<apiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new apiResponse<>(ex.getMessage(), false, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<apiResponse<Void>> handleGenericException(Exception ex) {
        // Log the exception for debugging
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new apiResponse<>("An unexpected error occurred: " + ex.getMessage(), false, null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<apiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new apiResponse<>(ex.getMessage(), false, null));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<apiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new apiResponse<>(ex.getMessage(), false, null));
    }
}
