package com.eazybytes.eazystore.config;

import com.eazybytes.eazystore.entity.Customer;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // User login nahi hai
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return Optional.of("Anonymous user");
        }

        Object principal = authentication.getPrincipal();

        // Principal Customer object hai
        if (principal instanceof Customer customer) {
            return Optional.ofNullable(customer.getEmail())
                    .or(() -> Optional.of("system"));
        }

        // Principal UserDetails object hai
        if (principal instanceof UserDetails userDetails) {
            return Optional.ofNullable(userDetails.getUsername())
                    .or(() -> Optional.of("system"));
        }

        // Principal String hai
        if (principal instanceof String username) {
            return Optional.of(username);
        }

        // Unknown principal type
        return Optional.of("system");
    }
}