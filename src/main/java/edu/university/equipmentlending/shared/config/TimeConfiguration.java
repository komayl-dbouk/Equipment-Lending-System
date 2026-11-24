package edu.university.equipmentlending.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

    @Bean
    Clock applicationClock(@Value("${app.clock-zone}") String clockZone) {
        return Clock.system(ZoneId.of(clockZone));
    }
}
