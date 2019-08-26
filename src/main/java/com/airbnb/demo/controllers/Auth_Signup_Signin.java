package com.airbnb.demo.controllers;

import com.airbnb.demo.DTO.request.LoginRequest;
import com.airbnb.demo.DTO.request.SignUpRequest;
import com.airbnb.demo.DTO.response.JwtResponse;
import com.airbnb.demo.DTO.response.MessageResponse;
import com.airbnb.demo.entities.Role;
import com.airbnb.demo.entities.RoleName;
import com.airbnb.demo.entities.User;
import com.airbnb.demo.repositories.RoleRepository;
import com.airbnb.demo.repositories.UserRepository;
import com.airbnb.demo.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class Auth_Signup_Signin {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtProvider jwtProvider;

    @PostMapping("/sign-in")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateJwtToken(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), userDetails.getAuthorities()));
//        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getEmail(), userDetails.getAuthorities()));
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {

        System.out.println("abc");
        System.out.println(signUpRequest.getName());
        System.out.println(signUpRequest.getEmail());
        System.out.println(signUpRequest.getRole());
        System.out.println(signUpRequest.getUsername());
        System.out.println(signUpRequest.getPassword());
        System.out.println(signUpRequest.getProviderLogin());


        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            System.out.println("Kiem Tra email");
            if (!signUpRequest.getProviderLogin().isEmpty()) {
                System.out.println("kiem tra provider");
                signUpRequest.setPassword("123456");
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(signUpRequest.getUsername(), signUpRequest.getPassword()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtProvider.generateJwtToken(authentication);
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                System.out.println("SO OKE");
                return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), userDetails.getAuthorities()));
            }
            else{
                System.out.println("da ton tai email");
                return new ResponseEntity<>(new MessageResponse("Fail -> Email is already in use!"),
                        HttpStatus.BAD_REQUEST);
            }

        }
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {

            return new ResponseEntity<>(new MessageResponse("Fail -> Username is already taken!"),
                    HttpStatus.BAD_REQUEST);
        }

        // Creating user's account
        User user = new User(signUpRequest.getName(), signUpRequest.getUsername(), signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        strRoles.forEach(role -> {
            switch (role) {
                case "admin":
                    Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
                    roles.add(adminRole);

                    break;
                case "owner":
                    Role ownerRole = roleRepository.findByName(RoleName.ROLE_OWNER)
                            .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
                    roles.add(ownerRole);

                    break;
                default:
                    Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
                    roles.add(userRole);
            }
        });

        user.setRoles(roles);
        userRepository.save(user);

        return new ResponseEntity<>(new MessageResponse("User registered successfully!"), HttpStatus.OK);
    }
}