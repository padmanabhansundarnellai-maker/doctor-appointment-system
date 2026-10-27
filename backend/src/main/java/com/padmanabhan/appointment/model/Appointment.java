package com.padmanabhan.appointment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Slot slot;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User patient;
    @Enumerated(EnumType.STRING)
    private Status status = Status.BOOKED;
    private boolean reminderSent;
}
