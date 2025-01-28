package com.maiia.pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentCreateDto {
    private String practitionerId;
    private String patientId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
