package com.kcube.md.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.md.outline.MarkdownHeading;
import com.kcube.md.outline.MarkdownOutlinePage;
import com.kcube.md.preview.LivePreview;
import com.kcube.md.preview.MarkdownBrowser;
import com.kcube.md.util.EditorInputs;
import com.kcube.md.util.SourceScroll;

/**
 * Source / Preview / Split 세 탭으로 구성된 Markdown 에디터.
 * <p>
 * Source 탭은 이클립스 기본 텍스트 에디터({@link MarkdownTextEditor}), Preview 탭은 JS 뷰어를 로드한 브라우저다.
 * Split 탭은 Source 탭과 같은 에디터 컨트롤을 공유하면서 오른쪽에 실시간 미리보기를 붙여 보여 준다.
 */
public class MarkdownMultiPageEditor extends MultiPageEditorPart {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(MarkdownMultiPageEditor.class);

	/** Source 탭 페이지 인덱스 */
	private static final int PAGE_SOURCE = 0;

	/** Source·Split 탭이 공유하는 텍스트 에디터 */
	private MarkdownTextEditor _textEditor;

	/** Preview 탭의 브라우저 (브라우저 생성 실패 시 null) */
	private MarkdownBrowser _preview;

	/** Preview 탭 페이지 인덱스 */
	private int _previewIndex = -1;

	/** Split 탭 페이지 인덱스 (텍스트 에디터 생성 실패 시 -1) */
	private int _splitIndex = -1;

	/** Split 탭 오른쪽 미리보기 브라우저 (처음 Split 탭을 열 때 만든다) */
	private MarkdownBrowser _splitPreview;

	/** Split 탭 미리보기를 에디터에 맞춰 갱신하는 도우미 (Split 탭을 열 때 추적, 떠나면 해제) */
	private LivePreview _splitLive;

	/** Split 탭 미리보기 브라우저 생성을 시도했는지 여부 (실패 후 재시도하지 않기 위함) */
	private boolean _splitCreated;

	/** 직전에 보이던 페이지 인덱스 (탭 전환 시 스크롤 위치를 넘겨주기 위해 기억한다, 처음엔 -1) */
	private int _activePageIndex = -1;

