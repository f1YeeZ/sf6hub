package com.example.hubdemo.service;

import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import com.example.hubdemo.mapper.FrameSyncLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OfficialFrameSyncServiceContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CharacterMapper.class, () -> mock(CharacterMapper.class))
            .withBean(FrameDataMapper.class, () -> mock(FrameDataMapper.class))
            .withBean(FrameSyncLogMapper.class, () -> mock(FrameSyncLogMapper.class))
            .withBean(OfficialFrameSyncService.class)
            .withBean(FrameSyncLogService.class)
            .withBean(OfficialFrameSyncJob.class)
            .withPropertyValues(
                    "app.official-frame-sync.base-url=https://example.test/character",
                    "app.official-frame-sync.enabled=true",
                    "app.official-frame-sync.full-sync=false",
                    "app.official-frame-sync.request-delay-ms=0"
            );

    @Test
    void createsOfficialFrameSyncServiceWithSpringConstructorInjection() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OfficialFrameSyncService.class);
            assertThat(context).hasSingleBean(OfficialFrameSyncJob.class);
        });
    }
}
