# 설치 방법

KCube Markdown Viewer 는 Eclipse 4.33(2024-09) 이상, JDK 17 환경을 기준으로 한다.

## 1. 빌드

JDK 17 과 Maven 이 필요하다. 빌드 결과는 `dist/` 에 남는다 (`dist/` 는 git 추적 대상이 아니다).

```bash
./build.sh
```

| 산출물 | 용도 |
|---|---|
| `dist/kcube-markdown-viewer-<버전>.jar` | dropins 설치용 플러그인 jar |
| `dist/kcube-markdown-viewer-update-site-<버전>.zip` | Eclipse 의 Install New Software 용 p2 업데이트 사이트 |

`JAVA_HOME` 을 지정하지 않으면 macOS 에서는 JDK 17 을 자동으로 찾는다. 기본 `mvn` 이 다른 JDK 를 쓰면 빌드가 실패할 수 있으므로 JDK 17 로 맞춘다.

## 2. 설치

### 방법 A. dropins (권장)

```bash
./build.sh --install
```

빌드 후 `/Applications/Eclipse.app/Contents/Eclipse/dropins/` 의 기존 `com.kcube.md_*.jar`/`kcube-markdown-viewer-*.jar` 를 지우고 새 jar 를 복사한다. 다른 경로의 Eclipse 는 `ECLIPSE_HOME` 으로 지정한다.

```bash
ECLIPSE_HOME=/path/to/Eclipse.app/Contents/Eclipse ./build.sh --install
```

직접 설치하려면 `dist/kcube-markdown-viewer-*.jar` 를 `<Eclipse>/dropins/` 에 복사한다. 이때 옛 버전 jar 가 남아 있으면 Eclipse 가 그쪽을 로드하므로 반드시 지운다.

### 방법 B. 업데이트 사이트

1. Help > Install New Software… > Add… > Archive… 에서 `dist/kcube-markdown-viewer-update-site-<버전>.zip` 을 선택한다.
2. **KCube Tools** 카테고리의 KCube Markdown Viewer 를 선택하고 설치한다.

## 3. 재시작

Eclipse 를 `-clean` 옵션으로 재시작한다. 플러그인 구조나 리소스가 바뀐 뒤에는 필수다.

```bash
open -a Eclipse --args -clean
```

## 4. 확인

- `.md` / `.markdown` 파일을 열면 **Source** / **Preview** 탭이 보인다.
- Window > Show View > Other… > **Markdown** > Markdown Preview 로 실시간 미리보기 뷰를 연다.
- Preview 에서 Ctrl/Cmd+F 로 검색바를 연다 (대소문자 구분 없음, Enter/Shift+Enter 로 이동, Esc 로 닫기).

## 문제 해결

- 에디터가 열리지 않으면 `<workspace>/.metadata/.log` 에서 `com.kcube.md` 관련 오류를 확인한다.
- 이전 버전이 계속 동작하면 `dropins/` 에 옛 jar 가 남아 있는지 확인하고 `-clean` 으로 재시작한다.
