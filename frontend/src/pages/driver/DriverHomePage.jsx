import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  getAvailableShipments,
  getMyAssignedDispatches,
  getMyDispatchHistory,
  getDriverOffDays,
  registerDriverOffDay,
  cancelDriverOffDay,
  getDriverWorkHours,
  saveDriverWorkHours,
  deleteDriverWorkHours,
} from '../../api/dispatchApi'
import StatusBadge from '../../components/common/StatusBadge'

// 서버 시간("09:00:00")을 화면용 "09:00"으로
const hhmm = (time) => time?.slice(0, 5) || ''

// 날짜를 YYYY-MM-DD 형식으로 변환
const makeDateKey = (year, month, day) => {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

// 기본 근무: 월~토 09:00~18:00, 일요일 휴무. 날짜별 저장 내역이 우선 적용된다.
const isDefaultOff = (dateKey) => new Date(`${dateKey}T12:00:00`).getDay() === 0

// API의 상차 예정일에서 날짜만 추출
const getPickupDate = (item) => {
  return item.pickupAt?.slice(0, 10) || ''
}

// 기사 홈 — 운송 요청 · 배차 관리 · 월간 일정
const DriverHomePage = () => {
  const { hash } = useLocation()

  useEffect(() => {
    if (hash === '#schedule') {
      document.getElementById('driver-schedule')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }, [hash])
  const [currentMonth, setCurrentMonth] = useState(() => {
    const today = new Date()
    return {
      year: today.getFullYear(),
      month: today.getMonth(),
    }
  })

  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date()
    return makeDateKey(
      today.getFullYear(),
      today.getMonth(),
      today.getDate()
    )
  })

  // 신청 대기 및 배차 확정 데이터
  const [pendingShipments, setPendingShipments] = useState([])
  const [assignedDispatches, setAssignedDispatches] = useState([])
  const [completedShipments, setCompletedShipments] = useState([])

  const [loading, setLoading] = useState(true)
  const [pendingError, setPendingError] = useState(false)
  const [assignedError, setAssignedError] = useState(false)
  const [completedError, setCompletedError] = useState(false)

  // 기사 휴무일 관리
  const [offDays, setOffDays] = useState([])
  const [offLoading, setOffLoading] = useState(true)
  const [offError, setOffError] = useState('')
  const [offSaving, setOffSaving] = useState(false)
  const [offActionError, setOffActionError] = useState('')

  // 기사 근무 가능 시간 (DRV-01) — 입력 칸은 고른 날짜 기준 (다른 날짜를 고르면 그 날짜 값으로 다시 채운다)
  const [workHours, setWorkHours] = useState([])
  const [workError, setWorkError] = useState('')
  const [workSaving, setWorkSaving] = useState(false)
  const [workActionError, setWorkActionError] = useState('')
  const [workForm, setWorkForm] = useState({ date: '', start: '', end: '' })
  const [workEditing, setWorkEditing] = useState(false)

  // 기존 API를 활용하여 기사 일정 조회
  useEffect(() => {
    let isActive = true

    const loadSchedule = async () => {
      setLoading(true)

      const [pendingResult, assignedResult, historyResult] =
        await Promise.allSettled([
          getAvailableShipments(),
          getMyAssignedDispatches(),
          getMyDispatchHistory(),
        ])

      if (!isActive) return

      if (
        pendingResult.status === 'fulfilled' &&
        Array.isArray(pendingResult.value)
      ) {
        // 현재 REQUESTED 상태이며 내가 신청한 운송 요청
        setPendingShipments(
          pendingResult.value.filter(
            (shipment) => shipment.alreadyApplied === true
          )
        )
        setPendingError(false)
      } else {
        setPendingError(true)
        console.error('신청 대기 일정 조회 실패:', pendingResult)
      }

      if (
        assignedResult.status === 'fulfilled' &&
        Array.isArray(assignedResult.value)
      ) {
        setAssignedDispatches(assignedResult.value)
        setAssignedError(false)
      } else {
        setAssignedError(true)
        console.error('배차 확정 일정 조회 실패:', assignedResult)
      }

      // 실제 완료일이 기록된 배송만 달력에 표시
      if (
        historyResult.status === 'fulfilled' &&
        Array.isArray(historyResult.value)
      ) {
        setCompletedShipments(
          historyResult.value.filter(
            (history) =>
              history.applicationStatus === 'SELECTED' &&
              history.shipmentStatus === 'COMPLETED' &&
              history.completedAt
          )
        )
        setCompletedError(false)
      } else {
        setCompletedShipments([])
        setCompletedError(true)
        console.error('배송 완료 일정 조회 실패:', historyResult)
      }

      setLoading(false)
    }

    loadSchedule()

    return () => {
      isActive = false
    }
  }, [])


  // 현재 달의 휴무일 조회
  useEffect(() => {
    let isActive = true

    Promise.resolve()
      .then(() => {
        if (!isActive) return

        setOffLoading(true)
        setOffError('')

        return getDriverOffDays(currentMonth.year, currentMonth.month + 1)
      })
      .then((data) => {
        if (!isActive) return
        setOffDays(Array.isArray(data) ? data : [])
      })
      .catch((err) => {
        if (!isActive) return
        console.error('휴무일 조회 실패:', err)
        setOffDays([])
        setOffError('휴무일을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (isActive) setOffLoading(false)
      })

    return () => {
      isActive = false
    }
  }, [currentMonth.year, currentMonth.month])

  // 현재 달의 근무 가능 시간 조회 (DRV-01)
  useEffect(() => {
    let isActive = true

    getDriverWorkHours(currentMonth.year, currentMonth.month + 1)
      .then((data) => {
        if (!isActive) return
        setWorkHours(Array.isArray(data) ? data : [])
        setWorkError('')
      })
      .catch((err) => {
        if (!isActive) return
        console.error('근무 가능 시간 조회 실패:', err)
        setWorkHours([])
        setWorkError('근무 가능 시간을 불러오지 못했습니다.')
      })

    return () => {
      isActive = false
    }
  }, [currentMonth.year, currentMonth.month])


  // 이전 달 / 다음 달 이동
  const changeMonth = (amount) => {
    const newDate = new Date(
      currentMonth.year,
      currentMonth.month + amount,
      1
    )

    setCurrentMonth({
      year: newDate.getFullYear(),
      month: newDate.getMonth(),
    })

    setSelectedDate(
      makeDateKey(
        newDate.getFullYear(),
        newDate.getMonth(),
        1
      )
    )
    setWorkEditing(false)
    setWorkForm({ date: '', start: '', end: '' })
  }

  // 이번 달로 돌아가기
  const moveToToday = () => {
    const today = new Date()

    setCurrentMonth({
      year: today.getFullYear(),
      month: today.getMonth(),
    })

    setSelectedDate(
      makeDateKey(
        today.getFullYear(),
        today.getMonth(),
        today.getDate()
      )
    )
    setWorkEditing(false)
    setWorkForm({ date: '', start: '', end: '' })
  }

  // 달력 날짜 생성
  const firstDay = new Date(
    currentMonth.year,
    currentMonth.month,
    1
  ).getDay()

  const daysInMonth = new Date(
    currentMonth.year,
    currentMonth.month + 1,
    0
  ).getDate()

  const calendarDays = [
    ...Array(firstDay).fill(null),
    ...Array.from(
      { length: daysInMonth },
      (_, index) => index + 1
    ),
  ]

  // 선택 날짜의 일정
  const selectedPending = pendingShipments.filter(
    (shipment) => getPickupDate(shipment) === selectedDate
  )

  const selectedAssigned = assignedDispatches.filter(
    (dispatch) => getPickupDate(dispatch) === selectedDate
  )

  const selectedCompleted = completedShipments.filter(
    (history) => history.completedAt?.slice(0, 10) === selectedDate
  )

  const today = new Date()
  const todayKey = makeDateKey(
    today.getFullYear(),
    today.getMonth(),
    today.getDate()
  )

  // 선택한 날짜의 휴무 여부
  const selectedWork = workHours.find((work) => work.date === selectedDate)
  const selectedIsDefaultOff = isDefaultOff(selectedDate)
  const selectedIsOff = offDays.includes(selectedDate) || (selectedIsDefaultOff && !selectedWork)
  const isPastDate = selectedDate < todayKey

  // 휴무 등록 또는 취소
  const handleDayOff = async () => {
    if (offSaving || offLoading || offError || isPastDate) return

    const date = selectedDate
    const isOff = offDays.includes(date)
    // 기본 휴무인 일요일은 취소 대상이 아니다. 일요일 근무는 시간 예외로 등록한다.
    if (selectedIsDefaultOff && !selectedWork) return

    setOffSaving(true)
    setOffActionError('')

    try {
      if (isOff) {
        await cancelDriverOffDay(date)
        setOffDays((prev) => prev.filter((day) => day !== date))
      } else {
        await registerDriverOffDay(date)
        setOffDays((prev) => [...new Set([...prev, date])].sort())
        setWorkHours((prev) => prev.filter((work) => work.date !== date))
      }
      setWorkEditing(false)
      setWorkForm({ date: '', start: '', end: '' })
    } catch (err) {
      console.error('휴무일 처리 실패:', err)

      setOffActionError(
        err.response?.data?.message || '휴무일 처리에 실패했습니다.'
      )
    } finally {
      setOffSaving(false)
    }
  }

  // 선택한 날짜의 근무 가능 시간 (입력 칸 값은 고친 중이면 그 값, 아니면 저장된 값 · 기본 09:00 ~ 18:00)
  const editingSelected = workForm.date === selectedDate
  const formStart = editingSelected ? workForm.start : (hhmm(selectedWork?.startTime) || '09:00')
  const formEnd = editingSelected ? workForm.end : (hhmm(selectedWork?.endTime) || '18:00')

  // 근무 가능 시간 등록 · 수정
  const handleSaveWorkHours = async () => {
    if (workSaving || isPastDate) return
    if (!formStart || !formEnd || formStart === formEnd) {
      setWorkActionError('시작 시간과 끝 시간을 다르게 입력해 주세요.')
      return
    }

    const date = selectedDate
    setWorkSaving(true)
    setWorkActionError('')

    try {
      const saved = await saveDriverWorkHours(date, formStart, formEnd)
      setWorkHours((prev) =>
        [...prev.filter((work) => work.date !== date), saved].sort((a, b) => a.date.localeCompare(b.date)))
      setWorkForm({ date: '', start: '', end: '' })
      setOffDays((prev) => prev.filter((day) => day !== date))
      setWorkEditing(false)
    } catch (err) {
      console.error('근무 가능 시간 저장 실패:', err)
      setWorkActionError(err.response?.data?.message || '근무 가능 시간 저장에 실패했습니다.')
    } finally {
      setWorkSaving(false)
    }
  }

  // 근무 가능 시간 지우기
  const handleDeleteWorkHours = async () => {
    if (workSaving || isPastDate || !selectedWork) return

    const date = selectedDate
    setWorkSaving(true)
    setWorkActionError('')

    try {
      await deleteDriverWorkHours(date)
      setWorkHours((prev) => prev.filter((work) => work.date !== date))
      setWorkForm({ date: '', start: '', end: '' })
      setWorkEditing(false)
    } catch (err) {
      console.error('근무 가능 시간 삭제 실패:', err)
      setWorkActionError(err.response?.data?.message || '근무 가능 시간 삭제에 실패했습니다.')
    } finally {
      setWorkSaving(false)
    }
  }

  return (
    <section className="grid gap-6">

      {/* 페이지 제목 */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            기사 홈
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            운송 요청과 배차 일정을 한눈에 관리할 수 있습니다.
          </p>
        </div>

        {/* 마이페이지 및 월별 정산 이동 */}
        <div className="flex w-full gap-2 sm:w-auto">
          <Link
            to="/driver/settlements"
            className="flex h-10 min-w-0 flex-1 items-center justify-center rounded-xl border border-purple-200 bg-white px-3 text-center text-xs font-semibold text-purple-700 shadow-sm transition hover:border-purple-300 hover:bg-purple-50 sm:w-[138px] sm:flex-none sm:text-sm"
          >
            💰 월별 정산
          </Link>

          <Link
            to="/driver/me"
            className="flex h-10 min-w-0 flex-1 items-center justify-center rounded-xl border border-gray-200 bg-white px-3 text-center text-xs font-semibold text-gray-700 shadow-sm transition hover:bg-gray-50 sm:w-[138px] sm:flex-none sm:text-sm"
          >
            👤 마이페이지
          </Link>
        </div>
      </div>

      {/* 주요 메뉴 카드 */}
      <div className="grid gap-4 sm:grid-cols-2">

        {/* 운송 요청 목록 */}
        <Link
          to="/driver/shipments"
          className="block rounded-2xl border border-blue-100 bg-white p-4 text-inherit no-underline shadow-sm transition hover:border-blue-300 hover:shadow-md"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-2xl">
              🚛
            </div>

            <div className="flex min-w-0 flex-1 items-center justify-between gap-2">
              <h2 className="text-lg font-bold text-gray-900">
                운송 요청 목록
              </h2>

              <span className="shrink-0 text-sm font-semibold text-blue-600">
                <span className="hidden lg:inline">보러가기 </span>→
              </span>
            </div>
          </div>
        </Link>

        {/* 내 배차 관리 */}
        <Link
          to="/driver/dispatches"
          className="block rounded-2xl border border-green-100 bg-white p-4 text-inherit no-underline shadow-sm transition hover:border-green-300 hover:shadow-md"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-green-50 text-2xl">
              📋
            </div>

            <div className="flex min-w-0 flex-1 items-center justify-between gap-2">
              <h2 className="text-lg font-bold text-gray-900">
                내 배차 관리
              </h2>

              <span className="shrink-0 text-sm font-semibold text-green-700">
                <span className="hidden lg:inline">관리하기 </span>→
              </span>
            </div>
          </div>
        </Link>
      </div>

      {/* 월간 스케줄 달력 */}

      <div
        id="driver-schedule"
        className="scroll-mt-6 rounded-2xl border border-gray-100 bg-white p-4 shadow-sm sm:p-6"
      >

        {/* 달력 제목 및 월 이동 */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-gray-900">
              월간 배차 일정
            </h2>
            <p className="mt-1 text-xs text-gray-500">
              상차 예정일을 기준으로 표시합니다.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={moveToToday}
              className="rounded-lg border border-gray-200 px-3 py-2 text-xs text-gray-600 hover:bg-gray-50"
            >
              오늘
            </button>

            <button
              type="button"
              onClick={() => changeMonth(-1)}
              className="rounded-lg border border-gray-200 px-3 py-2 hover:bg-gray-50"
            >
              ‹
            </button>

            <span className="min-w-28 text-center text-base font-bold text-gray-900 sm:text-lg">
              {currentMonth.year}년 {currentMonth.month + 1}월
            </span>

            <button
              type="button"
              onClick={() => changeMonth(1)}
              className="rounded-lg border border-gray-200 px-3 py-2 hover:bg-gray-50"
            >
              ›
            </button>
          </div>
        </div>

        {/* 일정 상태 범례 */}
        <div className="mt-5 flex flex-wrap items-center gap-4 text-sm text-gray-600">
          <div className="flex items-center gap-2">
            <span className="h-4 w-4 rounded-full bg-green-500" />
            신청 대기
          </div>

          <div className="flex items-center gap-2">
            <span className="h-4 w-4 rounded-full bg-blue-500" />
            배차 확정 · 진행 중
          </div>

          <div className="flex items-center gap-2">
            <span className="h-4 w-4 rounded-full bg-purple-500" />
            배송 완료
          </div>

          <div className="flex items-center gap-2">
            <span className="h-4 w-4 rounded bg-slate-300" />
            휴무일
          </div>

          <div className="flex items-center gap-2">
            <span className="h-4 w-4 rounded border border-emerald-300 bg-emerald-100" />
            근무시간 변경 (기본 월~토 09~18시)
          </div>
        </div>

        {/* 달력 */}
        <div className="mt-5 grid grid-cols-7 gap-1 sm:gap-2">
          {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
            <div
              key={day}
              className="py-3 text-center text-sm font-semibold text-gray-600 sm:text-base"
            >
              {day}
            </div>
          ))}

          {calendarDays.map((day, index) => {
            if (day === null) {
              return <div key={`empty-${index}`} />
            }

            const dateKey = makeDateKey(
              currentMonth.year,
              currentMonth.month,
              day
            )

            const pendingCount = pendingShipments.filter(
              (shipment) => getPickupDate(shipment) === dateKey
            ).length

            const assignedCount = assignedDispatches.filter(
              (dispatch) => getPickupDate(dispatch) === dateKey
            ).length

            const completedCount = completedShipments.filter(
              (history) => history.completedAt?.slice(0, 10) === dateKey
            ).length

            const isSelected = selectedDate === dateKey
            const isToday = todayKey === dateKey
            const work = workHours.find((item) => item.date === dateKey)
            const isOff = offDays.includes(dateKey) || (isDefaultOff(dateKey) && !work)
            // 기본 일정과 다른 근무시간만 표시한다. 일요일 근무는 그 자체로 예외다.
            const isChangedWorkTime = !!work && (
              isDefaultOff(dateKey) ||
              hhmm(work.startTime) !== '09:00' ||
              hhmm(work.endTime) !== '18:00' ||
              work.overnight
            )

            return (
              <button
                key={dateKey}
                type="button"
                onClick={() => {
                  setSelectedDate(dateKey)
                  setWorkActionError('')
                  setWorkEditing(false)
                  setWorkForm({ date: '', start: '', end: '' })
                }}
                className={`flex min-h-[92px] flex-col items-center rounded-xl border-2 p-2 transition sm:min-h-[116px] sm:p-3 ${isToday
                  ? 'border-amber-400 bg-amber-50 ring-2 ring-amber-100'
                  : isOff
                    ? 'border-slate-300 bg-slate-100 hover:bg-slate-200'
                    : isSelected
                      ? 'border-blue-400 bg-blue-50'
                      : 'border-gray-100 bg-white hover:border-gray-200 hover:bg-gray-50'
                  }`}
              >
                <span
                  className={`flex h-9 w-9 items-center justify-center rounded-full text-base font-semibold sm:h-10 sm:w-10 sm:text-lg ${isToday
                    ? 'bg-amber-500 font-bold text-white'
                    : 'text-gray-900'
                    }`}
                >
                  {day}
                </span>

                {isToday && (
                  <span className="mt-1 text-[11px] font-bold text-amber-700">
                    오늘
                  </span>
                )}

                <div className="mt-2 flex flex-wrap justify-center gap-1.5">
                  {pendingCount > 0 && (
                    <span
                      title={`신청 대기 ${pendingCount}건`}
                      className="h-4 w-4 rounded-full bg-green-500 shadow-sm"
                    />
                  )}

                  {assignedCount > 0 && (
                    <span
                      title={`배차 확정 ${assignedCount}건`}
                      className="h-4 w-4 rounded-full bg-blue-500 shadow-sm"
                    />
                  )}

                  {completedCount > 0 && (
                    <span
                      title={`배송 완료 ${completedCount}건`}
                      className="h-4 w-4 rounded-full bg-purple-500 shadow-sm"
                    />
                  )}
                </div>

                {isOff && (
                  <span className="mt-1 rounded-md bg-slate-200 px-2 py-1 text-xs font-bold text-slate-700">
                    {isDefaultOff(dateKey) && !offDays.includes(dateKey) ? '기본 휴무' : '휴무'}
                  </span>
                )}

                {!isOff && isChangedWorkTime && (
                  <span
                    title={`변경 근무 ${hhmm(work.startTime)} ~ ${hhmm(work.endTime)}${work.overnight ? ' (다음 날)' : ''}`}
                    className="mt-1 rounded-md border border-emerald-200 bg-emerald-50 px-1.5 py-0.5 text-[11px] font-semibold text-emerald-700"
                  >
                    <span className="sm:hidden">변경</span>
                    <span className="hidden sm:inline">{hhmm(work.startTime)}~{hhmm(work.endTime)}</span>
                  </span>
                )}
              </button>
            )
          })}
        </div>

        {/* API 조회 상태 */}
        {loading && (
          <p className="mt-4 text-center text-sm text-gray-500">
            배차 일정을 불러오는 중...
          </p>
        )}

        {!loading && (pendingError || assignedError || completedError) && (
          <p className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {pendingError && '신청 대기 일정 조회에 실패했습니다. '}
            {assignedError && '배차 확정 일정 조회에 실패했습니다. '}
            {completedError && '배송 완료 일정 조회에 실패했습니다.'}
          </p>
        )}

        {/* 선택 날짜의 운송 일정 */}
        <div className="mt-6 border-t border-gray-100 pt-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h3 className="font-bold text-gray-900">
              {selectedDate.replaceAll('-', '. ')} 일정
            </h3>

            {!isPastDate && (
              <div className="flex flex-wrap items-center gap-2">
                {!offLoading && !offError && (
                  <button
                    type="button"
                    onClick={handleDayOff}
                    disabled={offSaving || workSaving || (selectedIsDefaultOff && !selectedWork)}
                    className={`rounded-lg border px-3 py-1.5 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${selectedIsOff
                      ? 'border-gray-300 bg-gray-100 text-gray-700 hover:bg-gray-200'
                      : 'border-blue-600 bg-blue-600 text-white hover:bg-blue-700'
                      }`}
                  >
                    {offSaving ? '처리 중...' : selectedIsDefaultOff && !selectedWork ? '기본 휴무' : selectedIsOff ? '휴무 취소' : '휴무일 등록'}
                  </button>
                )}
                <button
                  type="button"
                  aria-expanded={workEditing}
                  aria-controls="driver-work-hours-editor"
                  onClick={() => {
                    setWorkEditing((prev) => !prev)
                    setWorkForm({ date: '', start: '', end: '' })
                    setWorkActionError('')
                  }}
                  disabled={workSaving || !!workError || !!offError || offLoading}
                  className="rounded-lg border border-emerald-600 bg-white px-3 py-1.5 text-xs font-semibold text-emerald-700 transition hover:bg-emerald-50 disabled:opacity-50"
                >
                  근무시간 변경
                </button>
              </div>
            )}
          </div>

          {offLoading && (
            <p className="mt-3 text-sm text-gray-500">
              휴무일을 불러오는 중...
            </p>
          )}

          {offError && (
            <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-600">
              {offError}
            </p>
          )}

          {offActionError && (
            <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-600">
              {offActionError}
            </p>
          )}

          {workError && <p className="mt-3 text-xs text-red-600">{workError}</p>}
          {workEditing && (
            <div id="driver-work-hours-editor" className="mt-4 rounded-xl border border-emerald-100 bg-emerald-50/60 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-bold text-emerald-800">근무 가능 시간</p>
                <p className="text-sm font-semibold text-emerald-700">
                  {selectedIsOff
                    ? selectedIsDefaultOff && !offDays.includes(selectedDate) ? '매주 일요일 기본 휴무' : '이 날짜만 휴무'
                    : selectedWork
                      ? `${hhmm(selectedWork.startTime)} ~ ${hhmm(selectedWork.endTime)}${selectedWork.overnight ? ' (다음 날)' : ''} (변경)`
                      : '09:00 ~ 18:00 (기본)'}
                </p>
              </div>
              <p className="mt-2 text-xs text-gray-500">
                기본 일정은 월~토 09:00~18:00, 일요일 휴무예요. 변경한 날짜만 별도 저장됩니다.
              </p>

              {!isPastDate && !workError && (
                <div className="mt-3 flex flex-wrap items-end gap-2">
                  <label className="text-xs font-medium text-gray-600">
                    시작
                    <input
                      type="time"
                      value={formStart}
                      onChange={(event) => setWorkForm({ date: selectedDate, start: event.target.value, end: formEnd })}
                      className="mt-1 block rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-sm"
                    />
                  </label>
                  <label className="text-xs font-medium text-gray-600">
                    끝
                    <input
                      type="time"
                      value={formEnd}
                      onChange={(event) => setWorkForm({ date: selectedDate, start: formStart, end: event.target.value })}
                      className="mt-1 block rounded-lg border border-gray-200 bg-white px-2 py-1.5 text-sm"
                    />
                  </label>
                  <button
                    type="button"
                    onClick={handleSaveWorkHours}
                    disabled={workSaving || offSaving}
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {workSaving ? '저장 중...' : '시간 저장'}
                  </button>
                  {selectedWork && (
                    <button
                      type="button"
                      onClick={handleDeleteWorkHours}
                      disabled={workSaving || offSaving}
                      className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                    >
                      기본 일정으로
                    </button>
                  )}
                </div>
              )}
              {!isPastDate && (
                <p className="mt-2 text-xs text-gray-500">끝 시간이 시작보다 빠르면 다음 날까지 근무해요. 저장하면 해당 날짜만 적용됩니다.</p>
              )}
              {workActionError && (
                <p className="mt-2 rounded-lg bg-red-50 p-2 text-xs text-red-600">{workActionError}</p>
              )}
            </div>
          )}

          {!loading &&
            !selectedIsOff &&
            selectedPending.length === 0 &&
            selectedAssigned.length === 0 &&
            selectedCompleted.length === 0 &&
            !pendingError &&
            !assignedError &&
            !completedError && (
              <div className="mt-4 rounded-xl bg-gray-50 p-6 text-center text-sm text-gray-500">
                해당 날짜에 예정된 배차 일정이 없습니다.
              </div>
            )}

          <div className="mt-4 grid gap-3">

            {/* 신청 대기 일정 */}
            {selectedPending.map((shipment) => (
              <Link
                key={`pending-${shipment.shipmentId}`}
                to={`/driver/shipments/${shipment.shipmentId}?from=my-dispatches`}
                className="block rounded-2xl border border-gray-200 bg-white p-4 text-inherit no-underline shadow-sm transition hover:border-green-300 hover:shadow-md sm:p-5"
              >
                {/* 화물명 및 상태 */}
                <div className="flex flex-wrap items-center gap-3">
                  <h4 className="text-lg font-bold text-gray-900">
                    {shipment.cargoName}
                  </h4>

                  <span className="inline-flex items-center rounded-full border border-green-200 bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
                    신청 대기
                  </span>
                </div>

                {/* 출발지 및 도착지 */}
                <div className="mt-4 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3 rounded-xl bg-gray-50 p-4">
                  <div className="min-w-0">
                    <p className="mb-1 text-xs text-gray-500">
                      출발지
                    </p>
                    <p className="break-words text-sm font-semibold text-gray-900">
                      {shipment.originAddress}
                    </p>
                  </div>

                  <span className="text-lg font-bold text-green-500">
                    →
                  </span>

                  <div className="min-w-0">
                    <p className="mb-1 text-xs text-gray-500">
                      도착지
                    </p>
                    <p className="break-words text-sm font-semibold text-gray-900">
                      {shipment.destAddress}
                    </p>
                  </div>
                </div>

                {/* 상차 예정일 및 상세 보기 */}
                <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <p className="text-xs text-gray-500">
                      상차 예정일
                    </p>
                    <p className="mt-1 text-sm font-semibold text-gray-800">
                      {shipment.pickupAt
                        ? new Date(shipment.pickupAt).toLocaleString('ko-KR')
                        : '정보 없음'}
                    </p>
                  </div>

                  <span className="text-sm font-semibold text-green-700">
                    상세 보기 →
                  </span>
                </div>
              </Link>
            ))}


            {/* 배차 확정 및 배송 진행 중 일정 */}
            {selectedAssigned.map((dispatch) => (
              <Link
                key={`assigned-${dispatch.dispatchId}`}
                to={`/driver/dispatches/${dispatch.shipmentId}`}
                className="block rounded-2xl border border-gray-200 bg-white p-4 text-inherit no-underline shadow-sm transition hover:border-blue-300 hover:shadow-md sm:p-5"
              >
                {/* 화물명 및 상태 */}
                <div className="flex flex-wrap items-center gap-3">
                  <h4 className="text-lg font-bold text-gray-900">
                    {dispatch.cargoName}
                  </h4>

                  <StatusBadge status={dispatch.status} />
                </div>

                {/* 출발지 및 도착지 */}
                <div className="mt-4 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3 rounded-xl bg-gray-50 p-4">
                  <div className="min-w-0">
                    <p className="mb-1 text-xs text-gray-500">
                      출발지
                    </p>
                    <p className="break-words text-sm font-semibold text-gray-900">
                      {dispatch.originAddress}
                    </p>
                  </div>

                  <span className="text-lg font-bold text-blue-500">
                    →
                  </span>

                  <div className="min-w-0">
                    <p className="mb-1 text-xs text-gray-500">
                      도착지
                    </p>
                    <p className="break-words text-sm font-semibold text-gray-900">
                      {dispatch.destAddress}
                    </p>
                  </div>
                </div>

                {/* 상차 예정일 및 상세 보기 */}
                <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <p className="text-xs text-gray-500">
                      상차 예정일
                    </p>
                    <p className="mt-1 text-sm font-semibold text-gray-800">
                      {dispatch.pickupAt
                        ? new Date(dispatch.pickupAt).toLocaleString('ko-KR')
                        : '정보 없음'}
                    </p>
                  </div>

                  <span className="text-sm font-semibold text-blue-700">
                    배송 진행 상세 보기 →
                  </span>
                </div>
              </Link>
            ))}



            {/* 배송 완료 일정 */}
            {selectedCompleted.map((history) => (
              <Link
                key={`completed-${history.applicationId}`}
                to={`/driver/dispatches/history/${history.applicationId}`}
                className="block rounded-2xl border border-gray-200 bg-white p-4 text-inherit no-underline shadow-sm transition hover:border-purple-300 hover:shadow-md sm:p-5"
              >
                {/* 화물명 및 상태 */}
                <div className="flex flex-wrap items-center gap-3">
                  <h4 className="text-lg font-bold text-gray-900">
                    {history.cargoName}
                  </h4>

                  <span className="inline-flex items-center rounded-full border border-purple-200 bg-purple-50 px-3 py-1 text-xs font-semibold text-purple-700">
                    ✓ 배송 완료
                  </span>
                </div>

                {/* 출발지 및 도착지 */}
                <div className="mt-4 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3 rounded-xl bg-gray-50 p-4">
                  <div className="min-w-0">
                    <p className="mb-1 text-xs text-gray-500">
                      출발지
                    </p>
                    <p className="break-words text-sm font-semibold text-gray-900">
                      {history.originAddress}
                    </p>
                  </div>

                  <span className="text-lg font-bold text-purple-500">
                    →
                  </span>

                  <div className="min-w-0">
                    <p className="mb-1 text-xs text-gray-500">
                      도착지
                    </p>
                    <p className="break-words text-sm font-semibold text-gray-900">
                      {history.destAddress}
                    </p>
                  </div>
                </div>

                {/* 완료 일시 및 상세 보기 */}
                <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <p className="text-xs text-gray-500">
                      배송 완료 일시
                    </p>
                    <p className="mt-1 text-sm font-semibold text-gray-800">
                      {new Date(history.completedAt).toLocaleString('ko-KR')}
                    </p>
                  </div>

                  <span className="text-sm font-semibold text-purple-700">
                    상세 보기 →
                  </span>
                </div>
              </Link>
            ))}

          </div>
        </div>
      </div>

    </section>
  )
}

export default DriverHomePage
