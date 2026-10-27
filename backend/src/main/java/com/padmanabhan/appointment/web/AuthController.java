package com.padmanabhan.appointment.web;

import com.padmanabhan.appointment.model.Role;
import com.padmanabhan.appointment.model.User;
import com.padmanabhan.appointment.repo.UserRepository;
import com.padmanabhan.appointment.service.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    record RegisterReq(@NotBlank String name, @NotBlank @Email String email,
                       @NotBlank @Size(min = 6) String password, Role role, String specialization) {}
    record LoginReq(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/register")
    public User register(@Valid @RequestBody RegisterReq r) {
        if (users.findByEmail(r.email()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (r.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot self-register as admin");
        }
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(r.role() == null ? Role.PATIENT : r.role());
        u.setSpecialization(r.specialization());
        return users.save(u);
    }

    @PostMapping("/login")
    public User login(@Valid @RequestBody LoginReq r) {
        User u = users.findByEmail(r.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return u;
    }
}
