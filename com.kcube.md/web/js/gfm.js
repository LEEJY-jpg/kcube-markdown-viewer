/**
 * markdown-it 에 GitHub(GFM) 확장 문법을 더하는 플러그인. 표·취소선·자동 링크는 markdown-it 기본 기능이고,
 * 여기서는 그 밖의 세 가지를 직접 구현한다 (오프라인 동작을 위해 외부 플러그인을 쓰지 않는다).
 * <ul>
 * <li>작업 목록: <code>- [ ] 할 일</code>, <code>- [x] 한 일</code> → 체크박스 (읽기 전용)</li>
 * <li>알림 상자: 인용 첫 줄이 <code>[!NOTE]</code>, <code>[!TIP]</code>, <code>[!IMPORTANT]</code>,
 * <code>[!WARNING]</code>, <code>[!CAUTION]</code> 이면 색이 있는 상자</li>
 * <li>각주: 본문 <code>[^이름]</code> 과 정의 <code>[^이름]: 내용</code> (4칸 들여쓴 다음 줄까지) → 문서 끝 각주 목록</li>
 * </ul>
 * viewer.js 가 <code>md.use(kcGfm)</code> 로 붙인다.
 */

/** 알림 상자 종류 → 제목 */
var KC_ALERT_TITLES = { note: 'Note', tip: 'Tip', important: 'Important', warning: 'Warning', caution: 'Caution' };

/**
 * markdown-it 플러그인 진입점.
 * @param {Object} md markdown-it 인스턴스
 */
function kcGfm(md) {
	md.block.ruler.before('reference', 'kc_footnote_def', kcFootnoteDef, { alt: ['paragraph', 'reference'] });
	md.inline.ruler.after('image', 'kc_footnote_ref', kcFootnoteRef);
	md.core.ruler.after('inline', 'kc_footnote_tail', kcFootnoteTail);
	md.core.ruler.after('inline', 'kc_task_list', kcTaskLists);
	md.core.ruler.after('inline', 'kc_alert', kcAlerts);

	/**
	 * 작업 목록 체크박스를 렌더링한다.
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_task_checkbox = function(tokens, idx) {
		return '<input type="checkbox" class="task-list-item-checkbox" disabled' + (tokens[idx].meta.checked ? ' checked' : '') + '> ';
	};

	/**
	 * 알림 상자 제목을 렌더링한다.
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_alert_title = function(tokens, idx) {
		return '<p class="markdown-alert-title">' + KC_ALERT_TITLES[tokens[idx].meta.type] + '</p>\n';
	};

	/**
	 * 본문의 각주 참조(위 첨자 번호)를 렌더링한다.
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_footnote_ref = function(tokens, idx) {
		var meta = tokens[idx].meta;
		var n = meta.id + 1;
		var refId = 'fnref-' + n + (meta.subId > 0 ? '-' + meta.subId : '');
		return '<sup class="footnote-ref"><a href="#fn-' + n + '" id="' + refId + '">[' + n + ']</a></sup>';
	};

	/**
	 * 각주 목록 시작을 렌더링한다.
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_footnote_block_open = function() {
		return '<hr class="footnotes-sep">\n<section class="footnotes">\n<ol class="footnotes-list">\n';
	};

	/**
	 * 각주 목록 끝을 렌더링한다.
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_footnote_block_close = function() {
		return '</ol>\n</section>\n';
	};

	/**
	 * 각주 항목 시작을 렌더링한다.
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_footnote_open = function(tokens, idx) {
		return '<li id="fn-' + (tokens[idx].meta.id + 1) + '" class="footnote-item">';
	};

	/**
	 * 각주 항목 끝을 렌더링한다.
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_footnote_close = function() {
		return '</li>\n';
	};

	/**
	 * 각주 내용 끝의 되돌아가기 링크(참조한 곳마다 하나)를 렌더링한다.
	 * @param {Array} tokens 토큰 목록
	 * @param {number} idx 현재 토큰 인덱스
	 * @returns {string} 출력 HTML
	 */
	md.renderer.rules.kc_footnote_anchor = function(tokens, idx) {
		var meta = tokens[idx].meta;
		var n = meta.id + 1;
		var html = '';
		for (var i = 0; i < meta.count; i++) {
			html += ' <a href="#fnref-' + n + (i > 0 ? '-' + i : '') + '" class="footnote-backref">↩︎</a>';
		}
		return html;
	};
}

/**
 * 목록 항목 첫 문단이 <code>[ ]</code> / <code>[x]</code> 로 시작하면 체크박스로 바꾼다.
 * @param {Object} state markdown-it core 상태
 */
