package com.padmanabhan.appointment.repo;

import com.padmanabhan.appointment.model.Appointment;
import com.padmanabhan.appointment.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderBySlotStartTimeDesc(Long patientId);

    @Query("select a from Appointment a where a.slot.doctor.id = :doctorId order by a.slot.startTime")
    List<Appointment> findByDoctor(@Param("doctorId") Long doctorId);

    List<Appointment> findAllByOrderBySlotStartTimeDesc();

    long countByStatus(Status status);

    List<Appointment> findByStatusAndReminderSentFalseAndSlotStartTimeBetween(Status status, LocalDateTime from, LocalDateTime to);
}
