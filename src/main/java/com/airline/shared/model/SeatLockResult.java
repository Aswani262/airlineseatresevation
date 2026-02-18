package com.airline.shared.model;

import java.time.OffsetDateTime;
import java.util.List;

public record SeatLockResult(
            boolean success,
            List<String> seatNumbers,
            OffsetDateTime expiresAt
    ) {}