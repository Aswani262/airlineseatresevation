package com.airline.shared.annoation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BaseService
public @interface IntegrationService {
}
