package com.padmanabhan.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DoctorAppointmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoctorAppointmentApplication.class, args);
    }
}
