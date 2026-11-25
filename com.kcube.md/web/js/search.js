/**
 * Preview 본문 검색 (대소문자 구분 없음). 일치 텍스트를 mark 요소로 감싸 강조한다.
 */

/** 검색바 요소 */
var searchBar = document.getElementById('search-bar');
/** 검색어 입력창 */
var searchInput = document.getElementById('search-input');
/** 일치 개수 표시 */
var searchCount = document.getElementById('search-count');
/** 현재 강조된 일치 요소 목록 */
var searchHits = [];
/** 현재 선택된 일치 인덱스 */
var searchIndex = -1;
/** IME 조합 중 여부 (한글 입력 중간 상태 검색 방지) */
var searchComposing = false;
/** 검색어 입력 디바운스 지연 시간(ms) */
var SEARCH_DEBOUNCE_MS = 150;
/** 예약된 검색 디바운스 타이머 ID (없으면 0) */
var searchDebounceTimer = 0;

/**
 * 정규식 특수문자를 이스케이프한다.
 * @param {string} str 원본 문자열
 * @returns {string} 이스케이프된 문자열
 */
function escapeRegExp(str) {
	return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * 검색바를 열고 입력창에 포커스를 준다. 선택된 텍스트가 있으면 검색어로 사용한다.
 */
function openSearch() {
	var selected = String(window.getSelection ? window.getSelection() : '').trim();
	searchBar.style.display = 'flex';
	if (selected && selected.indexOf('\n') < 0) {
		searchInput.value = selected;
	}
	searchInput.focus();
	searchInput.select();
	runSearch(false, true);
}

/**
 * 검색바를 닫고 강조를 제거한다.
 */
function closeSearch() {
	cancelScheduledSearch();
	clearHighlights();
	searchBar.style.display = 'none';
}

/**
 * 기존 강조(mark)를 제거하고 텍스트 노드를 원래대로 합친다.
 */
function clearHighlights() {
	var parents = [];
	for (var i = 0; i < searchHits.length; i++) {
		var mark = searchHits[i];
		var parent = mark.parentNode;
		if (!parent) {
			continue;
		}
		parent.replaceChild(document.createTextNode(mark.textContent), mark);
		if (parents.indexOf(parent) < 0) {
			parents.push(parent);
		}
	}
	for (var j = 0; j < parents.length; j++) {
		parents[j].normalize();
	}
	searchHits = [];
	searchIndex = -1;
}

/**
 * 접힌 코드 블록 안에 있는지 확인한다. 인라인 스타일만 보므로 레이아웃을 건드리지 않는다.
 * @param {Element} el 검사할 요소
 * @returns {boolean} 접혀서 보이지 않으면 true
 */
function isCollapseHidden(el) {
	for (var node = el; node && node !== content; node = node.parentElement) {
		if (node.style && node.style.display === 'none') {
			return true;
		}
	}
	return false;
}

/**
 * 검색 대상 텍스트 노드인지 판단한다 (복사 버튼/언어 라벨/mermaid 다이어그램/수식 및 숨겨진 영역 제외).
 * @param {Text} node 텍스트 노드
 * @returns {boolean} 대상 여부
 */
function isSearchable(node) {
	var el = node.parentElement;
	if (!el || el.closest('.code-header, .kc-mermaid, .kc-math, button, script, style')) {
		return false;
	}
	return !isCollapseHidden(el);
}

/**
 * 예약된 디바운스 검색이 있으면 취소한다.
 */
function cancelScheduledSearch() {
	if (searchDebounceTimer) {
		clearTimeout(searchDebounceTimer);
		searchDebounceTimer = 0;
	}
}

/**
 * 검색어 입력에 대한 검색을 디바운스해 예약한다. 타이핑 중에는 스크롤하지 않는다.
 */
function scheduleSearch() {
	if (searchDebounceTimer) {
		clearTimeout(searchDebounceTimer);
	}
	searchDebounceTimer = setTimeout(function() {
		searchDebounceTimer = 0;
		runSearch(false, false);
	}, SEARCH_DEBOUNCE_MS);
}

/**
 * 현재 검색어로 본문을 다시 검색해 강조한다.
 * @param {boolean} keepIndex true 면 이전 선택 위치를 최대한 유지한다 (재렌더링 시)
 * @param {boolean} scroll true 면 선택된 일치 항목으로 스크롤한다
 */
function runSearch(keepIndex, scroll) {
	var previous = keepIndex ? searchIndex : 0;
	clearHighlights();
	var query = searchInput.value;
	if (!query) {
		updateCount();
		return;
	}
	var regex = new RegExp(escapeRegExp(query), 'gi');
	var walker = document.createTreeWalker(content, NodeFilter.SHOW_TEXT, null);
	var nodes = [];
	while (walker.nextNode()) {
		if (isSearchable(walker.currentNode)) {
			nodes.push(walker.currentNode);
		}
	}
	for (var i = 0; i < nodes.length; i++) {
		highlightNode(nodes[i], regex);
	}
	if (searchHits.length > 0) {
		selectHit(Math.min(Math.max(previous, 0), searchHits.length - 1), scroll);
	} else {
		updateCount();
	}
}

/**
 * 텍스트 노드 하나에서 일치 부분을 mark 로 감싼다.
 * @param {Text} node 텍스트 노드
 * @param {RegExp} regex 검색 정규식 (g, i 플래그)
 */
function highlightNode(node, regex) {
	var text = node.nodeValue;
	regex.lastIndex = 0;
	var match = regex.exec(text);
	if (!match) {
		return;
	}
	var fragment = document.createDocumentFragment();
	var last = 0;
	while (match) {
		if (match[0].length === 0) {
			regex.lastIndex++;
		} else {
			if (match.index > last) {
				fragment.appendChild(document.createTextNode(text.substring(last, match.index)));
			}
			var mark = document.createElement('mark');
			mark.className = 'kc-hit';
			mark.textContent = match[0];
			fragment.appendChild(mark);
			searchHits.push(mark);
			last = match.index + match[0].length;
		}
		match = regex.exec(text);
	}
	if (last < text.length) {
		fragment.appendChild(document.createTextNode(text.substring(last)));
	}
	node.parentNode.replaceChild(fragment, node);
}

/**
 * 지정한 일치 항목을 선택한다.
 * @param {number} index 일치 인덱스
 * @param {boolean} scroll true 가 아니면 화면 스크롤을 하지 않는다 (타이핑 중 호출용)
 */
function selectHit(index, scroll) {
	if (searchIndex >= 0 && searchHits[searchIndex]) {
		searchHits[searchIndex].className = 'kc-hit';
	}
	searchIndex = index;
	var hit = searchHits[index];
	hit.className = 'kc-hit kc-hit-active';
	if (scroll !== false) {
		hit.scrollIntoView({ block: 'center' });
	}
	updateCount();
}

/**
 * 다음/이전 일치 항목으로 이동한다 (끝에서 순환).
 * @param {number} step 1(다음) 또는 -1(이전)
 */
function moveHit(step) {
	if (searchDebounceTimer) {
		// 입력이 아직 반영되지 않았으면 먼저 검색을 끝내고 첫 일치 항목으로 이동한다
		cancelScheduledSearch();
		runSearch(false, true);
		return;
	}
	if (searchHits.length === 0) {
		return;
	}
	selectHit((searchIndex + step + searchHits.length) % searchHits.length);
}

/**
 * 일치 개수 표시를 갱신한다.
 */
function updateCount() {
	if (!searchInput.value) {
		searchCount.textContent = '';
	} else if (searchHits.length === 0) {
		searchCount.textContent = '0/0';
	} else {
		searchCount.textContent = (searchIndex + 1) + '/' + searchHits.length;
	}
}

/**
 * 재렌더링 후 검색바가 열려 있으면 검색을 다시 적용한다.
 */
function reapplySearch() {
	if (searchBar.style.display === 'flex') {
		runSearch(true, true);
	}
}

searchInput.addEventListener('compositionstart', function() { searchComposing = true; });
searchInput.addEventListener('compositionend', function() {
	searchComposing = false;
	scheduleSearch();
});
searchInput.addEventListener('input', function() {
	if (!searchComposing) {
		scheduleSearch();
	}
});
searchInput.addEventListener('keydown', function(e) {
	if (e.isComposing || e.keyCode === 229) {
		return;
	}
	if (e.key === 'Enter') {
		moveHit(e.shiftKey ? -1 : 1);
		e.preventDefault();
	} else if (e.key === 'Escape') {
		closeSearch();
		e.preventDefault();
	}
});
document.getElementById('search-prev').addEventListener('click', function() { moveHit(-1); });
document.getElementById('search-next').addEventListener('click', function() { moveHit(1); });
document.getElementById('search-close').addEventListener('click', closeSearch);
document.addEventListener('keydown', function(e) {
	if ((e.ctrlKey || e.metaKey) && (e.key === 'f' || e.key === 'F')) {
		openSearch();
		e.preventDefault();
	} else if (e.key === 'F3') {
		moveHit(e.shiftKey ? -1 : 1);
		e.preventDefault();
	}
});
