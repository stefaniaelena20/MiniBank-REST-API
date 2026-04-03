package ro.axonsoft.eval.minibank.config;

import org.springframework.context.annotation.Configuration;

/**
 * Security is not required for this challenge.
 * Spring Security is not on the classpath, so no configuration needed.
 */
@Configuration
public class SecurityConfig {
    // No security configured — all endpoints are open
}
