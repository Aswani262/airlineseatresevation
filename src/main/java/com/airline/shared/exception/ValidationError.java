package com.airline.shared.exception;

public record ValidationError(
     String code,
     String field,
     String message){
}
