package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Term;

// 약관
public interface TermRepository extends JpaRepository<Term, Long> {

	// 지금 쓰는 약관 — 가입 화면 순서: 필수 먼저, 그다음 등록 순 (개정하면 새 id가 생겨 id 순만으로는 선택 약관이 끼어든다)
	List<Term> findAllByActiveTrueOrderByRequiredDescIdAsc();

	// 관리자 목록: 사용 중인 약관(가입 화면 순서) 다음에 지난 버전 · 사용 중지
	List<Term> findAllByOrderByActiveDescRequiredDescIdAsc();

	// 같은 이름의 약관이 이미 사용 중인지 (가입 화면에 같은 약관이 두 번 뜨지 않게)
	boolean existsByTitleAndActiveTrue(String title);

	// 위와 같고, 개정 · 다시 사용하는 약관 자신은 뺀다
	boolean existsByTitleAndActiveTrueAndIdNot(String title, Long id);
}
