package com.kcube.md.source;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * 펜스 코드 블록(<code>```</code> 또는 <code>~~~</code> 로 시작하는 줄부터 같은 글자로 같은 길이 이상인 닫는 줄까지)을
 * 하나의 파티션으로 잡는다. 닫는 줄이 없으면 문서 끝까지다.
 * <p>
 * 여는 줄은 줄 맨 앞(공백 3칸까지 허용)에서만 인정한다. 파티션 중간부터 다시 검사할 때(resume)는 여는 줄 정보를 모르므로
 * 3글자 이상인 아무 펜스 줄이나 닫는 줄로 본다.
 */
class FenceRule implements IPredicateRule {

	/** 최소 펜스 길이 */
	private static final int MIN_FENCE = 3;

	/** 펜스로 인정하는 들여쓰기 최대 칸 수 */
	private static final int MAX_INDENT = 3;

	/** 성공 시 돌려줄 토큰 (파티션 종류) */
	private final IToken _token;

	/**
	 * @param token 성공 시 돌려줄 토큰
	 */
	FenceRule(IToken token) {
		_token = token;
	}

	/**
	 * @return 성공 시 돌려줄 토큰
	 */
	@Override
	public IToken getSuccessToken() {
		return _token;
	}

	/**
	 * @param scanner 스캐너
	 * @return 펜스 코드 블록이면 성공 토큰, 아니면 {@link Token#UNDEFINED}
	 */
	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}

	/**
	 * @param scanner 스캐너
	 * @param resume  파티션 중간부터 이어서 검사하면 true
	 * @return 펜스 코드 블록이면 성공 토큰, 아니면 {@link Token#UNDEFINED}
	 */
	@Override
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		int fenceChar = 0;
		int fenceLen = MIN_FENCE;
		if (resume) {
			if (scanner.getColumn() != 0) {
				ScannerUtil.readLine(scanner);
			}
		} else {
			if (scanner.getColumn() != 0) {
				return Token.UNDEFINED;
			}
			int indent = ScannerUtil.readRun(scanner, ' ', MAX_INDENT + 1);
			int c = scanner.read();
			if (indent > MAX_INDENT || (c != '`' && c != '~')) {
				ScannerUtil.unread(scanner, indent + 1);
				return Token.UNDEFINED;
			}
			int run = 1 + ScannerUtil.readRun(scanner, c, Integer.MAX_VALUE);
			if (run < MIN_FENCE) {
				ScannerUtil.unread(scanner, indent + run);
				return Token.UNDEFINED;
			}
			fenceChar = c;
			fenceLen = run;
			ScannerUtil.readLine(scanner);
		}
		while (true) {
			int c = scanner.read();
			scanner.unread();
			if (c == ICharacterScanner.EOF) {
				return _token;
			}
			boolean closing = isClosingFence(scanner, fenceChar, fenceLen);
			ScannerUtil.readLine(scanner);
			if (closing) {
				return _token;
			}
		}
	}

	/**
	 * 현재 줄(줄 맨 앞에 있다고 가정)이 닫는 펜스인지 본다. 스캐너 위치는 그대로 둔다.
	 *
	 * @param scanner   스캐너
	 * @param fenceChar 여는 펜스 글자 (모르면 0: <code>`</code>, <code>~</code> 둘 다 허용)
	 * @param fenceLen  여는 펜스 길이
	 * @return 닫는 펜스 줄이면 true
	 */
	private static boolean isClosingFence(ICharacterScanner scanner, int fenceChar, int fenceLen) {
		int indent = ScannerUtil.readRun(scanner, ' ', MAX_INDENT + 1);
		int c = scanner.read();
		int read = indent + 1;
		boolean closing = false;
		if (indent <= MAX_INDENT && (fenceChar == 0 ? c == '`' || c == '~' : c == fenceChar)) {
			int run = 1 + ScannerUtil.readRun(scanner, c, Integer.MAX_VALUE);
			read += run - 1;
			if (run >= fenceLen) {
				int spaces = ScannerUtil.readRun(scanner, ' ', Integer.MAX_VALUE);
				read += spaces;
				int next = scanner.read();
				read++;
				closing = next == '\n' || next == '\r' || next == ICharacterScanner.EOF;
			}
		}
		ScannerUtil.unread(scanner, read);
		return closing;
	}
}
