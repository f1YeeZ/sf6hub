package com.example.hubdemo.util;

public final class PageUtil {
    public static final long DEFAULT_MAX_PAGE_SIZE = 50;
    public static final long ADMIN_MAX_PAGE_SIZE = 100;
    public static final long MAX_PAGE = 10_000;

    private PageUtil() {
    }

    public static long page(long page) {
        return Math.min(Math.max(1, page), MAX_PAGE);
    }

    public static long pageSize(long pageSize) {
        return pageSize(pageSize, DEFAULT_MAX_PAGE_SIZE);
    }

    public static long adminPageSize(long pageSize) {
        return pageSize(pageSize, ADMIN_MAX_PAGE_SIZE);
    }

    private static long pageSize(long pageSize, long max) {
        if (pageSize < 1) {
            return 1;
        }
        return Math.min(pageSize, max);
    }
}
