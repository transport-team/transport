const KAKAO_MAP_SCRIPT_ID = 'kakao-map-sdk-script'

export const loadKakaoMapSdk = () => {
  const appKey = import.meta.env.VITE_KAKAO_MAP_KEY || import.meta.env.VITE_KAKAO_JS_KEY
  if (!appKey) {
    return Promise.reject(new Error('카카오맵 JavaScript 키가 설정되지 않았습니다.'))
  }

  if (window.kakao?.maps?.services) {
    return Promise.resolve(window.kakao.maps)
  }

  return new Promise((resolve, reject) => {
    const existing = document.getElementById(KAKAO_MAP_SCRIPT_ID)
    const onReady = () => {
      if (!window.kakao?.maps) {
        reject(new Error('카카오맵 SDK를 초기화할 수 없습니다.'))
        return
      }
      window.kakao.maps.load(() => {
        resolve(window.kakao.maps)
      })
    }

    if (existing) {
      existing.addEventListener('load', onReady, { once: true })
      existing.addEventListener('error', () => reject(new Error('카카오맵 SDK 로드 실패')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.id = KAKAO_MAP_SCRIPT_ID
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`
    script.async = true
    script.onload = onReady
    script.onerror = () => reject(new Error('카카오맵 SDK 로드 실패'))
    document.head.appendChild(script)
  })
}