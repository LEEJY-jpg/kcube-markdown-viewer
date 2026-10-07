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
	runSearch(false);
}

/**
 * 검색바를 닫고 강조를 제거한다.
 */
function closeSearch() {
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
 * 검색 대상 텍스트 노드인지 판단한다 (복사 버튼/언어 라벨 및 숨겨진 영역 제외).
 * @param {Text} node 텍스트 노드
 * @returns {boolean} 대상 여부
 */
function isSearchable(node) {
	var el = node.parentElement;
	if (!el || el.closest('.code-header, button, script, style')) {
		return false;
	}
	return el.getClientRects().length > 0;
}

/**
 * 현재 검색어로 본문을 다시 검색해 강조한다.
 * @param {boolean} keepIndex true 면 이전 선택 위치를 최대한 유지한다 (재렌더링 시)
 */
function runSearch(keepIndex) {
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
		selectHit(Math.min(Math.max(previous, 0), searchHits.length - 1));
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
 * 지정한 일치 항목을 선택하고 화면 중앙으로 스크롤한다.
 * @param {number} index 일치 인덱스
 */
function selectHit(index) {
	if (searchIndex >= 0 && searchHits[searchIndex]) {
		searchHits[searchIndex].className = 'kc-hit';
	}
	searchIndex = index;
	var hit = searchHits[index];
	hit.className = 'kc-hit kc-hit-active';
	hit.scrollIntoView({ block: 'center' });
	updateCount();
}

/**
 * 다음/이전 일치 항목으로 이동한다 (끝에서 순환).
 * @param {number} step 1(다음) 또는 -1(이전)
 */
function moveHit(step) {
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
		runSearch(true);
	}
}

searchInput.addEventListener('compositionstart', function() { searchComposing = true; });
searchInput.addEventListener('compositionend', function() {
	searchComposing = false;
	runSearch(false);
});
searchInput.addEventListener('input', function() {
	if (!searchComposing) {
		runSearch(false);
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
