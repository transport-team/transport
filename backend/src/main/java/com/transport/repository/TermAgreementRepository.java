package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.transport.domain.TermAgreement;

// 약관 동의 이력
public interface TermAgreementRepository extends JpaRepository<TermAgreement, Long> {

	// 약관별 동의 수 (관리자 목록 — 약관마다 따로 세지 않고 쿼리 한 번으로)
	@Query("select ta.term.id as termId, count(ta) as agreementCount from TermAgreement ta group by ta.term.id")
	List<TermAgreementCount> countGroupByTerm();

	// 한 약관에 동의한 회원 수 (0이면 삭제할 수 있다)
	long countByTermId(Long termId);

	// 약관별 동의 수 결과
	interface TermAgreementCount {
		Long getTermId();

		Long getAgreementCount();
	}
}
