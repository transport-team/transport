package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.CustomerSavedAddress;
import com.transport.domain.Member;
import com.transport.dto.CustomerSavedAddressRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CustomerSavedAddressRepository;

@ExtendWith(MockitoExtension.class)
class CustomerSavedAddressServiceImplTest {

    @Mock
    CustomerSavedAddressRepository savedAddressRepository;

    @Mock
    MemberService memberService;

    @InjectMocks
    CustomerSavedAddressServiceImpl service;

    private final Long myId = 10L;

    @Test
    void listOnlyMyAddresses() {
        CustomerSavedAddress home = CustomerSavedAddress.builder()
                .id(1L).label("우리 집").address("서울시 테스트로 1").detail("101호").build();
        CustomerSavedAddress office = CustomerSavedAddress.builder()
                .id(2L).label("회사").address("서울시 테스트로 2").build();
        when(savedAddressRepository.findByMemberIdOrderByIdDesc(myId))
                .thenReturn(List.of(office, home));

        var result = service.list(myId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).label()).isEqualTo("회사");
        assertThat(result.get(1).address()).isEqualTo("서울시 테스트로 1");
    }

    @Test
    void createOneLocationWithNickname() {
        Member me = Member.builder().id(myId).build();
        when(memberService.findById(myId)).thenReturn(Optional.of(me));
        when(savedAddressRepository.save(any(CustomerSavedAddress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(myId,
                new CustomerSavedAddressRequestDTO(" 우리 집 ", " 서울시 테스트로 1 ", " 101호 "));

        assertThat(result.label()).isEqualTo("우리 집");
        assertThat(result.address()).isEqualTo("서울시 테스트로 1");
        assertThat(result.detail()).isEqualTo("101호");
    }

    @Test
    void updateAnotherMembersAddressIsNotAllowed() {
        when(savedAddressRepository.findByIdAndMemberId(77L, myId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(myId, 77L,
                new CustomerSavedAddressRequestDTO("회사", "서울시 테스트로 2", "")))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void deleteOnlyMyOwnAddress() {
        CustomerSavedAddress address = CustomerSavedAddress.builder().id(3L).build();
        when(savedAddressRepository.findByIdAndMemberId(3L, myId))
                .thenReturn(Optional.of(address));

        service.delete(myId, 3L);

        verify(savedAddressRepository).delete(address);
    }
}
