package com.z.c.woodexcess_api.exception;

import com.z.c.woodexcess_api.exception.users.EmailAlredyExist;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlredyExist.class)
    public ResponseEntity<?> handleEmailAlredyExist(EmailAlredyExist e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
