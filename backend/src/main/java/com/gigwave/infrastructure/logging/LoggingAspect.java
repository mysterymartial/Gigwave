package com.gigwave.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("execution(* com.gigwave.application..*(..)) || execution(* com.gigwave.api.controllers..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;
            
            log.info("Method: {} executed in {}ms with args: {}", 
                    joinPoint.getSignature().toShortString(), 
                    executionTime,
                    Arrays.toString(joinPoint.getArgs()));
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("Method: {} failed after {}ms with error: {}", 
                    joinPoint.getSignature().toShortString(), 
                    executionTime,
                    e.getMessage(), e);
            throw e;
        }
    }
}
