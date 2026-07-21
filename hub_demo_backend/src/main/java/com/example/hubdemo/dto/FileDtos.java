package com.example.hubdemo.dto;

public final class FileDtos {
    private FileDtos() {
    }

    public record FileResponse(String url, String filename, String contentType, String usage) {
    }
}
