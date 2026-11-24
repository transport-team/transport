package com.transport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 작업 활성화 (7-4 — LocationCleanupScheduler용)
// 프로젝트에 @Scheduled 작업이 이게 처음이라 여기서 한 번에 켠다
@Configuration
@EnableScheduling
public class SchedulingConfig {
}