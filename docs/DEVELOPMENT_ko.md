# KCube Markdown Viewer — 요건 및 개발 규칙

[English](DEVELOPMENT.md) | **한국어**

> 이클립스용 Markdown 뷰어/에디터 플러그인
> 작성일: 2026-10-08 · 상태: 초안

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 목적 | 이클립스에서 Markdown 파일을 편집하고 렌더링 결과를 미리보기 |
| 형태 | Eclipse 플러그인 (PDE, Plug-in Project) |
| 대상 파일 | `*.md`, `*.markdown` |
| 기반 자산 | 기존에 보유한 **JavaScript 기반 Markdown 뷰어**를 재사용 |

## 2. 명명 규칙

| 구분 | 값 |
|---|---|
| 프로젝트/저장소명 | `kcube-markdown-viewer` |
| 접두어 | `kcube` (필수) |
| 번들 ID (Bundle-SymbolicName) | `com.kcube.md` |
| 표시 이름 (Bundle-Name) | `KCube Markdown Viewer` |
| 루트 패키지 | `com.kcube.md` |
| 에디터 ID | `com.kcube.md.editor` |
| 미리보기 뷰 ID | `com.kcube.md.preview` |
| 마켓플레이스 표기 | `KCube Markdown Viewer for Eclipse` |

- 번들 ID·패키지명은 **소문자, 하이픈 없이**, 역도메인 형식 사용
- 이름 앞에 `Eclipse`를 붙이지 않음 (Eclipse 재단 상표 가이드) → `... for Eclipse` 형태 사용
- 기능 분리 시 접미어로 구분: `com.kcube.md.core`, `com.kcube.md.ui`

## 3. 기능 요건

### 3.1 멀티탭 에디터 (주 기능)
- `.md` 파일을 열면 기본 에디터로 동작 (`default="true"`)
- **Source 탭**: 이클립스 기본 `TextEditor` 내장 (`MarkdownTextEditor`, 오른쪽에 숨긴 옆 패널을 둔 하위 클래스)
  - 저장, 다른 이름으로 저장, Undo/Redo, 찾기, dirty(*) 표시 지원
- **Preview 탭**: SWT `Browser`에 JS 뷰어 로드
  - Preview 탭으로 전환될 때 최신 내용으로 렌더링
  - 파일을 처음 열면 Preview 탭을 먼저 표시 (탭 순서는 Source, Preview, Split 으로 유지)
- **Split 탭**: 왼쪽 Source, 오른쪽 실시간 미리보기 (반반, 경계선 드래그로 조절)
  - 새 컨트롤을 만들지 않고 Source 탭의 에디터 컨트롤을 공유한다. 탭 항목 데이터에 텍스트 에디터를 넣어 Split 탭에서도 편집 단축키·전역 액션이 Source 탭과 똑같이 동작한다.
  - 오른쪽 브라우저는 처음 Split 탭을 열 때 만들고, Split 탭에 있는 동안만 문서·스크롤을 추적한다 (`LivePreview`, 3.2 뷰와 같은 300ms 디바운스·스크롤 동기화).
- 탭 전환 시 Undo/Copy/Find 등 전역 액션이 활성 탭에 맞게 연결/해제될 것

### 3.2 Markdown Preview 뷰 (보조 기능)
- 별도 뷰(`ViewPart`)로 에디터 옆에 띄워 **실시간 미리보기**
- 활성 에디터 추적 (`IPartListener2`), `.md` 파일만 대상
- 문서 변경 감지 (`IDocumentListener`) 후 **300ms 디바운스**로 갱신

