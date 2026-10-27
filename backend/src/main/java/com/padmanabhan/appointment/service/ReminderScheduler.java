package com.padmanabhan.appointment.service;

import com.padmanabhan.appointment.model.Appointment;
import com.padmanabhan.appointment.model.Status;
import com.padmanabhan.appointment.repo.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final AppointmentRepository appointments;

    /** Runs every minute; reminds patients of appointments starting within 24 hours. */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> due = appointments
                .findByStatusAndReminderSentFalseAndSlotStartTimeBetween(Status.BOOKED, now, now.plusHours(24));
        for (Appointment a : due) {
            log.info("REMINDER -> {} ({}): appointment with Dr. {} at {}",
                    a.getPatient().getName(), a.getPatient().getEmail(),
                    a.getSlot().getDoctor().getName(), a.getSlot().getStartTime());
            a.setReminderSent(true);
        }
    }
}
