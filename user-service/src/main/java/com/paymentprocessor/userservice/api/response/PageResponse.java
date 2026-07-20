package com.paymentprocessor.userservice.api.response;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Generic page response for paginated API results.
 *
 * @param <T> the type of content in the page
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {

    /**
     * Creates a PageResponse from a Spring Data Page.
     *
     * @param page the Spring Data Page
     * @param <T>  the type of content
     * @return PageResponse instance
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return empty();
        }
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Creates a PageResponse from a list and pagination parameters.
     *
     * @param content      the content list
     * @param totalElements total number of elements
     * @param page         the current page number (0-based)
     * @param size         the page size
     * @param <T>          the type of content
     * @return PageResponse instance
     */
    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        if (content == null) {
            return empty();
        }
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page == 0;
        boolean last = page >= totalPages - 1 || totalPages == 0;

        return new PageResponse<>(
                content,
                totalElements,
                totalPages,
                page,
                size,
                first,
                last
        );
    }

    /**
     * Creates an empty PageResponse.
     *
     * @param <T> the type of content
     * @return empty PageResponse
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(
                Collections.emptyList(),
                0L,
                0,
                0,
                0,
                true,
                true
        );
    }

    /**
     * Creates a PageResponse with just a list (no pagination info).
     * Useful for non-paginated responses that still use the same structure.
     *
     * @param content the content list
     * @param <T>     the type of content
     * @return PageResponse instance
     */
    public static <T> PageResponse<T> ofList(List<T> content) {
        if (content == null) {
            return empty();
        }
        int size = content.size();
        return new PageResponse<>(
                content,
                size,
                1,
                0,
                size,
                true,
                true
        );
    }

    /**
     * Maps the content to a different type.
     *
     * @param mapper the mapping function
     * @param <U>    the new content type
     * @return PageResponse with mapped content
     */
    public <U> PageResponse<U> map(Function<T, U> mapper) {
        if (content == null || content.isEmpty()) {
            return new PageResponse<>(
                    Collections.emptyList(),
                    totalElements,
                    totalPages,
                    page,
                    size,
                    first,
                    last
            );
        }
        List<U> mappedContent = content.stream()
                .map(mapper)
                .toList();
        return new PageResponse<>(
                mappedContent,
                totalElements,
                totalPages,
                page,
                size,
                first,
                last
        );
    }

    /**
     * Checks if the page has content.
     *
     * @return true if content is not empty
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * Checks if the page is empty.
     *
     * @return true if content is null or empty
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * Gets the number of elements in this page.
     *
     * @return the number of elements
     */
    public int numberOfElements() {
        return content != null ? content.size() : 0;
    }

    /**
     * Creates a new PageResponse with the same metadata but different content.
     *
     * @param newContent the new content
     * @param <U>        the new content type
     * @return PageResponse with new content
     */
    public <U> PageResponse<U> withContent(List<U> newContent) {
        return new PageResponse<>(
                newContent,
                totalElements,
                totalPages,
                page,
                size,
                first,
                last
        );
    }
}