### 3.3 렌더링 방식
- Java에서 Markdown을 직접 변환하지 않고 **JS 뷰어에 텍스트만 전달**
- `browser.execute("renderMarkdown(...)")` 호출
- 페이지 로딩 완료(`ProgressListener.completed`) 전에는 호출하지 않음
- JS 쪽은 본문만 교체하고 **스크롤 위치 유지**
- 문법: markdown-it 기본(CommonMark + 표, 취소선, 자동 링크 `linkify`)에 GitHub 확장을 `js/gfm.js` 플러그인으로 더한다 (외부 플러그인 없이 직접 구현).
  - 작업 목록: `- [ ]` / `- [x]` → 읽기 전용 체크박스 (`li.task-list-item`, 목록에 `contains-task-list`).
  - 알림 상자: 인용 첫 줄이 `[!NOTE]`/`[!TIP]`/`[!IMPORTANT]`/`[!WARNING]`/`[!CAUTION]` 이면 그 줄을 지우고 제목을 넣은 `blockquote.markdown-alert-*`.
  - 각주: `[^이름]` 참조와 `[^이름]: 내용` 정의 (4칸 들여쓴 다음 줄까지). 정의는 문서 끝 `section.footnotes` 로 옮겨 참조 순서로 번호를 매기고, 참조되지 않은 정의는 버린다. `#fn-N` / `#fnref-N` 링크로 오간다.
- 수식: `js/math.js` 플러그인이 GitHub 문법을 KaTeX 자리 표시 요소(`.kc-math`, TeX 원문은 `data-tex`)로 바꾸고, `renderMath()` 가 KaTeX 로 채운다.
  - 인라인 `$…$` (여는 `$` 뒤·닫는 `$` 앞 공백 금지, 닫는 `$` 뒤 숫자 금지 → `$5 and $10` 은 수식 아님), `` $`…`$ ``, 문단 안 `$$…$$`.
  - 블록 `$$` … `$$` (한 줄 `$$x$$` 포함, 닫는 줄이 없으면 수식 아님)와 ```` ```math ```` 코드 블록.
  - `katex/katex.min.js`·`katex.min.css`·woff2 글꼴(약 590KB)은 수식이 처음 나올 때만 불러온다. 불러온 뒤에는 동기로 채워 스크롤 복원 전에 높이가 확정된다. 오류는 KaTeX 가 그 수식만 빨갛게 표시한다(`throwOnError: false`).
  - `applyFont` 의 글꼴 강제 규칙은 `.katex` 안을 제외하고, 검색(`search.js`)도 수식 안은 건너뛴다.
- 목록 글머리: `MarkDownViewer.css` 가 모든 `li` 를 `decimal` 로 만들어 `ul` 에도 번호가 붙던 것을 `viewer.css` 에서 `ul > li { list-style-type: inherit }` 로 바로잡았다.

### 3.4 Preview 검색
- Ctrl/Cmd+F 로 Preview 우측 상단에 검색바를 연다 (**대소문자 구분 없음**, 한글 지원).
- Enter / F3 은 다음, Shift+Enter 는 이전 일치로 이동(끝에서 순환), Esc 또는 ✕ 버튼은 검색바를 닫고 강조를 제거한다. `n/m` 카운터를 표시한다.
- JS(`web/js/search.js`)로 구현: 텍스트 노드의 일치 부분을 `<mark class="kc-hit">`로 감싸고, 현재 항목은 `kc-hit-active` 로 구분한다.
- 검색 제외: 코드 블록 헤더(언어 라벨, 복사 버튼), 접힌(숨겨진) 코드
- 한글(IME) 조합 중에는 검색하지 않고, 조합이 끝나면 검색한다.
- 검색바가 열린 상태에서 재렌더링되면(소스 편집 시) 검색을 다시 적용하고 현재 위치를 유지한다 (`reapplySearch()`).
- Preview 탭이 활성일 때 Eclipse **Find** 전역 액션이 이 검색바 열기로 연결된다 (`MarkdownEditorContributor`, `MarkdownMultiPageEditor.openPreviewSearch()`). 검색어의 정규식 특수문자는 일반 문자로 처리한다.

### 3.5 Preview 글꼴
- Preview 는 Source 탭과 같은 글꼴, 즉 Eclipse 텍스트 글꼴(`JFaceResources.TEXT_FONT`)을 사용한다.
- Java 가 렌더링 전마다 `applyFont(family, sizePx)` 로 글꼴 이름과 크기(화면 DPI 로 pt → CSS px 환산)를 전달한다.
- 제목은 본문 기준 상대 크기(h1 1.6em, h2 1.4em, h3 1.25em, h4 1.1em), 코드 블록·인라인 코드는 본문과 같은 크기를 쓴다.
- Preferences 에서 텍스트 글꼴을 바꾸면 Preview 에도 즉시 반영된다.

### 3.6 Preview 링크
- Preview 의 링크 클릭은 브라우저 이동이 아니라 `viewer.js` 의 `onLinkClick` 에서 처리한다.
- `#앵커` 링크는 문서 안에서 스크롤한다. 해당 `id` 요소가 없으면 GitHub 방식 slug 가 같은 헤딩을 찾는다 (중복은 `-1`, `-2` ...).
- 그 밖의 링크는 md 파일 폴더 기준 절대 URL 로 바꿔 `BrowserFunction` `kcOpenLink(url)` 로 Java 에 넘기고, `LinkOpener` 가 연다.
  - `http(s)`/`mailto` → 시스템 브라우저/메일 프로그램.
  - `file:` → Eclipse 에디터 (워크스페이스 안이면 `IFile`, 밖이면 파일 스토어). `#L10` / `#L10-L20` / `#10` 은 텍스트 에디터에서 해당 줄을 선택한다.
  - 파일이 없으면 안내 대화상자를 띄운다.

