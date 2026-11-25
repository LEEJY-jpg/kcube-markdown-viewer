/**
 * 수식(GitHub 문법)을 KaTeX 로 그리는 markdown-it 플러그인과 렌더링 함수.
 * <ul>
 * <li>인라인: <code>$x^2$</code>, <code>$`x^2`$</code> — 여는 <code>$</code> 뒤와 닫는 <code>$</code> 앞에 공백이 없고,
 * 닫는 <code>$</code> 바로 뒤가 숫자가 아니어야 한다 (<code>$5 and $10</code> 같은 금액 오인 방지)</li>
 * <li>블록: <code>$$</code> 로 시작해 <code>$$</code> 로 끝나는 줄들 (한 줄 <code>$$x$$</code> 도 가능), <code>```math</code> 코드 블록</li>
 * </ul>
 * markdown-it 단계에서는 TeX 원문을 담은 자리 표시 요소(<code>.kc-math</code>)만 만들고, {@link renderMath} 가
 * KaTeX 로 채운다. katex.min.js 는 수식이 처음 나올 때만 불러온다 (mermaid 와 같은 방식).
 */

/** KaTeX 로딩 Promise (수식이 처음 나올 때 한 번만 불러온다) */
var katexLoading = null;

/** 렌더링 결과 캐시. 키는 표시 방식 + TeX 원문 */
var mathCache = {};

/** 캐시에 저장된 항목 수 */
var mathCacheSize = 0;

/** 캐시 최대 항목 수 (넘으면 비운다) */
var MATH_CACHE_LIMIT = 500;

/**
 * markdown-it 플러그인 진입점.
 * @param {Object} md markdown-it 인스턴스
 */
