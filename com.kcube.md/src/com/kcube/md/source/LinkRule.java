package com.kcube.md.source;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * 한 줄 안의 링크·이미지 문법을 잡는다: <code>[글](주소)</code>, <code>![대체글](주소)</code>, <code>[글][참조]</code>,
 * 참조 정의 <code>[참조]: 주소</code> (뒤의 주소까지).
 */
class LinkRule implements IRule {

	/** 링크 토큰 */
	private final IToken _token;

	/**
	 * @param token 링크 토큰
	 */
	LinkRule(IToken token) {
		_token = token;
	}

	/**
	 * @param scanner 스캐너
	 * @return 링크면 링크 토큰, 아니면 {@link Token#UNDEFINED}
	 */
	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int read = 0;
		int c = scanner.read();
		read++;
		if (c == '!') {
			c = scanner.read();
			read++;
		}
		if (c != '[') {
			ScannerUtil.unread(scanner, read);
			return Token.UNDEFINED;
		}
		int text = readUntil(scanner, ']');
		if (text < 0) {
			ScannerUtil.unread(scanner, read - text - 1);
			return Token.UNDEFINED;
		}
		read += text;
		int next = scanner.read();
		read++;
		int tail = -1;
		if (next == '(') {
			tail = readUntil(scanner, ')');
		} else if (next == '[') {
			tail = readUntil(scanner, ']');
		} else if (next == ':') {
			tail = ScannerUtil.readToLineEnd(scanner);
		}
		if (tail < 0) {
			ScannerUtil.unread(scanner, read - tail - 1);
			return Token.UNDEFINED;
		}
		return _token;
	}

	/**
	 * 같은 줄에서 닫는 글자까지 읽는다.
	 *
	 * @param scanner 스캐너
	 * @param close   닫는 글자
	 * @return 닫는 글자까지 포함해 읽은 글자 수. 줄 안에서 못 찾으면 <code>-(읽은 글자 수) - 1</code> (위치는 그대로 둔다)
	 */
	private static int readUntil(ICharacterScanner scanner, int close) {
		int count = 0;
		while (true) {
			int c = scanner.read();
			if (c == close) {
				return count + 1;
			}
			if (c == ICharacterScanner.EOF || c == '\n' || c == '\r') {
				scanner.unread();
				return -count - 1;
			}
			count++;
			if (c == '\\') {
				int escaped = scanner.read();
				if (escaped == ICharacterScanner.EOF || escaped == '\n' || escaped == '\r') {
					scanner.unread();
					return -count - 1;
				}
				count++;
			}
		}
	}
}
