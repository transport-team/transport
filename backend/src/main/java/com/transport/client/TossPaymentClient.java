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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class TossPaymentClient {

	private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
	private static final String CANCEL_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";

	private final RestTemplate restTemplate;
	private final String secretKey;

	public TossPaymentClient(
			@Value("${TOSS_SECRET_KEY:${app.toss.secret-key:}}") String secretKey,
			@Value("${app.toss.connect-timeout-ms:3000}") int connectTimeoutMs,
			@Value("${app.toss.read-timeout-ms:5000}") int readTimeoutMs) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeoutMs);
		requestFactory.setReadTimeout(readTimeoutMs);
		this.restTemplate = new RestTemplate(requestFactory);
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
		} catch (ResourceAccessException exception) {
			log.warn("토스 결제 승인 통신 실패: {}", exception.getMessage());
			throw new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 승인 서버와 통신하지 못했습니다.");
		}
	}

	public void cancelPayment(String paymentKey, String cancelReason) {
			if (secretKey.isBlank()) {
				throw new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 시크릿 키가 설정되지 않았습니다.");
			}

			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.set(HttpHeaders.AUTHORIZATION, authorizationValue());
				restTemplate.exchange(
						UriComponentsBuilder.fromUriString(CANCEL_URL)
								.buildAndExpand(paymentKey).toUri(),
						HttpMethod.POST,
						new HttpEntity<>(Map.of("cancelReason", cancelReason), headers),
						new ParameterizedTypeReference<Map<String, Object>>() {});
			} catch (HttpStatusCodeException exception) {
				log.error("토스 결제 취소 실패: status={}, body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
				throw new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 취소에 실패했습니다.");
			} catch (ResourceAccessException exception) {
				log.error("토스 결제 취소 통신 실패: {}", exception.getMessage());
				throw new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 취소 서버와 통신하지 못했습니다.");
			}
	}

	private String authorizationValue() {
		String credentials = Base64.getEncoder()
				.encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
		return "Basic " + credentials;
	}
}
