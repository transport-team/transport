package com.transport.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.Inquiry;
import com.transport.domain.enums.InquiryStatus;

// 1:1 문의
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	// 내 문의 (작성자를 함께 가져온다)
	@Query(value = "select i from Inquiry i join fetch i.member m where m.id = :memberId",
			countQuery = "select count(i) from Inquiry i where i.member.id = :memberId")
	Page<Inquiry> findMyInquiries(@Param("memberId") Long memberId, Pageable pageable);

	// 관리자 목록: 상태가 없으면 전체 (작성자 이름 · 역할을 보여 주려고 함께 가져온다)
	@Query(value = "select i from Inquiry i join fetch i.member m where (:status is null or i.status = :status)",
			countQuery = "select count(i) from Inquiry i where (:status is null or i.status = :status)")
	Page<Inquiry> search(@Param("status") InquiryStatus status, Pageable pageable);
}
