package com.minitiktok.api.logging;

import com.minitiktok.api.entity.RequestLog;
import com.minitiktok.api.mapper.RequestLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RequestLogAspect {

    @Autowired
    private RequestLogMapper requestLogMapper;

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

        // 健壮性处理：防止匿名访问（未登录）时 SecurityContextHolder 报空指针
        String userId = "ANONYMOUS";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            userId = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        // 2. 请求体防二进制脱敏
        String requestBody;
        if (path.contains("/upload")) {
            requestBody = "[Multipart File Upload Summary]";
        } else if (path.contains("/play")) {
            requestBody = "[Video Play Request ID]";
        } else {
            requestBody = Arrays.toString(joinPoint.getArgs());
            // 防止普通请求入参过长导致数据库 text 爆掉
            if (requestBody.length() > 2000) {
                requestBody = requestBody.substring(0, 2000) + "...(truncated)";
            }
        }

        Object result = null;
        int statusCode = 200;
        String responseBody = "";

        try {
            result = joinPoint.proceed();

            // 3. 补全响应体处理 & 彻底防范播放接口 OOM 风险
            if (path.contains("/play")) {
                responseBody = "[Binary Stream Content]";
            } else if (path.contains("/upload")) {
                responseBody = "[Upload Success Meta Data]";
            } else if (result != null) {
                responseBody = result.toString();
                if (responseBody.length() > 2000) {
                    responseBody = responseBody.substring(0, 2000) + "...(truncated)";
                }
            }

            // 尝试从真实 response 拿到状态码（支持动态控制器的映射）
            if (response != null) {
                statusCode = response.getStatus();
            }
        } catch (Throwable e) {
            // 4. 异常请求耗时监控和审计状态码捕获
            statusCode = 500;
            responseBody = "[Exception]: " + e.getMessage();
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
}
