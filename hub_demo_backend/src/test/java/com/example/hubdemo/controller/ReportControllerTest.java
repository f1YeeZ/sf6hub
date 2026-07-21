package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.ReportMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ReportControllerTest {
    @Test
    void rejectsSiteReportsBecauseFeedbackHasItsOwnModule() {
        ReportController controller = new ReportController(
                mock(CurrentUserService.class),
                mock(ReportMapper.class),
                mock(ComboMapper.class),
                mock(UserMapper.class),
                mock(RateLimitService.class),
                mock(AdminRealtimeService.class)
        );

        assertThatThrownBy(() -> controller.report(
                Map.of("targetType", "site", "targetId", 1, "reason", "other", "detail", "test"),
                new MockHttpServletRequest()
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("举报对象类型不合法");
    }
}
