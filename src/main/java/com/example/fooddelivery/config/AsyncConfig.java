package com.example.fooddelivery.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	// Dedicated, bounded executor for notification fan-out (Section 11) - deliberately not
	// Spring's default SimpleAsyncTaskExecutor, which is unbounded and would let a burst of
	// status changes spawn unlimited threads. CallerRunsPolicy means a saturated pool makes
	// the triggering async caller (a notification-executor thread, not the request thread,
	// since this only runs after commit) execute the task itself instead of dropping it or
	// throwing - a burst slows down, it doesn't OOM or silently lose notifications.
	@Bean("notificationExecutor")
	public ThreadPoolTaskExecutor notificationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("notification-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
