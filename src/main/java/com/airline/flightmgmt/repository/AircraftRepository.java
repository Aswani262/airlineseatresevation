package com.airline.flightmgmt.repository;


import com.airline.flightmgmt.domain.Aircraft;

public interface AircraftRepository {
    void save(Aircraft aircraft);
    boolean existsByRegistrationNumber(String registrationNumber);
}
