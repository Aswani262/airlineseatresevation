package com.airline.shared.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BaseService
public @interface UtilityService {
}
