package com.example.hubdemo.service;

import com.example.hubdemo.entity.AdminAuditLog;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.AdminAuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAuditService {
    private final AdminAuditLogMapper adminAuditLogMapper;

    public AdminAuditService(AdminAuditLogMapper adminAuditLogMapper) {
        this.adminAuditLogMapper = adminAuditLogMapper;
    }

    public void record(User admin, String action, String targetType, Long targetId, String detail) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(admin == null ? null : admin.getId());
        log.setAdminName(admin == null ? "" : admin.getUsername());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        adminAuditLogMapper.insert(log);
    }
}
