package com.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.CustomerAddress;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

	Optional<CustomerAddress> findByMemberId(Long memberId);
	
	List<CustomerAddress> findByMemberIdOrderByUpdatedAtDesc(Long memberId);
	
}
