/**
 * 通用分页响应结构
 * 支持两种分页模式：
 * 1. 游标分页（Cursor-based）：使用 hasMore + nextCursorId
 * 2. Offset/Limit 分页：使用 pageNo + pageSize + totalPages + total
 *
 * 与后端 PageResponse<T> 对应
 */
export interface PageResponse<T> {
  /** 数据列表 */
  items: T[]
  /** 总记录数（全表总数） */
  total: number
  /** 是否还有更多数据（游标分页用） */
  hasMore: boolean
  /** 下一页游标ID（游标分页用） */
  nextCursorId?: number | string | null
  /** 当前页码（Offset分页用，从1开始） */
  pageNo?: number
  /** 每页大小（Offset分页用） */
  pageSize?: number
  /** 总页数（Offset分页用） */
  totalPages?: number
}

/**
 * 游标分页查询参数
 */
export interface CursorQueryParams {
  /** 游标ID（用于游标分页） */
  cursorId?: number | string
  /** 每页数量 */
  limit?: number
}

/**
 * Offset 分页查询参数
 */
export interface OffsetQueryParams {
  /** 页码（从1开始） */
  pageNo?: number
  /** 每页数量 */
  pageSize?: number
}

/**
 * 通用分页查询参数（两种模式都支持）
 */
export interface PageQueryParams extends CursorQueryParams, OffsetQueryParams {}
