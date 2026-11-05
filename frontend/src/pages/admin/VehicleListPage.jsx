import { useEffect, useState } from 'react'
import { getVehicleList } from '../../api/vehicleApi'
import { Link, useLocation, useNavigate } from 'react-router-dom'

const VehicleListPage = () => {
    const [vehicles, setVehicles] = useState([])
    const [page, setPage] = useState(1)
    const [totalPage, setTotalPage] = useState(1)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const location = useLocation()
    const navigate = useNavigate()

    // 목록으로 전달된 토스트를 1초 후 제거
    useEffect(() => {
        if (!location.state?.toast) return

        const timer = setTimeout(() => {
            navigate(location.pathname + location.search, {
                replace: true,
                state: null
            })
        }, 1000)

        return () => clearTimeout(timer)
    }, [location.state, location.pathname, location.search, navigate])

    // 현재 페이지의 차량 목록 조회 (로딩 표시 · 오류 지우기는 movePage에서)
    useEffect(() => {
        let active = true

        getVehicleList(page, 10)
            .then((data) => {
                if (!active) return

                setVehicles(data.dtoList ?? [])
                setTotalPage(data.totalPage ?? 1)
            })
            .catch(() => {
                if (active) {
                    setError('차량 목록을 불러오지 못했습니다.')
                }
            })
            .finally(() => {
                if (active) setLoading(false)
            })

        return () => {
            active = false
        }
    }, [page])

    // 페이지 이동: 로딩 표시를 켜고 이전 오류를 지운 뒤 페이지를 바꾼다
    // (effect 안에서 바로 setState를 부르지 않는다 — react-hooks/set-state-in-effect)
    const movePage = (nextPage) => {
        setLoading(true)
        setError('')
        setPage(nextPage)
    }

    return (
        <section className="space-y-6">
            {/* 등록·수정 완료 토스트 */}
            {location.state?.toast && (
                <div
                    role="status"
                    className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 rounded-xl bg-emerald-700 px-10 py-6 text-lg font-semibold text-white shadow-2xl"
                >
                    ✓ {location.state.toast}
                </div>
            )}

            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold">차량 관리</h1>
                    <p className="mt-2 text-sm text-gray-600">
                        등록된 차량의 정보를 확인할 수 있습니다.
                    </p>
                </div>

                {/* 차량 등록 페이지로 이동 */}
                <Link
                    to="/admin/vehicles/new"
                    className="rounded bg-emerald-700 px-4 py-2 text-sm text-white"
                >
                    + 차량 등록
                </Link>
            </div>

            <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
                <table className="w-full text-left text-sm">
                    <thead className="bg-gray-100">
                        <tr>
                            <th className="px-4 py-3">번호</th>
                            <th className="px-4 py-3">차량번호</th>
                            <th className="px-4 py-3">상태</th>
                            <th className="px-4 py-3">관리</th>
                        </tr>
                    </thead>

                    <tbody>
                        {vehicles.map((vehicle) => (
                            <tr
                                key={vehicle.id}
                                className={`border-t border-gray-200 ${vehicle.status === 'INACTIVE' ? 'bg-gray-100 text-gray-500' : ''
                                    }`}
                            >
                                <td className="px-4 py-3">{vehicle.id}</td>
                                <td className="px-4 py-3">{vehicle.plateNo}</td>

                                <td className="px-4 py-3">
                                    {/* 차량 상태별 색상과 문구 표시 */}
                                    <span
                                        className={`inline-block rounded px-2 py-1 text-xs font-medium ${vehicle.status === 'AVAILABLE'
                                            ? 'bg-emerald-100 text-emerald-800'
                                            : vehicle.status === 'MAINTENANCE'
                                                ? 'bg-amber-100 text-amber-800'
                                                : 'bg-red-100 text-red-700'
                                            }`}
                                    >
                                        {vehicle.status === 'AVAILABLE'
                                            ? '✓ 운행 가능'
                                            : vehicle.status === 'MAINTENANCE'
                                                ? '⚒ 정비 중'
                                                : vehicle.status === 'INACTIVE'
                                                    ? '✕ 비활성'
                                                    : vehicle.status}
                                    </span>
                                </td>

                                <td className="px-4 py-3">
                                    {/* 선택한 차량 정보를 수정 페이지로 전달 */}
                                    <Link
                                        to={`/admin/vehicles/${vehicle.id}/edit`}
                                        state={{ vehicle }}
                                        className="rounded border border-gray-300 px-3 py-1"
                                    >
                                        수정
                                    </Link>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {loading && (
                    <p className="p-4 text-center text-gray-500">불러오는 중...</p>
                )}

                {error && (
                    <p className="p-4 text-center text-red-600">{error}</p>
                )}

                {!loading && !error && vehicles.length === 0 && (
                    <p className="p-4 text-center text-gray-500">
                        등록된 차량이 없습니다.
                    </p>
                )}
            </div>

            <div className="flex items-center justify-center gap-4">
                <button
                    type="button"
                    disabled={page <= 1}
                    onClick={() => movePage(page - 1)}
                    className="rounded border px-4 py-2 disabled:opacity-40"
                >
                    이전
                </button>

                <span>{page} / {totalPage}</span>

                <button
                    type="button"
                    disabled={page >= totalPage}
                    onClick={() => movePage(page + 1)}
                    className="rounded border px-4 py-2 disabled:opacity-40"
                >
                    다음
                </button>
            </div>
        </section>
    )
}

export default VehicleListPage
