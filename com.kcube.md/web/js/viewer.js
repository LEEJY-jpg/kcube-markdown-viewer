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
	if (lang === 'mermaid' && window.mermaid) {
		return '<pre class="kc-mermaid" data-source="' + escapeHtml(str) + '"></pre>';
	}
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
 * Eclipse 텍스트 에디터(Source 탭)와 동일한 글꼴을 본문 전체에 적용한다.
 * @param {string} family 글꼴 이름
 * @param {number} sizePx 글꼴 크기(px)
 */
function applyFont(family, sizePx) {
	var style = document.getElementById('eclipse-font');
	if (!style) {
		style = document.createElement('style');
		style.id = 'eclipse-font';
		document.head.appendChild(style);
	}
	var face = '"' + family.replace(/["\\]/g, '') + '", monospace';
	style.textContent = '.MarkDownViewer, .MarkDownViewer * { font-family: ' + face + ' !important; }'
		+ '.MarkDownViewer { font-size: ' + sizePx + 'px !important; }'
		+ '.MarkDownViewer .kc-mermaid, .MarkDownViewer .kc-mermaid * { font-family: "trebuchet ms", verdana, arial, sans-serif !important; }'
		+ '.MarkDownViewer pre, .MarkDownViewer code { font-size: 1em !important; }'
		+ '.MarkDownViewer h1 { font-size: 1.6em !important; }'
		+ '.MarkDownViewer h2 { font-size: 1.4em !important; }'
		+ '.MarkDownViewer h3 { font-size: 1.25em !important; }'
		+ '.MarkDownViewer h4 { font-size: 1.1em !important; }';
}

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
	renderMermaidBlocks();
}

/** mermaid 렌더링 결과(SVG) 캐시. 키는 테마 + 원문이며 같은 원문은 다시 그리지 않는다. */
var mermaidCache = {};
/** 캐시에 저장된 항목 수 (한도를 넘으면 캐시를 비운다) */
var mermaidCacheSize = 0;
/** 블록 순서별 직전 SVG. 재렌더 중 높이가 줄어 스크롤이 튀는 것을 막는 임시 표시용 */
var mermaidLastSvg = [];
/** 렌더링 세대 번호. 더 새로운 렌더링이 시작되면 이전 비동기 작업은 결과를 버린다. */
var mermaidSeq = 0;
/** mermaid 렌더링 요소 ID 생성용 카운터 */
var mermaidId = 0;
/** mermaid 에 마지막으로 초기화한 테마 (없으면 빈 문자열) */
var mermaidTheme = '';
/** 캐시 최대 항목 수 */
var MERMAID_CACHE_LIMIT = 100;

/**
 * 본문 배경이 어두운지 판단한다. mermaid 테마(dark/default) 선택에 사용한다.
 * @returns {boolean} 어두운 배경이면 true
 */
function isDarkBackground() {
	var m = /rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(getComputedStyle(document.body).backgroundColor);
	return !!m && (0.299 * m[1] + 0.587 * m[2] + 0.114 * m[3]) < 128;
}

/**
 * 본문의 mermaid 코드 블록을 SVG 다이어그램으로 바꾼다.
 * 캐시된 블록은 즉시(동기) 채우고, 나머지는 비동기로 그린다. 문법 오류는 해당 블록에만 표시한다.
 * @returns {Promise<void>} 모든 블록 처리가 끝나면 이행되는 Promise
 */
async function renderMermaidBlocks() {
	var blocks = content.querySelectorAll('pre.kc-mermaid');
	if (!blocks.length || !window.mermaid) {
		mermaidLastSvg = [];
		return;
	}
	var theme = isDarkBackground() ? 'dark' : 'default';
	if (theme !== mermaidTheme) {
		mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', suppressErrorRendering: true, theme: theme });
		mermaidTheme = theme;
	}
	var seq = ++mermaidSeq;
	mermaidLastSvg.length = blocks.length;

	var pending = [];
	for (var i = 0; i < blocks.length; i++) {
		var cached = mermaidCache[theme + '\n' + blocks[i].getAttribute('data-source')];
		if (cached !== undefined) {
			blocks[i].innerHTML = cached;
			mermaidLastSvg[i] = cached;
		} else {
			if (mermaidLastSvg[i]) {
				blocks[i].innerHTML = mermaidLastSvg[i];
			}
			pending.push(i);
		}
	}

	for (var j = 0; j < pending.length; j++) {
		var index = pending[j];
		var block = blocks[index];
		var source = block.getAttribute('data-source');
		var id = 'kc-mermaid-' + (++mermaidId);
		var html;
		try {
			html = (await mermaid.render(id, source)).svg;
			if (mermaidCacheSize >= MERMAID_CACHE_LIMIT) {
				mermaidCache = {};
				mermaidCacheSize = 0;
			}
			mermaidCache[theme + '\n' + source] = html;
			mermaidCacheSize++;
			mermaidLastSvg[index] = html;
		} catch (err) {
			var leftover = document.getElementById('d' + id);
			if (leftover) {
				leftover.remove();
			}
			html = '<div class="kc-mermaid-error">' + escapeHtml((err && err.message) || err) + '</div>';
		}
		if (seq !== mermaidSeq) {
			return;
		}
		block.innerHTML = html;
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
