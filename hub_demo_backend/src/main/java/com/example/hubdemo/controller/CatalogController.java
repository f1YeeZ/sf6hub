package com.example.hubdemo.controller;

import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.dto.ComboDtos.ComboFavoriteResponse;
import com.example.hubdemo.dto.ComboDtos.ComboLikeResponse;
import com.example.hubdemo.dto.ComboDtos.ComboFilterOptions;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckRequest;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckResponse;
import com.example.hubdemo.dto.ComboDtos.WeeklyContributor;
import com.example.hubdemo.entity.CharacterInfo;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.FrameData;
import com.example.hubdemo.service.AntiScrapingService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.CatalogService;
import com.example.hubdemo.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.time.Duration;

@RestController
public class CatalogController {
    private final CatalogService catalogService;
    private final CurrentUserService currentUserService;
    private final RateLimitService rateLimitService;
    private final AntiScrapingService antiScrapingService;

    public CatalogController(CatalogService catalogService, CurrentUserService currentUserService,
                             RateLimitService rateLimitService, AntiScrapingService antiScrapingService) {
        this.catalogService = catalogService;
        this.currentUserService = currentUserService;
        this.rateLimitService = rateLimitService;
        this.antiScrapingService = antiScrapingService;
    }

    @GetMapping("/characters")
    public List<CharacterInfo> characters() {
        return catalogService.characters();
    }

    @GetMapping("/characters/{id}")
    public CharacterInfo character(@PathVariable Long id) {
        return catalogService.character(id);
    }

    @GetMapping("/characters/{id}/frames")
    public List<FrameData> frames(@PathVariable Long id) {
        return catalogService.frames(id);
    }

    @GetMapping("/characters/{id}/combos")
    public PageResult<Combo> characterCombos(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "12") long pageSize,
                                             @RequestParam(required = false) String controlType,
                                             @RequestParam(required = false) String starter,
                                             @RequestParam(required = false) String tags,
                                             @RequestParam(required = false) Integer saCost,
                                             @RequestParam(required = false) BigDecimal driveCost,
                                             @RequestParam(required = false) Integer damageMin,
                                             @RequestParam(required = false) Integer damageMax,
                                             @RequestParam(defaultValue = "createdAt") String sort,
                                             HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.characterCombos(id, controlType, starter, tags, saCost, driveCost, damageMin, damageMax, sort,
                currentUserService.from(request).orElse(null), page, pageSize);
    }

    @GetMapping("/characters/{id}/combo-filter-options")
    public ComboFilterOptions characterComboFilterOptions(@PathVariable Long id,
                                                           @RequestParam(required = false) String controlType,
                                                           HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.characterComboFilterOptions(id, controlType);
    }

    @GetMapping("/characters/{id}/combo-parent-options")
    public List<Combo> characterComboParentOptions(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "classic") String controlType,
                                                    HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.parentComboOptions(id, controlType);
    }

    @GetMapping("/world-tour/combos")
    public PageResult<Combo> worldTourCombos(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "12") long pageSize,
                                             @RequestParam(required = false) String starter,
                                             @RequestParam(required = false) String tags,
                                             @RequestParam(required = false) Integer saCost,
                                             @RequestParam(required = false) BigDecimal driveCost,
                                             @RequestParam(required = false) Integer damageMin,
                                             @RequestParam(required = false) Integer damageMax,
                                             @RequestParam(defaultValue = "createdAt") String sort,
                                             HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.worldTourCombos(starter, tags, saCost, driveCost, damageMin, damageMax, sort,
                currentUserService.from(request).orElse(null), page, pageSize);
    }

    @GetMapping("/world-tour/combo-filter-options")
    public ComboFilterOptions worldTourComboFilterOptions(HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.worldTourComboFilterOptions();
    }

    @GetMapping("/combos/weekly-contributors")
    public List<WeeklyContributor> weeklyContributors() {
        return catalogService.weeklyContributors();
    }

    @GetMapping("/combos/{id}")
    public Combo combo(@PathVariable Long id, HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.visibleCombo(id, currentUserService.from(request).orElse(null));
    }

    @GetMapping("/combos/{id}/followups")
    public List<Combo> comboFollowups(@PathVariable Long id, HttpServletRequest request) {
        antiScrapingService.checkComboRead(request);
        return catalogService.followupCombos(id, currentUserService.from(request).orElse(null));
    }

    @PostMapping("/combos")
    public Combo createCombo(@Valid @RequestBody com.example.hubdemo.dto.ComboDtos.ComboRequest request,
                             HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "combos:create", 20, Duration.ofMinutes(10));
        return catalogService.createCombo(request, currentUserService.require(servletRequest));
    }

    @PostMapping("/combos/duplicate-check")
    public ComboDuplicateCheckResponse duplicateCheck(@Valid @RequestBody ComboDuplicateCheckRequest body,
                                                       HttpServletRequest request) {
        rateLimitService.check(request, "combos:duplicate-check", 120, Duration.ofMinutes(10));
        return catalogService.duplicateCandidates(body, currentUserService.require(request));
    }

    @PutMapping("/combos/{id}")
    public Combo updateCombo(@PathVariable Long id, @Valid @RequestBody com.example.hubdemo.dto.ComboDtos.ComboRequest request,
                             HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "combos:update", 40, Duration.ofMinutes(10));
        return catalogService.updateCombo(id, request, currentUserService.require(servletRequest));
    }

    @DeleteMapping("/combos/{id}")
    public void deleteCombo(@PathVariable Long id, HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "combos:delete", 30, Duration.ofMinutes(10));
        catalogService.deleteCombo(id, currentUserService.require(servletRequest));
    }

    @PostMapping("/combos/{id}/like")
    public ComboLikeResponse likeCombo(@PathVariable Long id,
                                       HttpServletRequest request) {
        rateLimitService.check(request, "combos:like", 120, Duration.ofMinutes(10));
        return catalogService.toggleComboLike(id, currentUserService.require(request));
    }

    @PostMapping("/combos/{id}/favorite")
    public ComboFavoriteResponse favoriteCombo(@PathVariable Long id,
                                               HttpServletRequest request) {
        rateLimitService.check(request, "combos:favorite", 120, Duration.ofMinutes(10));
        return catalogService.toggleComboFavorite(id, currentUserService.require(request));
    }

}
