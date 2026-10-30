package com.satish.mailservice.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satish.mailservice.entity.EmailAuditLog;
import com.satish.mailservice.entity.TransactionStatus;
import com.satish.mailservice.repository.EmailAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionIdAspect {

    private final EmailAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final ThreadLocal<Long> tidThreadLocal = new ThreadLocal<>();

    @Around("@annotation(com.satish.mailservice.aop.TrackTransaction)")
    public Object trackTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        EmailAuditLog auditLog = new EmailAuditLog();
        auditLog.setTxnStatus(TransactionStatus.PENDING);
        auditLog.setStatusCode(202);
        auditLog.setStatusMsg("Request accepted and processing");

        try {
            // Capture HTTP method and URL
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setMethod(request.getMethod());
                auditLog.setUrl(request.getRequestURI());
                captureUserId(joinPoint, auditLog);
            }

            // Capture request body
            String requestBody = captureRequestBody(joinPoint);
            auditLog.setReq(requestBody);

            // Save initial audit log - TID will be auto-generated
            EmailAuditLog savedLog = auditLogRepository.save(auditLog);
            Long tid = savedLog.getTid();
            tidThreadLocal.set(tid);
            
            log.info("Created transaction with TID: {}", tid);

            // Proceed with the actual method execution
            Object result = joinPoint.proceed();

            return result;
        } catch (Exception e) {
            log.error("Error in transaction tracking for TID: {}", tidThreadLocal.get(), e);
            throw e;
        }
    }

    private void captureUserId(ProceedingJoinPoint joinPoint, EmailAuditLog auditLog) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] arguments = joinPoint.getArgs();
        for (int i = 0; i < method.getParameterCount(); i++) {
            RequestParam requestParam = method.getParameters()[i].getAnnotation(RequestParam.class);
            if (requestParam != null
                    && "user_id".equals(requestParam.value())
                    && arguments[i] instanceof Long userId) {
                auditLog.setUserId(userId);
                return;
            }
        }
    }

    private String captureRequestBody(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            for (int i = 0; i < method.getParameterCount(); i++) {
                if (method.getParameters()[i].isAnnotationPresent(RequestBody.class)) {
                    return objectMapper.writeValueAsString(args[i]);
                }
            }
            for (Object arg : args) {
                if (arg instanceof org.springframework.web.multipart.MultipartFile file) {
                    return String.format("{\"fileName\":\"%s\",\"fileSize\":%d,\"contentType\":\"%s\"}",
                            file.getOriginalFilename(), file.getSize(), file.getContentType());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to capture request body", e);
        }
        return "Request body unavailable";
    }

    public static Long getCurrentTid() {
        return tidThreadLocal.get();
    }

    public static void clearTid() {
        tidThreadLocal.remove();
    }
}
