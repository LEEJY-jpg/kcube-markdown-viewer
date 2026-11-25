package com.kcube.md.outline;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * Markdown 헤딩을 트리로 보여 주는 Outline 페이지.
 * <p>
 * 문서가 바뀌면 잠시(500ms) 기다렸다가 다시 해석한다. 헤딩을 고르면 생성자에서 받은 콜백으로 알린다.
 */
public class MarkdownOutlinePage extends ContentOutlinePage {

	/** 입력이 멈춘 뒤 Outline 을 갱신하기까지 기다리는 시간(ms) */
	private static final int REFRESH_DELAY = 500;

	/** 헤딩을 골랐을 때 호출할 콜백 */
	private final Consumer<MarkdownHeading> _onSelect;

	/** 헤딩을 읽어 오는 문서 (없으면 null) */
	private IDocument _document;

	/** 갱신 예약용 작업 (같은 Runnable 로 timerExec 하면 이전 예약이 미뤄진다) */
	private final Runnable _refreshTask = this::refresh;

	/** 문서 변경 시 갱신을 예약하는 리스너 */
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

	/**
	 * @param document 헤딩을 읽어 올 문서 (null 허용)
	 * @param onSelect 헤딩을 골랐을 때 호출할 콜백
	 */
	public MarkdownOutlinePage(IDocument document, Consumer<MarkdownHeading> onSelect) {
		_onSelect = onSelect;
		_document = document;
	}

	/**
	 * 트리를 만들고 문서 변경을 듣기 시작한다.
	 *
	 * @param parent 부모 컴포짓
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(new HeadingContentProvider());
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((MarkdownHeading) element).getText();
			}
		});
		viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		if (_document != null) {
			_document.addDocumentListener(_documentListener);
		}
		refresh();
	}

	/**
	 * 문서를 바꾼다 (다른 이름으로 저장 등). 리스너를 옮기고 바로 갱신한다.
	 *
	 * @param document 새 문서 (null 허용)
	 */
	public void setDocument(IDocument document) {
		if (document == _document) {
			return;
		}
		if (_document != null) {
			_document.removeDocumentListener(_documentListener);
		}
		_document = document;
		if (_document != null && getControl() != null) {
			_document.addDocumentListener(_documentListener);
		}
		refresh();
	}

	/**
	 * 문서를 다시 해석해 트리를 갱신한다. 펼침 상태는 모두 펼친 상태로 되돌린다.
	 */
	public void refresh() {
		Control control = getControl();
		if (control == null || control.isDisposed()) {
			return;
		}
		List<MarkdownHeading> headings = HeadingParser.parse(_document == null ? "" : _document.get());
		TreeViewer viewer = getTreeViewer();
		control.setRedraw(false);
		try {
			viewer.setInput(headings);
			viewer.expandAll();
		} finally {
			control.setRedraw(true);
		}
	}

	/**
	 * 고른 헤딩을 콜백으로 알린다.
	 *
	 * @param event 선택 이벤트
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		super.selectionChanged(event);
		if (event.getSelection() instanceof IStructuredSelection selection
				&& selection.getFirstElement() instanceof MarkdownHeading heading) {
			_onSelect.accept(heading);
		}
	}

	/**
	 * 문서 리스너를 떼고 예약된 갱신을 취소한다.
	 */
	@Override
	public void dispose() {
		if (_document != null) {
			_document.removeDocumentListener(_documentListener);
		}
		Control control = getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().timerExec(-1, _refreshTask);
		}
		super.dispose();
	}

	/**
	 * 입력이 멈춘 뒤 갱신하도록 예약한다. 연속 입력 중에는 예약이 계속 미뤄진다.
	 */
	private void scheduleRefresh() {
		Control control = getControl();
		if (control == null || control.isDisposed()) {
			return;
		}
		control.getDisplay().asyncExec(() -> {
			if (!control.isDisposed()) {
				control.getDisplay().timerExec(REFRESH_DELAY, _refreshTask);
			}
		});
	}

	/**
	 * 헤딩 목록(트리 입력)과 하위 헤딩을 트리에 제공한다.
	 */
	private static final class HeadingContentProvider implements ITreeContentProvider {

		/**
		 * @param inputElement 헤딩 목록
		 * @return 최상위 헤딩 배열
		 */
		@Override
		public Object[] getElements(Object inputElement) {
			return inputElement instanceof List<?> list ? list.toArray() : new Object[0];
		}

		/**
		 * @param parentElement 상위 헤딩
		 * @return 하위 헤딩 배열
		 */
		@Override
		public Object[] getChildren(Object parentElement) {
			return ((MarkdownHeading) parentElement).getChildren().toArray();
		}

		/**
		 * @param element 헤딩
		 * @return 상위 헤딩 (최상위면 null)
		 */
		@Override
		public Object getParent(Object element) {
			return ((MarkdownHeading) element).getParent();
		}

		/**
		 * @param element 헤딩
		 * @return 하위 헤딩이 있으면 true
		 */
		@Override
		public boolean hasChildren(Object element) {
			return !((MarkdownHeading) element).getChildren().isEmpty();
		}
	}
}
