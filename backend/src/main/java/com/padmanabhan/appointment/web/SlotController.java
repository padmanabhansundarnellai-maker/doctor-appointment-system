package com.padmanabhan.appointment.web;

import com.padmanabhan.appointment.model.Role;
import com.padmanabhan.appointment.model.Slot;
import com.padmanabhan.appointment.model.User;
import com.padmanabhan.appointment.repo.SlotRepository;
import com.padmanabhan.appointment.repo.UserRepository;
import com.padmanabhan.appointment.service.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SlotController {

    private final SlotRepository slots;
    private final UserRepository users;

    record SlotReq(@NotNull Long doctorId, @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {}

    @GetMapping("/doctors")
    public List<User> doctors() {
        return users.findByRole(Role.DOCTOR);
    }

    /** Available (future, unbooked) slots of one doctor. */
    @GetMapping("/slots")
    public List<Slot> available(@RequestParam Long doctorId) {
        return slots.findByDoctorIdAndBookedFalseAndStartTimeAfterOrderByStartTime(doctorId, LocalDateTime.now());
    }

    /** All slots of a doctor (for the doctor dashboard). */
    @GetMapping("/slots/doctor/{doctorId}")
    public List<Slot> byDoctor(@PathVariable Long doctorId) {
        return slots.findByDoctorIdOrderByStartTime(doctorId);
    }

    @PostMapping("/slots")
    public Slot create(@Valid @RequestBody SlotReq r) {
        User doctor = users.findById(r.doctorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (doctor.getRole() != Role.DOCTOR) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not a doctor");
        }
        if (!r.endTime().isAfter(r.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (r.startTime().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot create slots in the past");
        }
        if (slots.existsOverlap(doctor.getId(), r.startTime(), r.endTime())) {
            throw new ApiException(HttpStatus.CONFLICT, "Overlaps with an existing slot");
        }
        Slot s = new Slot();
        s.setDoctor(doctor);
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        return slots.save(s);
    }

    @DeleteMapping("/slots/{id}")
    public void delete(@PathVariable Long id) {
        Slot s = slots.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (s.isBooked()) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete a booked slot");
        }
        slots.delete(s);
    }
}
