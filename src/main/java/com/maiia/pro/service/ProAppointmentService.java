package com.maiia.pro.service;

import com.maiia.pro.dto.AppointmentDto;
import com.maiia.pro.entity.Appointment;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.dto.AppointmentCreateDto;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProAppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    ProPractitionerService proPractitionerService;

    @Autowired
    ProAvailabilityService proAvailabilityService;

    @Autowired
    ProPatientService proPatientService;

    public Appointment find(String appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> findByPractitionerId(Integer practitionerId) {
        return appointmentRepository.findByPractitionerId(practitionerId);
    }

    public AppointmentDto createAppointment(AppointmentCreateDto appointmentCreateDto) throws Exception {
        if(appointmentCreateDto.getPractitionerId() == null) {
            throw new IllegalArgumentException("practitionerId is null");
        }
        if(appointmentCreateDto.getPatientId() == null) {
            throw new IllegalArgumentException("patientId is null");
        }
        if (appointmentCreateDto.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is null");
        }
        if (appointmentCreateDto.getEndDate() == null) {
            throw new IllegalArgumentException("endDate is null");
        }
        if(appointmentCreateDto.getStartDate().isAfter(appointmentCreateDto.getEndDate())){
            throw new IllegalArgumentException("startDate is after endDate");
        }

        if(this.proPractitionerService.find(appointmentCreateDto.getPractitionerId()) == null) {
            throw new NotFoundException("practitioner not found with id " + appointmentCreateDto.getPractitionerId());
        }
        if(this.proPatientService.find(appointmentCreateDto.getPatientId()) == null) {
            throw new NotFoundException("patient not found with id " + appointmentCreateDto.getPatientId());
        }

        List<Appointment> existingsApp = this.appointmentRepository.findByPractitionerId(Integer.parseInt(appointmentCreateDto.getPractitionerId()));
        if(existingsApp
                .stream()
                .anyMatch(app ->
                        app.getStartDate().equals(appointmentCreateDto.getStartDate()) &&
                                app.getEndDate().equals(appointmentCreateDto.getEndDate())
                )
        ) {
            throw new Exception("Availability is already taken");
        }

        Appointment appointment =
                this.appointmentRepository.save(Appointment.builder()
                        .practitionerId(Integer.parseInt(appointmentCreateDto.getPractitionerId()))
                        .patientId(Integer.parseInt(appointmentCreateDto.getPatientId()))
                        .startDate(appointmentCreateDto.getStartDate())
                        .endDate(appointmentCreateDto.getEndDate())
                        .build()
                );

        this.proAvailabilityService.removeAvailibility(
                Integer.parseInt(appointmentCreateDto.getPractitionerId()),
                appointmentCreateDto.getStartDate(),
                appointmentCreateDto.getEndDate()
        );

        return this.toDTO(appointment);
    }

    private AppointmentDto toDTO(Appointment appointment) {
        AppointmentDto appointmentDto = new AppointmentDto();
        appointmentDto.setPractitionerId(appointment.getPractitionerId());
        appointmentDto.setPatientId(appointment.getPatientId());
        appointmentDto.setStartDate(appointment.getStartDate());
        appointmentDto.setEndDate(appointment.getEndDate());
        return appointmentDto;
    }
}
