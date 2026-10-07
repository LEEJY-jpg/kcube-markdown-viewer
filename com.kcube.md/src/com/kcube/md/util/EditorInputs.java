package com.kcube.md.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;

/**
 * 에디터 입력(IEditorInput)에서 Markdown 파일 여부와 기준 경로를 얻는 유틸리티.
 */
public final class EditorInputs {

	private EditorInputs() {
	}

	/**
	 * 에디터 입력이 Markdown 파일(*.md, *.markdown)인지 확인한다.
	 *
	 * @param input 에디터 입력 (null 허용)
	 * @return Markdown 파일이면 true
	 */
	public static boolean isMarkdown(IEditorInput input) {
		if (input == null) {
			return false;
		}
		String name = input.getName().toLowerCase(Locale.ROOT);
		return name.endsWith(".md") || name.endsWith(".markdown");
	}

	/**
	 * md 파일이 있는 폴더의 URI 문자열(끝에 '/' 포함)을 반환한다. 상대 경로 이미지 보정에 사용한다.
	 *
	 * @param input 에디터 입력 (null 허용)
	 * @return 폴더 URI. 로컬 파일이 아니면 빈 문자열
	 */
	public static String baseUri(IEditorInput input) {
		if (!(input instanceof IURIEditorInput uriInput)) {
			return "";
		}
		URI uri = uriInput.getURI();
		if (uri == null || !"file".equalsIgnoreCase(uri.getScheme())) {
			return "";
		}
		try {
			Path parent = Paths.get(uri).getParent();
			if (parent == null) {
				return "";
			}
			String base = parent.toUri().toString();
			return base.endsWith("/") ? base : base + "/";
		} catch (RuntimeException e) {
			return "";
		}
	}
}
