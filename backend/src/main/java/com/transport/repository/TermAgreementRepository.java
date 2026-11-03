package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.TermAgreement;

// 약관 동의 이력
public interface TermAgreementRepository extends JpaRepository<TermAgreement, Long> {
}
