# Docker Compose에서 외부 카탈로그 사용

관련 작업: [Issue #29](https://github.com/seoJing/opensourceSW/issues/29) · 선행 코드: [PR #23](https://github.com/seoJing/opensourceSW/pull/23), [Issue #24](https://github.com/seoJing/opensourceSW/issues/24)

기본 `docker compose up`은 파일 적재 없이 기존 세 서비스를 실행한다. #24의 DB 구현이 포함되면 이미 적재된 카탈로그는 DB에서 계속 조회할 수 있다. 실제 카탈로그가 필요한 시연에서만 저장소 루트의 `docker-compose.yml`에 `backend/compose.catalog.yml`을 더한다. 확장 파일은 서비스 수를 늘리지 않고 백엔드에 외부 JSON을 **읽기 전용**으로 연결한다. 확장 파일은 적재 원본을 공급하고, 실제 PostgreSQL 저장은 #24의 시작 실행기가 담당한다. HTTP API는 별도 작업이다.

실행자는 자신의 PC에서 파일 경로를 지정한다. 특정 사용자 계정, 공유 서비스나 원격 PC 접근을 전제로 하지 않는다. 이 문서의 목적은 다른 사용자가 실행할 수 있는 설정·절차를 제공하는 것이다.

## 준비

1. Docker Engine과 Compose v2 또는 Docker Desktop을 준비하고 `docker compose version`, `docker info`로 확인한다. 저장소 루트에서 실행하며 3000·8080 포트를 사용할 수 있어야 한다. 호스트 Java·Maven 설치는 필요하지 않다.
2. 버전별 묶음을 [전달 절차 PR #28](https://github.com/seoJing/opensourceSW/pull/28)에 따라 풀고 SHA-256, `dataVersion`, 카페·메뉴 수를 확인한다. 전달 매체는 실행자가 선택한다.
3. `catalog.json`의 절대 경로를 확인한다. 파일은 백엔드 컨테이너의 비루트 사용자도 읽을 수 있어야 한다. 파일·개인 경로는 Git에 커밋하지 않는다.
4. PR #23의 로더와 Issue #24의 PostgreSQL 적재 코드가 실행할 브랜치에 있어야 한다. 둘 중 하나가 없으면 카탈로그 기능을 확인할 수 없다.

WSL·Linux·macOS 셸에서는 저장소 루트에서 실행한다. 예시 절대 경로를 자신의 파일 위치로 바꾸고, 공백이 있는 경로도 따옴표 안에 넣는다.

```sh
export CATALOG_FILE="/absolute/path/haenggung-2026-11-18.2/catalog.json"
test -f "$CATALOG_FILE" && test -r "$CATALOG_FILE" &&
  docker compose -f docker-compose.yml -f backend/compose.catalog.yml config --quiet &&
  docker compose -f docker-compose.yml -f backend/compose.catalog.yml up --build -d
```

Windows PowerShell에서는 같은 저장소 루트에서 실행한다.

```powershell
$catalogInput = Get-Item -LiteralPath 'C:\path\to\haenggung-2026-11-18.2\catalog.json' -ErrorAction Stop
if ($catalogInput.PSIsContainer) { throw 'catalog.json 파일을 지정하세요.' }
$env:CATALOG_FILE = $catalogInput.FullName
docker compose -f docker-compose.yml -f backend/compose.catalog.yml config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose 설정 검사가 실패했습니다.' }
docker compose -f docker-compose.yml -f backend/compose.catalog.yml up --build -d
```

`CATALOG_FILE`을 설정하지 않으면 확장 Compose 설정 자체가 실패한다. 파일이 없거나 JSON이 잘못되면 백엔드가 시작에 실패해야 한다. Compose의 파일 경로는 **호스트 경로**이며, 애플리케이션의 `APP_CATALOG_PATH=/app/data/catalog.json`은 **컨테이너 내부 경로**다. 두 값을 바꿔 쓰지 않는다.

WSL에서 실행할 때는 WSL 경로를 사용하고 Docker Desktop의 해당 배포판 통합을 활성화한다. Windows PowerShell에서는 Windows 경로를 사용한다. Docker Desktop의 파일 공유·읽기 권한도 해당 경로를 허용해야 한다.

## 실행 확인과 종료

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml ps
docker compose -f docker-compose.yml -f backend/compose.catalog.yml logs backend
curl -fsS http://localhost:3000/api/health
```

Windows PowerShell의 health 조회는 `Invoke-RestMethod http://localhost:3000/api/health`를 사용한다.

세 서비스가 healthy이고 백엔드 로그에 `Catalog stored in database: cafes=320, menus=5011, inserted=true`이 있어야 최초 적재를 확인할 수 있다. 같은 버전을 다시 지정하면 `inserted=false`이며 중복 저장하지 않는다. DB의 활성 버전과 실제 행 수도 함께 확인한다. `GET /api/health`는 프론트에서 백엔드·DB 연결을 확인한다. 현재 카탈로그 조회 HTTP API와 프론트 `/api/v1` 프록시는 별도 작업이므로 health 성공만으로 메뉴 화면 연동을 주장하지 않는다.

로그의 건수는 전달받은 `manifest.json`과 대조한다. 위의 320곳·5,011건은 `haenggung-2026-11-18.2`의 예시이며 코드에 고정된 수량이 아니다. `postgres_data` 볼륨(실제 이름 `suwon_trip_postgres_data`)은 #24가 적재한 카페·메뉴와 데이터 버전까지 보존한다. 같은 버전의 다른 파일 내용은 거부하므로, JSON을 수정하면 새 `dataVersion`을 사용해야 한다.

새 버전은 별도 폴더에 검증한 뒤 `CATALOG_FILE`을 새 파일로 바꾸고 아래 명령으로 백엔드를 다시 생성한다. 단순 `restart`는 변경된 호스트 파일 경로를 적용하지 않는다. 이전 버전으로 되돌릴 때도 경로를 바꾼 뒤 같은 명령을 사용한다. 적재 로그·DB 활성 버전·health를 다시 확인한다. 이전 버전의 행은 보존하며 원본을 다시 지정하면 활성 버전만 전환한다.

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml up -d --no-deps --force-recreate backend
```

적재한 뒤 원본 파일 없이 DB만 사용하는 실행은 아래와 같다. 기본 Compose로 다시 생성하면 파일 마운트와 적재 환경변수가 제거된다.

```sh
docker compose -f docker-compose.yml up -d --force-recreate --wait
docker compose exec -T db psql -U suwon_app -d suwon_trip -c "SELECT data_version, cafe_count, menu_count FROM catalog_dataset JOIN catalog_active USING (data_version);"
docker compose exec -T db psql -U suwon_app -d suwon_trip -c "SELECT COUNT(*) FROM catalog_cafe JOIN catalog_active USING (data_version); SELECT COUNT(*) FROM catalog_menu JOIN catalog_active USING (data_version);"
```

DB 사용자·이름을 바꿨다면 `-U`, `-d`도 바꾼다. 다른 PC의 Docker 볼륨은 별개이므로 최초에는 원본 파일로 적재해야 한다.

종료는 `CATALOG_FILE`이 설정된 셸에서 실행한다.

```sh
docker compose -f docker-compose.yml -f backend/compose.catalog.yml down
```

`down`은 컨테이너를 중지·제거하며 named volume은 유지한다. `down -v`는 사용하지 않는다.

## 확인 범위

2026-11-18 로컬 Docker에서 #24의 PostgreSQL 구현과 이 확장 파일을 함께 실행했다. 이미지 빌드, 카페 320곳·메뉴 5,011건 적재, 원본과 DB의 모든 필드 대조, 세 서비스 health와 프론트 `/api/health`를 확인했다. 이후 기본 Compose만으로 세 컨테이너를 재생성하여 JSON 마운트·적재 설정이 없는 상태에서도 같은 DB 볼륨과 전체 데이터가 유지되는 것을 확인했다.

일반 경로·공백이 있는 경로의 설정 해석과 필수 변수 누락 오류도 확인했다. 다른 PC 자체를 직접 검증한 것은 아니며, CI의 기본 Compose smoke test에는 실제 외부 자료와 이 확장 파일이 포함되지 않는다. 특정 PC 접근이나 전달 장소 확정을 이번 구성 작업의 완료 조건으로 두지 않는다.
