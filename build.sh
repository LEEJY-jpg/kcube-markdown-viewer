#!/bin/bash
# KCube Markdown Viewer 빌드 스크립트 (Maven Tycho).
#
# 사용법:
#   ./build.sh             빌드해서 dist/ 에 플러그인 jar 와 update-site zip 을 남긴다
#   ./build.sh --install   빌드 후 ECLIPSE_HOME 의 dropins 에 설치 (Eclipse 는 -clean 으로 재시작)
#
# 환경변수(생략 시 자동 탐지):
#   ECLIPSE_HOME  Eclipse.app/Contents/Eclipse 경로 (--install 대상)
#   JAVA_HOME     JDK 17
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
	JAVA_HOME="$(/usr/libexec/java_home -v 17)"
	export JAVA_HOME
fi

mvn -q -B clean verify

rm -rf "$ROOT/dist"
mkdir -p "$ROOT/dist"
JAR="$(ls "$ROOT"/com.kcube.md/target/com.kcube.md-*.jar | head -1)"
VERSION="$(unzip -p "$JAR" META-INF/MANIFEST.MF | tr -d '\r' | sed -n 's/^Bundle-Version: *//p')"
cp "$JAR" "$ROOT/dist/com.kcube.md_${VERSION}.jar"
cp "$ROOT"/com.kcube.md.update-site/target/com.kcube.md.update-site-*.zip "$ROOT/dist/"
echo "built: dist/com.kcube.md_${VERSION}.jar"

if [ "${1:-}" = "--install" ]; then
	ECLIPSE_HOME="${ECLIPSE_HOME:-/Applications/Eclipse.app/Contents/Eclipse}"
	[ -d "$ECLIPSE_HOME/dropins" ] || { echo "ECLIPSE_HOME 을 지정하세요 (…/Eclipse.app/Contents/Eclipse)"; exit 1; }
	# 옛 버전이 남아 있으면 Eclipse 가 그쪽을 로드하므로 기존 설치본을 모두 치운다.
	rm -rf "$ECLIPSE_HOME"/dropins/com.kcube.md_*.jar
	cp "$ROOT/dist/com.kcube.md_${VERSION}.jar" "$ECLIPSE_HOME/dropins/"
	echo "installed to $ECLIPSE_HOME/dropins — Eclipse 를 -clean 으로 재시작하세요"
fi
