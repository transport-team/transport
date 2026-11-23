import { useEffect, useState } from 'react'
import { getShipmentImages } from '../../api/shipmentApi'
import { API_SERVER_HOST } from '../../api/apiClient'

const IMAGE_TYPE = {
  PICKUP: '상차 사진',
  RECEIPT: '배송 완료 · 인수증 사진',
  ETC: '기타 사진',
}

const ShipmentPhotoGallery = ({ shipmentId, refreshKey = 0 }) => {
  const [state, setState] = useState({ shipmentId: null, images: [], error: '' })
  const loading = String(state.shipmentId) !== String(shipmentId)

  useEffect(() => {
    let active = true
    getShipmentImages(shipmentId)
      .then((images) => {
        if (active) setState({ shipmentId, images: Array.isArray(images) ? images : [], error: '' })
      })
      .catch((error) => {
        if (active) setState({ shipmentId, images: [], error: error.response?.data?.message || '배송 사진을 불러오지 못했습니다.' })
      })
    return () => { active = false }
  }, [shipmentId, refreshKey])

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
      <h2 className="text-sm font-bold text-emerald-700">상차 · 배송 증빙 사진</h2>
      <p className="mt-2 text-xs text-gray-500">기사가 업로드한 사진을 확인할 수 있습니다.</p>
      {loading ? (
        <p className="mt-4 text-sm text-gray-500">사진을 불러오는 중입니다...</p>
      ) : state.error ? (
        <p className="mt-4 text-sm text-red-600" role="alert">{state.error}</p>
      ) : state.images.length === 0 ? (
        <p className="mt-4 text-sm text-gray-500">아직 업로드된 사진이 없습니다.</p>
      ) : (
        <div className="mt-4 space-y-5">
          {Object.entries(IMAGE_TYPE).map(([type, title]) => {
            const images = state.images.filter((item) => item.type === type)
            if (!images.length) return null
            return (
              <div key={type}>
                <h3 className="mb-2 text-xs font-semibold text-gray-700">{title} ({images.length}장)</h3>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                  {images.map((item) => (
                    <a key={item.id} href={`${API_SERVER_HOST}${item.url}`} target="_blank" rel="noopener noreferrer"
                      className="overflow-hidden rounded-xl border border-gray-200 hover:border-emerald-400"
                      title={`${title} 원본 보기`}>
                      <img src={`${API_SERVER_HOST}${item.url}`} alt={`${title} ${item.id}`}
                        loading="lazy" className="h-32 w-full object-cover" />
                    </a>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default ShipmentPhotoGallery
