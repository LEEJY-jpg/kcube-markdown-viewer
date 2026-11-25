package com.kcube.md.source;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;

/**
 * 기본 파티션(펜스 코드·주석 밖의 본문)의 Markdown 문법을 토큰으로 나눈다.
 * <p>
 * 규칙 순서가 우선순위다: 줄머리 블록 문법 → 역슬래시 이스케이프 → 인라인 코드 → 링크 → 굵게 → 취소선 → 기울임.
 * 밑줄 한 개(<code>_글_</code>) 기울임은 snake_case 같은 이름에 잘못 걸리기 쉬워 잡지 않는다.
 */
public class MarkdownScanner extends RuleBasedScanner {

	/**
	 * @param colors 문법 색 토큰
	 */
	public MarkdownScanner(MarkdownColors colors) {
		setRules(new IRule[] {
				new LineStartRule(colors.heading, colors.quote, colors.marker),
				new EscapeRule(),
				new InlineRule("`", "`", colors.code, false),
				new LinkRule(colors.link),
				new InlineRule("<http", ">", colors.link, false),
				new InlineRule("**", "**", colors.strong, true),
				new InlineRule("__", "__", colors.strong, true),
				new InlineRule("~~", "~~", colors.strike, true),
				new InlineRule("*", "*", colors.emphasis, true)
		});
	}
}
