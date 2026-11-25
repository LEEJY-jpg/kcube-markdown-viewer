package com.kcube.md.outline;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 헤딩 하나. 하위 헤딩을 자식으로 가진다.
 */
public final class MarkdownHeading {

	/** 헤딩 수준 (1~6) */
	private final int _level;

	/** 표시할 헤딩 텍스트 (인라인 문법 제거) */
	private final String _text;

	/** 원본 줄 번호 (0 부터) */
	private final int _line;

	/** 상위 헤딩 (최상위면 null) */
	private MarkdownHeading _parent;

	/** 하위 헤딩 목록 */
	private final List<MarkdownHeading> _children = new ArrayList<>();

	/**
	 * @param level 헤딩 수준 (1~6)
	 * @param text  표시할 텍스트
	 * @param line  원본 줄 번호 (0 부터)
	 */
	public MarkdownHeading(int level, String text, int line) {
		_level = level;
		_text = text;
		_line = line;
	}

	/**
	 * @return 헤딩 수준 (1~6)
	 */
	public int getLevel() {
		return _level;
	}

	/**
	 * @return 표시할 헤딩 텍스트
	 */
	public String getText() {
		return _text;
	}

	/**
	 * @return 원본 줄 번호 (0 부터)
	 */
	public int getLine() {
		return _line;
	}

	/**
	 * @return 상위 헤딩 (최상위면 null)
	 */
	public MarkdownHeading getParent() {
		return _parent;
	}

	/**
	 * @return 하위 헤딩 목록
	 */
	public List<MarkdownHeading> getChildren() {
		return _children;
	}

	/**
	 * 하위 헤딩을 추가한다.
	 *
	 * @param child 하위 헤딩
	 */
	void addChild(MarkdownHeading child) {
		child._parent = this;
		_children.add(child);
	}

	/**
	 * @return 디버깅용 문자열
	 */
	@Override
	public String toString() {
		return "H" + _level + "@" + _line + " " + _text;
	}
}
