package com.kcube.md.util;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Eclipse 텍스트 에디터 배경색으로 밝은/어두운 테마를 판별한다. Preview 테마와 Source 문법 색이 함께 쓴다.
 */
public final class EditorTheme {

	private EditorTheme() {
	}

	/**
	 * Eclipse 텍스트 에디터의 배경색을 구한다. 다크 테마는 이 설정을 어두운 색으로 바꾼다.
	 * 시스템 기본색을 쓰도록 설정돼 있으면 OS 목록 배경색(macOS 다크 모드 반영)을 쓴다.
	 *
	 * @param display 디스플레이
	 * @return 에디터 배경색
	 */
	public static RGB background(Display display) {
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		if (!store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
				&& !store.getString(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND).isEmpty()) {
			return PreferenceConverter.getColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		}
		return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
	}

	/**
	 * @param background 배경색
	 * @return 밝기가 128 미만이면 true (어두운 테마)
	 */
	public static boolean isDark(RGB background) {
		return (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue) < 128;
	}

	/**
	 * @param display 디스플레이
	 * @return 에디터 배경이 어두우면 true
	 */
	public static boolean isDark(Display display) {
		return isDark(background(display));
	}

	/**
	 * @param property 바뀐 설정 키
	 * @return 에디터 배경색 관련 설정(테마 전환 시 바뀜)이면 true
	 */
	public static boolean isBackgroundProperty(String property) {
		return AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND.equals(property)
				|| AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property);
	}
}
