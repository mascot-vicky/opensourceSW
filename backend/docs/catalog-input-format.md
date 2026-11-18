# 카페·메뉴 카탈로그 입력 형식

관련 작업: [Issue #22](https://github.com/seoJing/opensourceSW/issues/22) · 선행 조사: [Issue #21](https://github.com/seoJing/opensourceSW/issues/21)

백엔드가 검토된 JSON 파일을 읽기 위한 내부 형식입니다. 프론트 응답, DB 테이블, 설문·추천 점수 계약이 아닙니다. Issue #24는 이 입력을 시작 시 검증하여 PostgreSQL에 적재하고 DB에서 조회합니다. HTTP API는 별도 작업입니다.

PR #23은 로더·검증을 제공합니다. 아래의 시작 시 PostgreSQL 적재·조회는 PR #31까지 포함한 코드에서 적용되며, 로더만으로 DB 적재가 실행되지는 않습니다.

## 입력 구조

파일은 UTF-8 JSON으로 작성합니다. 최상위 객체 하나에 `schemaVersion`, `dataVersion`, `cafes`, `menus`를 넣습니다.

| 필드 | 규칙 |
| --- | --- |
| `schemaVersion` | JSON 정수 `1`. 다른 버전은 거부합니다. |
| `dataVersion` | 공백이 아닌 문자열. 자료를 교체할 때 변경하며 설문 버전과 구분합니다. DB 적재 시 최대 128자입니다. |
| `cafes` | 카페 객체 배열. 누락·null·빈 배열·null 원소를 거부합니다. |
| `menus` | 메뉴 객체 배열. 누락·null·빈 배열·null 원소를 거부합니다. |

### 카페

| 필드 | 타입·규칙 |
| --- | --- |
| `id` | 카탈로그 내 고유 문자열 ID |
| `name` | 공백이 아닌 상호 |
| `roadAddress` | 공백이 아닌 도로명 주소 |
| `location` | 미확인은 null 또는 생략. 값이 있으면 `latitude`, `longitude` 숫자가 모두 필요합니다. |
| `source` | 상호·주소 확인에 사용한 출처 객체 |

위도는 -90~90, 경도는 -180~180이며 NaN·무한대를 거부합니다. 카페 위치를 메뉴 출처나 맛·분위기의 근거로 사용하지 않습니다.

### 메뉴

| 필드 | 타입·규칙 |
| --- | --- |
| `id` | 카탈로그 내 고유 문자열 ID |
| `cafeId` | `cafes`에 존재하는 카페 ID |
| `name` | 공백이 아닌 메뉴명 |
| `description` | 문자열 또는 null. 생략 가능하며 원문의 미확인 상태를 유지합니다. |
| `priceKrw` | 0 이상의 JSON 정수 또는 null. 생략 가능. Java `Long` 범위를 초과하면 거부합니다. |
| `isDemo` | 필수 JSON boolean. 가상·시연용 자료는 true이며, 누락을 false로 대체하지 않습니다. |
| `availability` | `UNKNOWN`, `AVAILABLE`, `UNAVAILABLE`, `SEASON_ENDED` 중 하나 |
| `saleConfirmation` | 판매 확인 기록. 아래 상태별 규칙을 적용합니다. |
| `source` | 해당 메뉴의 이름·설명·표시 가격을 확인한 출처 객체 |

`UNKNOWN`에는 판매 확인 기록을 넣지 않습니다. `AVAILABLE`·`UNAVAILABLE`에는 `confirmedOn` 날짜와 공백이 아닌 `reference` 확인 근거가 필요합니다. `SEASON_ENDED`는 원자료에 명시된 시즌 종료를 기록하는 상태로, 별도 판매 확인 기록 없이도 보존합니다.

판매 상태는 기록 시점의 확인 결과이며 실시간 재고를 보장하지 않습니다. 로더는 상태에 따라 메뉴를 자동 삭제하지 않습니다. 추천 단계에서 판매 상태를 기준으로 후보를 선택해야 합니다.

가격 null은 미확인, 0은 명시적인 0원입니다. 빈 문자열이나 공백·탭·줄바꿈만 있는 문자열은 null로 바꾸지 않고 타입 오류로 거부합니다. 원자료의 `무료` 표시를 일괄 0원 상품으로 변환하지 않습니다. 가격은 옵션을 가산하지 않은 검토된 단일 표시 가격이며, 범위 가격·옵션별 가격은 v1에 포함하지 않습니다.

### 출처와 날짜

`source`에는 다음 세 필드를 넣습니다.

| 필드 | 규칙 |
| --- | --- |
| `url` | 호스트가 있는 절대 HTTP(S) URL. 사용자명·비밀번호가 들어간 URL은 거부합니다. 로더가 URL에 접속하지는 않습니다. |
| `externalId` | 공백이 아닌 원자료 식별자. 내부 ID와 별도로 보존합니다. |
| `observedOn` | 원자료를 조회한 날짜. 판매 확인일로 자동 복사하지 않습니다. |

날짜는 정확히 `yyyy-MM-dd` 문자열로 작성하고 실제 달력에 존재해야 합니다. 배열·숫자·잘못된 날짜를 허용하지 않습니다. 조회일과 판매 확인일은 미래 날짜가 될 수 없습니다. 기본 기준 시간대는 `Asia/Seoul`이며 테스트는 고정 `Clock`을 주입합니다. 조회일과 판매 확인일은 서로 다른 근거이므로 같을 필요가 없습니다.

출처 필드의 존재·형식 검사는 실제 점포 일치나 판매 확인의 진위를 입증하지 않습니다. 해당 사실은 입력 자료 검토 단계에서 확인합니다. v1은 URL로 추적 가능한 출처를 사용하며, 별도 오프라인 증빙 형식은 필요할 때 버전을 변경해 도입합니다.

## 식별자와 미확인 값

- 내부 ID는 1~64자의 영문·숫자·`-`·`_`를 사용하고 첫 글자는 영문 또는 숫자로 합니다. 카페와 메뉴는 각각 중복을 검사합니다.
- 메뉴 ID는 카페별이 아니라 메뉴 배열 전체에서 고유합니다. `cafeId`가 없는 카페를 참조하면 거부합니다.
- 이름·가격이 바뀌어도 같은 카페·메뉴의 내부 ID를 유지합니다. 로더가 ID를 자동 발급하거나 기존 버전과 대조하지는 않습니다.
- 설명·가격·위치를 모르더라도 정상 입력으로 읽을 수 있습니다. 빈 옵션 배열을 옵션 미지원으로 바꾸거나, 없는 재료·맛 점수를 생성하지 않습니다.
- 재료·옵션·사진·추천 가중치 필드는 v1에 없습니다. 추가 필드를 조용히 버리지 않고 오류로 알려 계약 변경 여부를 확인하도록 합니다.
- JSON 필드 이름 중복, 알 수 없는 필드, 문서 뒤에 붙은 다른 JSON, 문자열 숫자나 소수 가격 등 타입 자동 변환을 거부합니다.

## 예시

아래 매장·주소·메뉴·출처는 형식 설명을 위한 가상 자료입니다. 실제 수집 자료는 공개 테스트에 포함하지 않습니다.

```json
{
  "schemaVersion": 1,
  "dataVersion": "demo-2026-11-18.1",
  "cafes": [{
    "id": "cafe-demo-001",
    "name": "가상 시연 카페",
    "roadAddress": "시연 전용 주소",
    "location": null,
    "source": {
      "url": "https://example.invalid/cafe/demo-001",
      "externalId": "demo-001",
      "observedOn": "2026-11-18"
    }
  }],
  "menus": [{
    "id": "menu-demo-001",
    "cafeId": "cafe-demo-001",
    "name": "가상 시연 음료",
    "description": null,
    "priceKrw": null,
    "isDemo": true,
    "availability": "UNKNOWN",
    "saleConfirmation": null,
    "source": {
      "url": "https://example.invalid/cafe/demo-001/menu",
      "externalId": "external-menu-001",
      "observedOn": "2026-11-18"
    }
  }]
}
```

## 사용과 오류 처리

```java
Catalog catalog = new CatalogLoader().load(Path.of("/path/to/catalog.json"));
```

파일을 읽고 전체 검증이 끝난 뒤에만 `Catalog`를 반환합니다. 입력 스트림은 성공·실패 모두 닫으며 카페·메뉴 목록은 불변 복사본입니다. 모델을 직접 생성한 호출자는 `CatalogValidator.validate`를 거쳐야 합니다.

파일 읽기, JSON 파싱, 자료 검증에 실패하면 `CatalogLoadException`을 던집니다. `getFieldPath()`는 `$.menus[0].cafeId` 같은 오류 위치를 반환합니다. 파일 전체 오류는 `$`로 표시하며 읽기·파싱 실패의 원인 예외를 보존합니다. 임의 기본 자료나 빈 카탈로그로 대체하지 않습니다.

기존 HTTP용 `ObjectMapper`를 변경하지 않고 로더 전용 파서를 사용합니다. 파일 로더는 기존 Jackson을 사용하며, DB 마이그레이션은 Flyway를 사용합니다. 실제 운영 데이터 파일은 커밋하지 않습니다.

## 시작 시 PostgreSQL 적재 — Issue #24

| 설정 | 동작 |
| --- | --- |
| `app.catalog.enabled` | `true`이면 시작 시 파일을 적재합니다. 생략·`false`이면 파일을 읽지 않으며 기존 DB 조회는 유지합니다. |
| `app.catalog.path` | 적재 활성화 시 필수인 로컬 JSON 파일 경로입니다. URL·classpath 위치는 지원하지 않습니다. |

환경변수는 `APP_CATALOG_ENABLED`, `APP_CATALOG_PATH`입니다. Flyway가 테이블을 생성한 뒤 시작 실행기가 파일을 읽고 전체 검증을 통과한 자료를 한 트랜잭션에서 저장합니다. 완료 전에는 애플리케이션이 ready 상태가 되지 않습니다. `Catalog` 객체는 검증·적재 동안만 사용하며 싱글턴 Bean으로 보관하지 않습니다. 조회는 `CatalogRepository`가 SQL로 수행합니다.

같은 `dataVersion`·같은 파일 SHA-256은 중복 저장하지 않습니다. 같은 버전의 내용이 다르면 시작을 거부합니다. 공백·줄바꿈만 바꿔도 파일 해시가 달라지므로 이미 전달한 버전의 파일은 수정하지 않습니다. 새 버전은 전체 저장 성공 뒤 활성화하며, 실패하면 기존 활성 버전을 유지합니다. 이전 버전은 삭제하지 않습니다. 이전 파일을 다시 지정하면 해당 버전으로 전환할 수 있습니다.

```sh
java -jar backend/target/backend-0.1.0-SNAPSHOT.jar \
  --app.catalog.enabled=true \
  --app.catalog.path=/absolute/path/catalog.json
```

위 실행에는 PostgreSQL 연결 설정이 필요합니다. 적재 이후에는 `app.catalog.enabled=false`로 실행해도 DB에 저장된 자료를 조회할 수 있습니다. 파일을 교체하는 것만으로 실행 중인 DB를 변경하지 않습니다. 경로·파일·내용이 잘못되거나 DB 적재가 실패하면 앱 시작을 거부하며, 빈 데이터로 대체하지 않습니다.

테이블, 조회 SQL, PostgreSQL 테스트와 볼륨 보존 확인은 [DB 운영 안내](catalog-database.md)를 참고합니다. Docker의 읽기 전용 파일 연결은 Issue #29 범위입니다. 적재 성공은 원자료의 최신 판매 여부나 추천 적합성 확인을 뜻하지 않습니다.

## 검증

`backend/`에서 다음 명령을 실행합니다.

```sh
sh mvnw -B -ntp verify
```

카탈로그 테스트는 정상 로딩, 미확인 값, ID 중복·잘못된 참조, 가격·좌표 경계, 출처·판매 확인 모순, JSON 타입·날짜·필드 오류와 불변성을 확인합니다. 기존 Spring Context와 health 테스트도 함께 실행합니다. 외부 페이지 접근과 실제 자료의 최신성은 이 테스트 범위가 아닙니다.

DB 테스트는 중복 방지·버전 충돌·원자적 교체·SQL 실패 롤백·동시 적재·DB 직접 조회를 확인합니다. `-Ppostgres` 프로필은 같은 계약을 실제 PostgreSQL에서도 검증합니다. 실행 절차는 DB 운영 안내에 있습니다.
