package config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RazorpayWebhookSecurityConfig extends OncePerRequestFilter {

    // Razorpay webhook IPs (update with actual IPs from Razorpay docs)
    private static final List<String> RAZORPAY_IPS = Arrays.asList(
            "182.76.237.170",
            "182.76.237.171",
            "182.76.237.172",
            "182.76.237.173",
            "182.76.237.174",
            "182.76.237.175");

    private static final List<String> LOCAL_IPS = Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "localhost");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Only apply IP check to webhook endpoint
        if (requestUri.contains("/api/payments/webhook")) {
            String clientIp = getClientIp(request);

            boolean isAllowed = RAZORPAY_IPS.contains(clientIp) || LOCAL_IPS.contains(clientIp);

            if (!isAllowed) {
                logger.warn("Blocked webhook request from unauthorized IP: " + clientIp);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized IP address\"}");
                return;
            }

            logger.info("Webhook request allowed from IP: " + clientIp);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
