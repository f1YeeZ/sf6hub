package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.ComboDtos.ComboRequest;
import com.example.hubdemo.dto.ComboDtos.WeeklyContributor;
import com.example.hubdemo.dto.AdminDtos.CharacterDataRequest;
import com.example.hubdemo.dto.AdminDtos.CharacterRequest;
import com.example.hubdemo.entity.CharacterData;
import com.example.hubdemo.entity.CharacterInfo;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.ComboFollowup;
import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.CharacterDataMapper;
import com.example.hubdemo.mapper.ComboFavoriteMapper;
import com.example.hubdemo.mapper.ComboFollowupMapper;
import com.example.hubdemo.mapper.ComboLikeMapper;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class CatalogServiceTest {
    private ComboMapper comboMapper;
    private ComboFollowupMapper comboFollowupMapper;
    private UserMapper userMapper;
    private CharacterMapper characterMapper;
    private CharacterDataMapper characterDataMapper;
    private UploadOwnershipService uploadOwnershipService;
    private ComboDuplicateService comboDuplicateService;
    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        comboMapper = mock(ComboMapper.class);
        comboFollowupMapper = mock(ComboFollowupMapper.class);
        userMapper = mock(UserMapper.class);
        characterMapper = mock(CharacterMapper.class);
        characterDataMapper = mock(CharacterDataMapper.class);
        uploadOwnershipService = mock(UploadOwnershipService.class);
        comboDuplicateService = mock(ComboDuplicateService.class);
        catalogService = new CatalogService(
                characterMapper,
                characterDataMapper,
                mock(FrameDataMapper.class),
                comboMapper,
                mock(ComboLikeMapper.class),
                mock(ComboFavoriteMapper.class),
                comboFollowupMapper,
                userMapper,
                mock(NotificationService.class),
                mock(AdminRealtimeService.class),
                uploadOwnershipService,
                comboDuplicateService
        );
    }

    @Test
    void weeklyContributorsUsesCurrentMondayWindowAndMapsLeaderboardRows() {
        when(comboMapper.selectWeeklyContributors(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        Map.of("userId", 7L, "username", "first", "avatar", "/avatar.png", "comboCount", 5L),
                        Map.of("userId", 9L, "username", "second", "avatar", "", "comboCount", 3L)
                ));

        List<WeeklyContributor> result = catalogService.weeklyContributors();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(comboMapper).selectWeeklyContributors(startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getValue().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(startCaptor.getValue().toLocalTime()).isEqualTo(java.time.LocalTime.MIDNIGHT);
        assertThat(endCaptor.getValue()).isEqualTo(startCaptor.getValue().plusWeeks(1));
        assertThat(result).containsExactly(
                new WeeklyContributor(7L, "first", "/avatar.png", 5L),
                new WeeklyContributor(9L, "second", "", 3L)
        );
    }

    @Test
    void createCharacterCreatesEditableDataTemplate() {
        when(characterMapper.insert(any(CharacterInfo.class))).thenAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(88L);
            return 1;
        });
        CharacterDataRequest dataRequest = new CharacterDataRequest(
                "10000", "0.8", "0.047", "0.032", "19", "23", "1.25", "0.92",
                "4+38+3", "1.9", "1.52", "4f", "SuperCombo", "https://example.com/frame-data"
        );

        CharacterInfo saved = catalogService.createCharacter(new CharacterRequest(
                "TEST", "", "测试角色", "shoto", dataRequest
        ));

        ArgumentCaptor<CharacterData> captor = ArgumentCaptor.forClass(CharacterData.class);
        verify(characterDataMapper).insert(captor.capture());
        assertThat(captor.getValue().getCharacterId()).isEqualTo(88L);
        assertThat(captor.getValue().getHp()).isEqualTo("10000");
        assertThat(captor.getValue().getJumpSpeed()).isEqualTo("4+38+3");
        assertThat(saved.getCharacterData()).isSameAs(captor.getValue());
    }

    @Test
    void createCharacterWithoutStatsStillCreatesBlankTemplate() {
        when(characterMapper.insert(any(CharacterInfo.class))).thenAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(89L);
            return 1;
        });

        catalogService.createCharacter(new CharacterRequest("EMPTY", "", "", "", null));

        ArgumentCaptor<CharacterData> captor = ArgumentCaptor.forClass(CharacterData.class);
        verify(characterDataMapper).insert(captor.capture());
        assertThat(captor.getValue().getCharacterId()).isEqualTo(89L);
        assertThat(captor.getValue().getHp()).isNull();
    }

    @Test
    void approvedComboListExcludesFollowupOnlyCombos() {
        Combo baseCombo = combo(1L, 10L, "2MK > DR");
        Combo followupCombo = combo(2L, 10L, "5HP > SA3");
        ComboFollowup relation = new ComboFollowup();
        relation.setParentComboId(baseCombo.getId());
        relation.setFollowupComboId(followupCombo.getId());
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(baseCombo, followupCombo));
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation));

        List<Combo> combos = catalogService.combos(null, 10L, null, null, null);

        assertThat(combos).extracting(Combo::getId).containsExactly(1L);
    }

    @Test
    void followupComboRequiresSameControlTypeAsParent() {
        Combo parent = combo(1L, 10L, "2MK > DR");
        parent.setControlType("classic");
        Combo followup = combo(2L, 10L, "5HP > SA3");
        followup.setControlType("modern");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(comboMapper.selectById(1L)).thenReturn(parent);
        when(comboMapper.selectById(2L)).thenReturn(followup);

        assertThatThrownBy(() -> catalogService.updateCombo(2L, comboRequest(10L, "5HP > SA3", "modern", 1L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("同一连招类型");
    }

    @Test
    void followupComboRejectsNormalComboTags() {
        Combo followup = combo(2L, 10L, "5HP > SA3");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(comboMapper.selectById(2L)).thenReturn(followup);
        ComboRequest valid = comboRequest(10L, "5HP > SA3", "classic", 1L);
        ComboRequest invalid = new ComboRequest(
                valid.characterId(), valid.starter(), valid.route(), valid.comboText(), valid.damage(), valid.driveCost(),
                valid.saCost(), valid.advantageFrames(), valid.difficulty(), valid.cornerOnly(), valid.controlType(),
                valid.routeCharacterIds(), "counter-hit", List.of("counter-hit"), valid.videoUrl(), valid.trainingNotes(),
                valid.difficultyNote(), valid.followupParentId()
        );

        assertThatThrownBy(() -> catalogService.updateCombo(2L, invalid))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能使用普通连招标签");
    }

    @Test
    void normalComboRejectsPressureTagsFromBypassedClient() {
        ComboRequest valid = comboRequest(10L, "5HP > SA3", "classic", null);
        ComboRequest invalid = new ComboRequest(
                valid.characterId(), valid.starter(), valid.route(), valid.comboText(), valid.damage(), valid.driveCost(),
                valid.saCost(), valid.advantageFrames(), valid.difficulty(), valid.cornerOnly(), valid.controlType(),
                valid.routeCharacterIds(), "safe-jump", List.of("safe-jump"), valid.videoUrl(), valid.trainingNotes(),
                valid.difficultyNote(), null
        );

        assertThatThrownBy(() -> catalogService.createAdminCombo(invalid))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("普通连招不能使用压制类型");
    }

    @Test
    void followupComboRejectsUnapprovedParent() {
        Combo parent = combo(1L, 10L, "2MK > DR");
        parent.setStatus("rejected");
        Combo followup = combo(2L, 10L, "5HP > SA3");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(comboMapper.selectById(1L)).thenReturn(parent);
        when(comboMapper.selectById(2L)).thenReturn(followup);

        assertThatThrownBy(() -> catalogService.updateCombo(2L, comboRequest(10L, "5HP > SA3", "classic", 1L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已审核通过");
    }

    @Test
    void followupComboUsesAtomicSingleParentUpsert() {
        Combo parent = combo(1L, 10L, "2MK > DR");
        Combo followup = combo(2L, 10L, "5HP > SA3");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(comboMapper.selectById(1L)).thenReturn(parent);
        when(comboMapper.selectById(2L)).thenReturn(followup);

        catalogService.updateCombo(2L, comboRequest(10L, "5HP > SA3", "classic", 1L));

        verify(comboFollowupMapper).upsertParent(eq(1L), eq(2L), isNull(), any(LocalDateTime.class));
    }

    @Test
    void parentComboOptionsReturnsEveryApprovedTopLevelOptionWithoutPaging() {
        CharacterInfo character = new CharacterInfo();
        character.setId(10L);
        List<Combo> options = java.util.stream.LongStream.rangeClosed(1, 75)
                .mapToObj(id -> combo(id, 10L, "5MP > 236MP"))
                .toList();
        when(characterMapper.selectById(10L)).thenReturn(character);
        when(comboMapper.selectParentComboOptions(10L, "modern")).thenReturn(options);

        List<Combo> result = catalogService.parentComboOptions(10L, "现代");

        assertThat(result).hasSize(75);
        verify(comboMapper).selectParentComboOptions(10L, "modern");
    }

    @Test
    void followupComboRejectsAnotherFollowupAsParent() {
        Combo parent = combo(1L, 10L, "2MK > DR");
        Combo followup = combo(2L, 10L, "5HP > SA3");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(comboMapper.selectById(1L)).thenReturn(parent);
        when(comboMapper.selectById(2L)).thenReturn(followup);
        when(comboFollowupMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> catalogService.updateCombo(2L, comboRequest(10L, "5HP > SA3", "classic", 1L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能继续作为原连招");
    }

    @Test
    void videoReviewUpdatesOnlyReviewFields() {
        Combo combo = combo(74L, 10L, "214HP > SA3");
        combo.setLikes(12);
        combo.setFavorites(8);
        when(comboMapper.selectById(74L)).thenReturn(combo);
        User admin = new User();
        admin.setUsername("reviewer");

        Combo result = catalogService.updateComboVideoReview(74L, "video_rejected", "视频违规", admin);

        assertThat(result).isSameAs(combo);
        verify(comboMapper).updateVideoReviewFields(eq(74L), eq("video_rejected"), eq("视频违规"), any(LocalDateTime.class));
        verify(comboMapper).rejectForVideo(eq(74L), eq("reviewer"), any(LocalDateTime.class));
        verify(comboMapper, never()).updateById(any(Combo.class));
    }

    @Test
    void createComboNormalizesControlType() {
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        Combo saved = catalogService.createAdminCombo(comboRequest(10L, "2MK > DR", "现代", null));

        assertThat(saved.getControlType()).isEqualTo("modern");
    }

    @Test
    void createComboNormalizesWorldTourControlType() {
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        Combo saved = catalogService.createAdminCombo(comboRequest(10L, "2MK > DR", "环球游历", null));

        assertThat(saved.getControlType()).isEqualTo("world-tour");
        assertThat(saved.getRouteCharacterIds()).containsExactly(10L, 10L);
        assertThat(saved.getCharacterId()).isEqualTo(10L);
    }

    @Test
    void worldTourComboRequiresCharacterForEveryMove() {
        ComboRequest request = comboRequest(10L, "2MK > DR", "classic", null);
        ComboRequest invalid = new ComboRequest(
                null, request.starter(), request.route(), request.comboText(), request.damage(), request.driveCost(),
                request.saCost(), request.advantageFrames(), request.difficulty(), request.cornerOnly(), "world-tour",
                List.of(10L), request.type(), request.tags(), request.videoUrl(), request.trainingNotes(),
                request.difficultyNote(), request.followupParentId()
        );

        assertThatThrownBy(() -> catalogService.createAdminCombo(invalid))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("每一招选择角色");
    }

    @Test
    void userComboSubmissionRequiresVideo() {
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");

        assertThatThrownBy(() -> catalogService.createCombo(comboRequestWithoutVideo(10L, "2MK > DR", "classic", null), user))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("演示视频");
    }

    @Test
    void adminComboAlsoRequiresVideo() {
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> catalogService.createAdminCombo(comboRequestWithoutVideo(10L, "2MK > DR", "classic", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("演示视频");
    }

    @Test
    void userComboSubmissionWithVideoIsAccepted() {
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        Combo saved = catalogService.createCombo(comboRequestWithVideo(10L, "2MK > DR", "classic", null), user);

        assertThat(saved.getStatus()).isEqualTo("manual_review");
        assertThat(saved.getVideoReviewStatus()).isEqualTo("unchecked");
    }

    @Test
    void renamedUsernameCannotTakeOverRejectedComboOwnedByAnotherUserId() {
        Combo rejected = combo(91L, 10L, "2MK > DR");
        rejected.setStatus("rejected");
        rejected.setAuthorId(7L);
        rejected.setAuthor("old_name");
        User attacker = new User();
        attacker.setId(8L);
        attacker.setUsername("old_name");
        when(comboMapper.selectById(91L)).thenReturn(rejected);

        assertThatThrownBy(() -> catalogService.deleteCombo(91L, attacker))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能删除自己发布的连招");
    }

    @Test
    void regularUserCannotSubmitExternalVideoUrl() {
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        doThrow(new BizException("连招视频必须使用本人上传且仍然存在的文件"))
                .when(uploadOwnershipService).requireOwnedComboVideo("https://example.com/combo.mp4", 7L);

        assertThatThrownBy(() -> catalogService.createCombo(
                comboRequestWithVideo(10L, "2MK > DR", "classic", null), user))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("本人上传");
    }

    @Test
    void comboListUsesLatestUsernameForAuthorId() {
        Combo combo = combo(1L, 10L, "2MK > DR");
        combo.setAuthorId(7L);
        combo.setAuthor("old_name");
        User user = new User();
        user.setId(7L);
        user.setUsername("new_name");
        when(comboMapper.selectList(any(Wrapper.class))).thenReturn(List.of(combo));
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(user));

        List<Combo> combos = catalogService.combos(null, 10L, null, null, null);

        assertThat(combos).extracting(Combo::getAuthor).containsExactly("new_name");
    }

    @Test
    void comboDetailUsesLatestUsernameForAuthorId() {
        Combo combo = combo(1L, 10L, "2MK > DR");
        combo.setAuthorId(7L);
        combo.setAuthor("old_name");
        User user = new User();
        user.setId(7L);
        user.setUsername("new_name");
        when(comboMapper.selectById(1L)).thenReturn(combo);
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(user));

        Combo result = catalogService.combo(1L, null);

        assertThat(result.getAuthor()).isEqualTo("new_name");
    }

    @Test
    void publicComboDetailRejectsUnapprovedComboFromAnotherUser() {
        Combo combo = combo(1L, 10L, "2MK > DR");
        combo.setStatus("manual_review");
        combo.setAuthorId(7L);
        User viewer = new User();
        viewer.setId(8L);
        when(comboMapper.selectById(1L)).thenReturn(combo);
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> catalogService.visibleCombo(1L, viewer))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权查看");
    }

    @Test
    void authorCanReadOwnUnapprovedCombo() {
        Combo combo = combo(1L, 10L, "2MK > DR");
        combo.setStatus("manual_review");
        combo.setAuthorId(7L);
        User author = new User();
        author.setId(7L);
        when(comboMapper.selectById(1L)).thenReturn(combo);
        when(comboFollowupMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThat(catalogService.visibleCombo(1L, author)).isSameAs(combo);
    }

    @Test
    void comboFilterOptionsComeFromAllApprovedTopLevelCombos() {
        Combo first = combo(1L, 10L, "2MK > DR");
        first.setTags("counter-hit,corner");
        Combo second = combo(2L, 10L, "5HP > SA3");
        second.setTags("corner");
        when(comboMapper.selectFilterOptionRows(10L, "classic")).thenReturn(List.of(first, second));

        var options = catalogService.characterComboFilterOptions(10L, "classic");

        assertThat(options.starters()).containsExactly("2MK", "5HP");
        assertThat(options.tags()).containsExactly("corner", "counter-hit");
    }

    @Test
    void worldTourFilterOptionsOnlyUseWorldTourCombos() {
        Combo combo = combo(1L, null, "2MK > DR");
        combo.setControlType("world-tour");
        combo.setTags("corner");
        when(comboMapper.selectFilterOptionRows(null, "world-tour")).thenReturn(List.of(combo));

        var options = catalogService.worldTourComboFilterOptions();

        verify(comboMapper).selectFilterOptionRows(null, "world-tour");
        assertThat(options.starters()).containsExactly("2MK");
        assertThat(options.tags()).containsExactly("corner");
    }

    @Test
    void worldTourCombosForwardEveryFilterToThePagedQuery() {
        Page<Combo> page = new Page<>(1, 12, 0);
        page.setRecords(List.of());
        when(comboMapper.selectTopLevelPage(any(Page.class), isNull(), eq("world-tour"), eq("2MK"),
                eq(List.of("counter-hit", "corner")), eq(1), eq(new BigDecimal("2.5")), eq(1000), eq(5000), eq("likes")))
                .thenReturn(page);

        catalogService.worldTourCombos("2mk", "counter-hit, corner", 1, new BigDecimal("2.5"), 1000, 5000,
                "likes", null, 1, 12);

        verify(comboMapper).selectTopLevelPage(any(Page.class), isNull(), eq("world-tour"), eq("2MK"),
                eq(List.of("counter-hit", "corner")), eq(1), eq(new BigDecimal("2.5")), eq(1000), eq(5000), eq("likes"));
    }

    private static Combo combo(Long id, Long characterId, String route) {
        Combo combo = new Combo();
        combo.setId(id);
        combo.setCharacterId(characterId);
        combo.setStarter(route.split(" > ")[0]);
        combo.setRoute(route);
        combo.setComboText(route);
        combo.setControlType("classic");
        combo.setStatus("approved");
        return combo;
    }

    private static ComboRequest comboRequest(Long characterId, String route, String controlType, Long followupParentId) {
        return comboRequestWithVideo(characterId, route, controlType, followupParentId);
    }

    private static ComboRequest comboRequestWithoutVideo(Long characterId, String route, String controlType, Long followupParentId) {
        return comboRequest(characterId, route, controlType, followupParentId, "");
    }

    private static ComboRequest comboRequestWithVideo(Long characterId, String route, String controlType, Long followupParentId) {
        return comboRequest(characterId, route, controlType, followupParentId, "https://example.com/combo.mp4");
    }

    private static ComboRequest comboRequest(Long characterId, String route, String controlType, Long followupParentId, String videoUrl) {
        String starter = route.split(" > ")[0];
        String primaryTag = followupParentId == null ? "counter-hit" : "meaty-strike";
        return new ComboRequest(
                characterId,
                starter,
                route,
                route,
                1000,
                BigDecimal.ZERO,
                0,
                "+2",
                "中等",
                false,
                controlType,
                controlType.contains("world") || controlType.contains("环球") ? List.of(characterId, characterId) : List.of(),
                primaryTag,
                List.of(primaryTag),
                videoUrl,
                "",
                "",
                followupParentId
        );
    }
}
