package io.coreplatform.storage.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 请求上下文 — 从 X-User-* 请求头读取当前用户身份。
 * 不做 JWT 验证，信任调用方（API Gateway / core-identity）已鉴权。
 */
@Component
@RequestScope
public class AccessContext {

    private final String userId;
    private final List<String> roles;
    private final String userType;
    private final String clientIp;

    public AccessContext(HttpServletRequest request) {
        this.userId = headerOrNull(request, "X-User-Id");
        this.roles = parseList(headerOrNull(request, "X-User-Roles"));
        this.userType = headerOrNull(request, "X-User-Type");
        this.clientIp = request.getRemoteAddr();
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getUserType() {
        return userType;
    }

    public String getClientIp() {
        return clientIp;
    }

    /** 是否已登录。 */
    public boolean isAuthenticated() {
        return userId != null && !userId.isBlank();
    }

    /** 是否为系统内部调用。 */
    public boolean isSystem() {
        return "SYSTEM".equalsIgnoreCase(userType);
    }

    /** 是否拥有某个角色。 */
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase(roleName));
    }

    private static String headerOrNull(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value != null && !value.isBlank()) ? value : null;
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}