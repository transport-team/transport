package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Dispatch;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.repository.DispatchRepository;

@ExtendWith(MockitoExtension.class)
class DispatchServiceImplTest {

    @Mock
    private DispatchRepository dispatchRepository;

    @InjectMocks
    private DispatchServiceImpl dispatchService;

    @Test
    @DisplayName("활성 배차가 있으면 기사 회원 ID를 반환한다")
    void getActiveDriver_success() {

        // 기사 회원 ID는 15
        Member member = Member.builder()
                .id(15L)
                .build();

        // 기사 정보 ID는 3
        Driver driver = Driver.builder()
                .id(3L)
                .member(member)
                .build();

        // 배송 10에 해당 기사가 배정된 상황
        Dispatch dispatch = Dispatch.builder()
                .id(1L)
                .driver(driver)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .build();

        when(dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L))
                .thenReturn(Optional.of(dispatch));

        Long result = dispatchService.getActiveDriver(10L);

        // driver.id(3)가 아니라 member.id(15)를 반환해야 함
        assertThat(result).isEqualTo(15L);

        verify(dispatchRepository)
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L);
    }

    @Test
    @DisplayName("활성 배차가 없으면 null을 반환한다")
    void getActiveDriver_notFound() {

        when(dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L))
                .thenReturn(Optional.empty());

        Long result = dispatchService.getActiveDriver(10L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("활성 배차를 해제하면 기록은 남고 해제 시각이 기록된다")
    void releaseDispatch_success() {
        Dispatch dispatch = Dispatch.builder()
                .id(1L)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .build();

        when(dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L))
                .thenReturn(Optional.of(dispatch));

        dispatchService.releaseDispatch(10L);

        assertThat(dispatch.isActive()).isFalse();
        assertThat(dispatch.getReleasedAt()).isNotNull();

        LocalDateTime releasedAt = dispatch.getReleasedAt();
        dispatchService.releaseDispatch(10L);
        assertThat(dispatch.getReleasedAt()).isEqualTo(releasedAt);
    }

    @Test
    @DisplayName("활성 배차가 없는 배송은 안전하게 해제 요청을 무시한다")
    void releaseDispatch_notFound() {
        when(dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L))
                .thenReturn(Optional.empty());

        dispatchService.releaseDispatch(10L);

        verify(dispatchRepository)
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L);
    }

    @Test
    @DisplayName("배송의 활성 배차 — 있으면 그 배차, 없으면 빈 값 (배송 상세가 저장소 대신 부름, 11-25)")
    void findActiveDispatch_returnsLatestActiveOrEmpty() {
        Dispatch active = Dispatch.builder().id(7L).active(true).assignedAt(LocalDateTime.now()).build();
        when(dispatchRepository.findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L)).thenReturn(Optional.of(active));
        when(dispatchRepository.findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(11L)).thenReturn(Optional.empty());

        assertThat(dispatchService.findActiveDispatch(10L)).containsSame(active);
        assertThat(dispatchService.findActiveDispatch(11L)).isEmpty();
    }
}
