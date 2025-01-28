package com.maiia.pro.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentDto {
    private Integer practitionerId;
    private Integer patientId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
