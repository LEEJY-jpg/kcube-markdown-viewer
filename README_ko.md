# KCube Markdown Viewer

[English](README.md) | **한국어**

Eclipse에서 Markdown(`.md`, `.markdown`) 파일을 **Source / Preview** 탭으로 편집하고 바로 미리 볼 수 있는 플러그인입니다.

## 주요 기능

- **Source / Preview / Split 탭**: Source 탭에서 Eclipse 텍스트 에디터로 편집하고, Preview 탭에서 렌더링된 결과를 보며, Split 탭에서는 둘을 나란히 봅니다. 파일을 처음 열면 Preview 탭이 먼저 표시됩니다.
- **Source 탭 문법 색**: 헤딩, 목록, 인용, 코드, 링크, 강조에 색을 입히며 다크 테마용 색을 따로 씁니다.
- **스크롤 동기화**: 탭을 바꿔도 보던 위치가 이어지고, Split 탭과 Markdown Preview 뷰는 Source 스크롤을 따라갑니다.
- **실시간 Markdown Preview 뷰**: Window > Show View > Other… > Markdown > *Markdown Preview* 에서 편집 내용을 입력 즉시 옆 뷰로 확인할 수 있습니다.
- **Outline**: Outline 뷰에 헤딩 목차가 나오고, 누르면 Source 나 Preview 에서 그 위치로 이동합니다.
- **Preview 검색**: Ctrl/Cmd+F 로 검색바를 열어 대소문자 구분 없이 검색합니다. 한글을 지원하며 Enter / Shift+Enter 로 이동합니다.
- **Source와 같은 글꼴·테마**: Eclipse 텍스트 글꼴을 Preview에 그대로 적용하고, 에디터 배경이 어두우면 다크 테마로 바뀝니다.
- **링크**: 웹 링크는 시스템 브라우저로, 다른 파일 링크는 Eclipse 에디터로 엽니다 (`#L10` 은 해당 줄로 이동).
- **GitHub 확장 문법**: 표, 취소선, 작업 목록, 알림 상자(`> [!NOTE]`), 각주.
- **코드 하이라이트**: 코드 블록 구문 강조, 복사 버튼, 접기/펼치기를 제공합니다. 인라인 코드를 클릭하면 복사됩니다. ```` ```mermaid ```` 블록은 다이어그램으로 그립니다.
- **수식**: `$...$`, `$$...$$`, ```` ```math ```` 블록을 KaTeX 로 그립니다.
- **이미지 지원**: md 파일 기준 상대 경로 이미지를 표시합니다.
- **완전 오프라인**: markdown-it, highlight.js, Mermaid, KaTeX, CSS를 모두 플러그인에 포함하며 CDN을 사용하지 않습니다.

## 요구 사항

- Eclipse 4.33(2024-09) 이상
- Java 17 이상 (빌드 및 실행)

## 설치

jar 한 개를 Eclipse `dropins` 폴더에 복사하고 `-clean` 으로 재시작하면 됩니다. 자세한 절차와 문제 해결은 [INSTALL_ko.md](docs/INSTALL_ko.md) 의 *일반 사용자 설치 가이드*를 참고하세요.

## 빌드

JDK 17 과 Maven 이 필요합니다. Maven Tycho 로 빌드합니다.

```bash
./build.sh             # dist/ 에 플러그인 jar 와 update-site zip 생성
./build.sh --install   # 빌드 후 Eclipse dropins 에 설치
```

프로젝트 구조와 설계는 [DEVELOPMENT_ko.md](docs/DEVELOPMENT_ko.md) 를 참고하세요.

```
com.kcube.md              플러그인 (에디터, 뷰, web/ 뷰어)
com.kcube.md.feature      Eclipse feature
com.kcube.md.update-site  p2 업데이트 사이트
```

## 서드파티 구성요소

| 구성요소 | 라이선스 |
|---|---|
| [markdown-it](https://github.com/markdown-it/markdown-it) 13.0.1 | MIT |
| [highlight.js](https://highlightjs.org/) 11.8.0 | BSD-3-Clause |
| [Bootstrap](https://getbootstrap.com/) CSS | MIT |
| [Mermaid](https://mermaid.js.org/) 11.4.1 | MIT |
| [KaTeX](https://katex.org/) 0.16.11 | MIT |

자세한 내용은 [THIRD_PARTY_NOTICES.txt](com.kcube.md/web/THIRD_PARTY_NOTICES.txt) 를 참고하세요.

## 라이선스

[Apache License 2.0](LICENSE)
