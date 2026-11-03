package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;

// 기사 등록 · 상태 조회 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

	@Mock
	private DriverRepository driverRepository;

	@InjectMocks
	private DriverServiceImpl driverService;

	@Captor
	private ArgumentCaptor<Driver> driverCaptor;

	private final Member member = Member.builder().id(5L).email("driver@test.com").role(MemberRole.DRIVER).build();

	@Test
	void registerPendingDriver() {
		when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
			Driver driver = invocation.getArgument(0);
			ReflectionTestUtils.setField(driver, "id", 7L);
			return driver;
		});

		Long driverId = driverService.register(member, "1종 보통", 5);

		assertThat(driverId).isEqualTo(7L);
		verify(driverRepository).save(driverCaptor.capture());
		Driver saved = driverCaptor.getValue();
		assertThat(saved.getMember()).isSameAs(member);
		assertThat(saved.getLicenseType()).isEqualTo("1종 보통");
		assertThat(saved.getCareerYears()).isEqualTo(5);
		assertThat(saved.getStatus()).isEqualTo(DriverStatus.PENDING);
		assertThat(saved.getApprovedAt()).isNull();
	}

	@Test
	void getStatus() {
		when(driverRepository.findByMemberId(5L))
				.thenReturn(Optional.of(Driver.builder().member(member).status(DriverStatus.ACTIVE).build()));

		assertThat(driverService.getStatus(5L)).isEqualTo(DriverStatus.ACTIVE);
	}

	@Test
	void noDriverInfo() {
		when(driverRepository.findByMemberId(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> driverService.getStatus(5L))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}
}
