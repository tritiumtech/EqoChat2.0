package com.eqochat.framework.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应基类
 * 支持两种分页模式：
 * 1. 游标分页（Cursor-based）：使用 nextCursorId，适合实时性要求高的场景
 * 2. Offset/Limit 分页：使用 pageNo/pageSize/totalPages，支持跳转到指定页码
 *
 * @param <T> 列表项数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> items;

    /**
     * 总记录数（全表总数，非当前页数量）
     */
    private Long total;

    /**
     * 是否还有更多数据（游标分页用）
     */
    private Boolean hasMore;

    /**
     * 下一页游标ID（游标分页用）
     */
    private Long nextCursorId;

    /**
     * 当前页码（Offset分页用，从1开始）
     */
    private Integer pageNo;

    /**
     * 每页大小（Offset分页用）
     */
    private Integer pageSize;

    /**
     * 总页数（Offset分页用）
     */
    private Integer totalPages;

    /**
     * 创建空分页响应
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .items(List.of())
                .total(0L)
                .hasMore(false)
                .nextCursorId(null)
                .pageNo(1)
                .pageSize(20)
                .totalPages(0)
                .build();
    }

    /**
     * 创建游标分页响应
     *
     * @param items        数据列表
     * @param hasMore      是否还有更多
     * @param nextCursorId 下一页游标
     */
    public static <T> PageResponse<T> of(List<T> items, boolean hasMore, Long nextCursorId) {
        return PageResponse.<T>builder()
                .items(items != null ? items : List.of())
                .total(items != null ? (long) items.size() : 0L)
                .hasMore(hasMore)
                .nextCursorId(nextCursorId)
                .build();
    }

    /**
     * 创建游标分页响应（不带游标）
     *
     * @param items   数据列表
     * @param hasMore 是否还有更多
     */
    public static <T> PageResponse<T> of(List<T> items, boolean hasMore) {
        return of(items, hasMore, null);
    }

    /**
     * 创建 Offset 分页响应
     *
     * @param items    数据列表
     * @param total    总记录数
     * @param pageNo   当前页码（从1开始）
     * @param pageSize 每页大小
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int pageNo, int pageSize) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        boolean hasMore = pageNo < totalPages;
        Long nextCursorId = hasMore ? (long) (pageNo + 1) : null;

        return PageResponse.<T>builder()
                .items(items != null ? items : List.of())
                .total(total)
                .hasMore(hasMore)
                .nextCursorId(nextCursorId)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    /**
     * 创建 Offset 分页响应（带总页数）
     *
     * @param items      数据列表
     * @param total      总记录数
     * @param pageNo     当前页码（从1开始）
     * @param pageSize   每页大小
     * @param totalPages 总页数
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int pageNo, int pageSize, int totalPages) {
        boolean hasMore = pageNo < totalPages;
        Long nextCursorId = hasMore ? (long) (pageNo + 1) : null;

        return PageResponse.<T>builder()
                .items(items != null ? items : List.of())
                .total(total)
                .hasMore(hasMore)
                .nextCursorId(nextCursorId)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }
}
