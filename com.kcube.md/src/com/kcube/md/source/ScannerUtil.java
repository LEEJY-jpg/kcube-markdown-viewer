package com.kcube.md.source;

import org.eclipse.jface.text.rules.ICharacterScanner;

/**
 * 규칙 구현에서 쓰는 {@link ICharacterScanner} 도우미. 읽은 글자 수를 세어 두었다가 실패하면 그만큼 되돌린다.
 */
final class ScannerUtil {

	private ScannerUtil() {
	}

	/**
	 * 읽은 글자를 모두 되돌린다.
	 *
	 * @param scanner 스캐너
	 * @param count   되돌릴 글자 수
	 */
	static void unread(ICharacterScanner scanner, int count) {
		for (int i = 0; i < count; i++) {
			scanner.unread();
		}
	}

	/**
	 * 줄 끝 문자 직전까지 읽는다 (줄바꿈은 읽지 않는다).
	 *
	 * @param scanner 스캐너
	 * @return 읽은 글자 수
	 */
	static int readToLineEnd(ICharacterScanner scanner) {
		int count = 0;
		int c;
		while ((c = scanner.read()) != ICharacterScanner.EOF && c != '\n' && c != '\r') {
			count++;
		}
		scanner.unread();
		return count;
	}

	/**
	 * 줄바꿈까지 포함해 한 줄을 끝까지 읽는다 (CRLF 는 한 번에).
	 *
	 * @param scanner 스캐너
	 * @return 읽은 글자 수
	 */
	static int readLine(ICharacterScanner scanner) {
		int count = readToLineEnd(scanner);
		int c = scanner.read();
		if (c == ICharacterScanner.EOF) {
			scanner.unread();
			return count;
		}
		count++;
		if (c == '\r') {
			if (scanner.read() == '\n') {
				count++;
			} else {
				scanner.unread();
			}
		}
		return count;
	}

	/**
	 * 같은 글자가 연속된 만큼 읽는다.
	 *
	 * @param scanner 스캐너
	 * @param ch      글자
	 * @param max     최대 글자 수
	 * @return 읽은 글자 수
	 */
	static int readRun(ICharacterScanner scanner, int ch, int max) {
		int count = 0;
		while (count < max && scanner.read() == ch) {
			count++;
		}
		if (count < max) {
			scanner.unread();
		}
		return count;
	}
}
