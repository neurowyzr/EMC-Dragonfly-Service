package com.neurowyzr.nw.dragon.service.di.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;

public class MatchesSetValidator implements ConstraintValidator<MatchesSet, String> {
  private java.util.Set<String> acceptedValues;

  @Override
  public void initialize(MatchesSet constraintAnnotation) {
    acceptedValues = new HashSet<>(Arrays.asList(constraintAnnotation.acceptedValues()));
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return acceptedValues.contains(value);
  }
}