function kcTaskLists(state) {
	var tokens = state.tokens;
	for (var i = 2; i < tokens.length; i++) {
		var inline = tokens[i];
		if (inline.type !== 'inline' || tokens[i - 1].type !== 'paragraph_open' || tokens[i - 2].type !== 'list_item_open') {
			continue;
		}
		var match = /^\[([ xX])\](?:[ \t]+|$)/.exec(inline.content);
		var first = inline.children && inline.children[0];
		if (!match || !first || first.type !== 'text' || first.content.indexOf(match[0]) !== 0) {
			continue;
		}
		first.content = first.content.substring(match[0].length);
		var box = new state.Token('kc_task_checkbox', '', 0);
		box.meta = { checked: match[1] !== ' ' };
		inline.children.unshift(box);
		var item = tokens[i - 2];
		item.attrJoin('class', 'task-list-item');
		for (var j = i - 3; j >= 0; j--) {
			var list = tokens[j];
			if (list.level === item.level - 1 && (list.type === 'bullet_list_open' || list.type === 'ordered_list_open')) {
				if ((list.attrGet('class') || '').indexOf('contains-task-list') < 0) {
					list.attrJoin('class', 'contains-task-list');
				}
				break;
			}
		}
	}
}

/**
 * 인용 첫 줄이 <code>[!NOTE]</code> 등이면 알림 상자로 바꾼다 (첫 줄은 지우고 제목을 넣는다).
 * @param {Object} state markdown-it core 상태
 */
function kcAlerts(state) {
	var tokens = state.tokens;
	for (var i = 0; i + 3 < tokens.length; i++) {
		if (tokens[i].type !== 'blockquote_open' || tokens[i + 1].type !== 'paragraph_open' || tokens[i + 2].type !== 'inline') {
			continue;
		}
		var inline = tokens[i + 2];
		var match = /^\[!(note|tip|important|warning|caution)\][ \t]*(?:\n|$)/i.exec(inline.content);
		if (!match) {
			continue;
		}
		var type = match[1].toLowerCase();
		var children = inline.children || [];
		var cut = 0;
		while (cut < children.length && children[cut].type !== 'softbreak' && children[cut].type !== 'hardbreak') {
			cut++;
		}
		inline.children = children.slice(cut + 1);
		inline.content = inline.content.substring(match[0].length);
		if (!inline.children.length) {
			tokens[i + 1].hidden = true;
			tokens[i + 3].hidden = true;
		}
		tokens[i].attrJoin('class', 'markdown-alert markdown-alert-' + type);
		var title = new state.Token('kc_alert_title', '', 0);
		title.meta = { type: type };
		tokens.splice(i + 1, 0, title);
	}
}

/**
 * 각주 정의 <code>[^이름]: 내용</code> 을 읽는다. 내용은 4칸 들여쓴 다음 줄까지 블록으로 해석한다.
 * @param {Object} state markdown-it block 상태
 * @param {number} startLine 시작 줄
 * @param {number} endLine 끝 줄
 * @param {boolean} silent 검사만 하면 true
 * @returns {boolean} 각주 정의면 true
 */
function kcFootnoteDef(state, startLine, endLine, silent) {
	var start = state.bMarks[startLine] + state.tShift[startLine];
	var max = state.eMarks[startLine];
	if (start + 4 > max || state.src.charCodeAt(start) !== 0x5B || state.src.charCodeAt(start + 1) !== 0x5E) {
		return false;
	}
	var pos;
	for (pos = start + 2; pos < max; pos++) {
		var ch = state.src.charCodeAt(pos);
		if (ch === 0x20) {
			return false;
		}
		if (ch === 0x5D) {
			break;
		}
	}
	if (pos === start + 2 || pos + 1 >= max || state.src.charCodeAt(++pos) !== 0x3A) {
		return false;
	}
	if (silent) {
		return true;
	}
	pos++;
	var footnotes = state.env.kcFootnotes || (state.env.kcFootnotes = { refs: {}, list: [] });
	var label = state.src.slice(start + 2, pos - 2);
	if (footnotes.refs[':' + label] === undefined) {
		footnotes.refs[':' + label] = -1;
	}
	var open = new state.Token('kc_footnote_reference_open', '', 1);
	open.meta = { label: label };
	open.level = state.level++;
	state.tokens.push(open);

	var oldBMark = state.bMarks[startLine];
	var oldTShift = state.tShift[startLine];
	var oldSCount = state.sCount[startLine];
	var oldParentType = state.parentType;
	var posAfterColon = pos;
	var initial = state.sCount[startLine] + pos - (state.bMarks[startLine] + state.tShift[startLine]);
	var offset = initial;
	while (pos < max) {
		var c = state.src.charCodeAt(pos);
		if (c === 0x09) {
			offset += 4 - offset % 4;
		} else if (c === 0x20) {
			offset++;
		} else {
			break;
		}
		pos++;
	}
	state.tShift[startLine] = pos - posAfterColon;
	state.sCount[startLine] = offset - initial;
	state.bMarks[startLine] = posAfterColon;
	state.blkIndent += 4;
	state.parentType = 'footnote';
	if (state.sCount[startLine] < state.blkIndent) {
		state.sCount[startLine] += state.blkIndent;
	}
	state.md.block.tokenize(state, startLine, endLine, true);
	state.parentType = oldParentType;
	state.blkIndent -= 4;
	state.tShift[startLine] = oldTShift;
	state.sCount[startLine] = oldSCount;
	state.bMarks[startLine] = oldBMark;

	var close = new state.Token('kc_footnote_reference_close', '', -1);
	close.level = --state.level;
	state.tokens.push(close);
	return true;
}

