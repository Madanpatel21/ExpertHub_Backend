package Util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import entity.user;
import exception.badRequestException;
import org.springframework.stereotype.Component;

@Component
public class securityUtil {
    public user getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof user)) {
            throw new badRequestException("Unauthorized");
        }

        return (user) auth.getPrincipal();
    }
}
