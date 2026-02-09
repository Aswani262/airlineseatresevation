package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.api.dto.FareClassResponse;

public interface FareClassRepository {
    FareClassResponse getByCode(String code);
}
