package com.transport.service;

import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;

// 기사 (팀원 2 도메인 — 팀원 2 부재 기간에 팀원 1이 회원가입 · 로그인에 필요한 메서드만 작성, 복귀 후 인계)
// 다른 도메인은 이 메서드로만 기사 정보를 다룬다 (docs/collaboration.md 약속 표)
public interface DriverService {

	// 기사 회원가입 → 승인 대기(PENDING) 기사 생성 → 기사 id
	Long register(Member member, String licenseType, int careerYears);

	// 기사 상태 (로그인할 때 승인 여부 확인)
	DriverStatus getStatus(Long memberId);
}
