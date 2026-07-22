package com.example.fooddelivery.service;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mocked payment gateway (Section 4, assumption 1). Deterministically fails for the
 * documented sentinel amount so the rollback path is testable; succeeds otherwise.
 */
@Service
public class PaymentService {

	public static final BigDecimal FAILURE_TRIGGER_AMOUNT = new BigDecimal("13.13");
	public static final String MOCK_METHOD = "MOCK_WALLET";

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	public boolean charge(BigDecimal amount) {
		return amount.compareTo(FAILURE_TRIGGER_AMOUNT) != 0;
	}

	public boolean refund(BigDecimal amount) {
		log.info("Mock refund issued for amount {}", amount);
		return true;
	}
}