### 3.7 Preview 테마
- Source 탭(텍스트 에디터) 배경색을 따라 밝은/어두운 테마를 자동으로 고른다. 배경 밝기가 128 미만이면 다크 테마.
  - 배경색: `EditorsUI` 설정의 `AbstractTextEditor.Color.Background` (Eclipse 다크 테마가 바꾸는 값). "시스템 기본색" 이면 `SWT.COLOR_LIST_BACKGROUND`.
  - 설정이 바뀌면 (테마 전환 포함) 즉시 다시 적용한다.
- 다크 테마는 `applyTheme(true, 배경색)` 이 `<html>` 에 `kc-dark` 클래스를 붙이고, 본문 배경을 에디터 배경색에 맞춘다. 스타일은 `viewer.css` 의 `html.kc-dark` 규칙 (코드 하이라이트 색 포함).
- mermaid 는 본문 배경 밝기로 `dark`/`default` 테마를 고르므로 테마가 바뀌면 다시 렌더링한다.

### 3.8 Outline (목차)
- 에디터가 `IContentOutlinePage` 어댑터로 `MarkdownOutlinePage` 를 제공해 Outline 뷰에 헤딩 트리를 보여 준다.
- 헤딩은 Java `HeadingParser` 가 찾는다 (ATX `#`, Setext `===`/`---`, 펜스 코드 블록 안은 제외). 문서가 바뀌면 500ms 뒤 갱신한다.
- 헤딩을 고르면 Source 탭에서는 그 줄을 선택하고, Preview 탭에서는 그 헤딩으로 스크롤한다.
  - Preview 는 블록 요소에 붙은 원본 줄 번호(`data-line`, 0 부터, 3.9 참고)를 보고 `scrollToLine(line)` 이 그 줄로 스크롤한다.
  - `HeadingParser` 의 줄 번호는 markdown-it 토큰 `map[0]` 과 같은 기준이어야 한다.

