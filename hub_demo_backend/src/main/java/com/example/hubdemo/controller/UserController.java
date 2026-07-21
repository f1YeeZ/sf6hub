package com.example.hubdemo.controller;

import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.dto.AuthDtos.SendCodeRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfileEmailRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfilePasswordRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfileUsernameRequest;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.AntiScrapingService;
import com.example.hubdemo.service.CatalogService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import com.example.hubdemo.service.UserService;
import com.example.hubdemo.util.PageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
public class UserController {
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final CatalogService catalogService;
    private final RateLimitService rateLimitService;
    private final AdminRealtimeService adminRealtimeService;
    private final AntiScrapingService antiScrapingService;

    public UserController(UserService userService, CurrentUserService currentUserService,
                          CatalogService catalogService, RateLimitService rateLimitService,
                          AdminRealtimeService adminRealtimeService, AntiScrapingService antiScrapingService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.catalogService = catalogService;
        this.rateLimitService = rateLimitService;
        this.adminRealtimeService = adminRealtimeService;
        this.antiScrapingService = antiScrapingService;
    }

    @GetMapping("/me")
    public User me(HttpServletRequest request) {
        return currentUserService.from(request)
                .map(userService::privateProfile)
                .orElseThrow(() -> new com.example.hubdemo.common.BizException("请先登录"));
    }

    @PutMapping("/profile/username")
    public User updateProfileUsername(@Valid @RequestBody UpdateProfileUsernameRequest body,
                                      HttpServletRequest request) {
        User user = userService.updateUsername(currentUserService.require(request), body);
        adminRealtimeService.publishAll("profile:update", "users", "profile", "userCombos", "favoriteCombos", "combos", "dashboard");
        return user;
    }

    @PostMapping("/profile/email/current-code")
    public Map<String, String> sendCurrentEmailProfileCode(HttpServletRequest request) {
        rateLimitService.check(request, "profile:current-email-code", 5, Duration.ofMinutes(10));
        userService.sendCurrentEmailProfileCode(currentUserService.require(request));
        return Map.of("message", "当前邮箱验证码已发送");
    }

    @PostMapping("/profile/email/new-code")
    public Map<String, String> sendNewEmailProfileCode(@Valid @RequestBody SendCodeRequest body,
                                                       HttpServletRequest request) {
        rateLimitService.check(request, "profile:new-email-code", 5, Duration.ofMinutes(10));
        currentUserService.require(request);
        userService.sendNewEmailProfileCode(body.email());
        return Map.of("message", "新邮箱验证码已发送");
    }

    @PutMapping("/profile/email")
    public User updateProfileEmail(@Valid @RequestBody UpdateProfileEmailRequest body,
                                   HttpServletRequest request) {
        User user = userService.updateEmail(currentUserService.require(request), body);
        adminRealtimeService.publishAll("profile:update", "users", "profile", "dashboard");
        return user;
    }

    @PostMapping("/profile/password/code")
    public Map<String, String> sendPasswordProfileCode(HttpServletRequest request) {
        rateLimitService.check(request, "profile:password-code", 5, Duration.ofMinutes(10));
        userService.sendPasswordProfileCode(currentUserService.require(request));
        return Map.of("message", "密码修改验证码已发送");
    }

    @PutMapping("/profile/password")
    public User updateProfilePassword(@Valid @RequestBody UpdateProfilePasswordRequest body,
                                      HttpServletRequest request) {
        User user = userService.updatePassword(currentUserService.require(request), body);
        adminRealtimeService.publishPublic("profile:update", "profile");
        return user;
    }

    @GetMapping("/users/{id}/combos")
    public PageResult<Combo> userCombos(@PathVariable Long id,
                                        @RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "4") long pageSize,
                                        HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        userService.requireUser(id);
        User viewer = currentUserService.from(request).orElse(null);
        boolean includeUnapproved = viewer != null && viewer.getId().equals(id);
        return catalogService.userCombos(id, includeUnapproved, viewer, PageUtil.page(page), PageUtil.pageSize(pageSize));
    }

    @GetMapping("/users/{id}/favorite-combos")
    public PageResult<Combo> userFavoriteCombos(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "4") long pageSize,
                                                HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        User favoriteOwner = userService.requireUser(id);
        User viewer = currentUserService.from(request).orElse(null);
        return catalogService.userFavoriteCombos(favoriteOwner, viewer, PageUtil.page(page), PageUtil.pageSize(pageSize));
    }

}
