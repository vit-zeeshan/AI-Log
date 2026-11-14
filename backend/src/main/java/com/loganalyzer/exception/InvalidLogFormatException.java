package com.loganalyzer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidLogFormatException extends RuntimeException {
    public InvalidLogFormatException(String message) {
        super(message);
    }
}
