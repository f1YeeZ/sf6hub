package com.example.hubdemo.common;

public class RateLimitException extends BizException {
    public RateLimitException(String message) {
        super(message);
    }
}
