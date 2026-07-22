package com.example.fooddelivery.event;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the mocked notification channel (Section 11, point 4: "log the notification
 * or persist a Notification record"). Section 6's domain model has no Notification entity,
 * so rather than adding one, this keeps an in-memory record of what was "sent" - enough to
 * prove the async, after-commit wiring works without a schema change or real delivery.
 */
@Component
public class NotificationRegistry {

	private final List<NotificationRecord> records = new CopyOnWriteArrayList<>();

	public void record(NotificationRecord record) {
		records.add(record);
	}

	public List<NotificationRecord> findByOrderId(Long orderId) {
		return records.stream().filter(r -> r.orderId().equals(orderId)).toList();
	}

	public List<NotificationRecord> all() {
		return Collections.unmodifiableList(records);
	}

	public void clear() {
		records.clear();
	}
}
