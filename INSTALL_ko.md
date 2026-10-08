# 설치 방법

[English](INSTALL.md) | **한국어**

- 플러그인을 **받아서 쓰기만** 하는 사용자는 [일반 사용자 설치 가이드](#일반-사용자-설치-가이드)를 본다.
- 소스에서 직접 빌드하는 **개발자**는 아래 1~4 절을 따른다.

KCube Markdown Viewer 는 Eclipse 4.33(2024-09) 이상, JDK 17 환경을 기준으로 한다.

## 1. 빌드

JDK 17 과 Maven 이 필요하다. 빌드 결과는 `dist/` 에 남는다 (`dist/` 는 git 추적 대상이 아니다).

```bash
./build.sh
```

| 산출물 | 용도 |
|---|---|
| `dist/com.kcube.md.kcube-markdown-viewer-<버전>.jar` | dropins 설치용 플러그인 jar |
| `dist/kcube-markdown-viewer-update-site-<버전>.zip` | Eclipse 의 Install New Software 용 p2 업데이트 사이트 |

`JAVA_HOME` 을 지정하지 않으면 macOS 에서는 JDK 17 을 자동으로 찾는다. 기본 `mvn` 이 다른 JDK 를 쓰면 빌드가 실패할 수 있으므로 JDK 17 로 맞춘다.

## 2. 설치

### 방법 A. dropins (권장)

```bash
./build.sh --install
```

빌드 후 `/Applications/Eclipse.app/Contents/Eclipse/dropins/` 의 기존 `com.kcube.md_*.jar`/`com.kcube.md.kcube-markdown-viewer-*.jar` 를 지우고 새 jar 를 복사한다. 다른 경로의 Eclipse 는 `ECLIPSE_HOME` 으로 지정한다.

```bash
ECLIPSE_HOME=/path/to/Eclipse.app/Contents/Eclipse ./build.sh --install
```

직접 설치하려면 `dist/com.kcube.md.kcube-markdown-viewer-*.jar` 를 `<Eclipse>/dropins/` 에 복사한다. 이때 옛 버전 jar 가 남아 있으면 Eclipse 가 그쪽을 로드하므로 반드시 지운다.

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

---

# 일반 사용자 설치 가이드

개발자에게 받은 jar 파일 하나만 있으면 된다. 소스, Maven, JDK 빌드 환경은 필요 없다.

## 준비물

- Eclipse 4.33(2024-09) 이상 (Eclipse IDE for Java Developers 등 일반 배포판)
- Eclipse 를 실행할 JRE/JDK 17 이상 (Eclipse 4.33 자체 요구사항)
- 배포받은 `com.kcube.md.kcube-markdown-viewer-<버전>.jar`

## 설치 (dropins 방식)

1. **Eclipse 를 종료**한다.
2. jar 파일을 Eclipse 설치 폴더의 `dropins` 폴더에 복사한다.

   | OS | dropins 위치 (기본 설치 기준) |
   |---|---|
   | macOS | `/Applications/Eclipse.app/Contents/Eclipse/dropins/` |
   | Windows | `C:\eclipse\dropins\` (Eclipse 를 푼 폴더 아래) |
   | Linux | `~/eclipse/dropins/` (Eclipse 를 푼 폴더 아래) |

   macOS 에서는 Finder 에서 Eclipse.app 우클릭 > 패키지 내용 보기 > Contents > Eclipse > dropins 로 이동하거나, 터미널에서 다음을 실행한다.

   ```bash
   cp ~/Downloads/com.kcube.md.kcube-markdown-viewer-*.jar /Applications/Eclipse.app/Contents/Eclipse/dropins/
   ```

3. Eclipse 를 **`-clean` 옵션으로 한 번 실행**한다. (최초 설치 시 필수)

   ```bash
   # macOS
   open -a Eclipse --args -clean
   # Windows (명령 프롬프트)
   C:\eclipse\eclipse.exe -clean
   # Linux
   ~/eclipse/eclipse -clean
   ```

   이후에는 평소처럼 실행하면 된다.

> macOS 에서 내려받은 jar 에 "다운로드한 파일" 격리 속성이 붙어 문제가 되면 `xattr -d com.apple.quarantine <jar 경로>` 를 실행한 뒤 복사한다.

## 설치 확인

1. 아무 `.md` 파일을 연다 (Package Explorer 에서 더블클릭).
2. 에디터 하단에 **Source** / **Preview** 탭이 보이면 설치된 것이다.
3. Preview 탭에서 렌더링된 화면이 보이고, Ctrl/Cmd+F 로 검색바가 열리는지 확인한다.
4. (선택) Window > Show View > Other… > **Markdown** > Markdown Preview 로 실시간 미리보기 뷰를 연다.

`.md` 파일이 다른 에디터로 열리면 파일 우클릭 > Open With > **KCube Markdown Viewer** 를 선택하거나, Preferences > General > Editors > File Associations 에서 `*.md` 의 기본 에디터를 KCube Markdown Viewer 로 지정한다.

## 업데이트

1. Eclipse 를 종료한다.
2. `dropins` 폴더의 기존 `com.kcube.md.kcube-markdown-viewer-*.jar`(또는 옛 이름의 `com.kcube.md_*.jar`)를 **삭제**하고 새 jar 를 복사한다. 옛 jar 가 남아 있으면 옛 버전이 로드될 수 있다.
3. `-clean` 옵션으로 Eclipse 를 실행한다.

## 삭제

Eclipse 를 종료하고 `dropins` 폴더에서 jar 를 삭제한 뒤 `-clean` 으로 실행한다.

## 문제 해결

| 증상 | 확인 |
|---|---|
| Source/Preview 탭이 보이지 않는다 | `-clean` 으로 재시작했는지, jar 가 `dropins` 에 있는지 확인한다. |
| Preview 가 비어 있다 | Window > Preferences > General > Web Browser 설정과 무관하다. 에디터를 닫고 다시 열어 보고, 계속되면 `<workspace>/.metadata/.log` 를 개발자에게 전달한다. |
| Eclipse 버전이 낮다는 오류 | Eclipse 4.33(2024-09) 이상으로 업그레이드한다. |
| 로그 위치 | `<workspace>/.metadata/.log` (Window > Show View > Error Log 에서도 확인 가능) |
