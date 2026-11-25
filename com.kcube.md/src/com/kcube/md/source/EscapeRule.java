package com.kcube.md.source;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * 역슬래시 이스케이프(<code>\*</code>, <code>\`</code> 등)를 기본 색 토큰으로 먼저 소비해, 이스케이프된 기호가 다른 인라인
 * 규칙의 여는 기호로 잡히지 않게 한다.
 */
class EscapeRule implements IRule {

	/** 기본 색 토큰 (색 없음) */
	private final IToken _plain = new Token(null);

	/**
	 * @param scanner 스캐너
	 * @return 역슬래시와 다음 한 글자면 기본 색 토큰, 아니면 {@link Token#UNDEFINED}
	 */
	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		if (scanner.read() != '\\') {
			scanner.unread();
			return Token.UNDEFINED;
		}
		int next = scanner.read();
		if (next == ICharacterScanner.EOF || next == '\n' || next == '\r') {
			ScannerUtil.unread(scanner, 2);
			return Token.UNDEFINED;
		}
		return _plain;
	}
}
