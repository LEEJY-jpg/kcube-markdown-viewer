/**
 * KCube Markdown Viewer (Eclipse 임베디드 뷰어).
 * <p>
 * 기존 MarkDownViewer(jQuery/DOMPurify 의존)를 jQuery 없이 동작하도록 옮긴 버전.
 * Java(Eclipse)가 renderMarkdown() 을 호출해 본문을 교체한다.
 */

/** 렌더링 결과를 담는 컨테이너 */
var content = document.getElementById('content');

/**
 * 문자열을 HTML 로 이스케이프한다.
 * @param {string} str 원본 문자열
 * @returns {string} 이스케이프된 문자열
 */
function escapeHtml(str) {
	return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/**
 * 코드 블록을 복사/접기 버튼이 있는 HTML 로 렌더링한다. (markdown-it highlight 콜백)
 * @param {string} str 코드 원문
 * @param {string} lang 언어 (없으면 빈 문자열)
 * @returns {string} 코드 블록 HTML
 */
function highlightCode(str, lang) {
	var label = escapeHtml(lang || 'Text');
	var body;
	if (lang && window.hljs && hljs.getLanguage(lang)) {
		body = hljs.highlight(str, { language: lang }).value;
	} else {
		body = escapeHtml(str);
	}
	return '<pre class="code-container">' +
		'<div class="code-header">' +
		'<div class="code-language-label" onclick="toggleCode(this)" data-language="' + label + '">' + label + ' ▲</div>' +
		'<button class="copy-btn" onclick="copyCode(this)">copy</button>' +
		'</div>' +
		'<code class="hljs ' + label + '">' + body + '</code>' +
		'</pre>';
}

/** markdown-it 인스턴스 (html:false — 본문 내 원시 HTML 은 렌더링하지 않는다) */
var md = window.markdownit({
	html: false,
	linkify: true,
	typographer: true,
	breaks: false,
	highlight: highlightCode
});

/**
 * 인라인 코드를 클릭하면 복사되도록 렌더링한다.
 * @param {Array} tokens 토큰 목록
 * @param {number} idx 현재 토큰 인덱스
 * @returns {string} 인라인 코드 HTML
 */
md.renderer.rules.code_inline = function(tokens, idx) {
	return '<code class="inline-code" onclick="copyInlineCode(this)">' + escapeHtml(tokens[idx].content) + '</code>';
};

/**
 * Java(Eclipse)에서 호출하는 렌더링 진입점. 본문만 교체하므로 스크롤 위치가 유지된다.
 * @param {string} markdown 원본 Markdown 텍스트
 * @param {string} baseUri md 파일이 있는 폴더 URI (상대 경로 이미지 보정용, 없으면 빈 문자열)
 */
function renderMarkdown(markdown, baseUri) {
	var scrollX = window.pageXOffset;
	var scrollY = window.pageYOffset;
	content.innerHTML = md.render(markdown || '');
	resolveImages(baseUri);
	window.scrollTo(scrollX, scrollY);
	if (typeof reapplySearch === 'function') {
		reapplySearch();
	}
}

/**
 * 상대 경로 이미지를 md 파일 기준 절대 URI 로 보정한다.
 * @param {string} baseUri md 파일이 있는 폴더 URI
 */
function resolveImages(baseUri) {
	if (!baseUri) {
		return;
	}
	var images = content.querySelectorAll('img');
	for (var i = 0; i < images.length; i++) {
		var src = images[i].getAttribute('src');
		if (src && !/^([a-z][a-z0-9+.\-]*:|\/\/|#)/i.test(src)) {
			images[i].setAttribute('src', new URL(src, baseUri).href);
		}
	}
}

/**
 * 텍스트를 클립보드로 복사한다.
 * @param {string} text 복사할 텍스트
 * @returns {boolean} 성공 여부
 */
function copyText(text) {
	var textarea = document.createElement('textarea');
	textarea.value = text;
	textarea.style.cssText = 'position:fixed;top:0;left:0;opacity:0';
	document.body.appendChild(textarea);
	textarea.select();
	var ok = false;
	try {
		ok = document.execCommand('copy');
	} catch (err) {
		console.error('Failed to copy text: ', err);
	}
	document.body.removeChild(textarea);
	return ok;
}

/**
 * 코드 블록 내용을 복사한다.
 * @param {HTMLElement} button copy 버튼
 */
function copyCode(button) {
	var code = button.closest('.code-container').querySelector('code');
	if (copyText(code.textContent)) {
		button.textContent = 'copied';
		setTimeout(function() { button.textContent = 'copy'; }, 600);
	}
}

/**
 * 코드 블록의 표시/숨김을 토글한다.
 * @param {HTMLElement} label 언어 라벨
 */
function toggleCode(label) {
	var code = label.closest('.code-container').querySelector('code');
	var hidden = code.style.display === 'none';
	code.style.display = hidden ? 'block' : 'none';
	label.textContent = label.getAttribute('data-language') + (hidden ? ' ▲' : ' ▼');
}

/**
 * 인라인 코드를 복사하고 'copied' 오버레이를 잠깐 표시한다.
 * @param {HTMLElement} element 인라인 코드 요소
 */
function copyInlineCode(element) {
	if (!copyText(element.textContent.trim())) {
		return;
	}
	var rect = element.getBoundingClientRect();
	var overlay = document.createElement('span');
	overlay.className = 'MarkDownViewer_copy-overlay';
	overlay.style.left = rect.left + 'px';
	overlay.style.top = (rect.top - 20) + 'px';
	overlay.textContent = 'copied';
	document.body.appendChild(overlay);
	setTimeout(function() { document.body.removeChild(overlay); }, 600);
}
