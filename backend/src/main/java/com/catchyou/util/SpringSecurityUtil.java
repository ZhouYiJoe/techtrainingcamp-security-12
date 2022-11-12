package com.catchyou.util;

import com.catchyou.constant.UserType;
import com.catchyou.pojo.dto.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSecurityUtil {
    public static String getCurrentUserId() {
        return ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    public static UserType getCurrentUserType() {
        return ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserType();
    }
}
