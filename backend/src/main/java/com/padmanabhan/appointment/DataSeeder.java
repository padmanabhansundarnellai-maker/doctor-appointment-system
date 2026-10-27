package com.padmanabhan.appointment;

import com.padmanabhan.appointment.model.*;
import com.padmanabhan.appointment.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/** Creates demo accounts + slots on first run so you can test immediately. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final SlotRepository slots;

    public DataSeeder(UserRepository users, SlotRepository slots) {
        this.users = users;
        this.slots = slots;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        make("Admin", "admin@demo.com", "admin123", Role.ADMIN, null, enc);
        make("Patient Demo", "patient@demo.com", "patient123", Role.PATIENT, null, enc);
        User d1 = make("Anitha Rao", "doctor@demo.com", "doctor123", Role.DOCTOR, "Cardiologist", enc);
        User d2 = make("Karthik Raja", "doctor2@demo.com", "doctor123", Role.DOCTOR, "Dermatologist", enc);
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < 4; i++) {
            slot(d1, base.plusMinutes(30L * i));
            slot(d2, base.plusHours(3).plusMinutes(30L * i));
        }
    }

    private User make(String name, String email, String pw, Role role, String spec, BCryptPasswordEncoder enc) {
        User u = new User();
        u.setName(name); u.setEmail(email); u.setPassword(enc.encode(pw));
        u.setRole(role); u.setSpecialization(spec);
        return users.save(u);
    }

    private void slot(User doctor, LocalDateTime start) {
        Slot s = new Slot();
        s.setDoctor(doctor); s.setStartTime(start); s.setEndTime(start.plusMinutes(30));
        slots.save(s);
    }
}
