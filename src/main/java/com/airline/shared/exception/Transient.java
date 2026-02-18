package com.airline.shared.exception;

//This marker interface is used to indicate that an exception is transient,
// meaning it may be resolved by retrying the operation that caused it.
// Transient exceptions are typically temporary and may occur due to network issues,
// timeouts, or other transient conditions. By implementing this interface,
// an exception can be categorized as transient,
// allowing for appropriate handling strategies such as retries or fallback mechanisms.
public interface Transient {
}
