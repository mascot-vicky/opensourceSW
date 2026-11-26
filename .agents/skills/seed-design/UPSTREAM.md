# SEED Design 공식 스킬 출처

- 원본: https://github.com/daangn/seed-design/tree/a477579ef21a9c0aef97bbd1b956d057a93fac9f/skills/seed-design
- 고정 커밋: `a477579ef21a9c0aef97bbd1b956d057a93fac9f` (`dev`에서 확인, 2026-11-19)
- 설치: Codex `skill-installer`의 `install-skill-from-github.py`에 `--repo daangn/seed-design --path skills/seed-design --ref <위 커밋> --dest .agents/skills` 지정
- 라이선스: 원본 저장소의 Apache-2.0 `LICENSE`와 `NOTICE`를 같은 커밋에서 복사했습니다.
- 로컬 변경: 원본 스킬·references·rules·assets는 수정하지 않았습니다. 출처 기록인 이 파일과 LICENSE·NOTICE만 추가했습니다.

이 스킬은 소비자 React 앱인 `frontend/`에 적용합니다. `SKILL.md`의 SEED 저장소 전용 라우팅에 있는 형제 스킬은 SEED 자체 개발용이며 이 저장소에는 설치하지 않았습니다. 해당 조건(`@seed-design/project`)은 여기서 성립하지 않습니다. 공식 링크는 이식 과정에서 변경하지 않았습니다.

Codex에서 저장소를 다시 읽는 다음 턴부터 사용할 수 있습니다. 자동 검색을 지원하지 않는 도구에서는 `frontend/AGENTS.md`의 링크로 읽습니다. 개인 전역 스킬 디렉터리를 변경하지 않았습니다.

## 갱신

1. 공식 upstream의 변경과 references/rules/assets 차이를 검토하고 새 커밋을 정합니다.
2. installer를 임시 목적지에 실행해 전체 스킬을 내려받고 기존 파일과 비교합니다. 기존 목적지는 installer가 덮어쓰지 않습니다.
3. 승인된 Issue·브랜치에서 스킬 파일과 LICENSE·NOTICE를 함께 갱신하고 이 문서의 커밋·날짜·로컬 변경 내역을 고칩니다.
4. Repository Guard와 하네스 테스트를 실행하고 PR 리뷰를 받습니다. SEED npm 패키지 갱신은 별도로 호환성을 검증합니다.
