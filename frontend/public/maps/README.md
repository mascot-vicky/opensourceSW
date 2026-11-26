# 행궁동 지도 이미지

`haenggung.svg`는 런타임 타일 요청 없이 사용하는 로컬 지도 자산입니다. Leaflet ImageOverlay로 같은 지리적 범위에 표시하고 카페 핀을 별도 레이어로 올립니다.

- 원자료: [OpenStreetMap](https://www.openstreetmap.org/)
- 취득: 2026-11-19, [공식 map API의 bbox 추출](https://www.openstreetmap.org/api/0.6/map?bbox=127.002,37.277,127.026,37.298)
- 범위: 서쪽 127.002°, 남쪽 37.277°, 동쪽 127.026°, 북쪽 37.298°. 행궁동 관광권의 시연 범위이며 행정 경계 도형은 아닙니다.
- 투영: EPSG:3857(Web Mercator). 경도는 radian, 위도는 `log(tan(pi/4 + latitude*pi/360))`로 변환한 뒤 동일 축척으로 SVG 좌표에 배치했습니다. `viewBox` 너비 2000과 높이는 이 범위의 투영 비율을 유지합니다.
- 원본 추출 SHA-256: `85b0e2352db893dc9c271198ce787287e2c4b21e3cd1927b364746ce84afdd7d`
- 렌더링: 원본의 건물 5,726개·도로 구간 1,414개와 공원·수로·성곽을 단순화한 벡터 이미지입니다. 지명 라벨은 가독성을 위해 대표 위치에 배치했으며 길찾기나 경계 판정 자료가 아닙니다.
- 저작권: **© OpenStreetMap contributors**. 원본 지도 데이터는 [Open Database License(ODbL)](https://opendatacommons.org/licenses/odbl/1-0/)입니다. [OSM 저작권 안내](https://www.openstreetmap.org/copyright), [Produced Work 출처 표시 기준](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines)을 따릅니다. 이 SVG는 해당 데이터로 만든 지도 이미지(Produced Work)입니다.

화면에는 확대·축소·메뉴 탐색 상태에서도 출처 링크를 유지합니다. SVG 자체에도 출처·투영·범위·날짜를 포함했습니다. 지도 갱신 시 원자료의 이용 조건·출처를 확인하고 동일 투영·범위로 다시 제작하거나 `demoMap.bounds`를 함께 맞춰야 합니다. 원본 추출 파일·다운로드 캐시·실행 로그는 저장소에 넣지 않습니다.

가상 카페의 좌표는 실제 업체의 위치를 뜻하지 않습니다. 자체 카페·메뉴 mock은 OSM POI 데이터셋에 병합하지 않으며 이 지도 위에 별도 표시합니다. 브라우저에서 OSM 타일·검색 API에 접속하지 않습니다.
