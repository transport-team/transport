package com.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.CustomerSavedAddress;

public interface CustomerSavedAddressRepository extends JpaRepository<CustomerSavedAddress, Long> {

    List<CustomerSavedAddress> findByMemberIdOrderByIdDesc(Long memberId);

    Optional<CustomerSavedAddress> findByIdAndMemberId(Long id, Long memberId);
}
