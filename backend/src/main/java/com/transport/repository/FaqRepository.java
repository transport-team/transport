package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Faq;

// FAQ
public interface FaqRepository extends JpaRepository<Faq, Long> {

	// 공개 FAQ: 노출 중인 것만 순서대로
	List<Faq> findByActiveTrueOrderBySortOrderAscIdAsc();

	// 공개 FAQ: 분류 하나만
	List<Faq> findByActiveTrueAndCategoryOrderBySortOrderAscIdAsc(String category);

	// 관리자: 숨긴 것까지 순서대로
	List<Faq> findAllByOrderBySortOrderAscIdAsc();
}
