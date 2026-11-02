package com.transport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseEntity의 created_at, updated_at 자동 입력
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
