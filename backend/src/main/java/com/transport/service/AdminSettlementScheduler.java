package com.transport.service;

import java.time.YearMonth;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 서울 시각 매일 00:05 지난달 마감을 재시도한다.
// 1일에 자동 확정하며, 서버가 꺼졌거나 수수료 미설정 시 다음 실행에서 재시도한다.
@Component
public class AdminSettlementScheduler {
    private static final Logger log = LoggerFactory.getLogger(AdminSettlementScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final AdminSettlementService settlementService;

    public AdminSettlementScheduler(AdminSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        closePreviousMonth();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void closePreviousMonth() {
        String month = YearMonth.now(SEOUL).minusMonths(1).toString();
        try {
            settlementService.closeMonth(month);
        } catch (Exception ex) {
            // 수수료 미설정 등으로 중단되면 서버는 유지하고 관리자 조치 후 재시도.
            log.warn("월 정산 자동 마감 실패 ({}): {}", month, ex.getMessage());
        }
    }
}
