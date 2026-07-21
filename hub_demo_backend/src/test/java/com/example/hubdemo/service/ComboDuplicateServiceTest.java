package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckRequest;
import com.example.hubdemo.dto.ComboDtos.ComboRequest;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.ComboFollowup;
import com.example.hubdemo.mapper.ComboFollowupMapper;
import com.example.hubdemo.mapper.ComboMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComboDuplicateServiceTest {
    private ComboMapper comboMapper;
    private ComboFollowupMapper comboFollowupMapper;
    private ComboDuplicateService service;

    @BeforeEach
    void setUp() {
        comboMapper = mock(ComboMapper.class);
        comboFollowupMapper = mock(ComboFollowupMapper.class);
        service = new ComboDuplicateService(comboMapper, comboFollowupMapper);
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
    }

    @Test
    void exactIdentityIgnoresDamageResourcesDifficultyAndDisplayTags() {
        Combo existing = combo(7L, "2MK > DRC > 5HP", "approved");
        existing.setDamage(9000);
        existing.setDriveCost(new BigDecimal("5.0"));
        existing.setDifficulty("困难");
        existing.setTags("counter-hit,fun,knockdown");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        var result = service.check(checkRequest(null, List.of("knockdown", "counter-hit"), List.of()));

        assertThat(result.exactDuplicate()).isTrue();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.matchType()).isEqualTo("exact");
            assertThat(candidate.similarity()).isEqualTo(100);
        });
    }

    @Test
    void sameFollowupRouteUnderAnotherParentIsNotAnExactDuplicate() {
        Combo existing = combo(8L, "5MP > THROW", "approved");
        existing.setTags("throw-pressure");
        ComboFollowup relation = new ComboFollowup();
        relation.setFollowupComboId(8L);
        relation.setParentComboId(100L);
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation));

        var result = service.check(new ComboDuplicateCheckRequest(
                10L, "5MP", "5MP > THROW", "classic", List.of(), "throw-pressure",
                List.of("throw-pressure"), false, 200L, null
        ));

        assertThat(result.exactDuplicate()).isFalse();
        assertThat(result.candidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.matchType()).isEqualTo("same-route"));
    }

    @Test
    void worldTourCharacterSequenceIsPartOfTheIdentity() {
        Combo existing = combo(9L, "5MP > 236HP", "approved");
        existing.setControlType("world-tour");
        existing.setRouteCharacterIds(List.of(10L, 20L));
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        var result = service.check(new ComboDuplicateCheckRequest(
                null, "5MP", "5MP > 236HP", "world-tour", List.of(10L, 30L), "counter-hit",
                List.of("counter-hit"), false, null, null
        ));

        assertThat(result.exactDuplicate()).isFalse();
        assertThat(result.candidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.matchType()).isEqualTo("same-route"));
    }

    @Test
    void rejectedIdentityIsHistoricalAndDoesNotBlockSubmission() {
        Combo existing = combo(10L, "2MK > DRC > 5HP", "rejected");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        var result = service.check(checkRequest(null, List.of("counter-hit"), List.of()));

        assertThat(result.exactDuplicate()).isFalse();
        assertThat(result.candidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.matchType()).isEqualTo("historical"));
    }

    @Test
    void oneDifferentEnderIsReportedAsRouteVariant() {
        Combo existing = combo(11L, "2MK > DRC > 5HP > 623HP", "approved");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        var result = service.check(new ComboDuplicateCheckRequest(
                10L, "2MK", "2MK > DRC > 5HP > SA3", "classic", List.of(), "counter-hit",
                List.of("counter-hit"), false, null, null
        ));

        assertThat(result.exactDuplicate()).isFalse();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.matchType()).isEqualTo("similar");
            assertThat(candidate.similarity()).isEqualTo(75);
        });
    }

    @Test
    void dedupeKeyIsStableAcrossTagOrderAndIgnoresPresentationTags() {
        ComboRequest left = comboRequest(List.of("counter-hit", "fun"));
        ComboRequest right = comboRequest(List.of("knockdown", "counter-hit"));

        assertThat(service.dedupeKey(left)).isEqualTo(service.dedupeKey(right));
    }

    private static ComboDuplicateCheckRequest checkRequest(Long parentId, List<String> tags, List<Long> routeCharacterIds) {
        return new ComboDuplicateCheckRequest(
                10L, "2MK", "2MK > DRC > 5HP", "classic", routeCharacterIds,
                tags.get(0), tags, false, parentId, null
        );
    }

    private static ComboRequest comboRequest(List<String> tags) {
        return new ComboRequest(
                10L, "2MK", "2MK > DRC > 5HP", "2MK > DRC > 5HP", 1000, BigDecimal.ZERO, 0,
                "+2", "中等", false, "classic", List.of(), tags.get(0), tags,
                "/uploads/combo.mp4", "", "", null
        );
    }

    private static Combo combo(Long id, String route, String status) {
        Combo combo = new Combo();
        combo.setId(id);
        combo.setCharacterId(10L);
        combo.setStarter(route.split(" > ")[0]);
        combo.setRoute(route);
        combo.setComboText(route);
        combo.setControlType("classic");
        combo.setTags("counter-hit");
        combo.setCornerOnly(false);
        combo.setStatus(status);
        return combo;
    }
}
