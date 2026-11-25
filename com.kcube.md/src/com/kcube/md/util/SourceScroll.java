package com.kcube.md.util;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * 텍스트 에디터(Source)의 스크롤 위치를 원본 줄 번호로 읽고 쓴다. Preview 스크롤 동기화에 쓴다.
 */
public final class SourceScroll {

	private SourceScroll() {
	}

	/**
	 * @param editor 텍스트 에디터 (null 허용)
	 * @return 에디터의 텍스트 뷰어 (없거나 위젯이 해제됐으면 null)
	 */
	public static ITextViewer viewer(ITextEditor editor) {
		if (editor == null) {
			return null;
		}
		ITextOperationTarget target = editor.getAdapter(ITextOperationTarget.class);
		if (target instanceof ITextViewer viewer && viewer.getTextWidget() != null && !viewer.getTextWidget().isDisposed()) {
			return viewer;
		}
		return null;
	}

	/**
	 * 화면 맨 위에 보이는 원본 줄 번호를 구한다. 위 줄이 일부만 보이면 가려진 비율을 소수로 더한다.
	 *
	 * @param editor 텍스트 에디터 (null 허용)
	 * @return 원본 줄 번호 (0 부터, 소수 포함). 구할 수 없으면 -1
	 */
	public static double topLine(ITextEditor editor) {
		ITextViewer viewer = viewer(editor);
		if (viewer == null) {
			return -1;
		}
		StyledText text = viewer.getTextWidget();
		int widgetLine = text.getLineIndex(0);
		int lineTop = text.getLinePixel(widgetLine);
		int height = widgetLine + 1 < text.getLineCount() ? text.getLinePixel(widgetLine + 1) - lineTop : text.getLineHeight();
		double fraction = height > 0 ? Math.min(Math.max(-lineTop / (double) height, 0), 0.999) : 0;
		int modelLine = viewer instanceof ITextViewerExtension5 ext ? ext.widgetLine2ModelLine(widgetLine) : viewer.getTopIndex();
		return modelLine < 0 ? viewer.getTopIndex() : modelLine + fraction;
	}

	/**
	 * 원본 줄이 화면 맨 위에 오도록 스크롤한다 (소수 부분은 버린다). 커서는 옮기지 않는다.
	 *
	 * @param editor 텍스트 에디터 (null 허용)
	 * @param line   원본 줄 번호 (0 부터, 음수면 무시)
	 */
	public static void setTopLine(ITextEditor editor, double line) {
		ITextViewer viewer = viewer(editor);
		if (viewer == null || line < 0 || Double.isNaN(line)) {
			return;
		}
		viewer.setTopIndex((int) Math.floor(line));
	}
}
