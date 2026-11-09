package com.transport.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class TossPaymentClient {

	private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

	private final RestTemplate restTemplate;
	private final String secretKey;

	public TossPaymentClient(
			@Value("${TOSS_SECRET_KEY:${app.toss.secret-key:}}") String secretKey) {
		this.restTemplate = new RestTemplate();
		this.secretKey = secretKey;
	}

	public Map<String, Object> confirm(String paymentKey, String orderId, int amount) {
		if (secretKey.isBlank()) {
			throw new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 시크릿 키가 설정되지 않았습니다.");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String credentials = Base64.getEncoder()
				.encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
		headers.set(HttpHeaders.AUTHORIZATION, "Basic " + credentials);

		try {
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
					CONFIRM_URL,
					HttpMethod.POST,
					new HttpEntity<>(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount), headers),
					new ParameterizedTypeReference<>() {});
			return response.getBody() == null ? Map.of() : response.getBody();
		} catch (HttpStatusCodeException exception) {
			log.warn("토스 결제 승인 실패: status={}, body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
			throw new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 승인이 거절되었습니다.");
		}
	}
}
