package com.camerarental.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationHelperTest {

    private final PaginationHelper helper = new PaginationHelper();

    private static final Set<String> ALLOWED = Set.of("name", "createdAt", "price");
    private static final String DEFAULT = "name";

    // =========================================================================
    // resolveSortField
    // =========================================================================

    @Nested
    @DisplayName("resolveSortField")
    class ResolveSortFieldTests {

        @Test
        @DisplayName("returns requested field when allowed")
        void allowed_returnsField() {
            assertThat(helper.resolveSortField("price", ALLOWED, DEFAULT))
                    .isEqualTo("price");
        }

        @Test
        @DisplayName("falls back to default when field not allowed")
        void notAllowed_returnsDefault() {
            assertThat(helper.resolveSortField("hackField", ALLOWED, DEFAULT))
                    .isEqualTo("name");
        }

        @Test
        @DisplayName("falls back to default when field is null")
        void nullField_returnsDefault() {
            assertThat(helper.resolveSortField(null, ALLOWED, DEFAULT))
                    .isEqualTo("name");
        }
    }

    // =========================================================================
    // buildSort
    // =========================================================================

    @Nested
    @DisplayName("buildSort")
    class BuildSortTests {

        @Test
        @DisplayName("ascending sort order")
        void asc() {
            Sort sort = helper.buildSort("price", "asc", ALLOWED, DEFAULT);

            Sort.Order order = sort.getOrderFor("price");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("case-insensitive ascending")
        void ascUppercase() {
            Sort sort = helper.buildSort("price", "ASC", ALLOWED, DEFAULT);

            assertThat(sort.getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("descending sort order")
        void desc() {
            Sort sort = helper.buildSort("price", "desc", ALLOWED, DEFAULT);

            assertThat(sort.getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("unknown sort order defaults to descending")
        void unknownOrder_defaultsDesc() {
            Sort sort = helper.buildSort("price", "random", ALLOWED, DEFAULT);

            assertThat(sort.getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("disallowed field falls back to default in sort")
        void disallowedField_fallsBack() {
            Sort sort = helper.buildSort("injection", "asc", ALLOWED, DEFAULT);

            assertThat(sort.getOrderFor("name")).isNotNull();
            assertThat(sort.getOrderFor("injection")).isNull();
        }
    }

    // =========================================================================
    // buildPageable
    // =========================================================================

    @Nested
    @DisplayName("buildPageable")
    class BuildPageableTests {

        @Test
        @DisplayName("builds pageable with correct page, size, and sort")
        void buildsCorrectly() {
            Pageable pageable = helper.buildPageable(2, 25, "createdAt", "desc", ALLOWED, DEFAULT);

            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(25);
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("zero page number is valid")
        void zeroPage() {
            Pageable pageable = helper.buildPageable(0, 10, "name", "asc", ALLOWED, DEFAULT);

            assertThat(pageable.getPageNumber()).isZero();
        }
    }
}
