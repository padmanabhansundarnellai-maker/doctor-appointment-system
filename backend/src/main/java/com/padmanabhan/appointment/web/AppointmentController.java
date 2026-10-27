package com.padmanabhan.appointment.web;

import com.padmanabhan.appointment.model.*;
import com.padmanabhan.appointment.repo.*;
import com.padmanabhan.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final AppointmentRepository appointments;
    private final UserRepository users;

    record BookReq(@NotNull Long slotId, @NotNull Long patientId) {}
    record RescheduleReq(@NotNull Long newSlotId) {}

    @PostMapping("/appointments")
    public Appointment book(@Valid @RequestBody BookReq r) {
        return service.book(r.slotId(), r.patientId());
    }

    @PutMapping("/appointments/{id}/cancel")
    public Appointment cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PutMapping("/appointments/{id}/reschedule")
    public Appointment reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleReq r) {
        return service.reschedule(id, r.newSlotId());
    }

    @GetMapping("/appointments/patient/{patientId}")
    public List<Appointment> forPatient(@PathVariable Long patientId) {
        return appointments.findByPatientIdOrderBySlotStartTimeDesc(patientId);
    }

    @GetMapping("/appointments/doctor/{doctorId}")
    public List<Appointment> forDoctor(@PathVariable Long doctorId) {
        return appointments.findByDoctor(doctorId);
    }

    @GetMapping("/admin/appointments")
    public List<Appointment> all() {
        return appointments.findAllByOrderBySlotStartTimeDesc();
    }

    @GetMapping("/admin/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "totalAppointments", appointments.count(),
                "booked", appointments.countByStatus(Status.BOOKED),
                "cancelled", appointments.countByStatus(Status.CANCELLED),
                "doctors", (long) users.findByRole(Role.DOCTOR).size(),
                "patients", (long) users.findByRole(Role.PATIENT).size());
    }
}
