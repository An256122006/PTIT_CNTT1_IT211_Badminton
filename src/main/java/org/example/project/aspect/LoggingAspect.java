package org.example.project.aspect;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* org.example.project.controller..*(..))")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request =
                attributes != null ? attributes.getRequest() : null;

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - start;

            if (request != null) {
                log.info("[{} {}] {} executed in {} ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        joinPoint.getSignature().toShortString(),
                        duration);
            }

            return result;

        } catch (Exception e) {
            log.error("Exception in {}: {}",
                    joinPoint.getSignature().toShortString(),
                    e.getMessage());

            throw e;
        }
    }
}
