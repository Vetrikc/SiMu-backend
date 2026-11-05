package org.example.simu.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN = 6;
    private static final int MAX = 100;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotBlank reports the missing value
        }
        if (value.length() < MIN || value.length() > MAX) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasDigit = false;
        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasUpper && hasDigit;
    }
}
