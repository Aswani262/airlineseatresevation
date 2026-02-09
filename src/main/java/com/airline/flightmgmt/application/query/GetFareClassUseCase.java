package com.airline.flightmgmt.application.query;


import com.airline.flightmgmt.api.dto.FareClassResponse;

public interface GetFareClassUseCase {
    FareClassResponse getByCode(String code);
}
