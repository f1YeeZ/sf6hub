package com.example.hubdemo.storage;

import java.time.Instant;

public record StoredObject(String key, long size, Instant lastModified) {
}
