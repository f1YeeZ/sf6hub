package com.example.hubdemo.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.hubdemo.entity.FrameData;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.dto.AdminDtos.ComboVideoReviewRequest;
import com.example.hubdemo.mapper.AdminAuditLogMapper;
import com.example.hubdemo.mapper.AdminDashboardMapper;
import com.example.hubdemo.mapper.AnnouncementMapper;
import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.FeedbackMapper;
import com.example.hubdemo.mapper.FrameChangeHistoryMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import com.example.hubdemo.mapper.FrameSyncLogMapper;
import com.example.hubdemo.mapper.ReportMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.service.AdminAuditService;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CatalogService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.FrameSyncLogService;
import com.example.hubdemo.service.NotificationService;
import com.example.hubdemo.service.OfficialFrameSyncService;
import com.example.hubdemo.service.UserService;
import com.example.hubdemo.service.VisitAnalyticsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerFrameSearchTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void frameSearchIsAppliedBeforePagination() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FrameData.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        User admin = new User();
        admin.setRole("admin");
        when(currentUserService.requireAdminSession(any())).thenReturn(admin);
        when(frameDataMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<FrameData> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        AdminController controller = new AdminController(
                mock(CatalogService.class),
                mock(UserService.class),
                mock(OfficialFrameSyncService.class),
                mock(FrameSyncLogService.class),
                currentUserService,
                mock(NotificationService.class),
                mock(AdminAuditService.class),
                mock(AdminRealtimeService.class),
                mock(UserMapper.class),
                mock(CharacterMapper.class),
                frameDataMapper,
                mock(FrameSyncLogMapper.class),
                mock(FrameChangeHistoryMapper.class),
                mock(ComboMapper.class),
                mock(ReportMapper.class),
                mock(FeedbackMapper.class),
                mock(AnnouncementMapper.class),
                mock(AdminAuditLogMapper.class),
                mock(VisitAnalyticsService.class),
                mock(AdminDashboardMapper.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/frames").param("q", "波动拳"))
                .andExpect(status().isOk());

        ArgumentCaptor<LambdaQueryWrapper<FrameData>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(frameDataMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<FrameData> wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("move_name");
        assertThat(wrapper.getParamNameValuePairs().values())
                .anyMatch(value -> String.valueOf(value).contains("波动拳"));
    }

    @Test
    void videoRejectionAlsoRejectsComboWithFixedReason() {
        CatalogService catalogService = mock(CatalogService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ComboMapper comboMapper = mock(ComboMapper.class);
        User admin = new User();
        admin.setRole("admin");
        admin.setUsername("reviewer");
        Combo combo = new Combo();
        combo.setId(74L);
        combo.setStarter("214HP");
        combo.setStatus("rejected");
        combo.setRejectionReason("视频违规");
        combo.setVideoReviewReason("视频违规");
        combo.setManualReviewReason("");
        combo.setReviewedBy("reviewer");
        when(currentUserService.requireAdminSession(any())).thenReturn(admin);
        when(catalogService.updateComboVideoReview(74L, "video_rejected", "视频违规", admin)).thenReturn(combo);

        AdminController controller = new AdminController(
                catalogService,
                mock(UserService.class),
                mock(OfficialFrameSyncService.class),
                mock(FrameSyncLogService.class),
                currentUserService,
                mock(NotificationService.class),
                mock(AdminAuditService.class),
                mock(AdminRealtimeService.class),
                mock(UserMapper.class),
                mock(CharacterMapper.class),
                mock(FrameDataMapper.class),
                mock(FrameSyncLogMapper.class),
                mock(FrameChangeHistoryMapper.class),
                comboMapper,
                mock(ReportMapper.class),
                mock(FeedbackMapper.class),
                mock(AnnouncementMapper.class),
                mock(AdminAuditLogMapper.class),
                mock(VisitAnalyticsService.class),
                mock(AdminDashboardMapper.class)
        );

        controller.updateComboVideoReview(74L,
                new ComboVideoReviewRequest("video_rejected", "其他原因"),
                mock(HttpServletRequest.class));

        assertThat(combo.getStatus()).isEqualTo("rejected");
        assertThat(combo.getRejectionReason()).isEqualTo("视频违规");
        assertThat(combo.getVideoReviewReason()).isEqualTo("视频违规");
        assertThat(combo.getManualReviewReason()).isEmpty();
        assertThat(combo.getReviewedBy()).isEqualTo("reviewer");
        verify(catalogService).updateComboVideoReview(74L, "video_rejected", "视频违规", admin);
    }
}
