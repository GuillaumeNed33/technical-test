package com.maiia.pro.service;

import com.maiia.pro.EntityFactory;
import com.maiia.pro.dto.AppointmentCreateDto;
import com.maiia.pro.dto.AppointmentDto;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.Patient;
import com.maiia.pro.entity.Practitioner;
import com.maiia.pro.repository.*;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProAppointmentServiceTest {
    private final  EntityFactory entityFactory = new EntityFactory();

    @Autowired
    private ProAppointmentService proAppointmentService;

    @Autowired
    private ProAvailabilityService proAvailabilityService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private PatientRepository patientRepository;
    
    Practitioner practitioner;
    
    Patient patient;
    
    @BeforeEach
    void setUp() {
        practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        patient = patientRepository.save(entityFactory.createPatient());
    }

    @Test
    void createAppointment() throws Exception {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(4, availabilities.size());
        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        expectedStartDate.add(startDate.plusMinutes(15));
        expectedStartDate.add(startDate.plusMinutes(30));
        expectedStartDate.add(startDate.plusMinutes(45));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), patient.getId().toString(), startDate, startDate.plusMinutes(15));
        AppointmentDto appointmentDto = proAppointmentService.createAppointment(payload);
        assertNotNull(appointmentDto);

        List<Availability> newAvailabilities = this.proAvailabilityService.findByPractitionerId(practitioner.getId());
        assertEquals(3, newAvailabilities.size());
        List<LocalDateTime> newAvailabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> newExpectedStartDate = new ArrayList<>();
        newExpectedStartDate.add(startDate.plusMinutes(15));
        newExpectedStartDate.add(startDate.plusMinutes(30));
        newExpectedStartDate.add(startDate.plusMinutes(45));
        assertTrue(newAvailabilitiesStartDate.containsAll(newExpectedStartDate));
    }

    @Test
    void createAppointmentWithNullPractitionerId() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto(null, patient.getId().toString(), startDate, startDate.plusMinutes(15));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an IllegalArgumentException"
        );

        assertEquals("practitionerId is null", exception.getMessage());
    }

    @Test
    void createAppointmentWithNullPatientId() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), null, startDate, startDate.plusMinutes(15));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an IllegalArgumentException"
        );

        assertEquals("patientId is null", exception.getMessage());
    }

    @Test
    void createAppointmentWithNullStartDate() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), patient.getId().toString(), null, startDate.plusMinutes(15));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an IllegalArgumentException"
        );

        assertEquals("startDate is null", exception.getMessage());
    }

    @Test
    void createAppointmentWithNullEnDate() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), patient.getId().toString(), startDate, null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an IllegalArgumentException"
        );

        assertEquals("endDate is null", exception.getMessage());
    }
    
    @Test
    void createAppointmentWithInvalidDates() throws Exception{
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), patient.getId().toString(), startDate, startDate.minusMinutes(15));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an IllegalArgumentException"
        );

        assertEquals("startDate is after endDate", exception.getMessage());
    }

    @Test
    void createAppointmentWithInvalidPractitionerId() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto("987654321", patient.getId().toString(), startDate, startDate.plusMinutes(15));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an NotFoundException"
        );

        assertEquals("practitioner not found with id 987654321", exception.getMessage());
    }

    @Test
    void createAppointmentWithInvalidPatientId() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), "987654321", startDate, startDate.plusMinutes(15));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an NotFoundException"
        );

        assertEquals("patient not found with id 987654321", exception.getMessage());
    }

    @Test
    void createAppointmentAlreadyTaken() {
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient.getId(),
                startDate,
                startDate.plusMinutes(15)));
        
        AppointmentCreateDto payload = new AppointmentCreateDto(practitioner.getId().toString(), patient.getId().toString(), startDate, startDate.plusMinutes(15));

        Exception exception = assertThrows(
                Exception.class,
                () -> proAppointmentService.createAppointment(payload),
                "Expected createAppointment to throw an Exception"
        );

        assertEquals("Availability is already taken", exception.getMessage());
    }
}
