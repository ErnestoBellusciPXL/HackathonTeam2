package be.codeforbelgium.openinzichten.api;

import be.codeforbelgium.openinzichten.exceptions.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler {

    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String VALIDATION_ERROR = "validation_error";

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<Map<String, String>> handleUsernameTaken(UsernameTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR, "username_taken", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(UsernameTooShortException.class)
    public ResponseEntity<Map<String, String>> handleUsernameTooShort(UsernameTooShortException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, "username_too_short", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(UsernameTooLongException.class)
    public ResponseEntity<Map<String, String>> handleUsernameTooLong(UsernameTooLongException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, "username_too_long", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(EmailTakenException.class)
    public ResponseEntity<Map<String, String>> handleEmailTaken(EmailTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR, "email_taken", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEmail(InvalidEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, "invalid_email", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR, "account_not_found", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStoryNotFound(StoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR, "story_not_found", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTicketNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR, "ticket_not_found", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(TicketAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleTicketAlreadyExists(TicketAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR, "ticket_already_exists", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(StoryAlreadyLikedException.class)
    public ResponseEntity<Map<String, String>> handleStoryAlreadyLiked(StoryAlreadyLikedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR, "story_already_liked", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(StoryNotLikedException.class)
    public ResponseEntity<Map<String, String>> handleStoryNotLiked(StoryNotLikedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, "story_not_liked", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPassword(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, "invalid_password", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Prefer returning the first useful field error mapped to a specific code
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Optional<ResponseEntity<Map<String, String>>> response = mapFieldError(fieldError);
            if (response.isPresent()) {
                return response.get();
            }
        }
        // fallback for other validation errors
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("validation failed");

        return ResponseEntity.badRequest().body(Map.of(ERROR, VALIDATION_ERROR, MESSAGE, msg));
    }

    private Optional<ResponseEntity<Map<String, String>>> mapFieldError(FieldError fieldError) {
        String field = fieldError.getField();
        String defaultMessage = fieldError.getDefaultMessage();

        switch (field) {
            case "username":
                // Determine if too short or too long from message content
                if (defaultMessage != null && defaultMessage.contains("between 3 and 50")) {
                    return Optional.of(ResponseEntity.badRequest()
                            .body(Map.of(ERROR, "username_invalid", MESSAGE, defaultMessage)));
                }
                return Optional.of(ResponseEntity.badRequest().body(Map.of(ERROR, "username_invalid", MESSAGE,
                        defaultMessage != null ? defaultMessage : "invalid username")));

            case "email":
                return Optional.of(ResponseEntity.badRequest().body(Map.of(ERROR, "invalid_email", MESSAGE,
                        defaultMessage != null ? defaultMessage : "invalid email")));

            case "password":
                return Optional.of(ResponseEntity.badRequest().body(Map.of(ERROR, "invalid_password", MESSAGE,
                        defaultMessage != null ? defaultMessage : "invalid password")));

            default:
                return Optional.empty();
        }
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                // Sort so the output order is deterministic regardless of Set implementation
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of(ERROR, VALIDATION_ERROR, MESSAGE, msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "invalid argument";
        return ResponseEntity.badRequest().body(Map.of(ERROR, "invalid_argument", MESSAGE, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
        String msg = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of(ERROR, VALIDATION_ERROR, MESSAGE, msg));
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleChatNotFound(ChatNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR, "chat_not_found", MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(ChatAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleChatAccessDenied(ChatAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(ERROR, "chat_forbidden", MESSAGE, ex.getMessage()));
    }
}
