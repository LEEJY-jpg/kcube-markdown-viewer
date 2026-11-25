package com.kcube.md.source;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * 줄 맨 앞에서만 의미가 있는 블록 문법을 잡는다.
 * <ul>
 * <li>ATX 헤딩 (<code># 제목</code>) — 줄 전체</li>
 * <li>구분선·Setext 밑줄 (<code>---</code>, <code>***</code>, <code>___</code>, <code>===</code>) — 줄 전체</li>
 * <li>인용 (<code>&gt; 글</code>) — 줄 전체</li>
 * <li>목록 기호 (<code>-</code>, <code>*</code>, <code>+</code>, <code>1.</code>, <code>1)</code>) — 기호까지만</li>
 * </ul>
 */
class LineStartRule implements IRule {

	/** 헤딩 최대 레벨 */
	private static final int MAX_HEADING = 6;

	/** 순서 목록 번호 최대 자릿수 */
	private static final int MAX_DIGITS = 9;

	/** 헤딩 토큰 */
	private final IToken _heading;

	/** 인용 토큰 */
	private final IToken _quote;

	/** 목록 기호·구분선 토큰 */
	private final IToken _marker;

	/**
	 * @param heading 헤딩 토큰
	 * @param quote   인용 토큰
	 * @param marker  목록 기호·구분선 토큰
	 */
	LineStartRule(IToken heading, IToken quote, IToken marker) {
		_heading = heading;
		_quote = quote;
		_marker = marker;
	}

	/**
	 * @param scanner 스캐너
	 * @return 잡은 문법의 토큰, 해당 없으면 {@link Token#UNDEFINED}
	 */
	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		if (scanner.getColumn() != 0) {
			return Token.UNDEFINED;
		}
		int read = ScannerUtil.readRun(scanner, ' ', Integer.MAX_VALUE);
		int c = scanner.read();
		read++;
		if (c == '#') {
			int run = 1 + ScannerUtil.readRun(scanner, '#', MAX_HEADING);
			read += run - 1;
			int next = scanner.read();
			scanner.unread();
			if (run <= MAX_HEADING && isBlankOrEnd(next)) {
				ScannerUtil.readToLineEnd(scanner);
				return _heading;
			}
		} else if (c == '>') {
			ScannerUtil.readToLineEnd(scanner);
			return _quote;
		} else if (c == '-' || c == '*' || c == '_' || c == '=' || c == '+') {
			int lineRead = readRestOfLine(scanner);
			if (isRuleLine(c, lineRead, scanner)) {
				return _marker;
			}
			ScannerUtil.unread(scanner, lineRead);
			int next = scanner.read();
			scanner.unread();
			if (c != '_' && c != '=' && isBlankOrEnd(next)) {
				return _marker;
			}
		} else if (c >= '0' && c <= '9') {
			int digits = 1;
			int d;
			while (digits < MAX_DIGITS && (d = scanner.read()) >= '0' && d <= '9') {
				digits++;
			}
			if (digits < MAX_DIGITS) {
				scanner.unread();
			}
			read += digits - 1;
			int delim = scanner.read();
			read++;
			int next = scanner.read();
			scanner.unread();
			if ((delim == '.' || delim == ')') && isBlankOrEnd(next)) {
				return _marker;
			}
		}
		ScannerUtil.unread(scanner, read);
		return Token.UNDEFINED;
	}

	/**
	 * 줄 끝까지 읽어 둔다 (구분선 판정용). 읽은 글자는 {@link #isRuleLine} 이 판정한 뒤 필요하면 되돌린다.
	 *
	 * @param scanner 스캐너
	 * @return 읽은 글자 수
	 */
	private static int readRestOfLine(ICharacterScanner scanner) {
		return ScannerUtil.readToLineEnd(scanner);
	}

	/**
	 * 방금 읽은 줄 나머지가 구분선(같은 글자 3개 이상과 공백만)인지 본다. 스캐너 위치는 줄 끝 그대로 둔다.
	 *
	 * @param first    줄의 첫 글자
	 * @param lineRead 첫 글자 뒤로 읽은 글자 수
	 * @param scanner  스캐너
	 * @return 구분선 줄이면 true
	 */
	private static boolean isRuleLine(int first, int lineRead, ICharacterScanner scanner) {
		if (first == '+') {
			return false;
		}
		ScannerUtil.unread(scanner, lineRead);
		int count = 1;
		boolean ok = true;
		for (int i = 0; i < lineRead; i++) {
			int c = scanner.read();
			if (c == first) {
				count++;
			} else if (c != ' ' && c != '\t') {
				ok = false;
			}
		}
		return ok && (count >= 3 || first == '=');
	}

	/**
	 * @param c 글자
	 * @return 공백, 탭, 줄바꿈 또는 문서 끝이면 true
	 */
	private static boolean isBlankOrEnd(int c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ICharacterScanner.EOF;
	}
}