function kcMath(md) {
	md.inline.ruler.after('escape', 'kc_math_inline', kcMathInline);
	md.block.ruler.before('fence', 'kc_math_block', kcMathBlock, { alt: ['paragraph', 'reference', 'blockquote', 'list'] });

	/**
	 * 인라인 수식 자리 표시 요소를 렌더링한다.
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_math_inline = function(tokens, idx) {
		return mathPlaceholder('span', tokens[idx].content, tokens[idx].meta.display, null);
	};

	/**
	 * 블록 수식 자리 표시 요소를 렌더링한다 (스크롤 동기화용 data-line 포함).
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_math_block = function(tokens, idx) {
		var map = tokens[idx].map;
		return mathPlaceholder('div', tokens[idx].content, true, map ? map[0] : null) + '\n';
	};
}

/**
 * KaTeX 로 채울 자리 표시 요소를 만든다. KaTeX 를 불러오기 전(또는 실패 시)에는 TeX 원문이 그대로 보인다.
 * @param {string} tag 태그 이름 (span, div, pre)
 * @param {string} tex TeX 원문
 * @param {boolean} display 블록(디스플레이) 수식이면 true
 * @param {?number} line 원본 줄 번호 (없으면 null)
 * @returns {string} 출력 HTML
 */
function mathPlaceholder(tag, tex, display, line) {
	return '<' + tag + ' class="kc-math' + (display ? ' kc-math-display' : '') + '"' +
		(line !== null ? ' data-line="' + line + '"' : '') +
		' data-tex="' + escapeHtml(tex) + '">' + escapeHtml(display ? tex : '$' + tex + '$') + '</' + tag + '>';
}

/**
 * @param {number} code 문자 코드
 * @returns {boolean} 공백 문자(스페이스, 탭, 줄바꿈)면 true
 */
function isMathSpace(code) {
	return code === 0x20 || code === 0x09 || code === 0x0A || code === 0x0D;
}

/**
 * 인라인 수식 <code>$...$</code>, <code>$`...`$</code>, <code>$$...$$</code>(문단 안) 를 읽는다.
 * @param {Object} state markdown-it inline 상태
 * @param {boolean} silent 검사만 하면 true
 * @returns {boolean} 수식이면 true
 */
function kcMathInline(state, silent) {
	var src = state.src;
	var start = state.pos;
	var max = state.posMax;
	if (src.charCodeAt(start) !== 0x24) {
		return false;
	}
	var open;
	var close;
	var display = false;
	var end;
	if (src.charCodeAt(start + 1) === 0x60) {
		// $`...`$
		open = start + 2;
		close = src.indexOf('`$', open);
		if (close < 0 || close >= max || close === open) {
			return false;
		}
		end = close + 2;
	} else if (src.charCodeAt(start + 1) === 0x24) {
		// 문단 안의 $$...$$ 는 디스플레이 수식으로 본다
		open = start + 2;
		close = src.indexOf('$$', open);
		if (close < 0 || close + 2 > max || !src.slice(open, close).trim()) {
			return false;
		}
		end = close + 2;
		display = true;
	} else {
		open = start + 1;
		if (open >= max || isMathSpace(src.charCodeAt(open))) {
			return false;
		}
		close = open;
		while (true) {
			close = src.indexOf('$', close);
			if (close < 0 || close >= max) {
				return false;
			}
			var backslashes = 0;
			for (var b = close - 1; b >= open && src.charCodeAt(b) === 0x5C; b--) {
				backslashes++;
			}
			var next = src.charCodeAt(close + 1);
			if (backslashes % 2 === 0 && !isMathSpace(src.charCodeAt(close - 1)) && !(next >= 0x30 && next <= 0x39)) {
				break;
			}
			close++;
		}
		if (close === open) {
			return false;
		}
		end = close + 1;
	}
	if (!silent) {
		var token = state.push('kc_math_inline', 'math', 0);
		token.content = src.slice(open, close);
		token.meta = { display: display };
		token.markup = src.slice(start, open);
	}
	state.pos = end;
	return true;
}

/**
 * 블록 수식 <code>$$ ... $$</code> 를 읽는다. 닫는 <code>$$</code> 가 없으면 수식으로 보지 않는다.
 * @param {Object} state markdown-it block 상태
 * @param {number} startLine 시작 줄
 * @param {number} endLine 끝 줄
 * @param {boolean} silent 검사만 하면 true
 * @returns {boolean} 블록 수식이면 true
 */
function kcMathBlock(state, startLine, endLine, silent) {
	if (state.sCount[startLine] - state.blkIndent >= 4) {
		return false;
	}
	var src = state.src;
	var pos = state.bMarks[startLine] + state.tShift[startLine];
	var max = state.eMarks[startLine];
	if (pos + 2 > max || src.charCodeAt(pos) !== 0x24 || src.charCodeAt(pos + 1) !== 0x24) {
		return false;
	}
	var first = src.slice(pos + 2, max).trim();
	var lines = [];
	var last = startLine;
	var found = false;
	if (first.length >= 2 && first.slice(-2) === '$$') {
		lines.push(first.slice(0, -2));
		found = true;
	} else {
		if (first) {
			lines.push(first);
		}
		for (last = startLine + 1; last < endLine; last++) {
			var p = state.bMarks[last] + state.tShift[last];
			var m = state.eMarks[last];
			if (p < m && state.sCount[last] < state.blkIndent) {
				break;
			}
			var line = src.slice(p, m).trim();
			if (line.slice(-2) === '$$') {
				lines.push(line.slice(0, -2));
				found = true;
				break;
			}
			lines.push(src.slice(p, m));
		}
	}
	if (!found) {
		return false;
	}
	if (silent) {
		return true;
	}
	state.line = last + 1;
	var token = state.push('kc_math_block', 'div', 0);
	token.block = true;
	token.content = lines.join('\n').trim();
	token.map = [startLine, state.line];
	token.markup = '$$';
	return true;
}

/**
 * katex.min.js / katex.min.css 를 한 번만 동적으로 불러온다. 실패하면 다음 호출에서 다시 시도한다.
 * @returns {Promise<void>} 로딩이 끝나면 이행되는 Promise
 */
function loadKatex() {
	if (window.katex) {
		return Promise.resolve();
	}
	if (!katexLoading) {
		var css = document.createElement('link');
		css.rel = 'stylesheet';
		css.href = 'katex/katex.min.css';
		document.head.appendChild(css);
		katexLoading = new Promise(function(resolve, reject) {
			var script = document.createElement('script');
			script.src = 'katex/katex.min.js';
			script.onload = function() {
				if (window.katex) {
					resolve();
				} else {
					katexLoading = null;
					reject(new Error('katex is not defined after loading katex/katex.min.js'));
				}
			};
			script.onerror = function() {
				katexLoading = null;
				script.remove();
				css.remove();
				reject(new Error('Failed to load katex/katex.min.js'));
			};
			document.head.appendChild(script);
		});
	}
	return katexLoading;
}

/**
 * 본문의 수식 자리 표시 요소를 KaTeX 로 채운다. KaTeX 가 이미 있으면 바로(동기) 채워 렌더링 직후 높이가 확정되고,
 * 처음이면 불러온 뒤 채운다. 문법 오류는 KaTeX 가 해당 수식만 빨간 글씨로 보여 준다.
 */
function renderMath() {
	if (!content.querySelector('.kc-math')) {
		return;
	}
	if (window.katex) {
		fillMath();
		return;
	}
	loadKatex().then(fillMath, function(err) {
		if (window.console) {
			console.error(err);
		}
	});
}

/**
 * 아직 채우지 않은 수식 요소를 KaTeX HTML 로 채운다 (같은 수식은 캐시를 쓴다).
 */
function fillMath() {
	var nodes = content.querySelectorAll('.kc-math:not(.kc-math-done)');
	for (var i = 0; i < nodes.length; i++) {
		var node = nodes[i];
		var display = node.classList.contains('kc-math-display');
		var tex = node.getAttribute('data-tex');
		var key = (display ? 'D' : 'I') + tex;
		var html = mathCache[key];
		if (html === undefined) {
			html = katex.renderToString(tex, { displayMode: display, throwOnError: false, strict: 'ignore', output: 'html' });
			if (mathCacheSize >= MATH_CACHE_LIMIT) {
				mathCache = {};
				mathCacheSize = 0;
			}
			mathCache[key] = html;
			mathCacheSize++;
		}
		node.innerHTML = html;
		node.classList.add('kc-math-done');
	}
}
