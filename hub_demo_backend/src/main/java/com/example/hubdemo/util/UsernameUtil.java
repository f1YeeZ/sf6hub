package com.example.hubdemo.util;

import com.example.hubdemo.common.BizException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class UsernameUtil {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 20;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private UsernameUtil() {
    }

    public static String normalize(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BizException("用户名不能为空");
        }
        String normalized = username.trim();
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new BizException("用户名长度需为 3-20 个字符");
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new BizException("用户名只能包含字母、数字、下划线或短横线");
        }
        return normalized;
    }
}
