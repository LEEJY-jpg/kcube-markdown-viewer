package com.kcube.md.source;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

/**
 * Source 탭 문법 색 토큰 모음. 밝은/어두운 배경용 색 두 벌을 두고, 테마가 바뀌면 같은 토큰 객체의 색만 바꾼다
 * (스캐너가 토큰을 그대로 쥐고 있으므로 다시 그리기만 하면 된다).
 */
public class MarkdownColors {

	/** 헤딩 */
	final Token heading = new Token(null);

	/** 인용 */
	final Token quote = new Token(null);

	/** 목록 기호·구분선 */
	final Token marker = new Token(null);

	/** 인라인 코드·펜스 코드 블록 */
	final Token code = new Token(null);

	/** 굵게 */
	final Token strong = new Token(null);

	/** 기울임 */
	final Token emphasis = new Token(null);

	/** 취소선 */
	final Token strike = new Token(null);

	/** 링크·이미지 */
	final Token link = new Token(null);

	/** HTML 주석 */
	final Token comment = new Token(null);

	/**
	 * @param dark 어두운 배경이면 true
	 */
	public MarkdownColors(boolean dark) {
		update(dark);
	}

	/**
	 * 배경에 맞는 색으로 토큰을 바꾼다.
	 *
	 * @param dark 어두운 배경이면 true
	 */
	public final void update(boolean dark) {
		heading.setData(attr(dark ? 0x6cb6ff : 0x0b5cad, SWT.BOLD));
		quote.setData(attr(dark ? 0x8b949e : 0x6a737d, SWT.ITALIC));
		marker.setData(attr(dark ? 0xe0a050 : 0xb35900, SWT.BOLD));
		code.setData(attr(dark ? 0x7ee787 : 0x116329, SWT.NORMAL));
		strong.setData(new TextAttribute(null, null, SWT.BOLD));
		emphasis.setData(new TextAttribute(null, null, SWT.ITALIC));
		strike.setData(new TextAttribute(null, null, TextAttribute.STRIKETHROUGH));
		link.setData(attr(dark ? 0x58a6ff : 0x0969da, SWT.NORMAL));
		comment.setData(attr(dark ? 0x7d8590 : 0x8a8a8a, SWT.ITALIC));
	}

	/**
	 * @param rgb   0xRRGGBB 글자색
	 * @param style SWT 글꼴 스타일
	 * @return 글자 속성
	 */
	private static TextAttribute attr(int rgb, int style) {
		return new TextAttribute(new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff), null, style);
	}
}
