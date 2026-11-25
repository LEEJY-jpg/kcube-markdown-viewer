package com.kcube.md.source;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * Source 탭 텍스트 뷰어 설정. 기본 텍스트 에디터 설정(맞춤법 검사, 하이퍼링크 등)은 그대로 두고 Markdown 문법 색만 더한다.
 */
public class MarkdownSourceViewerConfiguration extends TextSourceViewerConfiguration {

	/** 문법 색 토큰 */
	private final MarkdownColors _colors;

	/**
	 * @param store  에디터 설정 저장소
	 * @param colors 문법 색 토큰
	 */
	public MarkdownSourceViewerConfiguration(IPreferenceStore store, MarkdownColors colors) {
		super(store);
		_colors = colors;
	}

	/**
	 * @param sourceViewer 소스 뷰어
	 * @return 이 플러그인 전용 파티셔닝 이름
	 */
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return MarkdownPartitions.PARTITIONING;
	}

	/**
	 * @param sourceViewer 소스 뷰어
	 * @return 본문·펜스 코드·HTML 주석 파티션 종류
	 */
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, MarkdownPartitions.CODE, MarkdownPartitions.COMMENT };
	}

	/**
	 * 파티션마다 색을 칠하는 리컨사일러를 만든다. 펜스 코드와 주석은 파티션 전체를 한 색으로 칠한다.
	 *
	 * @param sourceViewer 소스 뷰어
	 * @return 프레젠테이션 리컨사일러
	 */
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(MarkdownPartitions.PARTITIONING);
		add(reconciler, IDocument.DEFAULT_CONTENT_TYPE, new MarkdownScanner(_colors));
		add(reconciler, MarkdownPartitions.CODE, singleToken(_colors.code));
		add(reconciler, MarkdownPartitions.COMMENT, singleToken(_colors.comment));
		return reconciler;
	}

	/**
	 * @param reconciler  리컨사일러
	 * @param contentType 파티션 종류
	 * @param scanner     그 파티션의 토큰 스캐너
	 */
	private static void add(PresentationReconciler reconciler, String contentType, ITokenScanner scanner) {
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, contentType);
		reconciler.setRepairer(dr, contentType);
	}

	/**
	 * @param token 칠할 토큰
	 * @return 범위 전체를 한 토큰으로 돌려주는 스캐너
	 */
	private static RuleBasedScanner singleToken(IToken token) {
		RuleBasedScanner scanner = new RuleBasedScanner();
		scanner.setDefaultReturnToken(token);
		return scanner;
	}
}
