package com.kcube.md.source;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * 같은 줄 안에서 여는 기호와 닫는 기호로 감싼 인라인 문법(<code>**굵게**</code>, <code>`코드`</code> 등)을 잡는다.
 * <p>
 * {@link org.eclipse.jface.text.rules.SingleLineRule} 은 닫는 기호가 없어도 줄 끝에서 성공해 <code>2*3</code> 같은
 * 글이 줄 끝까지 칠해지므로 쓰지 않는다. 여기서는 줄 안에서 닫는 기호를 찾아야만 성공하고, 여는 기호 바로 뒤나 닫는 기호
 * 바로 앞이 공백이면 (<code>a * b *</code>) 문법으로 보지 않는다 (코드 스팬은 예외).
 */
class InlineRule implements IRule {

	/** 여는 기호 */
	private final char[] _start;

	/** 닫는 기호 */
	private final char[] _end;

	/** 성공 시 토큰 */
	private final IToken _token;

	/** 역슬래시 다음 글자를 건너뛸지 여부 (코드 스팬은 역슬래시가 이스케이프가 아니다) */
	private final boolean _escapes;

	/**
	 * @param start   여는 기호
	 * @param end     닫는 기호
	 * @param token   성공 시 토큰
	 * @param escapes 역슬래시 이스케이프를 인정하면 true
	 */
	InlineRule(String start, String end, IToken token, boolean escapes) {
		_start = start.toCharArray();
		_end = end.toCharArray();
		_token = token;
		_escapes = escapes;
	}

	/**
	 * @param scanner 스캐너
	 * @return 닫는 기호까지 찾으면 성공 토큰, 아니면 {@link Token#UNDEFINED}
	 */
	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int read = 0;
		for (char ch : _start) {
			read++;
			if (scanner.read() != ch) {
				ScannerUtil.unread(scanner, read);
				return Token.UNDEFINED;
			}
		}
		int first = scanner.read();
		scanner.unread();
		if (first == ' ' || first == '\t' || first == '\n' || first == '\r' || first == ICharacterScanner.EOF) {
			ScannerUtil.unread(scanner, read);
			return Token.UNDEFINED;
		}
		int prev = first;
		while (true) {
			int c = scanner.read();
			read++;
			if (c == ICharacterScanner.EOF || c == '\n' || c == '\r') {
				ScannerUtil.unread(scanner, read);
				return Token.UNDEFINED;
			}
			if (_escapes && c == '\\') {
				int next = scanner.read();
				read++;
				if (next == ICharacterScanner.EOF || next == '\n' || next == '\r') {
					ScannerUtil.unread(scanner, read);
					return Token.UNDEFINED;
				}
			} else if (c == _end[0] && (!_escapes || (prev != ' ' && prev != '\t')) && matchesRest(scanner)) {
				return _token;
			}
			prev = c;
		}
	}

	/**
	 * 닫는 기호의 나머지 글자가 이어지는지 본다. 맞으면 읽은 채로, 아니면 되돌린다.
	 *
	 * @param scanner 스캐너
	 * @return 닫는 기호 전체가 맞으면 true
	 */
	private boolean matchesRest(ICharacterScanner scanner) {
		for (int i = 1; i < _end.length; i++) {
			if (scanner.read() != _end[i]) {
				ScannerUtil.unread(scanner, i);
				return false;
			}
		}
		return true;
	}
}
