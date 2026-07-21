package com.example.hubdemo.common;

import java.util.List;

public record PageResult<T>(List<T> list, long page, long pageSize, long total) {
}
