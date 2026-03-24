package com.camerarental.backend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Helper class for building pagination and sorting configurations.
 * Centralizes pagination logic to reduce code duplication across services.
 */
@Component
public class PaginationHelper {

    /**
     * Builds a Sort object based on the provided field and order.
     *
     * @param sortBy        The field to sort by
     * @param sortOrder     The sort direction ("asc" or "desc")
     * @param allowedFields Set of allowed sort fields
     * @param defaultField  Default field to sort by if sortBy is invalid
     * @return A Sort object configured for the specified field and direction
     */
    public Sort buildSort(String sortBy, String sortOrder, Set<String> allowedFields, String defaultField) {
        String sortField = resolveSortField(sortBy, allowedFields, defaultField);
        return sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();
    }

    /**
     * Builds a Pageable object with sorting.
     *
     * @param pageNumber    Zero-based page number
     * @param pageSize      Number of items per page
     * @param sortBy        The field to sort by
     * @param sortOrder     The sort direction ("asc" or "desc")
     * @param allowedFields Set of allowed sort fields
     * @param defaultField  Default field to sort by if sortBy is invalid
     * @return A Pageable object configured for the specified pagination and sorting
     */
    public Pageable buildPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder,
                                  Set<String> allowedFields, String defaultField) {
        Sort sort = buildSort(sortBy, sortOrder, allowedFields, defaultField);
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    /**
     * Resolves the sort field, falling back to default if the provided field is not allowed.
     *
     * @param sortBy        The requested sort field
     * @param allowedFields Set of allowed sort fields
     * @param defaultField  Default field to use if sortBy is invalid
     * @return The validated sort field
     */
    public String resolveSortField(String sortBy, Set<String> allowedFields, String defaultField) {
        if (sortBy != null && allowedFields.contains(sortBy)) {
            return sortBy;
        }
        return defaultField;
    }
}
