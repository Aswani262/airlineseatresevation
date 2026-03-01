package com.airline.shared.model;

import java.time.OffsetDateTime;

public record SeatLockResult(
            boolean success,
            OffsetDateTime expiresAt
    ) {}