package com.padmanabhan.appointment.repo;

import com.padmanabhan.appointment.model.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    /** Row-level lock: two patients can never grab the same slot at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    List<Slot> findByDoctorIdAndBookedFalseAndStartTimeAfterOrderByStartTime(Long doctorId, LocalDateTime after);

    List<Slot> findByDoctorIdOrderByStartTime(Long doctorId);

    @Query("select count(s) > 0 from Slot s where s.doctor.id = :doctorId and s.startTime < :end and s.endTime > :start")
    boolean existsOverlap(@Param("doctorId") Long doctorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
