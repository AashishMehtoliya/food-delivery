package com.example.fooddelivery.event;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

	private final NotificationRegistry notificationRegistry;

	public OrderNotificationListener(NotificationRegistry notificationRegistry) {
		this.notificationRegistry = notificationRegistry;
	}

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderStatusChanged(OrderStatusChangedEvent event) {
		String threadName = Thread.currentThread().getName();
		log.info(
				"Notifying customer, restaurant, and delivery partner: order {} status {} -> {} (thread={})",
				event.orderId(),
				event.oldStatus(),
				event.newStatus(),
				threadName);
		notificationRegistry.record(
				new NotificationRecord(event.orderId(), event.oldStatus(), event.newStatus(), threadName, Instant.now()));
	}
}
