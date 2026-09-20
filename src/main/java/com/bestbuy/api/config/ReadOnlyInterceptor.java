package com.bestbuy.api.config;

import com.bestbuy.api.exception.ReadOnlyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class ReadOnlyInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Value("${app.readonly}")
    private boolean readonly;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (readonly && MUTATING_METHODS.contains(request.getMethod())) {
            throw new ReadOnlyException();
        }
        return true;
    }
}
