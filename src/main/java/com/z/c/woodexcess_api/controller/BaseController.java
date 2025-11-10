package com.z.c.woodexcess_api.controller;


import com.z.c.woodexcess_api.security.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public abstract class BaseController {

    protected UUID getAuthenticatedUserID() throws IllegalAccessException {
        var main = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(main instanceof CustomUserDetails userDetails){
                return userDetails.getId();
            }
            throw new IllegalAccessException("User not found");

    }
}
