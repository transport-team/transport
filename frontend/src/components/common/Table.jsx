// 공통 테이블 (팀원 4 관리)
// columns: [{ key, header, render?, className? }], data: [], keyField: string, emptyMessage, rowClick?
import { forwardRef } from 'react'

const Table = forwardRef((
  {
    columns = [],
    data = [],
    keyField = 'id',
    emptyMessage = '데이터가 없습니다.',
    rowClick,
    className = '',
    ...props
  },
  ref
) => {
  return (
    <div className={`overflow-x-auto rounded-lg border border-border ${className}`} ref={ref} {...props}>
      <table className="w-full text-sm" role="grid">
        <thead className="bg-gray-light">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                className={`px-4 py-3 text-left font-medium text-text-secondary ${col.className || ''}`}
                scope="col"
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-10 text-center text-text-muted">
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row) => (
              <tr
                key={row[keyField]}
                className={rowClick ? 'cursor-pointer hover:bg-gray-50 transition-colors' : ''}
                onClick={() => rowClick && rowClick(row)}
              >
                {columns.map((col) => (
                  <td key={col.key} className={`px-4 py-3 ${col.className || ''}`}>
                    {col.render ? col.render(row[col.key], row) : row[col.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
})

Table.displayName = 'Table'
export default Table