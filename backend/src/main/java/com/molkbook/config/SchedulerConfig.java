package com.molkbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring Boot 自动配置定时任务
    // 定时任务实现在 AIContentScheduler 中
}
