package com.airline.shared.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Notification {

    private final List<ValidationError> errors = new ArrayList<>();

    public void add(String code, String field, String message) {
        errors.add(new ValidationError(code, field, message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ValidationError> view() {
        return Collections.unmodifiableList(errors);
    }
}
