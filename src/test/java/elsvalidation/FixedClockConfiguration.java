package elsvalidation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Explicitly loaded by the packaged-app validation runner; excluded from the delivery JAR. */
@Configuration(proxyBeanMethods = false)
public class FixedClockConfiguration {
    @Bean
    @Primary
    public Clock validationClock() {
        return Clock.fixed(Instant.parse("2030-06-15T09:00:00Z"), ZoneId.of("Asia/Beirut"));
    }
}
