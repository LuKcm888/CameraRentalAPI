package com.camerarental.backend.payload.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic paginated response wrapper that can be used for any entity type.
 * Eliminates the need for entity-specific response classes like CostumeResponse, PropResponse, etc.
 *
 * @param <T> The type of content items in the response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
    private boolean lastPage;

    /**
     * Creates a PagedResponse from a Spring Data Page object.
     *
     * @param page The Spring Data Page
     * @param <T>  The type of content items
     * @return A PagedResponse containing the page data
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(page.getContent());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLastPage(page.isLast());
        return response;
    }

    /**
     * Creates a PagedResponse from a Spring Data Page object with a mapper function
     * to transform entities to DTOs.
     *
     * @param page   The Spring Data Page containing entities
     * @param mapper Function to convert each entity to its DTO representation
     * @param <E>    The entity type in the page
     * @param <D>    The DTO type for the response
     * @return A PagedResponse containing the mapped DTOs
     */
    public static <E, D> PagedResponse<D> from(Page<E> page, Function<E, D> mapper) {
        PagedResponse<D> response = new PagedResponse<>();
        response.setContent(page.getContent().stream().map(mapper).toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLastPage(page.isLast());
        return response;
    }
}
