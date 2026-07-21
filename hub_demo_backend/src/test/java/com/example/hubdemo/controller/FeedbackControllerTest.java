package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.FeedbackDtos.FeedbackRequest;
import com.example.hubdemo.entity.Feedback;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.FeedbackMapper;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackControllerTest {
    @Test
    void storesFrontendFeedbackInFeedbackModule() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
        RateLimitService rateLimitService = mock(RateLimitService.class);
        AdminRealtimeService adminRealtimeService = mock(AdminRealtimeService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        when(currentUserService.require(request)).thenReturn(user);

        FeedbackController controller = new FeedbackController(currentUserService, feedbackMapper, rateLimitService, adminRealtimeService);
        Feedback result = controller.submitFeedback(new FeedbackRequest("wrong_data", "  帧数显示错了  "), request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(rateLimitService).check(eq(request), eq("feedback:create"), eq(10), any());
        verify(feedbackMapper).insert(captor.capture());
        verify(adminRealtimeService).publishAdmin("feedback:create", "feedbacks", "dashboard");
        Feedback saved = captor.getValue();
        assertThat(result).isSameAs(saved);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getUsername()).isEqualTo("tester");
        assertThat(saved.getReason()).isEqualTo("wrong_data");
        assertThat(saved.getDetail()).isEqualTo("帧数显示错了");
        assertThat(saved.getStatus()).isEqualTo("pending");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsBlankFeedbackDetail() {
        FeedbackController controller = new FeedbackController(
                mock(CurrentUserService.class),
                mock(FeedbackMapper.class),
                mock(RateLimitService.class),
                mock(AdminRealtimeService.class)
        );

        assertThatThrownBy(() -> controller.submitFeedback(
                new FeedbackRequest("other", "   "),
                new MockHttpServletRequest()
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("反馈内容不能为空");
    }
}
