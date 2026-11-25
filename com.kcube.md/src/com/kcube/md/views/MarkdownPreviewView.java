package com.kcube.md.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.md.preview.LivePreview;
import com.kcube.md.preview.MarkdownBrowser;
import com.kcube.md.util.EditorInputs;

/**
 * 활성 Markdown 에디터를 따라가며 실시간으로 렌더링 결과를 보여 주는 보조 뷰.
 */
public class MarkdownPreviewView extends ViewPart {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(MarkdownPreviewView.class);

	/** 렌더링 브라우저 (생성 실패 시 null) */
	private MarkdownBrowser _preview;

	/** 활성 에디터를 따라가며 브라우저를 갱신하는 도우미 (브라우저 생성 실패 시 null) */
	private LivePreview _live;

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
			ITextEditor tracked = _live == null ? null : _live.getTracked();
			if (tracked != null && part != null && part.getAdapter(ITextEditor.class) == tracked || part == tracked) {
				_live.untrack();
				_preview.clear();
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
		_live = new LivePreview(_preview);
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
		if (_live != null) {
			_live.untrack();
		}
		super.dispose();
	}

	/**
	 * Markdown 파일을 편집하는 에디터를 추적 대상으로 삼고 즉시 렌더링한다.
	 *
	 * @param editor 활성화된 에디터 (null 허용)
	 */
	private void track(IEditorPart editor) {
		ITextEditor textEditor = toTextEditor(editor);
		if (_live == null || textEditor == null || !EditorInputs.isMarkdown(textEditor.getEditorInput())) {
			return;
		}
		if (textEditor != _live.getTracked() && _live.track(textEditor) && _log.isDebugEnabled()) {
			_log.debug("Tracking markdown editor: {}", textEditor.getEditorInput().getName());
		}
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
}
