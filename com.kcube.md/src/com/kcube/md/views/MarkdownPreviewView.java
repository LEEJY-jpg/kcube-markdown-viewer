package com.kcube.md.views;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.md.preview.MarkdownBrowser;
import com.kcube.md.util.EditorInputs;

/**
 * 활성 Markdown 에디터를 따라가며 실시간으로 렌더링 결과를 보여 주는 보조 뷰.
 */
public class MarkdownPreviewView extends ViewPart {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(MarkdownPreviewView.class);

	/** 문서 변경 후 갱신까지의 지연(ms) */
	private static final int DEBOUNCE_MS = 300;

	/** 렌더링 브라우저 (생성 실패 시 null) */
	private MarkdownBrowser _preview;

	/** 현재 추적 중인 텍스트 에디터 */
	private ITextEditor _tracked;

	/** 현재 추적 중인 문서 */
	private IDocument _document;

	/** 디바운스되어 실행되는 갱신 작업 */
	private final Runnable _refreshTask = this::refreshNow;

	/** 문서 변경을 감지해 갱신을 예약하는 리스너 */
	private final IDocumentListener _documentListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			// nothing to do
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			scheduleRefresh();
		}
	};

	/** 활성 에디터 변경을 감지하는 파트 리스너 */
	private final IPartListener2 _partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference ref) {
			IWorkbenchPart part = ref.getPart(false);
			if (part instanceof IEditorPart editor) {
				track(editor);
			}
		}

		@Override
		public void partClosed(IWorkbenchPartReference ref) {
			IWorkbenchPart part = ref.getPart(false);
			if (_tracked != null && part != null && part.getAdapter(ITextEditor.class) == _tracked
					|| part == _tracked) {
				untrack();
				if (_preview != null) {
					_preview.clear();
				}
			}
		}
	};

	/**
	 * 브라우저를 만들고 현재 활성 에디터 추적을 시작한다.
	 *
	 * @param parent 부모 컴포짓
	 */
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		try {
			_preview = new MarkdownBrowser(parent);
		} catch (SWTError | SWTException e) {
			if (_log.isErrorEnabled()) {
				_log.error("Failed to create browser for preview view", e);
			}
			new Label(parent, SWT.WRAP).setText("Preview is not available: " + e.getMessage());
			return;
		}
		IWorkbenchPage page = getSite().getPage();
		page.addPartListener(_partListener);
		track(page.getActiveEditor());
	}

	/**
	 * 브라우저에 포커스를 준다.
	 */
	@Override
	public void setFocus() {
		if (_preview != null) {
			_preview.setFocus();
		}
	}

	/**
	 * 리스너를 해제한다.
	 */
	@Override
	public void dispose() {
		getSite().getPage().removePartListener(_partListener);
		untrack();
		super.dispose();
	}

	/**
	 * Markdown 파일을 편집하는 에디터를 추적 대상으로 삼고 즉시 렌더링한다.
	 *
	 * @param editor 활성화된 에디터 (null 허용)
	 */
	private void track(IEditorPart editor) {
		ITextEditor textEditor = toTextEditor(editor);
		if (textEditor == null || textEditor == _tracked || !EditorInputs.isMarkdown(textEditor.getEditorInput())) {
			return;
		}
		IDocumentProvider provider = textEditor.getDocumentProvider();
		IDocument document = provider == null ? null : provider.getDocument(textEditor.getEditorInput());
		if (document == null) {
			return;
		}
		untrack();
		_tracked = textEditor;
		_document = document;
		_document.addDocumentListener(_documentListener);
		if (_log.isDebugEnabled()) {
			_log.debug("Tracking markdown editor: {}", textEditor.getEditorInput().getName());
		}
		refreshNow();
	}

	/**
	 * 현재 추적 중인 에디터/문서 리스너를 해제한다.
	 */
	private void untrack() {
		if (_document != null) {
			_document.removeDocumentListener(_documentListener);
		}
		_document = null;
		_tracked = null;
	}

	/**
	 * @param editor 에디터 (null 허용)
	 * @return 에디터에 내장된 텍스트 에디터. 없으면 null
	 */
	private static ITextEditor toTextEditor(IEditorPart editor) {
		if (editor == null) {
			return null;
		}
		if (editor instanceof ITextEditor te) {
			return te;
		}
		return editor.getAdapter(ITextEditor.class);
	}

	/**
	 * 300ms 디바운스로 갱신을 예약한다.
	 */
	private void scheduleRefresh() {
		if (_preview == null || _preview.getControl().isDisposed()) {
			return;
		}
		Display display = _preview.getControl().getDisplay();
		display.timerExec(-1, _refreshTask);
		display.timerExec(DEBOUNCE_MS, _refreshTask);
	}

	/**
	 * 추적 중인 문서를 즉시 렌더링한다.
	 */
	private void refreshNow() {
		if (_preview == null || _preview.getControl().isDisposed() || _document == null) {
			return;
		}
		IEditorInput input = _tracked.getEditorInput();
		_preview.render(_document.get(), EditorInputs.baseUri(input));
	}
}
