package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.TimeSlot;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProAvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }

    public List<Availability> generateAvailabilities(Integer practitionerId) {
        if(practitionerId != null) {
            List<TimeSlot> timeslots = timeSlotRepository.findByPractitionerId(practitionerId);
            List<Availability> existingAvailabilities = this.availabilityRepository.findByPractitionerId(practitionerId);
            List<Appointment> appointments = this.appointmentRepository.findByPractitionerId(practitionerId);
            List<Availability> availabilitiesToAdd = new ArrayList<>();

            for(TimeSlot timeSlot : timeslots) {
                LocalDateTime startDateNewAvailability = timeSlot.getStartDate();
                LocalDateTime endDateNewAvailability = startDateNewAvailability.plusMinutes(15);

                // Check if new availability not exceeding the slot endDate
                while (timeSlot.getEndDate().isAfter(endDateNewAvailability) ||
                        timeSlot.getEndDate().isEqual(endDateNewAvailability)) {

                    Availability existingAvailability = hasOverlapAvailability(existingAvailabilities, startDateNewAvailability, endDateNewAvailability);
                    Appointment existingAppointment = hasAppointmentInThisSlot(appointments, startDateNewAvailability, endDateNewAvailability);

                    if(existingAvailability == null && existingAppointment == null) {
                        availabilitiesToAdd.add(
                                new Availability(
                                        null,
                                        practitionerId,
                                        startDateNewAvailability,
                                        endDateNewAvailability
                                )
                        );
                        startDateNewAvailability = startDateNewAvailability.plusMinutes(15);
                    }

                    //If existing availability/appointment on current time -> start new availability at the end of existing availability/appointment
                    else {
                        LocalDateTime newStart;
                        if(existingAvailability != null && existingAppointment != null) {
                            newStart = existingAvailability.getEndDate().isAfter(existingAppointment.getEndDate())
                                    ? existingAvailability.getEndDate() : existingAppointment.getEndDate();
                        } else {
                            newStart = existingAvailability != null ? existingAvailability.getEndDate() : existingAppointment.getEndDate();
                        }
                        startDateNewAvailability = newStart;
                    }

                    //Try to generate 15min long new availability
                    long minutesBeforeEnd = Duration.between(startDateNewAvailability, timeSlot.getEndDate()).toMinutes();
                    if(minutesBeforeEnd >= 15) {
                        endDateNewAvailability = startDateNewAvailability.plusMinutes(15);
                    }

                    // if not possible, generate shorter one
                    else if(minutesBeforeEnd > 0) {
                        endDateNewAvailability = startDateNewAvailability.plusMinutes(minutesBeforeEnd);
                    }

                    //else : add value to close while loop
                    else {
                        endDateNewAvailability = startDateNewAvailability.plusMinutes(15);
                    }
                }
            }

            availabilityRepository.saveAll(availabilitiesToAdd);
            return availabilitiesToAdd;
        }
        throw new IllegalArgumentException("practitionerId is null");
    }


    // TODO optimisation : refactoriser hasAppointmentInThisSlot et hasOverlapAvailability
    // meme code pour 2 types d'objets
    private Appointment hasAppointmentInThisSlot(List<Appointment> appointments,
                                                 LocalDateTime startDateNewAvailability,
                                                 LocalDateTime endDateNewAvailability) {

        if(startDateNewAvailability.isAfter(endDateNewAvailability)) {
            throw new IllegalArgumentException("startDate is after endDate");
        }

        for(Appointment appointment : appointments) {
            if (!(!appointment.getEndDate().isAfter(startDateNewAvailability) ||
                    !appointment.getStartDate().isBefore(endDateNewAvailability))) {
                return appointment;
            }
        }
        return null;
    }


    private Availability hasOverlapAvailability(
            List<Availability> existingAvailabilities,
            LocalDateTime startDateNewAvailability,
            LocalDateTime endDateNewAvailability) {

        if(startDateNewAvailability.isAfter(endDateNewAvailability)) {
            throw new IllegalArgumentException("startDate is after endDate");
        }

        for(Availability availability : existingAvailabilities) {
            if (!(!availability.getEndDate().isAfter(startDateNewAvailability) ||
                    !availability.getStartDate().isBefore(endDateNewAvailability))) {
                return availability;
            }
        }
        return null;
    }

    public void removeAvailibility(int practitionerId, LocalDateTime startDate, LocalDateTime endDate) throws NotFoundException {
        List<Availability> practitionerAvailabilities = this.findByPractitionerId(practitionerId)
                .stream().filter(
                        availability ->
                                availability.getStartDate().equals(startDate)
                                        && availability.getEndDate().equals(endDate)
                ).collect(Collectors.toList());

        if(practitionerAvailabilities.isEmpty()) {
            throw new NotFoundException("Availability not found");
        }

        this.availabilityRepository.delete(practitionerAvailabilities.get(0));
    }
}
