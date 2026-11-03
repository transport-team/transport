package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Term;

// 약관
public interface TermRepository extends JpaRepository<Term, Long> {

	// 지금 쓰는 약관 (가입 화면에 보여 줄 순서대로)
	List<Term> findAllByActiveTrueOrderByIdAsc();
}
