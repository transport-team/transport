import { useEffect, useRef, useState } from 'react'
import { loadKakaoMapSdk } from '../../util/kakaoMapLoader'

// 출발지 · 도착지 경로 지도 (팀원 4 — DRV-03 기사 배송 처리 화면)
// props: origin · dest = { lat, lng, label } — 좌표가 없으면 그리지 않는다 (부모가 주소 글자를 그대로 보여 준다)
// 두 곳이 다 보이게 지도 범위를 맞추고, 아래에 카카오맵 길찾기 링크(휴대폰에서는 지도 앱으로 이어짐)를 둔다

const isValidPoint = (point) => Number.isFinite(point?.lat) && Number.isFinite(point?.lng) && point.lat !== 0 && point.lng !== 0

// 카카오맵 길찾기 주소: /link/to/이름,위도,경도
const directionsUrl = (point) => `https://map.kakao.com/link/to/${encodeURIComponent(point.label || '목적지')},${point.lat},${point.lng}`

const RouteMap = ({ origin, dest }) => {
  const mapRef = useRef(null)
  const [error, setError] = useState('')
  const ready = isValidPoint(origin) && isValidPoint(dest)

  useEffect(() => {
    if (!ready) return undefined
    let cancelled = false
    loadKakaoMapSdk()
      .then((maps) => {
        if (cancelled || !mapRef.current) return
        const originPos = new maps.LatLng(origin.lat, origin.lng)
        const destPos = new maps.LatLng(dest.lat, dest.lng)
        const map = new maps.Map(mapRef.current, { center: originPos, level: 7 })

        const addPoint = (position, text, color) => {
          new maps.Marker({ map, position, title: text })
          new maps.CustomOverlay({
            map,
            position,
            yAnchor: 2.6,
            content: `<span style="padding:2px 8px;border-radius:9999px;background:${color};color:#fff;font-size:12px;font-weight:700;white-space:nowrap">${text}</span>`,
          })
        }
        addPoint(originPos, '출발', '#059669')
        addPoint(destPos, '도착', '#dc2626')

        const bounds = new maps.LatLngBounds()
        bounds.extend(originPos)
        bounds.extend(destPos)
        map.setBounds(bounds)
      })
      .catch(() => { if (!cancelled) setError('지도를 불러올 수 없어요. 아래 주소를 확인해 주세요.') })
    return () => { cancelled = true }
  }, [ready, origin?.lat, origin?.lng, dest?.lat, dest?.lng])

  if (!ready) return null

  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
      {error ? (
        <p className="p-4 text-sm text-amber-700">{error}</p>
      ) : (
        <div ref={mapRef} className="h-56 w-full" role="img" aria-label="출발지와 도착지 지도" />
      )}
      <div className="flex flex-wrap gap-2 border-t border-gray-100 p-3">
        <a href={directionsUrl(origin)} target="_blank" rel="noopener noreferrer"
          className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-bold text-blue-800 hover:bg-blue-100">
          출발지 길찾기
        </a>
        <a href={directionsUrl(dest)} target="_blank" rel="noopener noreferrer"
          className="rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-bold text-red-700 hover:bg-red-100">
          도착지 길찾기
        </a>
        <span className="self-center text-xs text-gray-400">카카오맵에서 열려요</span>
      </div>
    </div>
  )
}

export default RouteMap
