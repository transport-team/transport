package com.transport.dto;

import java.util.List;

import com.transport.domain.enums.MemberRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원가입 요청 (기사는 면허 종류 · 경력도 받는다)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDTO {

	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 100, message = "이메일은 100자 이하로 입력해 주세요.")
	private String email;

	@NotBlank(message = "비밀번호를 입력해 주세요.")
	@Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.")
	private String password;

	@NotBlank(message = "이름을 입력해 주세요.")
	@Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
	private String name;

	@NotBlank(message = "연락처를 입력해 주세요.")
	@Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "연락처는 010-0000-0000 형식으로 입력해 주세요.")
	private String phone;

	@NotNull(message = "회원 유형을 선택해 주세요.")
	private MemberRole role;  // CUSTOMER · DRIVER (ADMIN은 가입할 수 없다)

	@NotNull(message = "약관 동의 정보가 없습니다.")
	private List<Long> agreedTermIds;  // 동의한 약관 id (필수 약관은 모두 있어야 한다)

	// 기사만 (고객은 비워 둔다)
	@Size(max = 20, message = "면허 종류는 20자 이하로 입력해 주세요.")
	private String licenseType;  // 예: 1종 보통

	@Min(value = 0, message = "경력은 0년 이상으로 입력해 주세요.")
	@Max(value = 60, message = "경력은 60년 이하로 입력해 주세요.")
	private Integer careerYears;  // 경력(년)
}
