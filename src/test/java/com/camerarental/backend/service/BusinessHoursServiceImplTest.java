package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.BusinessHours;
import com.camerarental.backend.payload.BusinessHoursDTO;
import com.camerarental.backend.repository.BusinessHoursRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BusinessHoursServiceImplTest {

    @Mock private ModelMapper modelMapper;
    @Mock private BusinessHoursRepository businessHoursRepository;

    @InjectMocks
    private BusinessHoursServiceImpl service;

    private BusinessHours sampleEntity() {
        BusinessHours bh = new BusinessHours();
        bh.setBusinessHoursId(UUID.randomUUID());
        bh.setDayOfWeek(DayOfWeek.MONDAY);
        bh.setOpenTime(LocalTime.of(9, 0));
        bh.setCloseTime(LocalTime.of(17, 0));
        bh.setClosed(false);
        return bh;
    }

    private BusinessHoursDTO sampleDto() {
        BusinessHoursDTO dto = new BusinessHoursDTO();
        dto.setDayOfWeek(DayOfWeek.MONDAY);
        dto.setOpenTime(LocalTime.of(9, 0));
        dto.setCloseTime(LocalTime.of(17, 0));
        dto.setClosed(false);
        return dto;
    }

    // =========================================================================
    // create
    // =========================================================================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates business hours for a new day")
        void create_success() {
            BusinessHoursDTO dto = sampleDto();
            BusinessHours mapped = sampleEntity();
            BusinessHours saved = sampleEntity();
            BusinessHoursDTO outputDto = sampleDto();

            given(businessHoursRepository.existsByDayOfWeek(DayOfWeek.MONDAY)).willReturn(false);
            given(modelMapper.map(dto, BusinessHours.class)).willReturn(mapped);
            given(businessHoursRepository.save(mapped)).willReturn(saved);
            given(modelMapper.map(saved, BusinessHoursDTO.class)).willReturn(outputDto);

            BusinessHoursDTO result = service.create(dto);

            assertThat(result.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            verify(businessHoursRepository).save(mapped);
        }

        @Test
        @DisplayName("throws ApiException when day already exists")
        void create_duplicateDay() {
            BusinessHoursDTO dto = sampleDto();
            given(businessHoursRepository.existsByDayOfWeek(DayOfWeek.MONDAY)).willReturn(true);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already exist");

            verify(businessHoursRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getAll
    // =========================================================================

    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        @Test
        @DisplayName("returns all business hours ordered by day")
        void getAll_returnsOrdered() {
            BusinessHours monday = sampleEntity();
            BusinessHoursDTO mondayDto = sampleDto();

            given(businessHoursRepository.findAllByOrderByDayOfWeekAsc()).willReturn(List.of(monday));
            given(modelMapper.map(monday, BusinessHoursDTO.class)).willReturn(mondayDto);

            List<BusinessHoursDTO> result = service.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("returns empty list when no hours configured")
        void getAll_empty() {
            given(businessHoursRepository.findAllByOrderByDayOfWeekAsc()).willReturn(List.of());

            List<BusinessHoursDTO> result = service.getAll();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getByDay
    // =========================================================================

    @Nested
    @DisplayName("getByDay")
    class GetByDayTests {

        @Test
        @DisplayName("returns DTO when day exists")
        void getByDay_found() {
            BusinessHours entity = sampleEntity();
            BusinessHoursDTO dto = sampleDto();

            given(businessHoursRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                    .willReturn(Optional.of(entity));
            given(modelMapper.map(entity, BusinessHoursDTO.class)).willReturn(dto);

            BusinessHoursDTO result = service.getByDay(DayOfWeek.MONDAY);

            assertThat(result.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when day not configured")
        void getByDay_notFound() {
            given(businessHoursRepository.findByDayOfWeek(DayOfWeek.SUNDAY))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByDay(DayOfWeek.SUNDAY))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // update
    // =========================================================================

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("updates open/close times")
        void update_success() {
            BusinessHours existing = sampleEntity();
            BusinessHoursDTO dto = sampleDto();
            dto.setOpenTime(LocalTime.of(8, 0));
            dto.setCloseTime(LocalTime.of(18, 0));

            BusinessHours saved = sampleEntity();
            saved.setOpenTime(LocalTime.of(8, 0));
            saved.setCloseTime(LocalTime.of(18, 0));
            BusinessHoursDTO outputDto = sampleDto();
            outputDto.setOpenTime(LocalTime.of(8, 0));
            outputDto.setCloseTime(LocalTime.of(18, 0));

            given(businessHoursRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                    .willReturn(Optional.of(existing));
            given(businessHoursRepository.save(existing)).willReturn(saved);
            given(modelMapper.map(saved, BusinessHoursDTO.class)).willReturn(outputDto);

            BusinessHoursDTO result = service.update(DayOfWeek.MONDAY, dto);

            assertThat(existing.getOpenTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(result.getOpenTime()).isEqualTo(LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when day not configured")
        void update_notFound() {
            given(businessHoursRepository.findByDayOfWeek(DayOfWeek.SUNDAY))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(DayOfWeek.SUNDAY, sampleDto()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deletes when day exists")
        void delete_success() {
            BusinessHours entity = sampleEntity();
            given(businessHoursRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                    .willReturn(Optional.of(entity));

            service.delete(DayOfWeek.MONDAY);

            verify(businessHoursRepository).delete(entity);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when day not configured")
        void delete_notFound() {
            given(businessHoursRepository.findByDayOfWeek(DayOfWeek.SUNDAY))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(DayOfWeek.SUNDAY))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
