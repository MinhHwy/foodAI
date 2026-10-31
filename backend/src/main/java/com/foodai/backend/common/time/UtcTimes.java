package com.foodai.backend.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class UtcTimes {

	public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	private UtcTimes() {
	}

	public static LocalDateTime nowUtc() {
		return LocalDateTime.now(ZoneOffset.UTC);
	}

	public static LocalDate today(ZoneId zone) {
		return LocalDate.now(zone);
	}

	public static ZoneId safeZone(String zoneName) {
		if (zoneName == null || zoneName.isBlank()) {
			return DEFAULT_ZONE;
		}
		try {
			return ZoneId.of(zoneName.trim());
		}
		catch (RuntimeException ex) {
			return DEFAULT_ZONE;
		}
	}
}
