package com.kcube.md.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;

import com.kcube.md.source.MarkdownColors;
import com.kcube.md.source.MarkdownPartitions;
import com.kcube.md.source.MarkdownSourceViewerConfiguration;
import com.kcube.md.util.EditorTheme;

/**
 * Source 탭과 Split 탭이 함께 쓰는 텍스트 에디터. 에디터 오른쪽에 미리보기를 붙일 수 있는 옆 패널을 둔다.
 * <p>
 * 평소(Source 탭)에는 옆 패널을 숨기고, Split 탭에서만 에디터와 옆 패널을 반반으로 보여 준다. 같은 에디터 컨트롤을 두
 * 탭이 공유하므로 커서·선택·실행 취소 이력이 그대로 이어진다.
 * <p>
 * Markdown 문법(헤딩, 굵게, 코드 등)에 색을 입히며, 에디터 배경이 바뀌면(테마 전환) 색 묶음을 바꿔 다시 칠한다.
 */
public class MarkdownTextEditor extends TextEditor {

	/**
	 * 문법 색 토큰. 생성자 안에서 호출되는 {@link #initializeEditor()} 가 채우므로 필드 초기값을 두지 않는다
	 * (초기값을 두면 생성자가 끝난 뒤 덮어쓴다).
	 */
	private MarkdownColors _colors;

	/** 에디터와 옆 패널을 나누는 분할 컨트롤 */
	private SashForm _sash;

	/** 텍스트 에디터가 들어가는 왼쪽 패널 */
	private Composite _sourcePane;

	/** 미리보기가 들어가는 오른쪽 패널 */
	private Composite _sidePane;

	/**
	 * 기본 텍스트 에디터 설정에 Markdown 문법 색을 더한 뷰어 설정을 쓴다.
	 */
	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		Display display = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();
		_colors = new MarkdownColors(EditorTheme.isDark(display));
		setSourceViewerConfiguration(new MarkdownSourceViewerConfiguration(getPreferenceStore(), _colors));
	}

	/**
	 * 입력을 연 뒤 문서에 Markdown 파티셔너(펜스 코드 블록·HTML 주석)를 붙인다.
	 *
	 * @param input 에디터 입력
	 * @throws CoreException 입력을 열 수 없는 경우
	 */
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (getDocumentProvider() != null) {
			MarkdownPartitions.install(getDocumentProvider().getDocument(input));
		}
	}

	/**
	 * 에디터 배경색 설정이 바뀌면(테마 전환 포함) 배경에 맞는 문법 색으로 다시 칠한다.
	 *
	 * @param event 설정 변경 이벤트
	 */
	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		super.handlePreferenceStoreChanged(event);
		ISourceViewer viewer = getSourceViewer();
		if (_colors != null && viewer != null && EditorTheme.isBackgroundProperty(event.getProperty())) {
			Display display = viewer.getTextWidget().getDisplay();
			display.asyncExec(() -> {
				if (!viewer.getTextWidget().isDisposed()) {
					_colors.update(EditorTheme.isDark(display));
					viewer.invalidateTextPresentation();
				}
			});
		}
	}

	/**
	 * 분할 컨트롤을 만들고 왼쪽에 텍스트 에디터를 만든다. 옆 패널은 숨긴 상태로 시작한다.
	 *
	 * @param parent 부모 컴포짓
	 */
	@Override
	public void createPartControl(Composite parent) {
		_sash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
		_sourcePane = new Composite(_sash, SWT.NONE);
		_sourcePane.setLayout(new FillLayout());
		super.createPartControl(_sourcePane);
		_sidePane = new Composite(_sash, SWT.NONE);
		_sidePane.setLayout(new FillLayout());
		_sash.setWeights(1, 1);
		_sash.setMaximizedControl(_sourcePane);
	}

	/**
	 * @return 미리보기를 넣을 오른쪽 패널 (컨트롤 생성 전이면 null)
	 */
	public Composite getSidePane() {
		return _sidePane;
	}

	/**
	 * 옆 패널을 보이거나 숨긴다.
	 *
	 * @param visible true 면 에디터와 옆 패널을 나란히, false 면 에디터만 보여 준다
	 */
	public void setSidePaneVisible(boolean visible) {
		if (_sash != null && !_sash.isDisposed()) {
			_sash.setMaximizedControl(visible ? null : _sourcePane);
		}
	}
}
