package com.kcube.md.outline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 원문에서 헤딩(ATX {@code # 제목}, Setext {@code ===}/{@code ---} 밑줄)을 찾아 트리로 만든다.
 * <p>
 * 렌더링은 JS(markdown-it)가 하므로 여기서는 Outline 표시용으로 가볍게 해석한다. 펜스 코드 블록 안은 건너뛴다.
 * 줄 번호는 markdown-it 토큰의 map[0] 과 같은 기준(0 부터)이라 Preview 의 data-line 과 맞는다.
 */
public final class HeadingParser {

	/** ATX 헤딩: 들여쓰기 3칸 이하, # 1~6 개 뒤 공백 또는 줄 끝 */
	private static final Pattern ATX = Pattern.compile("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?[ \\t]*$");

	/** ATX 헤딩 끝의 닫는 # (앞에 공백 필요) */
	private static final Pattern ATX_CLOSING = Pattern.compile("(?:^|[ \\t]+)#+$");

	/** Setext 밑줄 */
	private static final Pattern SETEXT = Pattern.compile("^ {0,3}(=+|-+)[ \\t]*$");

	/** 펜스 코드 블록 시작 (``` 또는 ~~~ 3개 이상) */
	private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");

	/** 문단이 아닌 블록 시작 (인용, 목록, 표, HTML) */
	private static final Pattern NON_PARAGRAPH = Pattern.compile("^ {0,3}(?:>|[-*+][ \\t]|\\d{1,9}[.)][ \\t]|\\||<)");

	/** 인라인 링크/이미지: [텍스트](url) → 텍스트 */
	private static final Pattern LINK = Pattern.compile("!?\\[([^\\]]*)\\]\\([^)]*\\)");

	/** 강조·코드 표시 문자 */
	private static final Pattern EMPHASIS = Pattern.compile("(\\*\\*|__|\\*|`|~~)");

	private HeadingParser() {
	}

	/**
	 * 원문에서 헤딩 트리를 만든다.
	 *
	 * @param markdown Markdown 원문 (null 허용)
	 * @return 최상위 헤딩 목록 (수준이 더 낮은 헤딩은 앞선 상위 헤딩의 자식이 된다)
	 */
	public static List<MarkdownHeading> parse(String markdown) {
		List<MarkdownHeading> roots = new ArrayList<>();
		if (markdown == null || markdown.isEmpty()) {
			return roots;
		}
		Deque<MarkdownHeading> stack = new ArrayDeque<>();
		String[] lines = markdown.split("\\r\\n|\\r|\\n", -1);
		String fence = null;
		int paragraphStart = -1;
		StringBuilder paragraph = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (fence != null) {
				if (isFenceClose(line, fence)) {
					fence = null;
				}
				continue;
			}
			Matcher m = FENCE.matcher(line);
			if (m.matches() && !(m.group(1).charAt(0) == '`' && m.group(2).contains("`"))) {
				fence = m.group(1);
				paragraphStart = -1;
				continue;
			}
			if (line.isBlank()) {
				paragraphStart = -1;
				continue;
			}
			if (paragraphStart >= 0 && SETEXT.matcher(line).matches()) {
				int level = line.trim().charAt(0) == '=' ? 1 : 2;
				add(roots, stack, new MarkdownHeading(level, clean(paragraph.toString()), paragraphStart));
				paragraphStart = -1;
				continue;
			}
			m = ATX.matcher(line);
			if (m.matches()) {
				String text = m.group(2) == null ? "" : ATX_CLOSING.matcher(m.group(2)).replaceFirst("");
				add(roots, stack, new MarkdownHeading(m.group(1).length(), clean(text), i));
				paragraphStart = -1;
				continue;
			}
			if (paragraphStart >= 0) {
				paragraph.append(' ').append(line.trim());
			} else if (!line.startsWith("    ") && !line.startsWith("\t") && !NON_PARAGRAPH.matcher(line).find()) {
				paragraphStart = i;
				paragraph.setLength(0);
				paragraph.append(line.trim());
			}
		}
		return roots;
	}

	/**
	 * 펜스 코드 블록을 닫는 줄인지 확인한다. 여는 펜스와 같은 문자로, 같거나 더 길어야 한다.
	 *
	 * @param line  현재 줄
	 * @param fence 여는 펜스 문자열
	 * @return 닫는 줄이면 true
	 */
	private static boolean isFenceClose(String line, String fence) {
		String trimmed = line.strip();
		if (trimmed.length() < fence.length() || line.length() - line.stripLeading().length() > 3) {
			return false;
		}
		char c = fence.charAt(0);
		for (int k = 0; k < trimmed.length(); k++) {
			if (trimmed.charAt(k) != c) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 헤딩을 수준에 맞는 위치에 넣는다.
	 *
	 * @param roots   최상위 헤딩 목록
	 * @param stack   현재 경로의 헤딩 스택 (맨 위가 가장 깊은 헤딩)
	 * @param heading 새 헤딩
	 */
	private static void add(List<MarkdownHeading> roots, Deque<MarkdownHeading> stack, MarkdownHeading heading) {
		while (!stack.isEmpty() && stack.peek().getLevel() >= heading.getLevel()) {
			stack.pop();
		}
		if (stack.isEmpty()) {
			roots.add(heading);
		} else {
			stack.peek().addChild(heading);
		}
		stack.push(heading);
	}

	/**
	 * 표시용으로 링크·강조·코드 표시를 걷어 낸다.
	 *
	 * @param text 헤딩 원문
	 * @return 정리된 텍스트
	 */
	private static String clean(String text) {
		String result = LINK.matcher(text).replaceAll("$1");
		result = EMPHASIS.matcher(result).replaceAll("");
		return result.trim();
	}
}
