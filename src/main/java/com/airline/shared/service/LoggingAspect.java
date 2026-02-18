package com.airline.shared.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(com.airline.shared.annotation.Log)")
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // Get parameter names and values
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        StringBuilder inputLog = new StringBuilder();
        if (paramNames != null && paramNames.length > 0) {
            for (int i = 0; i < paramNames.length; i++) {
                inputLog.append(paramNames[i])
                        .append(": ")
                        .append(args[i] != null ? args[i].toString() : "null")
                        .append(i < paramNames.length - 1 ? ", " : "");
            }
        } else {
            inputLog.append("No parameter names available (compile with -parameters)");
        }

        // Log input details
        logger.info("Class: {}, Method: {}, Inputs: {}", className, methodName, inputLog.toString());

        // Proceed with the method execution
        Object result = joinPoint.proceed();

        // Log output
        logger.info("Class: {}, Method: {}, Output: {}", className, methodName, result != null ? result.toString() : "null");

        return result;
    }
}