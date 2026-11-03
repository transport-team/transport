package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.DriverDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;

// 기사 등록 · 상태 조회 · 관리자 목록 · 승인/중지 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

	@Mock
	private DriverRepository driverRepository;

	@InjectMocks
	private DriverServiceImpl driverService;

	@Captor
	private ArgumentCaptor<Driver> driverCaptor;

	private final Member member = Member.builder()
			.id(5L).email("driver@test.com").name("김기사").phone("010-2222-3333").role(MemberRole.DRIVER).build();

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
		when(driverRepository.findByMemberId(5L)).thenReturn(Optional.of(driver(DriverStatus.ACTIVE, null)));

		assertThat(driverService.getStatus(5L)).isEqualTo(DriverStatus.ACTIVE);
	}

	@Test
	void noDriverInfo() {
		when(driverRepository.findByMemberId(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> driverService.getStatus(5L))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void getDrivers() {
		DriverSearchDTO search = DriverSearchDTO.builder().status(DriverStatus.PENDING).keyword("김").build();
		when(driverRepository.search(eq(DriverStatus.PENDING), eq("김"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(driver(DriverStatus.PENDING, null)), PageRequest.of(0, 10), 1));

		PageResponseDTO<DriverDTO> result = driverService.getDrivers(search);

		assertThat(result.getTotalCount()).isEqualTo(1);
		assertThat(result.getDtoList()).extracting(DriverDTO::getEmail).containsExactly("driver@test.com");
		assertThat(result.getDtoList().get(0).getName()).isEqualTo("김기사");
	}

	@Test
	void approveDriver() {
		when(driverRepository.findById(7L)).thenReturn(Optional.of(driver(DriverStatus.PENDING, null)));

		DriverDTO result = driverService.changeStatus(7L, DriverStatus.ACTIVE);

		assertThat(result.getStatus()).isEqualTo(DriverStatus.ACTIVE);
		assertThat(result.getApprovedAt()).isNotNull();
	}

	@Test
	void reapproveKeepsFirstApprovedAt() {
		LocalDateTime firstApprovedAt = LocalDateTime.of(2026, 11, 1, 9, 0);
		when(driverRepository.findById(7L)).thenReturn(Optional.of(driver(DriverStatus.INACTIVE, firstApprovedAt)));

		assertThat(driverService.changeStatus(7L, DriverStatus.ACTIVE).getApprovedAt()).isEqualTo(firstApprovedAt);
	}

	@Test
	void stopDriver() {
		when(driverRepository.findById(7L)).thenReturn(Optional.of(driver(DriverStatus.ACTIVE, LocalDateTime.now())));

		assertThat(driverService.changeStatus(7L, DriverStatus.INACTIVE).getStatus()).isEqualTo(DriverStatus.INACTIVE);
	}

	@Test
	void cannotGoBackToPending() {
		assertThatThrownBy(() -> driverService.changeStatus(7L, DriverStatus.PENDING))
				.isInstanceOf(CustomException.class)
				.hasMessage("승인 대기로 되돌릴 수 없습니다.");
	}

	@Test
	void changeStatusOfUnknownDriver() {
		when(driverRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> driverService.changeStatus(99L, DriverStatus.ACTIVE))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}

	private Driver driver(DriverStatus status, LocalDateTime approvedAt) {
		return Driver.builder()
				.id(7L).member(member).licenseType("1종 보통").careerYears(5).status(status).approvedAt(approvedAt).build();
	}
}
