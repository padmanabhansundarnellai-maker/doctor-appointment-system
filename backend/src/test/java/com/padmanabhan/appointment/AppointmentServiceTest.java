package com.padmanabhan.appointment;

import com.padmanabhan.appointment.model.*;
import com.padmanabhan.appointment.repo.*;
import com.padmanabhan.appointment.service.ApiException;
import com.padmanabhan.appointment.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointments;
    @Mock SlotRepository slots;
    @Mock UserRepository users;
    @InjectMocks AppointmentService service;

    User doctor, patient;

    @BeforeEach
    void setUp() {
        doctor = new User(); doctor.setId(1L); doctor.setRole(Role.DOCTOR);
        patient = new User(); patient.setId(2L); patient.setRole(Role.PATIENT);
    }

    private Slot slot(Long id, boolean booked, LocalDateTime start) {
        Slot s = new Slot();
        s.setId(id); s.setDoctor(doctor); s.setBooked(booked);
        s.setStartTime(start); s.setEndTime(start.plusMinutes(30));
        return s;
    }

    @Test
    void bookFreeSlotSucceeds() {
        Slot s = slot(10L, false, LocalDateTime.now().plusDays(1));
        when(slots.findByIdForUpdate(10L)).thenReturn(Optional.of(s));
        when(users.findById(2L)).thenReturn(Optional.of(patient));
        when(appointments.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment a = service.book(10L, 2L);

        assertEquals(Status.BOOKED, a.getStatus());
        assertTrue(s.isBooked());
        verify(slots).save(s);
    }

    @Test
    void bookAlreadyBookedSlotThrowsConflict() {
        when(slots.findByIdForUpdate(10L)).thenReturn(Optional.of(slot(10L, true, LocalDateTime.now().plusDays(1))));

        ApiException ex = assertThrows(ApiException.class, () -> service.book(10L, 2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(appointments, never()).save(any());
    }

    @Test
    void bookPastSlotIsRejected() {
        when(slots.findByIdForUpdate(10L)).thenReturn(Optional.of(slot(10L, false, LocalDateTime.now().minusHours(2))));

        ApiException ex = assertThrows(ApiException.class, () -> service.book(10L, 2L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void cancelFreesTheSlot() {
        Slot s = slot(10L, true, LocalDateTime.now().plusDays(1));
        Appointment a = new Appointment();
        a.setId(5L); a.setSlot(s); a.setPatient(patient); a.setStatus(Status.BOOKED);
        when(appointments.findById(5L)).thenReturn(Optional.of(a));
        when(appointments.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment result = service.cancel(5L);

        assertEquals(Status.CANCELLED, result.getStatus());
        assertFalse(s.isBooked());
    }

    @Test
    void cancelTwiceIsRejected() {
        Appointment a = new Appointment();
        a.setSlot(slot(10L, false, LocalDateTime.now().plusDays(1)));
        a.setStatus(Status.CANCELLED);
        when(appointments.findById(5L)).thenReturn(Optional.of(a));

        assertThrows(ApiException.class, () -> service.cancel(5L));
    }

    @Test
    void rescheduleMovesAppointmentAndSwapsSlots() {
        Slot oldSlot = slot(10L, true, LocalDateTime.now().plusDays(1));
        Slot newSlot = slot(11L, false, LocalDateTime.now().plusDays(2));
        Appointment a = new Appointment();
        a.setId(5L); a.setSlot(oldSlot); a.setPatient(patient); a.setStatus(Status.BOOKED); a.setReminderSent(true);
        when(appointments.findById(5L)).thenReturn(Optional.of(a));
        when(slots.findByIdForUpdate(11L)).thenReturn(Optional.of(newSlot));
        when(appointments.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment result = service.reschedule(5L, 11L);

        assertSame(newSlot, result.getSlot());
        assertTrue(newSlot.isBooked());
        assertFalse(oldSlot.isBooked());
        assertFalse(result.isReminderSent());
    }

    @Test
    void rescheduleToBookedSlotFails() {
        Appointment a = new Appointment();
        a.setSlot(slot(10L, true, LocalDateTime.now().plusDays(1)));
        a.setStatus(Status.BOOKED);
        when(appointments.findById(5L)).thenReturn(Optional.of(a));
        when(slots.findByIdForUpdate(11L)).thenReturn(Optional.of(slot(11L, true, LocalDateTime.now().plusDays(2))));

        ApiException ex = assertThrows(ApiException.class, () -> service.reschedule(5L, 11L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
