package com.transport.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CustomerAddress;
import com.transport.domain.Member;
import com.transport.dto.CustomerAddressRequestDTO;
import com.transport.dto.CustomerAddressResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CustomerAddressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAddressServiceImpl implements CustomerAddressService {

	private final CustomerAddressRepository customerAddressRepository;
	private final MemberService memberService;

	@Override
	@Transactional(readOnly = true)
	public Optional<CustomerAddressResponseDTO> getDefaultAddress(Long memberId) {
		return customerAddressRepository.findByMemberId(memberId)
				.map(CustomerAddressResponseDTO::from);
	}

	@Override
	public CustomerAddressResponseDTO saveDefaultAddress(Long memberId, CustomerAddressRequestDTO requestDTO) {
		Member member = memberService.findById(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		CustomerAddress address = customerAddressRepository.findByMemberId(memberId)
				.map(existing -> {
					existing.update(requestDTO.getLabel(), requestDTO.getOriginAddress(), requestDTO.getOriginDetail(),
							requestDTO.getDestAddress(), requestDTO.getDestDetail());
					return existing;
				})
				.orElseGet(() -> CustomerAddress.builder()
						.member(member)
						.label(requestDTO.getLabel())
						.originAddress(requestDTO.getOriginAddress())
						.originDetail(requestDTO.getOriginDetail())
						.destAddress(requestDTO.getDestAddress())
						.destDetail(requestDTO.getDestDetail())
						.build());

		return CustomerAddressResponseDTO.from(customerAddressRepository.save(address));
	}

	@Override
	public void deleteDefaultAddress(Long memberId) {
		customerAddressRepository.findByMemberId(memberId)
				.ifPresent(customerAddressRepository::delete);
	}

	@Override
    public List<CustomerAddressResponseDTO> findRecentAddressesByMemberId(Long memberId) {
        // 1. 레포지토리에서 엔티티 리스트 조회
        List<CustomerAddress> addresses = customerAddressRepository.findByMemberIdOrderByUpdatedAtDesc(memberId);
        
        // 2. 엔티티(Entity) 리스트를 DTO 리스트로 변환해서 반환
        return addresses.stream()
                .map(address -> {
                    // 💡 프로젝트에서 사용하는 엔티티 -> DTO 변환 방식에 맞게 수정하세요!
                    // (예: ModelMapper를 쓰신다면 modelMapper.map(address, CustomerAddressResponseDTO.class))
                    // 아래는 빌더나 생성자를 사용하는 예시입니다.
                    return CustomerAddressResponseDTO.builder()
                            .id(address.getId())
                            .label(address.getLabel())
                            .originAddress(address.getOriginAddress())
                            .originDetail(address.getOriginDetail())
                            .destAddress(address.getDestAddress())
                            .destDetail(address.getDestDetail())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
