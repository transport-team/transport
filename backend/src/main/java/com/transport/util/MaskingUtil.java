package com.transport.util;

// 개인정보 가리기 — 리뷰 작성 고객 이름 (11-19 결정 4: 리뷰 목록은 로그인 사용자 누구나 보므로 이름은 가린다)
public final class MaskingUtil {

	private MaskingUtil() {
	}

	/**
	 * 처음과 마지막 글자만 남기고 가운데를 '*'로 가린다.
	 * 김민수 → 김*수, 남궁현석 → 남**석, 이민 → 이*(두 글자는 뒤를), 김 → *(한 글자는 전부)
	 * null · 빈칸은 그대로 돌려주고, 앞뒤 공백은 떼고 가린다.
	 * 글자는 code point로 센다 (이모지처럼 char 2개인 글자가 반으로 잘리지 않게).
	 */
	public static String maskName(String name) {
		if (name == null || name.isBlank()) {
			return name;
		}
		int[] letters = name.strip().codePoints().toArray();
		int count = letters.length;
		if (count == 1) {
			return "*";
		}
		StringBuilder masked = new StringBuilder().appendCodePoint(letters[0]);
		if (count == 2) {
			return masked.append('*').toString();
		}
		return masked.append("*".repeat(count - 2))
				.appendCodePoint(letters[count - 1])
				.toString();
	}
}