/**
 * 본문의 각주 참조 <code>[^이름]</code> 를 읽는다. 정의가 있는 이름만 인정한다.
 * @param {Object} state markdown-it inline 상태
 * @param {boolean} silent 검사만 하면 true
 * @returns {boolean} 각주 참조면 true
 */
function kcFootnoteRef(state, silent) {
	var max = state.posMax;
	var start = state.pos;
	var footnotes = state.env.kcFootnotes;
	if (!footnotes || start + 3 > max || state.src.charCodeAt(start) !== 0x5B || state.src.charCodeAt(start + 1) !== 0x5E) {
		return false;
	}
	var pos;
	for (pos = start + 2; pos < max; pos++) {
		var ch = state.src.charCodeAt(pos);
		if (ch === 0x20 || ch === 0x0A) {
			return false;
		}
		if (ch === 0x5D) {
			break;
		}
	}
	if (pos === start + 2 || pos >= max) {
		return false;
	}
	pos++;
	var label = state.src.slice(start + 2, pos - 1);
	if (footnotes.refs[':' + label] === undefined) {
		return false;
	}
	if (!silent) {
		var id = footnotes.refs[':' + label];
		if (id < 0) {
			id = footnotes.list.length;
			footnotes.list.push({ label: label, count: 0 });
			footnotes.refs[':' + label] = id;
		}
		var token = state.push('kc_footnote_ref', '', 0);
		token.meta = { id: id, subId: footnotes.list[id].count, label: label };
		footnotes.list[id].count++;
	}
	state.pos = pos;
	return true;
}

/**
 * 본문 중간의 각주 정의 토큰을 빼내 문서 끝에 참조 순서대로 각주 목록으로 붙인다. 참조되지 않은 정의는 버린다.
 * @param {Object} state markdown-it core 상태
 */
function kcFootnoteTail(state) {
	var footnotes = state.env.kcFootnotes;
	if (!footnotes) {
		return;
	}
	var defs = {};
	var current = null;
	var label = null;
	state.tokens = state.tokens.filter(function(token) {
		if (token.type === 'kc_footnote_reference_open') {
			current = [];
			label = token.meta.label;
			return false;
		}
		if (token.type === 'kc_footnote_reference_close') {
			defs[':' + label] = current;
			current = null;
			return false;
		}
		if (current) {
			current.push(token);
			return false;
		}
		return true;
	});
	if (!footnotes.list.length) {
		return;
	}
	state.tokens.push(new state.Token('kc_footnote_block_open', '', 1));
	footnotes.list.forEach(function(note, id) {
		var open = new state.Token('kc_footnote_open', '', 1);
		open.meta = { id: id };
		state.tokens.push(open);
		var body = defs[':' + note.label] || [];
		var anchor = new state.Token('kc_footnote_anchor', '', 0);
		anchor.meta = { id: id, count: note.count };
		var last = body.length ? body[body.length - 1] : null;
		if (last && last.type === 'paragraph_close') {
			Array.prototype.push.apply(state.tokens, body.slice(0, -1));
			state.tokens.push(anchor, last);
		} else {
			Array.prototype.push.apply(state.tokens, body);
			state.tokens.push(anchor);
		}
		state.tokens.push(new state.Token('kc_footnote_close', '', -1));
	});
	state.tokens.push(new state.Token('kc_footnote_block_close', '', -1));
}