### 3.9 스크롤 동기화 (Source ↔ Preview)
- markdown-it core 규칙이 블록 요소(헤딩, 문단, 목록/항목, 인용, 표/행, 구분선)에 `data-line` = 토큰 `map[0]` 을 붙인다. `fence`/`code_block` 과 원시 HTML 문단은 바깥 태그에 직접 넣는다.
- `scrollToLine(line)` 과 `getTopLine()` 은 이웃한 `data-line` 블록 사이를 보간해 원본 줄과 스크롤 위치를 서로 바꾼다 (소수 줄 허용, 서로 역함수).
- 에디터 탭을 바꾸면 위치를 이어 준다. Source → Preview 는 Source 맨 위 줄로 스크롤하고, Preview → Source 는 Preview 맨 위 줄을 Source 맨 위로 올린다 (`MarkdownMultiPageEditor.pageChange`, `SourceScroll`).
- Split 탭과 Source 탭은 같은 에디터라 위치가 그대로이고, Preview ↔ Split 전환은 Preview ↔ Source 와 같다.
- *Markdown Preview* 뷰와 Split 탭 미리보기는 추적 중인 에디터의 Source 스크롤을 따라간다 (`IViewportListener`, 30ms 로 묶음). 다시 렌더링한 뒤에도 맞춘다. Preview 를 스크롤해도 Source 는 움직이지 않는다.
- 스크롤 뒤에 로딩이 끝나는 이미지·mermaid 다이어그램 때문에 위치가 조금 밀릴 수 있다.

