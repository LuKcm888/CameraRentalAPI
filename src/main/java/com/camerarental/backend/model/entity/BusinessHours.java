package com.camerarental.backend.model.entity;

import com.camerarental.backend.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a single day's operating hours for the rental business.
 *
 * <p>Each row maps one {@link DayOfWeek} to an open/close time window.
 * When {@code closed} is {@code true}, {@code openTime} and
 * {@code closeTime} may be null.</p>
 */
@Entity
@Table(name = "business_hours", uniqueConstraints = {
        @UniqueConstraint(columnNames = "day_of_week")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class BusinessHours extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "business_hours_id")
    @EqualsAndHashCode.Include
    private UUID businessHoursId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;
}
