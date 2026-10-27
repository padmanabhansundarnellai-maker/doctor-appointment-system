package com.padmanabhan.appointment.service;

import com.padmanabhan.appointment.model.*;
import com.padmanabhan.appointment.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointments;
    private final SlotRepository slots;
    private final UserRepository users;

    /** Book a slot. The pessimistic lock + @Transactional makes double booking impossible. */
    @Transactional
    public Appointment book(Long slotId, Long patientId) {
        Slot slot = lockFreeSlot(slotId);
        User patient = users.findById(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
        slot.setBooked(true);
        slots.save(slot);
        Appointment a = new Appointment();
        a.setSlot(slot);
        a.setPatient(patient);
        a.setStatus(Status.BOOKED);
        return appointments.save(a);
    }

    @Transactional
    public Appointment cancel(Long appointmentId) {
        Appointment a = get(appointmentId);
        if (a.getStatus() == Status.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Appointment already cancelled");
        }
        a.setStatus(Status.CANCELLED);
        a.getSlot().setBooked(false);
        slots.save(a.getSlot());
        return appointments.save(a);
    }

    @Transactional
    public Appointment reschedule(Long appointmentId, Long newSlotId) {
        Appointment a = get(appointmentId);
        if (a.getStatus() != Status.BOOKED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only booked appointments can be rescheduled");
        }
        Slot newSlot = lockFreeSlot(newSlotId);
        Slot old = a.getSlot();
        old.setBooked(false);
        newSlot.setBooked(true);
        slots.save(old);
        slots.save(newSlot);
        a.setSlot(newSlot);
        a.setReminderSent(false);
        return appointments.save(a);
    }

    private Slot lockFreeSlot(Long slotId) {
        Slot slot = slots.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (slot.isBooked()) {
            throw new ApiException(HttpStatus.CONFLICT, "Slot already booked");
        }
        if (slot.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slot is in the past");
        }
        return slot;
    }

    private Appointment get(Long id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }
}
