package com.kcube.md.preview;

import java.io.File;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preview 에서 클릭한 링크를 연다.
 * <ul>
 * <li>http(s)/mailto: 시스템 기본 프로그램(브라우저, 메일)으로 연다.</li>
 * <li>file: Eclipse 에디터로 연다. 워크스페이스 안의 파일은 {@link IFile} 로, 밖의 파일은 파일 스토어로 연다.
 * {@code #L10} 같은 조각이 있으면 해당 줄로 이동한다.</li>
 * </ul>
 */
public final class LinkOpener {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(LinkOpener.class);

	/** 줄 번호 조각 (#L10, #L10-L20, #10) */
	private static final Pattern LINE_FRAGMENT = Pattern.compile("^L?(\\d+)(?:-L?\\d+)?$");

	private LinkOpener() {
	}

	/**
	 * 링크를 연다. UI 스레드에서 호출해야 한다.
	 *
	 * @param shell 오류 메시지를 띄울 셸
	 * @param url   절대 URL (http, https, mailto, file)
	 */
	public static void open(Shell shell, String url) {
		if (url == null || url.isBlank()) {
			return;
		}
		String lower = url.toLowerCase(Locale.ROOT);
		if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:")) {
			Program.launch(url);
			return;
		}
		if (!lower.startsWith("file:")) {
			if (_log.isInfoEnabled()) {
				_log.info("Ignored link with unsupported scheme: {}", url);
			}
			return;
		}
		try {
			URI uri = new URI(url);
			File file = new File(new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null));
			if (!file.isFile()) {
				MessageDialog.openInformation(shell, "Markdown Preview", "File not found:\n" + file.getPath());
				return;
			}
			IEditorPart editor = openEditor(file);
			gotoLine(editor, uri.getFragment());
		} catch (Exception e) {
			if (_log.isErrorEnabled()) {
				_log.error("Failed to open link {}", url, e);
			}
			MessageDialog.openError(shell, "Markdown Preview", "Cannot open link:\n" + url + "\n\n" + e.getMessage());
		}
	}

	/**
	 * 파일을 기본 에디터로 연다. 워크스페이스에 연결된 파일이면 워크스페이스 리소스로 연다.
	 *
	 * @param file 로컬 파일
	 * @return 열린 에디터 (활성 페이지가 없으면 null)
	 * @throws Exception 에디터를 열 수 없을 때
	 */
	private static IEditorPart openEditor(File file) throws Exception {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window == null ? null : window.getActivePage();
		if (page == null) {
			return null;
		}
		URI location = file.toURI();
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(location);
		for (IFile candidate : files) {
			if (candidate.isAccessible()) {
				return IDE.openEditor(page, candidate);
			}
		}
		return IDE.openEditorOnFileStore(page, EFS.getStore(location));
	}

	/**
	 * 줄 번호 조각(#L10 등)이 있으면 텍스트 에디터에서 해당 줄을 선택한다.
	 *
	 * @param editor   열린 에디터 (null 허용)
	 * @param fragment URI 조각 (null 허용)
	 */
	private static void gotoLine(IEditorPart editor, String fragment) {
		if (editor == null || fragment == null) {
			return;
		}
		Matcher m = LINE_FRAGMENT.matcher(fragment);
		ITextEditor text = editor.getAdapter(ITextEditor.class);
		if (!m.matches() || text == null) {
			return;
		}
		IDocument doc = text.getDocumentProvider().getDocument(text.getEditorInput());
		if (doc == null) {
			return;
		}
		try {
			int line = Math.min(Math.max(Integer.parseInt(m.group(1)), 1), doc.getNumberOfLines()) - 1;
			IRegion region = doc.getLineInformation(line);
			text.selectAndReveal(region.getOffset(), region.getLength());
		} catch (BadLocationException | NumberFormatException e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Invalid line fragment {}", fragment, e);
			}
		}
	}
}
