package com.minitiktok.api.logging;

import com.minitiktok.api.entity.RequestLog;
import com.minitiktok.api.exception.ForbiddenVideoOperationException;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.mapper.RequestLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestLogAspect {

    private static final int MAX_LOG_TEXT_LENGTH = 2000;

    private final RequestLogMapper requestLogMapper;

    @Around("execution(* com.minitiktok.api.controller..*.*(..))")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        // 1. 获取基础上下文信息
        String method = request.getMethod();
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        if ("DELETE".equals(method) && "/api/request-logs".equals(path)) {
            return joinPoint.proceed();
        }

        // 健壮性处理：防止匿名访问（未登录）时 SecurityContextHolder 报空指针
        String userId = "ANONYMOUS";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            userId = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        String requestBody = summarizeRequest(path, joinPoint.getArgs());

        Object result = null;
        int statusCode = HttpStatus.OK.value();
        String responseBody = "";

        try {
            result = joinPoint.proceed();
            statusCode = resolveStatusCode(result, response);
            responseBody = summarizeResponse(path, result);
        } catch (Throwable e) {
            statusCode = resolveExceptionStatus(e);
            responseBody = truncate("[Exception]: " + safeMessage(e));
            throw e; // 抛回给全局异常处理器处理，但保证后续 finally 正常入库
        } finally {
            long duration = System.currentTimeMillis() - start;

            // 5. 构建并写入物理表与 Logback
            RequestLog entity = RequestLog.builder()
                    .userId(userId)
                    .method(method)
                    .path(path)
                    .requestBody(requestBody)
                    .responseBody(responseBody)
                    .statusCode(statusCode)
                    .durationMs(duration)
                    .ip(ip)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                requestLogMapper.insert(entity);
            } catch (Exception mysqlEx) {
                log.error("Failed to save request log to DB", mysqlEx);
            }

            // 满足“写入 Logback 文件日志”的指标要求
            log.info("Minitiktok API Audit | User: {} | {} {} | Status: {} | Duration: {}ms",
                    userId, method, path, statusCode, duration);
        }
        return result;
    }

    private String summarizeRequest(String path, Object[] args) {
        if (path.contains("/play")) {
            return "[Video Play Request: " + summarizeScalarArgs(args) + "]";
        }
        if (path.contains("/chunks/")) {
            return "[Binary Request: " + summarizeScalarArgs(args) + "]";
        }
        if (containsBinaryPayload(args)) {
            return truncate(Arrays.stream(args)
                    .map(this::summarizeArgument)
                    .collect(Collectors.joining(", ", "[Binary Request: ", "]")));
        }
        if (containsMultipart(args)) {
            return truncate(Arrays.stream(args)
                    .map(this::summarizeArgument)
                    .collect(Collectors.joining(", ", "[Multipart Request: ", "]")));
        }
        return truncate(Arrays.toString(args));
    }

    private String summarizeResponse(String path, Object result) {
        if (path.contains("/play")) {
            return "[Binary Stream Content]";
        }
        if (result == null) {
            return "";
        }
        return truncate(result.toString());
    }

    private int resolveStatusCode(Object result, HttpServletResponse response) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        if (response != null) {
            return response.getStatus();
        }
        return HttpStatus.OK.value();
    }

    private int resolveExceptionStatus(Throwable throwable) {
        if (throwable instanceof VideoNotFoundException) {
            return HttpStatus.NOT_FOUND.value();
        }
        if (throwable instanceof ForbiddenVideoOperationException || throwable instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN.value();
        }
        if (throwable instanceof AuthenticationException) {
            return HttpStatus.UNAUTHORIZED.value();
        }
        if (throwable instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST.value();
        }
        if (throwable instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode().value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private boolean containsMultipart(Object[] args) {
        return Arrays.stream(args).anyMatch(MultipartFile.class::isInstance);
    }

    private boolean containsBinaryPayload(Object[] args) {
        return Arrays.stream(args).anyMatch(byte[].class::isInstance);
    }

    private String summarizeArgument(Object arg) {
        if (arg instanceof MultipartFile file) {
            return "file{name=%s, originalFilename=%s, contentType=%s, size=%d}"
                    .formatted(file.getName(), file.getOriginalFilename(), file.getContentType(), file.getSize());
        }
        if (arg instanceof byte[] bytes) {
            return "bytes{size=%d}".formatted(bytes.length);
        }
        return String.valueOf(arg);
    }

    private String summarizeScalarArgs(Object[] args) {
        String summary = Arrays.stream(args)
                .filter(arg -> !(arg instanceof MultipartFile))
                .filter(arg -> !(arg instanceof byte[]))
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof HttpServletResponse))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return summary.isBlank() ? "no scalar arguments" : summary;
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private String truncate(String value) {
        if (value.length() <= MAX_LOG_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_TEXT_LENGTH) + "...(truncated)";
    }
}
