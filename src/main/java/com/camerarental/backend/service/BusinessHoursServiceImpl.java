package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.BusinessHours;
import com.camerarental.backend.payload.BusinessHoursDTO;
import com.camerarental.backend.repository.BusinessHoursRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Default implementation of {@link BusinessHoursService}.
 *
 * <p>Converts between {@link BusinessHours} entities and
 * {@link BusinessHoursDTO} objects using {@link ModelMapper} and
 * enforces the one-entry-per-day-of-week uniqueness rule. All
 * single-record operations are keyed by {@link DayOfWeek}.</p>
 */
@Service
@RequiredArgsConstructor
public class BusinessHoursServiceImpl implements BusinessHoursService {

    private final ModelMapper modelMapper;
    private final BusinessHoursRepository businessHoursRepository;

    @Override
    @Transactional
    public BusinessHoursDTO create(BusinessHoursDTO dto) {
        if (businessHoursRepository.existsByDayOfWeek(dto.getDayOfWeek())) {
            throw new ApiException("Business hours for " + dto.getDayOfWeek() + " already exist.");
        }

        BusinessHours entity = modelMapper.map(dto, BusinessHours.class);
        entity.setBusinessHoursId(null);
        BusinessHours saved = businessHoursRepository.save(entity);

        return modelMapper.map(saved, BusinessHoursDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessHoursDTO> getAll() {
        return businessHoursRepository.findAllByOrderByDayOfWeekAsc().stream()
                .map(entity -> modelMapper.map(entity, BusinessHoursDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessHoursDTO getByDay(DayOfWeek day) {
        BusinessHours entity = businessHoursRepository.findByDayOfWeek(day)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHours", "dayOfWeek", day));

        return modelMapper.map(entity, BusinessHoursDTO.class);
    }

    @Override
    @Transactional
    public BusinessHoursDTO update(DayOfWeek day, BusinessHoursDTO dto) {
        BusinessHours existing = businessHoursRepository.findByDayOfWeek(day)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHours", "dayOfWeek", day));

        existing.setOpenTime(dto.getOpenTime());
        existing.setCloseTime(dto.getCloseTime());
        existing.setClosed(dto.getClosed());

        BusinessHours saved = businessHoursRepository.save(existing);
        return modelMapper.map(saved, BusinessHoursDTO.class);
    }

    @Override
    @Transactional
    public void delete(DayOfWeek day) {
        BusinessHours entity = businessHoursRepository.findByDayOfWeek(day)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHours", "dayOfWeek", day));

        businessHoursRepository.delete(entity);
    }
}
