package com.kcube.md.util;

/**
 * Java 문자열을 JavaScript 문자열 리터럴로 변환하는 유틸리티.
 */
public final class JsUtils {

	private JsUtils() {
	}

	/**
	 * Java 문자열을 따옴표로 감싼 JS 문자열 리터럴로 변환한다.
	 * <p>
	 * {@code "}, {@code \}, 개행, 제어문자, U+2028/U+2029 를 이스케이프하고
	 * {@code <} 를 {@code <} 로 치환해 {@code </script>} 주입을 막는다.
	 *
	 * @param value 변환할 문자열 (null 이면 빈 문자열로 취급)
	 * @return 따옴표를 포함한 JS 문자열 리터럴
	 */
	public static String toJsString(String value) {
		if (value == null) {
			return "\"\"";
		}
		StringBuilder sb = new StringBuilder(value.length() + 16);
		sb.append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				case '<' -> sb.append("\\u003c");
				case ' ' -> sb.append("\\u2028");
				case ' ' -> sb.append("\\u2029");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
				}
			}
		}
		return sb.append('"').toString();
	}
}
