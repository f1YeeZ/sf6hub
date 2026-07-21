package com.example.hubdemo.service;

import com.example.hubdemo.dto.AnalyticsDtos.VisitTrackRequest;
import com.example.hubdemo.entity.VisitLog;
import com.example.hubdemo.mapper.DailyVisitStatsMapper;
import com.example.hubdemo.mapper.VisitLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitAnalyticsServiceTest {
    @Test
    void trackingOnlyAppendsLogAndDefersDailyAggregation() {
        VisitLogMapper visitLogMapper = mock(VisitLogMapper.class);
        DailyVisitStatsMapper dailyStatsMapper = mock(DailyVisitStatsMapper.class);
        when(visitLogMapper.countRecentSamePath(any(), any(), any())).thenReturn(0L);
        VisitAnalyticsService service = new VisitAnalyticsService(
                visitLogMapper,
                dailyStatsMapper,
                mock(CurrentUserService.class),
                mock(AdminRealtimeService.class),
                new ClientIpResolver("")
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");

        var result = service.track(new VisitTrackRequest("/", "0123456789abcdef", ""), request);

        assertThat(result.get("tracked")).isEqualTo(true);
        verify(visitLogMapper).insert(any(VisitLog.class));
        verify(dailyStatsMapper, never()).upsert(any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }
}
