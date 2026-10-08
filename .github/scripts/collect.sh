#!/bin/bash
# 빌드 산출물(jar, update-site zip)을 dist/ 에 이름 규칙에 맞춰 모은다.
set -ex
find . -path '*/target/*' \( -name '*.jar' -o -name '*.zip' \) -maxdepth 4
mkdir -p dist
JAR="$(ls com.kcube.md/target/com.kcube.md-*.jar | grep -v -e sources -e javadoc | head -1)"
ZIP="$(ls com.kcube.md.update-site/target/com.kcube.md.update-site-*.zip | head -1)"
VERSION="$(unzip -p "$JAR" META-INF/MANIFEST.MF | tr -d '\r' | sed -n 's/^Bundle-Version: *//p' | cut -d. -f1-3)"
cp -f "$JAR" "dist/com.kcube.md.kcube-markdown-viewer-${VERSION}.jar"
cp -f "$ZIP" "dist/kcube-markdown-viewer-update-site-${VERSION}.zip"
ls -l dist
