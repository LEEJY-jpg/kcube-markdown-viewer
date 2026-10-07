package com.kcube.md.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.md.preview.MarkdownBrowser;
import com.kcube.md.util.EditorInputs;

/**
 * Source / Preview 두 탭으로 구성된 Markdown 에디터.
 * <p>
 * Source 탭은 이클립스 기본 {@link TextEditor}, Preview 탭은 JS 뷰어를 로드한 브라우저다.
 */
public class MarkdownMultiPageEditor extends MultiPageEditorPart {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(MarkdownMultiPageEditor.class);

	/** Source 탭 페이지 인덱스 */
	private static final int PAGE_SOURCE = 0;

	/** Source 탭에 내장된 텍스트 에디터 */
	private TextEditor _textEditor;

	/** Preview 탭의 브라우저 (브라우저 생성 실패 시 null) */
	private MarkdownBrowser _preview;

	/** Preview 탭 페이지 인덱스 */
	private int _previewIndex = -1;

	/**
	 * 파일 기반 입력만 허용한다.
	 *
	 * @param site  에디터 사이트
	 * @param input 에디터 입력
	 * @throws PartInitException 파일 기반 입력이 아닌 경우
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof IURIEditorInput)) {
			throw new PartInitException("Unsupported editor input: " + input.getClass().getName());
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	/**
	 * Source / Preview 페이지를 만든다.
	 */
	@Override
	protected void createPages() {
		createEditPage();
		createPreviewPage();
		setPartName(getEditorInput().getName());
		// 파일을 처음 열면 Preview 탭을 먼저 보여 준다.
		if (_preview != null) {
			setActivePage(_previewIndex);
		}
	}

	/**
	 * Source 탭(텍스트 에디터)을 만든다.
	 */
	private void createEditPage() {
		try {
			_textEditor = new TextEditor();
			int index = addPage(_textEditor, getEditorInput());
			setPageText(index, "Source");
		} catch (PartInitException e) {
			if (_log.isErrorEnabled()) {
				_log.error("Failed to create text editor page", e);
			}
			ErrorDialog.openError(getSite().getShell(), "KCube Markdown Viewer", "Failed to create the editor.",
					new Status(IStatus.ERROR, "com.kcube.md", e.getMessage(), e));
		}
	}

	/**
	 * Preview 탭(브라우저)을 만든다. 브라우저를 쓸 수 없으면 안내 문구를 보여 준다.
	 */
	private void createPreviewPage() {
		Composite page = new Composite(getContainer(), SWT.NONE);
		page.setLayout(new FillLayout());
		try {
			_preview = new MarkdownBrowser(page);
		} catch (SWTError | SWTException e) {
			if (_log.isErrorEnabled()) {
				_log.error("Failed to create browser for preview", e);
			}
			new Label(page, SWT.WRAP).setText("Preview is not available: " + e.getMessage());
		}
		_previewIndex = addPage(page);
		setPageText(_previewIndex, "Preview");
	}

	/**
	 * Preview 탭으로 전환될 때 최신 내용으로 다시 렌더링한다.
	 *
	 * @param newPageIndex 전환된 페이지 인덱스
	 */
	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == _previewIndex) {
			refreshPreview();
		}
	}

	/**
	 * 편집 중인 문서를 JS 뷰어로 전달해 렌더링한다.
	 */
	private void refreshPreview() {
		if (_preview == null) {
			return;
		}
		IDocument document = getDocument();
		if (document != null) {
			_preview.render(document.get(), EditorInputs.baseUri(getEditorInput()));
		}
	}

	/**
	 * Preview 탭의 검색바를 연다. Preview 가 없으면 아무 일도 하지 않는다.
	 */
	public void openPreviewSearch() {
		if (_preview != null) {
			_preview.openSearch();
		}
	}

	/**
	 * @return 편집 중인 문서 (없으면 null)
	 */
	private IDocument getDocument() {
		if (_textEditor == null || _textEditor.getDocumentProvider() == null) {
			return null;
		}
		return _textEditor.getDocumentProvider().getDocument(_textEditor.getEditorInput());
	}

	/**
	 * Source 탭의 내용을 저장한다.
	 *
	 * @param monitor 진행 모니터
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		_textEditor.doSave(monitor);
	}

	/**
	 * 다른 이름으로 저장하고 새 입력으로 갱신한다.
	 */
	@Override
	public void doSaveAs() {
		_textEditor.doSaveAs();
		IEditorInput input = _textEditor.getEditorInput();
		setInput(input);
		setPartName(input.getName());
		if (getActivePage() == _previewIndex) {
			refreshPreview();
		}
	}

	/**
	 * @return 항상 true
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Preview 뷰 등이 텍스트 에디터를 얻을 수 있도록 {@link ITextEditor} 어댑터를 제공한다.
	 *
	 * @param adapter 요청 타입
	 * @return 어댑터 객체
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ITextEditor.class && _textEditor != null) {
			return adapter.cast(_textEditor);
		}
		return super.getAdapter(adapter);
	}
}
