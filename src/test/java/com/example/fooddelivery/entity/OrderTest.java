package com.example.fooddelivery.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

class OrderTest {

	@Test
	void transitionTo_followsTheDocumentedStateMachine() {
		Order order = new Order();
		order.setStatus(OrderStatus.PLACED);

		order.transitionTo(OrderStatus.ACCEPTED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);

		order.transitionTo(OrderStatus.PREPARING);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

		order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);

		order.transitionTo(OrderStatus.DELIVERED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	void transitionTo_rejectsSkippingStates() {
		Order order = new Order();
		order.setStatus(OrderStatus.PLACED);

		assertThrows(
				InvalidStateTransitionException.class, () -> order.transitionTo(OrderStatus.OUT_FOR_DELIVERY));
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
	}

	@Test
	void transitionTo_rejectsTransitionsFromTerminalStates() {
		Order order = new Order();
		order.setStatus(OrderStatus.DELIVERED);

		assertThrows(InvalidStateTransitionException.class, () -> order.transitionTo(OrderStatus.CANCELLED));
	}

	@Test
	void transitionTo_allowsRejectOrCancelOnlyFromPlaced() {
		Order rejectable = new Order();
		rejectable.setStatus(OrderStatus.PLACED);
		rejectable.transitionTo(OrderStatus.REJECTED);
		assertThat(rejectable.getStatus()).isEqualTo(OrderStatus.REJECTED);

		Order accepted = new Order();
		accepted.setStatus(OrderStatus.ACCEPTED);
		assertThrows(InvalidStateTransitionException.class, () -> accepted.transitionTo(OrderStatus.CANCELLED));
	}
}
