package com.kcube.md.source;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * Source 탭 문서를 본문 / 펜스 코드 블록 / HTML 주석 파티션으로 나눈다. 여러 줄에 걸친 문법이라 파티션으로 잡아야
 * 펜스를 입력·삭제했을 때 아래 줄 색까지 바로 다시 칠해진다.
 */
public final class MarkdownPartitions {

	/** 이 플러그인 전용 파티셔닝 이름 (다른 플러그인의 기본 파티셔닝과 겹치지 않게 따로 둔다) */
	public static final String PARTITIONING = "__kcube_markdown_partitioning";

	/** 펜스 코드 블록 파티션 */
	public static final String CODE = "__kcube_md_code";

	/** HTML 주석 파티션 */
	public static final String COMMENT = "__kcube_md_comment";

	/** 본문 외 파티션 종류 */
	static final String[] CONTENT_TYPES = { CODE, COMMENT };

	private MarkdownPartitions() {
	}

	/**
	 * 문서에 이 플러그인의 파티셔너가 없으면 붙인다. 같은 문서를 여는 에디터가 여럿이어도 한 번만 붙는다.
	 *
	 * @param document 문서 (null 허용)
	 */
	public static void install(IDocument document) {
		if (!(document instanceof IDocumentExtension3 ext) || ext.getDocumentPartitioner(PARTITIONING) != null) {
			return;
		}
		IDocumentPartitioner partitioner = new FastPartitioner(createScanner(), CONTENT_TYPES);
		ext.setDocumentPartitioner(PARTITIONING, partitioner);
		partitioner.connect(document);
	}

	/**
	 * @return 펜스 코드 블록·HTML 주석을 잡는 파티션 스캐너
	 */
	static RuleBasedPartitionScanner createScanner() {
		RuleBasedPartitionScanner scanner = new RuleBasedPartitionScanner();
		scanner.setPredicateRules(new IPredicateRule[] {
				new FenceRule(new Token(CODE)),
				new MultiLineRule("<!--", "-->", new Token(COMMENT), (char) 0, true)
		});
		return scanner;
	}
}
