package com.kcube.md.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * 활성 탭에 맞게 전역 액션(Undo/Copy/Find 등)을 연결·해제하는 컨트리뷰터.
 */
public class MarkdownEditorContributor extends MultiPageEditorActionBarContributor {

	/** Source 탭의 텍스트 에디터로 위임할 전역 액션 ID 목록 */
	private static final String[] GLOBAL_ACTIONS = {
			ActionFactory.UNDO.getId(),
			ActionFactory.REDO.getId(),
			ActionFactory.CUT.getId(),
			ActionFactory.COPY.getId(),
			ActionFactory.PASTE.getId(),
			ActionFactory.DELETE.getId(),
			ActionFactory.SELECT_ALL.getId(),
			ActionFactory.FIND.getId()
	};

	/**
	 * 활성 페이지가 바뀔 때 전역 액션 핸들러를 갈아 끼운다.
	 *
	 * @param activeEditor 활성 페이지의 내장 에디터 (Preview 탭이면 null)
	 */
	@Override
	public void setActivePage(IEditorPart activeEditor) {
		IActionBars bars = getActionBars();
		if (bars == null) {
			return;
		}
		ITextEditor textEditor = activeEditor instanceof ITextEditor te ? te : null;
		for (String id : GLOBAL_ACTIONS) {
			IAction action = textEditor == null ? null : textEditor.getAction(id);
			bars.setGlobalActionHandler(id, action);
		}
		bars.updateActionBars();
	}
}
