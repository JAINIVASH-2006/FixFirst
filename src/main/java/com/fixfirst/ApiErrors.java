package com.fixfirst;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",ObjectsOrDefault(e.getReason())));
    }
    private String ObjectsOrDefault(String s) { return s==null?"Request could not be completed.":s; }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var error=e.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(Map.of("message",error.getField()+": "+error.getDefaultMessage()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadable() { return ResponseEntity.badRequest().body(Map.of("message","Invalid request value.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Map.of("message","A matching record already exists. Refresh and try again.")); }
}
