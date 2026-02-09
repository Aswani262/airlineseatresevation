package com.airline.shared.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public  class BaseEntity {

    protected OffsetDateTime createdAt;
    protected OffsetDateTime updatedAt;
    
}
