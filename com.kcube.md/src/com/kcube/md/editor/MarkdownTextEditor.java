package com.kcube.md.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * Source 탭과 Split 탭이 함께 쓰는 텍스트 에디터. 에디터 오른쪽에 미리보기를 붙일 수 있는 옆 패널을 둔다.
 * <p>
 * 평소(Source 탭)에는 옆 패널을 숨기고, Split 탭에서만 에디터와 옆 패널을 반반으로 보여 준다. 같은 에디터 컨트롤을 두
 * 탭이 공유하므로 커서·선택·실행 취소 이력이 그대로 이어진다.
 */
public class MarkdownTextEditor extends TextEditor {

	/** 에디터와 옆 패널을 나누는 분할 컨트롤 */
	private SashForm _sash;

	/** 텍스트 에디터가 들어가는 왼쪽 패널 */
	private Composite _sourcePane;

	/** 미리보기가 들어가는 오른쪽 패널 */
	private Composite _sidePane;

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