	/** Outline 뷰에 보여 줄 헤딩 트리 페이지 (Outline 이 처음 요청할 때 만든다) */
	private MarkdownOutlinePage _outlinePage;

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
	 * Source / Preview / Split 페이지를 만든다.
	 */
	@Override
	protected void createPages() {
		createEditPage();
		createPreviewPage();
		createSplitPage();
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
			_textEditor = new MarkdownTextEditor();
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
	 * Split 탭을 만든다. 새 컨트롤을 만들지 않고 Source 탭의 에디터 컨트롤을 그대로 공유하며, 탭에 텍스트 에디터를 연결해
	 * Split 탭에서도 편집 단축키·전역 액션이 Source 탭과 똑같이 동작하게 한다.
	 */
	private void createSplitPage() {
		if (_textEditor == null || getControl(PAGE_SOURCE) == null) {
			return;
		}
		_splitIndex = addPage(getControl(PAGE_SOURCE));
		setPageText(_splitIndex, "Split");
		if (getContainer() instanceof CTabFolder folder) {
			folder.getItem(_splitIndex).setData(_textEditor);
		}
	}

	/**
	 * @param pageIndex 페이지 인덱스
	 * @return 텍스트 에디터가 보이는 탭(Source 또는 Split)이면 true
	 */
	private boolean isSourcePage(int pageIndex) {
		return pageIndex == PAGE_SOURCE || pageIndex >= 0 && pageIndex == _splitIndex;
	}

	/**
	 * 탭을 바꿀 때 보던 위치를 이어 준다. Preview 로 가면 최신 내용으로 렌더링하고 Source 맨 위 줄로 스크롤하며,
	 * Source 로 가면 Preview 맨 위에 보이던 줄을 Source 맨 위로 올린다.
	 *
	 * @param newPageIndex 전환된 페이지 인덱스
	 */
	@Override
	protected void pageChange(int newPageIndex) {
		int oldPageIndex = _activePageIndex;
		_activePageIndex = newPageIndex;
		double line = -1;
		if (isSourcePage(oldPageIndex) && newPageIndex == _previewIndex) {
			line = SourceScroll.topLine(_textEditor);
		} else if (oldPageIndex == _previewIndex && isSourcePage(newPageIndex) && _preview != null) {
			line = _preview.getTopLine();
		}
		super.pageChange(newPageIndex);
		if (_textEditor != null) {
			_textEditor.setSidePaneVisible(newPageIndex == _splitIndex);
		}
		if (newPageIndex == _previewIndex) {
			refreshPreview();
			if (_preview != null) {
				_preview.scrollToLine(line);
			}
		} else if (isSourcePage(newPageIndex)) {
			SourceScroll.setTopLine(_textEditor, line);
		}
		if (newPageIndex == _splitIndex) {
			startSplitPreview();
		} else if (_splitLive != null) {
			_splitLive.untrack();
		}
	}

	/**
	 * Split 탭 오른쪽 미리보기를 (처음이면 만들어서) 에디터에 연결하고 바로 렌더링한다.
	 */
	private void startSplitPreview() {
		if (!_splitCreated) {
			_splitCreated = true;
			Composite side = _textEditor.getSidePane();
			try {
				_splitPreview = new MarkdownBrowser(side);
				_splitLive = new LivePreview(_splitPreview);
			} catch (SWTError | SWTException e) {
				if (_log.isErrorEnabled()) {
					_log.error("Failed to create browser for split preview", e);
				}
				new Label(side, SWT.WRAP).setText("Preview is not available: " + e.getMessage());
			}
			side.layout();
		}
		if (_splitLive != null) {
			_splitLive.track(_textEditor);
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
		if (_outlinePage != null) {
			_outlinePage.setDocument(getDocument());
		}
		if (getActivePage() == _previewIndex) {
			refreshPreview();
		} else if (getActivePage() == _splitIndex && _splitLive != null) {
			// 다른 이름으로 저장하면 문서 객체가 바뀔 수 있으므로 다시 연결한다.
			_splitLive.untrack();
			_splitLive.track(_textEditor);
		}
	}

	/**
	 * Outline 에서 고른 헤딩으로 이동한다. Preview 탭이면 미리보기를 스크롤하고, Source·Split 탭이면 해당 줄을
	 * 선택한다 (Split 미리보기는 Source 스크롤을 따라간다).
	 *
	 * @param heading 고른 헤딩
	 */
	private void revealHeading(MarkdownHeading heading) {
		if (getActivePage() == _previewIndex && _preview != null) {
			_preview.scrollToLine(heading.getLine());
			return;
		}
		IDocument document = getDocument();
		if (document == null || heading.getLine() >= document.getNumberOfLines()) {
			return;
		}
		try {
			IRegion region = document.getLineInformation(heading.getLine());
			_textEditor.selectAndReveal(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Heading line out of range: {}", heading, e);
			}
		}
	}

	/**
	 * Split 미리보기의 문서·스크롤 리스너를 해제한다.
	 */
	@Override
	public void dispose() {
		if (_splitLive != null) {
			_splitLive.untrack();
		}
		super.dispose();
	}

	/**
	 * @return 항상 true
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Preview 뷰 등이 텍스트 에디터를 얻을 수 있도록 {@link ITextEditor} 어댑터를, Outline 뷰에는 헤딩 트리
	 * ({@link IContentOutlinePage}) 를 제공한다.
	 *
	 * @param adapter 요청 타입
	 * @return 어댑터 객체
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ITextEditor.class && _textEditor != null) {
			return adapter.cast(_textEditor);
		}
		if (adapter == IContentOutlinePage.class && _textEditor != null) {
			if (_outlinePage == null || (_outlinePage.getControl() != null && _outlinePage.getControl().isDisposed())) {
				_outlinePage = new MarkdownOutlinePage(getDocument(), this::revealHeading);
			}
			return adapter.cast(_outlinePage);
		}
		return super.getAdapter(adapter);
	}
}
