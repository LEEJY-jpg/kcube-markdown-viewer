package com.kcube.md.preview;

import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.graphics.FontData;
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

	/** Eclipse 텍스트 글꼴이 바뀌면 미리보기 글꼴도 다시 적용하는 리스너 */
	private final IPropertyChangeListener _fontListener = event -> onPropertyChange(event.getProperty());

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
				String url = _browser.getUrl();
				if (url != null && url.contains("viewer.html")) {
					_ready = true;
					apply();
				}
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
		JFaceResources.getFontRegistry().addListener(_fontListener);
		_browser.addDisposeListener(e -> JFaceResources.getFontRegistry().removeListener(_fontListener));
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
	 * 미리보기 안의 검색바를 열고 포커스를 준다.
	 */
	public void openSearch() {
		if (!_ready || _browser.isDisposed()) {
			return;
		}
		_browser.setFocus();
		if (!_browser.execute("openSearch();") && _log.isWarnEnabled()) {
			_log.warn("Failed to execute openSearch script");
		}
	}

	/**
	 * 보관 중인 Markdown 을 JS 의 renderMarkdown() 으로 전달한다.
	 */
	private void apply() {
		if (!_ready || _browser.isDisposed()) {
			return;
		}
		String script = fontScript() + "renderMarkdown(" + JsUtils.toJsString(_markdown) + "," + JsUtils.toJsString(_baseUri) + ");";
		if (!_browser.execute(script) && _log.isWarnEnabled()) {
			_log.warn("Failed to execute renderMarkdown script");
		}
	}

	/**
	 * 글꼴 레지스트리 변경을 처리한다. 텍스트 글꼴이 바뀌면 UI 스레드에서 다시 적용한다.
	 *
	 * @param property 변경된 속성 이름
	 */
	private void onPropertyChange(String property) {
		if (JFaceResources.TEXT_FONT.equals(property) && !_browser.isDisposed()) {
			_browser.getDisplay().asyncExec(this::apply);
		}
	}

	/**
	 * Source 탭(Eclipse 텍스트 글꼴)과 같은 글꼴을 적용하는 JS 호출문을 만든다.
	 *
	 * @return applyFont(...) 호출문 (글꼴 정보가 없으면 빈 문자열)
	 */
	private String fontScript() {
		FontData[] data = JFaceResources.getTextFont().getFontData();
		if (data.length == 0) {
			return "";
		}
		// SWT 글꼴 크기는 pt 이므로 화면 DPI 로 CSS px 로 환산한다 (macOS 72dpi 면 그대로).
		double px = data[0].getHeight() * _browser.getDisplay().getDPI().y / 72.0;
		return "applyFont(" + JsUtils.toJsString(data[0].getName()) + "," + px + ");";
	}

	/**
	 * 번들의 web/ 리소스를 모두 로컬로 풀고 viewer.html 을 브라우저에 로드한다.
	 * <p>
	 * jar 로 설치된 번들은 {@link FileLocator#toFileURL(URL)} 이 요청한 파일 하나만 풀기 때문에,
	 * css/js 등 하위 리소스도 각각 풀어 두어야 한다.
	 */
	private void loadViewer() {
		try {
			Bundle bundle = FrameworkUtil.getBundle(MarkdownBrowser.class);
			Enumeration<URL> entries = bundle.findEntries("web", "*", true);
			while (entries != null && entries.hasMoreElements()) {
				URL entry = entries.nextElement();
				if (!entry.getPath().endsWith("/")) {
					FileLocator.toFileURL(entry);
				}
			}
			URL fileUrl = FileLocator.toFileURL(bundle.getEntry(VIEWER_PATH));
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
