# MediBook - Doctor Appointment Booking System

Slot-based scheduling: doctors publish availability; patients book, cancel or reschedule.
Double bookings are prevented with `@Transactional` + a pessimistic row lock on the slot;
reminders are sent with `@Scheduled`; booking rules are covered by JUnit + Mockito tests.

**Stack:** Spring Boot 3 (Java 17), Spring Data JPA, MySQL 8, React 18 (Vite), JUnit 5, Mockito, Docker.

## Run locally
1. `docker compose up -d`            (starts MySQL on :3306)
2. `cd backend && mvn spring-boot:run`   (API on http://localhost:8080)
3. `cd frontend && npm install && npm run dev`   (UI on http://localhost:5173)

Demo logins: `patient@demo.com / patient123`, `doctor@demo.com / doctor123`, `admin@demo.com / admin123`

## Tests
`cd backend && mvn test`

## API summary
| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/auth/register, /api/auth/login | Auth |
| GET | /api/doctors | List doctors |
| GET | /api/slots?doctorId= | Free future slots |
| POST/DELETE | /api/slots, /api/slots/{id} | Doctor manages availability |
| POST | /api/appointments | Book a slot |
| PUT | /api/appointments/{id}/cancel | Cancel |
| PUT | /api/appointments/{id}/reschedule | Move to another slot |
| GET | /api/appointments/patient/{id}, /doctor/{id} | Lists |
| GET | /api/admin/appointments, /api/admin/stats | Admin panel |
