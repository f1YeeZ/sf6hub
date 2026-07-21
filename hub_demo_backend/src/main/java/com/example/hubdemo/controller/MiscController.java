package com.example.hubdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.hubdemo.entity.Announcement;
import com.example.hubdemo.mapper.AnnouncementMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MiscController {
    private final AnnouncementMapper announcementMapper;

    public MiscController(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @GetMapping("/announcements")
    public List<Announcement> announcements(@RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getPublished, true)
                .orderByDesc(Announcement::getCreatedAt)
                .last("limit " + safeLimit));
    }
}
