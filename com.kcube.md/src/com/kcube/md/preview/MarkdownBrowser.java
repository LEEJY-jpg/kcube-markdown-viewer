package com.kcube.md.preview;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.md.util.JsUtils;

/**
 * JS 뷰어(web/viewer.html)를 SWT Browser 에 로드하고 Markdown 텍스트를 전달하는 래퍼.
 * <p>
 * 페이지 로딩이 끝나기 전의 렌더링 요청은 마지막 한 건만 보관했다가 로딩 완료 후 실행한다.
 */
public class MarkdownBrowser {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(MarkdownBrowser.class);

	/** 번들 안의 JS 뷰어 페이지 경로 */
	private static final String VIEWER_PATH = "web/viewer.html";

	/** 내부 SWT 브라우저 */
	private final Browser _browser;

	/** 뷰어 페이지 로딩 완료 여부 */
	private boolean _ready;

	/** 로딩 완료 전에 요청된 Markdown 원문 */
	private String _markdown = "";

	/** 로딩 완료 전에 요청된 이미지 기준 경로 */
	private String _baseUri = "";

	/**
	 * 브라우저를 만들고 뷰어 페이지 로딩을 시작한다.
	 *
	 * @param parent 부모 컴포짓
	 */
	public MarkdownBrowser(Composite parent) {
		_browser = new Browser(parent, SWT.NONE);
		_browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				_ready = true;
				apply();
			}
		});
		_browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				handleNavigation(event);
			}

			@Override
			public void changed(LocationEvent event) {
				// nothing to do
			}
		});
		loadViewer();
	}

	/**
	 * 렌더링할 Markdown 을 전달한다. 페이지가 준비되지 않았으면 준비된 뒤 반영한다.
	 *
	 * @param markdown Markdown 원문
	 * @param baseUri  md 파일 폴더 URI (상대 경로 이미지 보정용, 없으면 빈 문자열)
	 */
	public void render(String markdown, String baseUri) {
		_markdown = markdown == null ? "" : markdown;
		_baseUri = baseUri == null ? "" : baseUri;
		apply();
	}

	/**
	 * 미리보기 내용을 비운다.
	 */
	public void clear() {
		render("", "");
	}

	/**
	 * @return 브라우저 컨트롤
	 */
	public Control getControl() {
		return _browser;
	}

	/**
	 * 브라우저에 포커스를 준다.
	 */
	public void setFocus() {
		_browser.setFocus();
	}

	/**
	 * 보관 중인 Markdown 을 JS 의 renderMarkdown() 으로 전달한다.
	 */
	private void apply() {
		if (!_ready || _browser.isDisposed()) {
			return;
		}
		String script = "renderMarkdown(" + JsUtils.toJsString(_markdown) + "," + JsUtils.toJsString(_baseUri) + ");";
		if (!_browser.execute(script) && _log.isWarnEnabled()) {
			_log.warn("Failed to execute renderMarkdown script");
		}
	}

	/**
	 * 번들의 viewer.html 을 로컬 파일 URL 로 풀어 브라우저에 로드한다.
	 */
	private void loadViewer() {
		try {
			Bundle bundle = FrameworkUtil.getBundle(MarkdownBrowser.class);
			URL entry = bundle.getEntry(VIEWER_PATH);
			URL fileUrl = FileLocator.toFileURL(entry);
			_browser.setUrl(URIUtil.toURI(fileUrl).toString());
		} catch (Exception e) {
			if (_log.isErrorEnabled()) {
				_log.error("Failed to load viewer page", e);
			}
		}
	}

	/**
	 * 뷰어 페이지를 벗어나는 이동을 막고, 외부 http(s) 링크는 시스템 브라우저로 연다.
	 *
	 * @param event 이동 이벤트
	 */
	private void handleNavigation(LocationEvent event) {
		String location = event.location;
		if (!_ready || location == null || location.startsWith("about:") || location.contains("viewer.html")) {
			return;
		}
		event.doit = false;
		if (location.startsWith("http://") || location.startsWith("https://")) {
			Program.launch(location);
		}
	}
}
