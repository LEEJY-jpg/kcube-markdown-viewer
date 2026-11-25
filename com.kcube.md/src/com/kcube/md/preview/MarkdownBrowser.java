package com.kcube.md.preview;

import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
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
		_browser = new Browser(parent, SWT.NONE);
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
		if ((AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND.equals(property)
				|| AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property)) && !_browser.isDisposed()) {
			_browser.getDisplay().asyncExec(this::apply);
		}
	}

	/**
	 * Source 탭(텍스트 에디터) 배경이 어두우면 다크 테마를, 아니면 밝은 테마를 적용하는 JS 호출문을 만든다.
	 *
	 * @return applyTheme(...) 호출문
	 */
	private String themeScript() {
		RGB bg = editorBackground();
		boolean dark = (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) < 128;
		String color = dark ? String.format("#%02x%02x%02x", bg.red, bg.green, bg.blue) : "";
		return "applyTheme(" + dark + "," + JsUtils.toJsString(color) + ");";
	}

	/**
	 * Eclipse 텍스트 에디터의 배경색을 구한다. 다크 테마는 이 설정을 어두운 색으로 바꾼다.
	 * 시스템 기본색을 쓰도록 설정돼 있으면 OS 목록 배경색(macOS 다크 모드 반영)을 쓴다.
	 *
	 * @return 에디터 배경색
	 */
	private RGB editorBackground() {
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		if (!store.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
				&& !store.getString(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND).isEmpty()) {
			return PreferenceConverter.getColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		}
		return _browser.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
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
	 * BrowserFunction 은 페이지가 다시 로드돼도 유지되고 브라우저와 함께 해제된다. 에디터 열기는 JS 콜백
	 * 안에서 하지 않고 다음 UI 이벤트로 미룬다 (콜백 중 포커스·파트 전환을 피하기 위함).
	 */
	private void createLinkFunction() {
		new BrowserFunction(_browser, "kcOpenLink") {
			@Override
			public Object function(Object[] arguments) {
				if (arguments.length > 0 && arguments[0] instanceof String url) {
					_browser.getDisplay().asyncExec(() -> {
						if (!_browser.isDisposed()) {
							LinkOpener.open(_browser.getShell(), url);
						}
					});
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
		LinkOpener.open(_browser.getShell(), location);
	}
}
