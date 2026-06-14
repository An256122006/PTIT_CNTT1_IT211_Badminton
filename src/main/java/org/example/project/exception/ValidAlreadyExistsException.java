package org.example.project.exception;

public class ValidAlreadyExistsException extends RuntimeException {
    private final String field;

    public ValidAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
