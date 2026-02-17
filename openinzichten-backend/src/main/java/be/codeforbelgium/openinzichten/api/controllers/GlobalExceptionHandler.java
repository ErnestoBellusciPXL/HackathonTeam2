package be.codeforbelgium.openinzichten.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseBody;
import be.codeforbelgium.openinzichten.api.response.MessageResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String reason = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("de input was niet geldig");
        return ResponseEntity.badRequest().body(new MessageResponse("de input was niet geldig: " + reason));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("de input was niet geldig: " + ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("de input was niet geldig: parameter ontbreekt: " + ex.getParameterName()));
    }

    // Add more handlers as needed
}
