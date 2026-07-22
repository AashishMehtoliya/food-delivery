package com.example.fooddelivery.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Mocked payment gateway (Section 4, assumption 1). Deterministically fails for the
 * documented sentinel amount so the rollback path is testable; succeeds otherwise.
 */
@Service
public class PaymentService {

	public static final BigDecimal FAILURE_TRIGGER_AMOUNT = new BigDecimal("13.13");
	public static final String MOCK_METHOD = "MOCK_WALLET";

	public boolean charge(BigDecimal amount) {
		return amount.compareTo(FAILURE_TRIGGER_AMOUNT) != 0;
	}
}
