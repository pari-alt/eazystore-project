package com.eazybytes.eazystore.security;

import com.eazybytes.eazystore.entity.Customer;
import com.eazybytes.eazystore.entity.Role;
import com.eazybytes.eazystore.repository.CustomerRepository;
import com.eazybytes.eazystore.repository.RoleRepository;
import com.eazybytes.eazystore.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect(
                    "http://localhost:5173/login?oauth2=error"
            );
            return;
        }

        Customer customer = customerRepository
                .findByEmail(email)
                .orElse(null);

        // Create customer if not already registered
        if (customer == null) {

            customer = new Customer();

            customer.setName(
                    name != null && !name.isBlank()
                            ? name
                            : email.split("@")[0]
            );

            customer.setEmail(email);

            // Required fields in Customer entity
            customer.setMobileNumber("0000000000");

            // Google users don't need password login
            customer.setPasswordHash(
                    "{noop}GOOGLE_USER"
            );

            Role userRole = roleRepository
                    .findByName("ROLE_USER")
                    .orElseGet(() ->
                            roleRepository.findByName("USER")
                                    .orElse(null)
                    );

            if (userRole == null) {
                response.sendRedirect(
                        "http://localhost:5173/login?oauth2=error"
                );
                return;
            }

            customer.getRoles().add(userRole);

            customer = customerRepository.save(customer);
        }

        String jwtToken =
                jwtUtil.generateJwtTokenForCustomer(customer);

        String encodedToken = URLEncoder.encode(
                jwtToken,
                StandardCharsets.UTF_8
        );

        String encodedName = URLEncoder.encode(
                customer.getName(),
                StandardCharsets.UTF_8
        );

        String encodedEmail = URLEncoder.encode(
                customer.getEmail(),
                StandardCharsets.UTF_8
        );

        String redirectUrl =
                "http://localhost:5173/login" +
                "?oauth2=success" +
                "&token=" + encodedToken +
                "&name=" + encodedName +
                "&email=" + encodedEmail;

        response.sendRedirect(redirectUrl);
    }
}