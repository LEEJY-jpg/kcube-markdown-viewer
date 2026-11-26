package com.kcube.md.preview;

import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.md.util.EditorTheme;
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

	/** Windows 에서 IE 로 동작할 때 뷰어 대신 보여 주는 Edge 전환 안내 페이지 (IE 에서도 보여야 하므로 정적 HTML) */
	private static final String IE_MODE_GUIDE_HTML = """
			<!DOCTYPE html>
			<html>
			<head><meta charset="UTF-8"></head>
			<body style="font-family: 'Segoe UI', 'Malgun Gothic', sans-serif; font-size: 14px; padding: 16px;">
			<h3>브라우저를 Edge 로 변경해 주세요</h3>
			<p>현재 <b>Internet Explorer(IE) 모드</b>로 동작하고 있어 Markdown 미리보기를 표시할 수 없습니다.
			Edge 로 전환하려면 <b>Microsoft Edge WebView2 Runtime</b> 이 필요합니다.</p>
			<ol>
			<li>아래 주소에서 WebView2 Runtime(Evergreen)을 설치합니다. 인터넷이 안 되는 PC 는 Standalone Installer 를 사용합니다.<br>
			https://developer.microsoft.com/microsoft-edge/webview2/</li>
			<li>Eclipse 를 다시 시작합니다.</li>
			</ol>
			<p>이미 설치했는데도 이 안내가 보이면 Error Log 뷰를 확인해 주세요. Source 탭은 그대로 사용할 수 있습니다.</p>
			<hr>
			<h3>Please switch the browser to Edge</h3>
			<p>The Preview is running in <b>Internet Explorer (IE) mode</b>, which cannot display Markdown.
			Switching to Edge requires the <b>Microsoft Edge WebView2 Runtime</b>.</p>
			<ol>
			<li>Install the Evergreen WebView2 Runtime from the address above
			(use the Standalone Installer on a PC without internet access).</li>
			<li>Restart Eclipse.</li>
			</ol>
			<p>If it is already installed, check the Error Log view. The Source tab works as usual.</p>
			</body>
			</html>
			""";

	/** 내부 SWT 브라우저 */
	private final Browser _browser;

	/** 뷰어 페이지 로딩 완료 여부 */
	private boolean _ready;

	/** 로딩 완료 전에 요청된 Markdown 원문 */
	private String _markdown = "";

	/** 로딩 완료 전에 요청된 이미지 기준 경로 */
	private String _baseUri = "";

	/** 마지막으로 브라우저에 전달한 Markdown 원문 (전달한 적 없으면 null) */
	private String _appliedMarkdown;

	/** 마지막으로 브라우저에 전달한 이미지 기준 경로 (전달한 적 없으면 null) */
	private String _appliedBaseUri;

	/** 마지막으로 적용한 글꼴·테마 스크립트 (적용한 적 없으면 null) */
	private String _appliedStyleScript;

	/** 로딩 완료 전에 요청된 스크롤 위치 (원본 줄 번호, 없으면 -1) */
	private double _pendingLine = -1;

	/** Eclipse 텍스트 글꼴이 바뀌면 미리보기 글꼴도 다시 적용하는 리스너 */
	private final IPropertyChangeListener _fontListener = event -> onPropertyChange(event.getProperty());

	/** 에디터 배경색 설정(테마 전환 포함)이 바뀌면 미리보기 테마를 다시 적용하는 리스너 */
	private final IPropertyChangeListener _themeListener = event -> onEditorPreferenceChange(event.getProperty());

	/**
	 * 브라우저를 만들고 뷰어 페이지 로딩을 시작한다.
	 *
	 * @param parent 부모 컴포짓
	 */
	public MarkdownBrowser(Composite parent) {
		_browser = createBrowser(parent);
		_browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				String url = _browser.getUrl();
				if (url != null && url.contains("viewer.html")) {
					// 페이지가 (다시) 로드되면 본문이 비어 있으므로 전달 이력을 버린다
					_appliedMarkdown = null;
					_appliedBaseUri = null;
					_appliedStyleScript = null;
					_ready = true;
					apply();
					if (_pendingLine >= 0) {
						scrollToLine(_pendingLine);
						_pendingLine = -1;
					}
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
		createLinkFunction();
		JFaceResources.getFontRegistry().addListener(_fontListener);
		EditorsUI.getPreferenceStore().addPropertyChangeListener(_themeListener);
		_browser.addDisposeListener(e -> {
			JFaceResources.getFontRegistry().removeListener(_fontListener);
			EditorsUI.getPreferenceStore().removePropertyChangeListener(_themeListener);
		});
		if (isWindows() && !"edge".equals(_browser.getBrowserType())) {
			// IE 에서는 뷰어가 동작하지 않는다. _ready 가 false 로 남으므로 이후의 JS 호출은 모두 생략된다
			if (_log.isWarnEnabled()) {
				_log.warn("Preview is disabled because the browser engine is {}", _browser.getBrowserType());
			}
			_browser.setText(IE_MODE_GUIDE_HTML);
		} else {
			loadViewer();
		}
	}

	/**
	 * 내부 브라우저를 만든다. Windows 의 SWT 기본 엔진은 Eclipse 버전에 따라 IE 이므로 Edge(WebView2) 를 먼저 요청한다.
	 * <p>
	 * Edge 를 만들지 못하면 안내 페이지를 보여 줄 수 있도록 기본 엔진으로 다시 만든다.
	 *
	 * @param parent 부모 컴포짓
	 * @return 생성한 브라우저
	 */
	private static Browser createBrowser(Composite parent) {
		if (!isWindows()) {
			return new Browser(parent, SWT.NONE);
		}
		List<Control> existing = Arrays.asList(parent.getChildren());
		try {
			return new Browser(parent, SWT.EDGE);
		} catch (SWTError e) {
			if (_log.isWarnEnabled()) {
				_log.warn("Failed to create Edge (WebView2) browser", e);
			}
			// 생성에 실패해도 만들다 만 컨트롤은 부모에 남으므로 치운다
			for (Control child : parent.getChildren()) {
				if (!existing.contains(child)) {
					child.dispose();
				}
			}
			return new Browser(parent, SWT.NONE);
		}
	}

	/**
	 * @return Windows 에서 실행 중이면 true
	 */
	private static boolean isWindows() {
		return "win32".equals(SWT.getPlatform());
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
	 * 원본 줄 번호 위치로 미리보기를 스크롤한다. 헤딩 줄이면 헤딩이 맨 위에 오고, 블록 사이는 비율로 보간한다.
	 * 페이지가 준비되지 않았으면 준비된 뒤 스크롤한다.
	 *
	 * @param line 원본 줄 번호 (0 부터, 소수 허용, 음수면 무시)
	 */
	public void scrollToLine(double line) {
		if (line < 0 || Double.isNaN(line) || _browser.isDisposed()) {
			return;
		}
		if (!_ready) {
			_pendingLine = line;
			return;
		}
		apply();
		if (!_browser.execute("scrollToLine(" + line + ");") && _log.isWarnEnabled()) {
			_log.warn("Failed to execute scrollToLine script");
		}
	}

	/**
	 * 미리보기 맨 위에 보이는 위치를 원본 줄 번호로 구한다.
	 *
	 * @return 원본 줄 번호 (0 부터, 소수 포함). 페이지가 준비되지 않았거나 실패하면 -1
	 */
	public double getTopLine() {
		if (!_ready || _browser.isDisposed()) {
			return -1;
		}
		try {
			return _browser.evaluate("return getTopLine();") instanceof Number n ? n.doubleValue() : -1;
		} catch (SWTException e) {
			if (_log.isWarnEnabled()) {
				_log.warn("Failed to evaluate getTopLine script", e);
			}
			return -1;
		}
	}

	/**
	 * 보관 중인 Markdown 을 JS 의 renderMarkdown() 으로 전달한다.
	 * <p>
	 * 내용·기준 경로·글꼴·테마가 마지막 전달과 같으면 브라우저 호출을 생략한다.
	 * 테마가 바뀌어도 다시 렌더링하는 것은 mermaid 다이어그램을 새 테마로 그리기 위해서다.
	 */
	private void apply() {
		if (!_ready || _browser.isDisposed()) {
			return;
		}
		String style = fontScript() + themeScript();
		if (_markdown.equals(_appliedMarkdown) && _baseUri.equals(_appliedBaseUri) && style.equals(_appliedStyleScript)) {
			return;
		}
		String script = style + "renderMarkdown(" + JsUtils.toJsString(_markdown) + "," + JsUtils.toJsString(_baseUri) + ");";
		if (!_browser.execute(script)) {
			if (_log.isWarnEnabled()) {
				_log.warn("Failed to execute renderMarkdown script");
			}
			return;
		}
		_appliedMarkdown = _markdown;
		_appliedBaseUri = _baseUri;
		_appliedStyleScript = style;
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
	 * 에디터 배경색 관련 설정 변경을 처리한다. Eclipse 테마를 바꾸면 이 설정이 바뀐다.
	 *
	 * @param property 변경된 설정 키
	 */
	private void onEditorPreferenceChange(String property) {
		if (EditorTheme.isBackgroundProperty(property) && !_browser.isDisposed()) {
			_browser.getDisplay().asyncExec(this::apply);
		}
	}

	/**
	 * Source 탭(텍스트 에디터) 배경이 어두우면 다크 테마를, 아니면 밝은 테마를 적용하는 JS 호출문을 만든다.
	 *
	 * @return applyTheme(...) 호출문
	 */
	private String themeScript() {
		RGB bg = EditorTheme.background(_browser.getDisplay());
		boolean dark = EditorTheme.isDark(bg);
		String color = dark ? String.format("#%02x%02x%02x", bg.red, bg.green, bg.blue) : "";
		return "applyTheme(" + dark + "," + JsUtils.toJsString(color) + ");";
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
	 * JS 의 링크 클릭 처리(onLinkClick)가 호출하는 kcOpenLink(url) 함수를 등록한다.
	 * <p>
	 * BrowserFunction 은 페이지가 다시 로드돼도 유지되고 브라우저와 함께 해제된다.
	 */
	private void createLinkFunction() {
		new BrowserFunction(_browser, "kcOpenLink") {
			@Override
			public Object function(Object[] arguments) {
				if (arguments.length > 0 && arguments[0] instanceof String url) {
					openLinkLater(url);
				}
				return null;
			}
		};
	}

	/**
	 * 뷰어 페이지를 벗어나는 이동을 막는다. JS 에서 가로채지 못한 링크는 {@link LinkOpener} 로 연다.
	 *
	 * @param event 이동 이벤트
	 */
	private void handleNavigation(LocationEvent event) {
		String location = event.location;
		if (!_ready || location == null || location.startsWith("about:") || location.contains("viewer.html")) {
			return;
		}
		event.doit = false;
		openLinkLater(location);
	}

	/**
	 * 링크를 다음 UI 이벤트에서 연다. 브라우저 콜백 안에서 바로 열면 포커스·파트 전환이 겹치고,
	 * Edge(WebView2) 는 콜백이 직렬 실행되어 교착이 생길 수 있다.
	 *
	 * @param url 열 링크
	 */
	private void openLinkLater(String url) {
		Display display = _browser.getDisplay();
		display.asyncExec(() -> {
			if (!_browser.isDisposed()) {
				LinkOpener.open(_browser.getShell(), url);
			}
		});
	}
}
