package com.camerarental.backend.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.DayOfWeek;

/**
 * General-purpose Spring beans that don't belong to a more specific
 * configuration class.
 *
 * <p>Registers the shared {@link ModelMapper} bean and custom type
 * converters (e.g. case-insensitive {@link DayOfWeek} parsing for
 * path variables and request parameters).</p>
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    /** Shared {@link ModelMapper} instance for entity/DTO conversions. */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /** Case-insensitive {@link DayOfWeek} parsing so path variables like {@code monday}, {@code Monday}, and {@code MONDAY} all resolve correctly. */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, DayOfWeek.class,
                source -> DayOfWeek.valueOf(source.trim().toUpperCase()));
    }
}