### 3.10 Source 문법 색
- `MarkdownTextEditor` 가 기본 텍스트 에디터 설정(`TextSourceViewerConfiguration`: 맞춤법 검사, 하이퍼링크 등)에 문법 색만 더한 `MarkdownSourceViewerConfiguration` 을 쓴다 (`source/` 패키지).
- 여러 줄 문법은 전용 파티셔닝(`__kcube_markdown_partitioning`)으로 나눈다: 펜스 코드 블록(<code>```</code>/<code>~~~</code>, 닫는 줄이 없으면 문서 끝까지)과 HTML 주석. 파티셔너는 `doSetInput` 에서 문서에 한 번만 붙인다.
- 본문 파티션은 `MarkdownScanner` 가 칠한다. 규칙 순서가 우선순위다.
  - 줄머리: ATX 헤딩(줄 전체), 구분선·Setext 밑줄, 인용(줄 전체), 목록 기호(기호만).
  - 인라인 (같은 줄 안에서 닫혀야 함, 여는 기호 뒤·닫는 기호 앞이 공백이면 무시): 역슬래시 이스케이프, `코드`, 링크·이미지·참조, `<http…>`, `**굵게**`/`__굵게__`, `~~취소선~~`, `*기울임*`. `_기울임_` 은 snake_case 오인 때문에 잡지 않는다.
- 색은 에디터 배경(3.7 과 같은 기준, `EditorTheme`)에 따라 밝은/어두운 두 벌 중 고른다. 배경 설정이 바뀌면 같은 토큰의 색만 바꾸고 다시 칠한다 (`handlePreferenceStoreChanged`).

### 3.11 향후 검토 항목
- [x] Source | Preview 좌우 분할 탭 — 완료 (3.1 Split 탭)
- [x] 수식 (KaTeX) — 완료 (3.3)
- [x] GFM 확장 (작업 목록, 알림 상자, 각주) — 완료 (3.3)
- [x] Source 탭 Markdown 문법 하이라이팅 — 완료 (3.10)
- [x] 목차 (Outline 뷰 연동) — 완료 (3.8)
- [x] 에디터 ↔ 미리보기 스크롤 동기화 — 완료 (3.9)
- [x] 코드 하이라이팅 (highlight.js) — 완료
- [x] Preview 검색 — 완료 (3.4)
- [x] 다크 테마 — 완료 (3.7, Eclipse 에디터 배경색 기준)
- [x] 미리보기 링크 클릭 시 이클립스에서 파일 열기 — 완료 (3.6)

## 4. 아키텍처

### 4.1 구성

업데이트 사이트 배포를 위해 **Maven Tycho 멀티 모듈** 구조로 구성한다. 모듈 배치는 `kcube-maven-goals-plugin` 과 동일한 **루트 직하 평면 구조**를 따른다. (상세는 8장 참고)

```
kcube-markdown-viewer/
├── pom.xml                                 # 부모 POM (Tycho 설정, p2 저장소, 모듈 목록)
├── .gitignore
├── com.kcube.md/                           # 플러그인 (실제 코드)
│   ├── META-INF/MANIFEST.MF
│   ├── plugin.xml
│   ├── build.properties                    # web/, icons/ 포함 필수
│   ├── pom.xml                             # eclipse-plugin
│   ├── icons/md.png
│   ├── web/                                # 기존 JS 뷰어 (오프라인 동작)
│   │   ├── viewer.html
│   │   ├── css/                            # bootstrap, highlight 테마, MarkDownViewer.css, viewer.css
│   │   ├── js/                             # markdown-it, highlight.js, gfm.js, math.js, viewer.js, search.js
│   │   └── katex/                          # KaTeX 0.16.11 (min js/css, woff2 글꼴)
│   └── src/com/kcube/md/
│       ├── editor/
│       │   ├── MarkdownMultiPageEditor.java    # Source/Preview/Split 탭 에디터
│       │   ├── MarkdownTextEditor.java         # Source·Split 공용 텍스트 에디터 (옆 패널)
│       │   └── MarkdownEditorContributor.java  # 전역 액션 연결
│       ├── views/
│       │   └── MarkdownPreviewView.java        # 실시간 미리보기 뷰
│       ├── outline/
│       │   ├── HeadingParser.java              # 헤딩 트리 해석
│       │   └── MarkdownOutlinePage.java        # Outline 뷰 페이지
│       ├── source/
│       │   ├── MarkdownSourceViewerConfiguration.java  # Source 문법 색 설정
│       │   ├── MarkdownPartitions.java         # 펜스 코드·주석 파티셔닝
│       │   ├── MarkdownScanner.java            # 본문 문법 토큰 스캐너
│       │   ├── MarkdownColors.java             # 밝은/어두운 색 토큰
│       │   └── *Rule.java, ScannerUtil.java    # 펜스·줄머리·링크·인라인·이스케이프 규칙
│       ├── preview/
│       │   ├── MarkdownBrowser.java            # JS 뷰어 로드 + 렌더링 호출 래퍼
│       │   └── LivePreview.java                # 에디터 추적 실시간 갱신 (뷰·Split 공용)
│       └── util/
│           ├── JsUtils.java                    # Java 문자열 → JS 리터럴 변환
│           ├── EditorInputs.java               # md 판별, 이미지 기준 경로
│           ├── SourceScroll.java               # Source 맨 위 줄 읽기/쓰기 (스크롤 동기화)
│           └── EditorTheme.java                # 에디터 배경색·다크 여부 (Preview 테마·문법 색 공용)
├── com.kcube.md.feature/                   # 설치 단위 (Feature)
│   ├── feature.xml
│   ├── build.properties
│   └── pom.xml                             # eclipse-feature
└── com.kcube.md.update-site/               # 업데이트 사이트 (p2 Repository)
    ├── category.xml
    └── pom.xml                             # eclipse-repository
```

### 4.2 의존 번들
- `org.eclipse.ui`
- `org.eclipse.ui.editors`
- `org.eclipse.ui.views` (Outline 페이지)
- `org.eclipse.jface.text`
- `org.eclipse.core.runtime`
- `org.eclipse.core.resources`, `org.eclipse.core.filesystem` (링크 파일 열기)
- `org.slf4j.api`

### 4.3 plugin.xml 확장점
- `org.eclipse.ui.editors` — 멀티탭 에디터 등록 (`extensions="md,markdown"`)
- `org.eclipse.ui.views` — Markdown 카테고리 및 Preview 뷰 등록

### 4.4 Java ↔ JS 연동 규칙
- JS 진입점 (Java 에서 `browser.execute` 로 호출): `renderMarkdown(markdown, baseUri)`, `applyFont(family, sizePx)`, `applyTheme(dark, background)`, `openSearch()`, `scrollToLine(line)`. `getTopLine()` 은 `browser.evaluate` 로 읽는다
- 뷰어 페이지 로드: `FrameworkUtil.getBundle(...).getEntry("web/viewer.html")` → `FileLocator.toFileURL()` → `browser.setUrl()`
- 문자열 전달은 반드시 `JsUtils.toJsString()` 사용
  - `"`, `\`, `\n`, `\r`, `\u2028`, `\u2029` 이스케이프
  - `<` → `\u003c` 치환 (`</script>` 주입 방지)

## 5. 기술 제약 및 주의사항

| 항목 | 내용 |
|---|---|
| Java 버전 | Java 17 |
| 브라우저 엔진 | Windows는 Edge(WebView2) 권장: `-Dorg.eclipse.swt.browser.DefaultType=edge` |
| JS 호환성 | 구형 IE 모드가 아닌 Edge/WebKit 기준으로 동작 확인 |
| 외부 리소스 | **CDN 사용 금지** — 사내망/오프라인 대비, 모든 라이브러리 로컬 포함 |
| 이미지 경로 | md 파일 기준 상대 경로 → JS에 base 경로 전달하여 보정 필요 |
| 스크롤 | `setText()` 재로딩 방식 금지 (스크롤 초기화됨), JS로 본문만 교체 |

## 6. 코딩 규칙

### 6.1 공통
- 프로젝트에 기존 프레임워크가 있으면 **그 프레임워크 방식을 따른다**
- 코드는 Java 17 기준 문법 사용 가능 (패턴 매칭 `instanceof`, switch 화살표, 텍스트 블록 등)

### 6.2 주석
- **Java의 모든 메소드에 Javadoc 주석** 작성 (`@param`, `@return` 포함)
- 의미가 분명하지 않은 **필드/변수에도 주석** 작성
- **JavaScript의 모든 함수에도 JSDoc 주석** 작성

```java
/**
 * 편집 중인 문서를 JS 뷰어로 전달해 렌더링한다.
 */
private void refreshPreview() { ... }
```

```javascript
/**
 * Java(Eclipse)에서 호출하는 렌더링 진입점.
 * @param {string} markdown 원본 Markdown 텍스트
 */
function renderMarkdown(markdown) { ... }
```

### 6.3 로그
- 로거 변수명: `_log` (SLF4J)
- 로그 호출은 **항상 레벨 체크로 감싼다**
- 로그 메시지는 **영어**로 작성
- 파라미터는 `{}` 플레이스홀더 사용

```java
/** 로거 */
private static final Logger _log = LoggerFactory.getLogger(MarkdownMultiPageEditor.class);

if (_log.isDebugEnabled()) {
    _log.debug("Tracking markdown editor: {}", name);
}

if (_log.isErrorEnabled()) {
    _log.error("Failed to load viewer page", e);
}
```

## 7. 개발 환경

- Eclipse IDE for RCP and RCP Developers
- 새 프로젝트: File > New > Plug-in Project
- 실행/디버그: Run As > Eclipse Application (런타임 워크벤치)
- 배포 빌드: Maven 3.9+ / JDK 17, `mvn clean verify` (8장 참고)

## 8. 배포 — 업데이트 사이트 설치 구조

### 8.1 목표
- 사용자는 **Help > Install New Software...** 에서 업데이트 사이트 URL을 추가해 설치
- 신규 버전 배포 시 **Help > Check for Updates** 로 자동 업데이트
- 사내망(오프라인) 환경에서는 압축된 업데이트 사이트(zip)를 **Archive...** 로 설치 가능

### 8.2 구성 단위

| 단위 | ID | 역할 |
|---|---|---|
| Plugin (Bundle) | `com.kcube.md` | 실제 코드, plugin.xml, web 리소스 |
| Feature | `com.kcube.md.feature` | 설치·업데이트 단위. 플러그인 묶음 + 라이선스/설명 |
| Update Site | `com.kcube.md.update-site` | p2 Repository 생성 (`content.jar`, `artifacts.jar`, `plugins/`, `features/`) |

- 사용자에게 보이는 설치 단위는 **Feature**이며, 플러그인을 직접 노출하지 않는다.
- 기능이 늘어 번들을 분리하면(`com.kcube.md.core`, `com.kcube.md.ui`) Feature에 추가만 하면 된다.

### 8.3 빌드 도구
- **Maven + Eclipse Tycho 4.x** 사용 (CI/명령행 빌드, 재현 가능한 p2 저장소 생성)
- 빌드 명령: `mvn clean verify`
- 결과물:
  - `com.kcube.md.update-site/target/repository/` → 업데이트 사이트 폴더 (웹서버 업로드용)
  - `com.kcube.md.update-site/target/com.kcube.md.update-site-<버전>.zip` → 오프라인 설치용

### 8.4 주요 설정 파일

**pom.xml (부모)** — 핵심 부분 (`kcube-maven-goals-plugin` 과 동일 방식)
```xml
<groupId>com.kcube</groupId>
<artifactId>kcube-markdown-viewer-parent</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<properties>
  <tycho.version>4.0.13</tycho.version>
  <eclipse.release>2024-12</eclipse.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<modules>
  <module>com.kcube.md</module>
  <module>com.kcube.md.feature</module>
  <module>com.kcube.md.update-site</module>
</modules>

<repositories>
  <repository>
    <id>eclipse-release</id>
    <layout>p2</layout>
    <url>https://download.eclipse.org/releases/${eclipse.release}</url>
  </repository>
</repositories>

<build>
  <plugins>
    <plugin>  <!-- tycho-maven-plugin, extensions=true -->
    <plugin>  <!-- target-platform-configuration: win32 / macosx(aarch64, x86_64) / linux 환경 -->
  </plugins>
</build>
```
- 별도 타깃 플랫폼 모듈·`.mvn/extensions.xml` 없이 **p2 저장소를 부모 POM에 직접 선언**한다.
- 컴파일 레벨은 `Bundle-RequiredExecutionEnvironment: JavaSE-17`(MANIFEST)과 `.settings/org.eclipse.jdt.core.prefs`로 지정한다.
- `IURIEditorInput` 이 `org.eclipse.ui.ide` 에서 비공개 API로 표시되므로, 플러그인 `.settings/org.eclipse.jdt.core.prefs` 에 `forbiddenReference=warning` 을 둔다.

**MANIFEST.MF (com.kcube.md)**
```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-SymbolicName: com.kcube.md;singleton:=true
Bundle-Name: KCube Markdown Viewer
Bundle-Vendor: KCube
Bundle-Version: 1.0.0.qualifier
Bundle-RequiredExecutionEnvironment: JavaSE-17
Automatic-Module-Name: com.kcube.md
Require-Bundle: org.eclipse.ui.ide,
 org.eclipse.ui,
 org.eclipse.ui.editors,
 org.eclipse.ui.views,
 org.eclipse.ui.workbench.texteditor,
 org.eclipse.jface.text,
 org.eclipse.core.runtime,
 org.eclipse.core.resources,
 org.eclipse.core.filesystem
Import-Package: org.slf4j;version="[1.7.0,3.0.0)"
Bundle-ActivationPolicy: lazy
```
- `plugin.xml`에 확장점을 쓰므로 `singleton:=true` 필수
- SLF4J는 `Require-Bundle`이 아닌 **`Import-Package`** 로 참조 (1.x/2.x 호환)

**build.properties (com.kcube.md)**
```
source.. = src/
output.. = bin/
bin.includes = META-INF/,\
               .,\
               plugin.xml,\
               icons/,\
               web/
```
- `web/`, `icons/` 누락 시 설치본에서 Preview가 빈 화면이 되므로 반드시 포함

**feature.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<feature id="com.kcube.md.feature"
         label="KCube Markdown Viewer"
         version="1.0.0.qualifier"
         provider-name="KCube">
   <description>Markdown editor and preview for Eclipse.</description>
   <copyright>Copyright (c) KCube.</copyright>
   <license url="">License text</license>

   <plugin id="com.kcube.md" version="0.0.0" unpack="false"/>
</feature>
```
- `unpack="false"`: jar 그대로 설치. web 리소스는 `FileLocator.toFileURL()`이 실행 시 캐시 폴더로 풀어 주므로 jar 형태로도 동작
- 이클립스 기본 번들(UI, 에디터, SLF4J)은 Feature에 **포함하지 않고** 의존성으로만 선언

**category.xml (com.kcube.md.update-site)**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <feature id="com.kcube.md.feature" version="0.0.0">
      <category name="kcube"/>
   </feature>
   <category-def name="kcube" label="KCube Tools">
      <description>KCube Eclipse plug-ins</description>
   </category-def>
</site>
```
- 카테고리가 없으면 설치 화면에서 "Group items by category" 체크 시 목록에 안 보이므로 반드시 정의
- 향후 다른 KCube 플러그인(예: 링크뷰)도 같은 `KCube Tools` 카테고리로 묶어 **하나의 업데이트 사이트**에서 배포 가능

**com.kcube.md.update-site/pom.xml**
```xml
<artifactId>com.kcube.md.update-site</artifactId>
<packaging>eclipse-repository</packaging>
```
- 각 모듈 packaging: bundle → `eclipse-plugin`, feature → `eclipse-feature`, site → `eclipse-repository`

### 8.5 버전 규칙

| 위치 | 형식 | 예 |
|---|---|---|
| MANIFEST.MF / feature.xml | `major.minor.micro.qualifier` | `1.0.0.qualifier` |
| pom.xml | `major.minor.micro-SNAPSHOT` | `1.0.0-SNAPSHOT` |
| 빌드 결과 | qualifier → 빌드 시각 치환 | `1.0.0.202610081530` |

- 세 위치의 버전(major.minor.micro)은 **항상 일치**시킨다 (불일치 시 Tycho 빌드 실패)
- 일괄 변경: `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=1.1.0-SNAPSHOT`
- qualifier가 매 빌드마다 증가하므로 같은 버전이라도 **Check for Updates로 갱신 인식**
- 버전 증가 기준: 버그 수정 → micro, 기능 추가 → minor, 호환성 깨짐 → major

### 8.6 배포·호스팅
- `target/repository/` 내용을 **정적 웹서버**(사내 웹서버, Nginx, Apache, GitHub Pages 등)에 그대로 업로드
- 업데이트 사이트 URL 예: `https://<서버>/eclipse/kcube/`
- 버전별 이력을 남기려면 버전 폴더 + **Composite Repository** 구성 권장
  ```
  /eclipse/kcube/
  ├── compositeContent.xml
  ├── compositeArtifacts.xml
  ├── 1.0.0/
  └── 1.1.0/
  ```
- HTTPS 사용 권장 (HTTP는 최신 이클립스에서 경고 표시)

### 8.7 서명 (선택)
- 미서명 시 설치 중 **"Unsigned content" 경고**가 뜨지만 설치는 가능
- 사내 배포는 미서명 허용, 외부(마켓플레이스) 배포 시 코드 서명 인증서로 jar 서명 권장
  - Tycho: `tycho-gpg-plugin`(PGP 서명) 또는 `maven-jarsigner-plugin` 사용

### 8.8 설치 검증 체크리스트
- [ ] 깨끗한 Eclipse(2024-12 이상)에 업데이트 사이트 URL로 설치 성공
- [ ] zip 파일(Archive)로 오프라인 설치 성공
- [ ] 설치 후 `.md` 파일이 KCube Markdown Viewer로 열림
- [ ] Preview 탭에서 web 리소스 정상 로드 (빈 화면 아님)
- [ ] Window > Show View > Markdown > Markdown Preview 표시
- [ ] 버전 올려 재배포 후 Check for Updates로 업데이트 감지
- [ ] Windows / macOS 양쪽에서 Browser(Edge/WebKit) 동작 확인
