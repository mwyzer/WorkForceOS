package com.workforceos.organization;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.workforceos.authentication.UserAccount;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolves the tenant for the current request from the authenticated user and stores it in
 * {@link TenantContext} for the duration of the request. Unauthenticated traffic and
 * accounts without an explicit organization fall back to the default organization.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;

    public TenantContextFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TenantContext.set(resolveOrganization());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private java.util.UUID resolveOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof UserAccount user) {
            return tenantResolver.resolve(user);
        }
        return tenantResolver.resolve(null);
    }
}