package com.itmentorcommunityplatform.dataimporter.validator;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public class RoleValidator {

    public static void validateAdminRole(List<String> roles) {
        if (roles == null || roles.stream().noneMatch(r -> r.equalsIgnoreCase("ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: missing ADMIN role");
        }
    }
}
