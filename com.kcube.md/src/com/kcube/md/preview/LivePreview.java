package com.kcube.md.preview;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.kcube.md.util.EditorInputs;
import com.kcube.md.util.SourceScroll;

/**
 * 텍스트 에디터 하나를 따라가며 {@link MarkdownBrowser} 를 실시간으로 갱신한다. 문서가 바뀌면 300ms 디바운스로 다시
 * 렌더링하고, Source 를 스크롤하면 미리보기도 같은 줄로 스크롤한다.
 * <p>
 * Markdown Preview 뷰와 에디터의 Split 탭이 함께 쓴다.
 */
public class LivePreview {

	/** 문서 변경 후 갱신까지의 지연(ms) */
	private static final int DEBOUNCE_MS = 300;

	/** 스크롤 동기화 지연(ms). 연속 스크롤 이벤트를 한 번의 JS 호출로 묶는다. */
	private static final int SCROLL_SYNC_MS = 30;

	/** 렌더링 대상 브라우저 */
	private final MarkdownBrowser _preview;

	/** 현재 추적 중인 텍스트 에디터 (없으면 null) */
	private ITextEditor _tracked;

	/** 현재 추적 중인 문서 (없으면 null) */
	private IDocument _document;

	/** 현재 추적 중인 에디터의 텍스트 뷰어 (스크롤 감지용, 없으면 null) */
	private ITextViewer _viewer;

	/** 디바운스되어 실행되는 갱신 작업 */
	private final Runnable _refreshTask = this::refreshNow;

	/** 스크롤 동기화 작업 */
	private final Runnable _scrollTask = this::syncScroll;

	/** Source 스크롤을 감지해 미리보기 스크롤을 예약하는 리스너 */
	private final IViewportListener _viewportListener = verticalOffset -> schedule(_scrollTask, SCROLL_SYNC_MS);

	/** 문서 변경을 감지해 갱신을 예약하는 리스너 */
	private final IDocumentListener _documentListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			// nothing to do
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			schedule(_refreshTask, DEBOUNCE_MS);
		}
	};

	/**
	 * @param preview 렌더링 대상 브라우저
	 */
	public LivePreview(MarkdownBrowser preview) {
		_preview = preview;
	}

	/**
	 * @return 현재 추적 중인 텍스트 에디터 (없으면 null)
	 */
	public ITextEditor getTracked() {
		return _tracked;
	}

	/**
	 * 텍스트 에디터를 추적 대상으로 삼고 즉시 렌더링한다. 이미 추적 중인 에디터면 아무 일도 하지 않는다.
	 *
	 * @param editor 텍스트 에디터 (null 허용)
	 * @return 추적을 시작했거나 이미 추적 중이면 true, 문서를 얻을 수 없으면 false
	 */
	public boolean track(ITextEditor editor) {
		if (editor == null) {
			return false;
		}
		if (editor == _tracked) {
			return true;
		}
		IDocumentProvider provider = editor.getDocumentProvider();
		IDocument document = provider == null ? null : provider.getDocument(editor.getEditorInput());
		if (document == null) {
			return false;
		}
		untrack();
		_tracked = editor;
		_document = document;
		_document.addDocumentListener(_documentListener);
		_viewer = SourceScroll.viewer(editor);
		if (_viewer != null) {
			_viewer.addViewportListener(_viewportListener);
		}
		refreshNow();
		return true;
	}

	/**
	 * 추적 중인 에디터의 리스너를 해제하고 예약된 작업을 취소한다.
	 */
	public void untrack() {
		if (_document != null) {
			_document.removeDocumentListener(_documentListener);
		}
		if (_viewer != null && _viewer.getTextWidget() != null && !_viewer.getTextWidget().isDisposed()) {
			_viewer.removeViewportListener(_viewportListener);
		}
		if (!_preview.getControl().isDisposed()) {
			Display display = _preview.getControl().getDisplay();
			display.timerExec(-1, _refreshTask);
			display.timerExec(-1, _scrollTask);
		}
		_viewer = null;
		_document = null;
		_tracked = null;
	}

	/**
	 * 작업을 지연 실행으로 예약한다. 같은 작업이 이미 예약돼 있으면 다시 센다.
	 *
	 * @param task    작업
	 * @param delayMs 지연(ms)
	 */
	private void schedule(Runnable task, int delayMs) {
		if (_preview.getControl().isDisposed()) {
			return;
		}
		Display display = _preview.getControl().getDisplay();
		display.timerExec(-1, task);
		display.timerExec(delayMs, task);
	}

	/**
	 * 추적 중인 문서를 즉시 렌더링하고 Source 의 맨 위 줄 위치로 스크롤한다.
	 */
	public void refreshNow() {
		if (_preview.getControl().isDisposed() || _document == null) {
			return;
		}
		_preview.render(_document.get(), EditorInputs.baseUri(_tracked.getEditorInput()));
		syncScroll();
	}

	/**
	 * 미리보기를 추적 중인 Source 의 맨 위 줄 위치로 스크롤한다.
	 */
	private void syncScroll() {
		if (_preview.getControl().isDisposed() || _tracked == null) {
			return;
		}
		_preview.scrollToLine(SourceScroll.topLine(_tracked));
	}
}
