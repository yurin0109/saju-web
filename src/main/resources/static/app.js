/* Among the Stars 화면 스크립트
 * index.html 안에 두지 않고 별도 파일로 분리: 보안 정책(CSP)에서 인라인 스크립트를 막을 수 있게
 */
// 캐릭터 이미지가 없으면 이모지로 바꿔 보여준다 (인라인 onerror 대신)
document.addEventListener('error', e => {
  const t = e.target;
  if (t instanceof HTMLImageElement && t.dataset.emoji) t.replaceWith(document.createTextNode(t.dataset.emoji));
}, true);

/* ════════════════════════════════════════════
   설정 & 상태
   ════════════════════════════════════════════ */
const API = '/api/profiles';
const REFLECT_API = '/api/reflections';

// 오행별 캐릭터 · 색상 (img 폴더에 Figma에서 내보낸 PNG를 넣으면 이미지로 표시)
const EL = {
  WOOD:  { ko: '목', hanja: '木', name: '나무', color: 'var(--wood)',  emoji: '🦌', img: 'img/wood.svg',  char: '초록 새싹 사슴' },
  FIRE:  { ko: '화', hanja: '火', name: '불',   color: 'var(--fire)',  emoji: '🦊', img: 'img/fire.svg',  char: '코랄 불꽃 여우' },
  EARTH: { ko: '토', hanja: '土', name: '흙',   color: 'var(--earth)', emoji: '🐻', img: 'img/earth.svg', char: '꿀빛 흙곰' },
  METAL: { ko: '금', hanja: '金', name: '금속', color: 'var(--metal)', emoji: '🐯', img: 'img/metal.svg', char: '은빛 아기 호랑이' },
  WATER: { ko: '수', hanja: '水', name: '물',   color: 'var(--water)', emoji: '🦦', img: 'img/water.svg', char: '푸른 물방울 수달' },
};
const PILLAR_MEAN = { 시주: '자녀 · 노년', 일주: '나 · 배우자', 월주: '부모 · 사회', 년주: '조상 · 유년' };
const RELATIONS = [
  ['비겁', '나와 같은 기운'], ['식상', '내가 생하는 기운'], ['재성', '내가 극하는 기운'],
  ['관성', '나를 극하는 기운'], ['인성', '나를 생하는 기운'],
];

const state = {
  clientId: getClientId(),
  calendar: 'SOLAR',
  ampm: 'AM',
  concern: null,
  profiles: [],
  currentId: readStore('saju.currentId'),
  editingId: null,
  gender: null,
  reflections: [],
  listView: 'profiles',   // 내 사주 목록: profiles | journal
  qOffset: {},            // 프로필·질문 종류별 "다른 질문" 누른 횟수
  drafts: {},             // 저장 전 적던 내용 (탭을 옮겨도 유지)
  journalFilter: '',
  products: [],
  purchases: [],
  payFor: null,          // { profileId, code }
  loveView: (['love', 'marriage', 'compat', 'reunion'].includes(readStore('saju.loveView')) ? readStore('saju.loveView') : 'love'),
  partnerId: null,
  workView: (readStore('saju.workView') === 'study' ? 'study' : 'career'),
  editingReflection: null,
  confirmReflection: null,
};

/* 로그인 전에는 브라우저마다 고유 ID로 "내 사주"를 구분한다 (로그인하면 서버가 계정 열쇠로 바꿔서 처리) */
function getClientId() {
  let id = readStore('saju.clientId');
  if (!id) {
    id = (crypto.randomUUID ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
          const r = Math.random() * 16 | 0;
          return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        }));
    writeStore('saju.clientId', id);
  }
  return id;
}
function readStore(k) { try { return localStorage.getItem(k); } catch { return null; } }
function writeStore(k, v) { try { v == null ? localStorage.removeItem(k) : localStorage.setItem(k, v); } catch {} }

/* ════════════════════════════════════════════
   API 호출
   ════════════════════════════════════════════ */
async function api(path = '', options = {}) {
  return request(API + path, options);
}
async function request(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', 'X-Client-Id': state.clientId, ...(options.headers || {}) },
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    // 서버가 우리 형식의 에러를 보내지 않았다면, 상태 코드와 주소를 보여줘서 원인을 찾기 쉽게 한다
    const where = url.replace(location.origin, '').split('?')[0];
    const err = new Error(data.message || `${res.status} 오류 · ${where} · 서버를 껐다 켜고 /api/version 을 확인해주세요`);
    err.status = res.status; err.code = data.code;
    throw err;
  }
  return data;
}

async function loadShop() {
  const [products, purchases] = await Promise.all([request('/api/products'), request('/api/purchases')]);
  state.products = products; state.purchases = purchases;
}
const product = code => state.products.find(x => x.code === code);
const owns = (p, code) => !!p && p.saju.owned.includes(code);
/** 궁합은 상대 1명마다 따로 산다: 그 상대로 산 기록이 있을 때만 열림 (묶음에도 궁합은 없음) */
const ownsCompat = (p, partnerId) => !!p && (p.saju.compatPartners || []).map(String).includes(String(partnerId));
/** 묶음으로 열리는 7종 (궁합 제외) */
const BUNDLE_CODES = () => REPORT_CODES.filter(c => c !== 'COMPAT');
const won = n => n.toLocaleString('ko-KR') + '원';

async function loadReflections() {
  state.reflections = await request(REFLECT_API);
}

async function loadProfiles() {
  state.profiles = await api();
  if (state.currentId && !state.profiles.some(p => String(p.id) === String(state.currentId))) {
    setCurrent(state.profiles[0]?.id ?? null);
  }
  if (!state.currentId && state.profiles.length) setCurrent(state.profiles[0].id);
}

function current() {
  return state.profiles.find(p => String(p.id) === String(state.currentId)) || null;
}
function setCurrent(id) {
  state.currentId = id == null ? null : String(id);
  writeStore('saju.currentId', state.currentId);
}

/* ════════════════════════════════════════════
   탭 전환
   ════════════════════════════════════════════ */
function showTab(name) {
  document.querySelectorAll('.tab').forEach(t => t.setAttribute('aria-selected', t.dataset.tab === name));
  document.querySelectorAll('.panel').forEach(p => p.classList.toggle('active', p.id === 'panel-' + name));
  if (name === 'result') renderResult();
  if (name === 'year') renderYear();
  if (name === 'career') renderWork();
  if (name === 'marriage') renderLove();
  if (name === 'family') renderFamily();
  if (name === 'list') { renderList(); renderJournal(); applyListView(); }
  const hash = name === 'list' && state.listView === 'journal' ? 'journal' : name === 'marriage' ? 'love-' + state.loveView : name === 'career' ? 'work-' + state.workView : name;
  if (location.hash !== '#' + hash) history.replaceState(null, '', '#' + hash);
  window.scrollTo({ top: 0, behavior: 'instant' in window ? 'instant' : 'auto' });
}

document.addEventListener('click', e => {
  const tab = e.target.closest('[data-tab]');
  if (tab) { if (tab.dataset.tab === 'input') resetForm(); showTab(tab.dataset.tab); return; }
  const go = e.target.closest('[data-go]');
  if (go) {
    const to = go.dataset.go;
    if (to === 'input') resetForm();
    if (to === 'journal') { state.listView = 'journal'; showTab('list'); return; }
    if (to === 'list') state.listView = 'profiles';
    showTab(to);
  }
});

/* ════════════════════════════════════════════
   공통 렌더 도우미
   ════════════════════════════════════════════ */
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c]));

function avatar(code, size = '') {
  const e = EL[code];
  return `<span class="avatar ${size}" style="border-color:${e.color}" title="${e.char}">
    <img src="${e.img}" alt="${e.char}" data-emoji="${e.emoji}"></span>`;
}

function birthText(p) {
  const d = p.calendar === 'LUNAR' && p.lunarDate
    ? `음력 ${p.lunarDate.replaceAll('-', '.')}${p.leapMonth ? '(윤달)' : ''} (양력 ${p.birthDate.replaceAll('-', '.')})`
    : `양력 ${p.birthDate.replaceAll('-', '.')}`;
  let t = '시간 모름';
  if (p.birthTime) {
    const h = +p.birthTime.slice(0, 2);
    t = `${h < 12 ? '오전' : '오후'} ${h % 12 === 0 ? 12 : h % 12}:${p.birthTime.slice(3, 5)}`;
  }
  const g = p.gender === 'F' ? '여성' : '남성';
  return [d, t, p.birthPlace, g].filter(Boolean).join(' · ');
}

function emptyState(icon, title, desc) {
  return `<div class="card empty-state"><span class="big">${icon}</span><h3>${title}</h3><p>${desc}</p>
    <button class="btn btn-primary" data-go="input">사주 입력하러 가기 ↗</button></div>`;
}

function toast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg; t.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => t.classList.remove('show'), 2200);
}

/* ════════════════════════════════════════════
   탭 3. 결과 화면
   ════════════════════════════════════════════ */
function renderResult() {
  const root = document.getElementById('result-root');
  const p = current();
  if (!p) { root.innerHTML = emptyState('🌙', '아직 읽을 사주가 없어요', '출생 정보를 입력하면 나의 사주팔자와 오행 균형을 보여드려요.'); return; }

  const s = p.saju;
  const firstName = p.name.length > 2 ? p.name.slice(1) : p.name;

  const pillarsHtml = s.pillars.map(pl => {
    if (!pl.korean) return `<div class="pillar"><span class="pillar-label">${pl.label}</span>
      <div class="pillar-char empty">시간 모름</div><div class="pillar-char empty">-</div></div>`;
    // (시간을 모르면 시주는 비워두고 나머지 여섯 글자로 풀이)
    const me = pl.label === '일주';
    return `<div class="pillar"><span class="pillar-label">${pl.label}${me ? ' · 나' : ''}<em class="pillar-mean">${PILLAR_MEAN[pl.label]}</em></span>
      <div class="pillar-char el-${pl.stemElement} ${me ? 'me' : ''}">${pl.hanja[0]}<small>${pl.korean[0]} · ${EL[pl.stemElement].ko}</small></div>
      <div class="pillar-char el-${pl.branchElement}">${pl.hanja[1]}<small>${pl.korean[1]} · ${EL[pl.branchElement].ko}</small></div></div>`;
  }).join('');

  const elementsHtml = s.elements.map(el => `
    <div class="el-item">
      <div class="el-head">
        <span class="el-label">${avatar(el.code)} ${el.korean} ${el.hanja} · ${el.meaning}</span>
        <span class="el-pct">${el.percent}%</span>
      </div>
      <div class="bar"><i data-w="${el.percent}" style="background:${EL[el.code].color}"></i></div>
    </div>`).join('');

  const strong = EL[s.balance.strongest], weak = EL[s.balance.weakest];

  root.innerHTML = `
  <div style="display:flex;flex-direction:column;gap:28px">
    <div class="crumb-row"><span class="crumb">출생 정보 / 사주 결과</span><span class="tag">무료 사주 결과</span></div>

    <div class="intro">
      <span class="eyebrow">사주 결과 · 한눈에 보기</span>
      <h1 class="title">${esc(p.name)} 님의 사주, 한눈에 보기</h1>
      <p class="lead">타고난 여덟 글자와 오행 분포, 나를 나타내는 핵심 성향을 확인해보세요.</p>
    </div>
    ${concernNotice(p)}

    <div class="card profile-bar">
      <span class="profile-name">${esc(p.name)} 님 <span class="tag">나의 프로필</span></span>
      <span class="profile-info">${esc(birthText(p))}</span>
      <button class="link" data-edit="${p.id}">정보 수정 ↗</button>
    </div>

    <div class="card">
      <div class="card-head" style="margin-bottom:20px">
        <div><div class="card-title">나의 사주팔자</div><div class="notice">테두리가 강조된 글자가 나를 나타내는 일간이에요</div></div>
        <span class="card-sub">네 기둥 · 여덟 글자</span>
      </div>
      <div class="pillars">${pillarsHtml}</div>

      <details class="howto" ${readStore('saju.howto') === 'closed' ? '' : 'open'}>
        <summary>📖 사주팔자, 이렇게 읽어요</summary>
        <div class="howto-grid">
          <div class="howto-item">
            <b>① 네 기둥</b>
            <p>태어난 <b>년 · 월 · 일 · 시</b>가 각각 하나의 기둥이에요. 전통 표기에 따라 오른쪽부터 년주 → 월주 → 일주 → 시주 순서로 읽어요.</p>
          </div>
          <div class="howto-item">
            <b>② 위 글자 · 아래 글자</b>
            <p>기둥마다 위 칸은 하늘의 기운인 <b>천간</b>, 아래 칸은 땅의 기운인 <b>지지</b>예요. 그래서 4기둥 × 2글자 = <b>여덟 글자(팔자)</b>가 돼요.</p>
          </div>
          <div class="howto-item">
            <b>③ 나를 나타내는 글자</b>
            <p>분홍 테두리가 있는 <b>일주의 위 글자(일간)</b>가 바로 '나'예요. ${esc(firstName)} 님은 <b>${s.core.dayMasterHanja} ${s.core.dayMaster}${EL[s.core.dayMasterElement].ko}</b>이에요. 나머지 글자는 모두 이 글자와의 관계로 풀이해요.</p>
          </div>
          <div class="howto-item">
            <b>④ 색 = 오행</b>
            <p>칸의 색은 그 글자의 오행이에요.
              <span class="howto-dots">${['WOOD','FIRE','EARTH','METAL','WATER'].map(c => `<span><i class="el-${c}"></i>${EL[c].ko}</span>`).join('')}</span>
              오른쪽 아래 작은 글씨는 한글 읽는 법과 오행이에요.</p>
          </div>
        </div>
        <p class="howto-foot">기둥마다 보는 의미도 달라요. <b>년주</b>는 조상과 어린 시절, <b>월주</b>는 부모·형제와 사회생활, <b>일주</b>는 나와 배우자, <b>시주</b>는 자녀와 노년을 나타낸다고 봐요. 태어난 계절을 알려주는 <b>월지</b>(월주 아래 글자)는 영향력이 커서 오행 비율에 더 크게 반영했어요.</p>
      </details>
    </div>

    <div class="grid-2">
      <div class="card core">
        <div class="card-head"><span class="crumb">${esc(firstName)} 님을 읽는 한 문장</span><span style="color:var(--accent)">✦</span></div>
        <p class="core-headline">${esc(s.core.headline)}</p>
        <p class="core-desc">${esc(s.core.description)}</p>
        <div class="chips">${s.core.keywords.map(k => `<span class="chip">${esc(k)}</span>`).join('')}</div>
        <div class="core-more">
          ${(s.core.more || []).map((t, i) => `<h4>${['사람들 사이에서', '일할 때', '지칠 때'][i]}</h4><p>${esc(t)}</p>`).join('')}
        </div>
        <p class="basis">풀이 근거 · 일간 <b>${s.core.dayMasterHanja} ${s.core.dayMaster}${EL[s.core.dayMasterElement].ko}</b> (${EL[s.core.dayMasterElement].name}의 기운)</p>
      </div>
      <div class="card">
        <div class="card-head" style="margin-bottom:17px"><span class="card-title">나의 오행 균형</span><span class="card-sub">월지 가중치 반영</span></div>
        <div class="el-list">${elementsHtml}</div>
        <p class="notice" style="margin-top:17px">높고 낮음은 좋고 나쁨을 의미하지 않아요.<br>태어난 계절(월지)의 기운은 3배로 반영했어요.</p>
      </div>
    </div>

    <div class="grid-3">
      <div class="card tip-card">
        <div class="head">${avatar(s.balance.strongest, 'lg')}<div><span class="eyebrow">가장 두드러진 기운</span><h3>${strong.ko} ${strong.hanja} · ${strong.char}</h3></div></div>
        <p>${esc(s.balance.strongText)}</p>
      </div>
      <div class="card tip-card">
        <div class="head">${avatar(s.balance.weakest, 'lg')}<div><span class="eyebrow">채워주면 좋은 기운</span><h3>${weak.ko} ${weak.hanja} · ${weak.char}</h3></div></div>
        <p>${esc(s.balance.weakTip)}</p>
      </div>
      <div class="card reflect" data-reflect="core"></div>
    </div>

    ${shinsalCard(p)}

    ${concernCard(p)}

    <div class="crumb-row" style="flex-wrap:wrap;gap:12px">
      <p class="notice">사주 해석은 자기 성찰을 위한 참고이며, 미래나 특정 결과를 보장하지 않습니다.</p>
    </div>
    ${paidSection(p)}
  </div>`;

  requestAnimationFrame(() => root.querySelectorAll('.bar i').forEach(b => b.style.width = b.dataset.w + '%'));
  const howto = root.querySelector('.howto');
  howto.addEventListener('toggle', () => writeStore('saju.howto', howto.open ? 'open' : 'closed'));
  mountReflect(root.querySelector('[data-reflect]'), p, 'core');
}

/** 내 결과: 신살 · 귀인 (역마살 · 도화살 · 천을귀인 …) */
const SS_ICON = { CHEONEUL: '🤝', MUNCHANG: '📜', WOLDEOK: '🌙', YEOKMA: '🐎', DOHWA: '🌸', HWAGAE: '🎨', YANGIN: '🗡️', GOEGANG: '👑' };
function shinsalCard(p) {
  const found = p.saju.shinsal || [], guide = p.saju.shinsalGuide || [];
  const has = new Set(found.map(f => f.code));
  const nobles = found.filter(f => f.kind === 'NOBLE').length;
  const cards = found.map(f => `
    <div class="card ss-card ${f.kind === 'NOBLE' ? 'noble' : ''}">
      <div class="ss-top"><span class="ss-icon">${SS_ICON[f.code] || '✦'}</span>
        <div><h3>${esc(f.name)}<small>${esc(f.hanja)}</small></h3>
          <div class="ss-where"><span class="tag">${f.kind === 'NOBLE' ? '귀인' : '신살'}</span>${f.where.map(w => `<span class="tag">${esc(w)}</span>`).join('')}</div></div></div>
      <p class="ss-sum">${esc(f.summary)}</p>
      <p class="body">${esc(f.meaning)}</p>
      <p class="ss-tip"><b>잘 쓰는 법</b>${esc(f.tip)}</p>
    </div>`).join('');
  const rest = guide.filter(g => !has.has(g.code));
  return `
  <section class="shinsal">
    <div class="shinsal-head">
      <span class="eyebrow">신살 · 귀인</span>
      <h2 class="section-title">${esc(p.name)} 님 사주의 특별한 별</h2>
      <p class="lead">${found.length
        ? `여덟 글자 안에서 ${found.length}개의 신살을 찾았어요${nobles ? ` (그중 귀인 ${nobles}개)` : ''}. '살'이라는 이름이 붙어도 나쁜 뜻이 아니라, 기운이 강하게 드러나는 자리라는 뜻이에요.`
        : '눈에 띄게 강한 신살은 없어요. 한쪽으로 치우치지 않은 고른 사주라는 뜻이기도 해요.'}${p.birthTime ? '' : ' 태어난 시간을 모르면 시주는 빼고 봤어요.'}</p>
    </div>
    ${found.length ? `<div class="shinsal-grid">${cards}</div>` : ''}
    ${rest.length ? `<details class="ss-more"><summary>다른 신살은 어떤 뜻일까? (${rest.length}개) ▾</summary>
      <ul>${rest.map(g => `<li><b>${SS_ICON[g.code] || '✦'} ${esc(g.name)} ${esc(g.hanja)}</b>${esc(g.summary)}. ${esc(g.meaning)}</li>`).join('')}</ul></details>` : ''}
  </section>`;
}

/** 내 결과: 입력한 고민에 맞춘 풀이 + 맞는 심화 리포트 추천 */
function concernCard(p) {
  const c = p.saju.concern;
  if (!c) return `
    <div class="card concern-card" id="concern">
      <span class="eyebrow">고민 맞춤 풀이</span>
      <h3>요즘 고민이 있다면 알려주세요</h3>
      <p class="body">사주 정보를 수정해서 요즘 가장 큰 고민을 적으면, ${esc(p.name)} 님의 사주로 그 고민을 함께 들여다보고 가장 잘 맞는 리포트를 추천해드려요.</p>
      <button class="btn btn-ghost" style="align-self:flex-start" data-edit="${p.id}">고민 적으러 가기 ↗</button>
    </div>`;
  const info = REPORT_INFO[c.product], pr = product(c.product), open = owns(p, c.product);
  const [empathy, insight, approach, timing] = c.paragraphs;
  return `
    <div class="card concern-card" id="concern">
      <div class="crumb-row"><span class="eyebrow">고민 맞춤 풀이</span><span class="tag">${esc(c.label)}${c.topic && c.topic !== c.label ? ' · ' + esc(c.topic) : ''}</span></div>
      <h3>${esc(p.name)} 님의 고민, 별에게 물어봤어요</h3>
      ${c.text ? `<blockquote class="concern-quote">${esc(c.text)}</blockquote>` : ''}
      <p class="body">${esc(c.text ? empathy.replace(/^“[\s\S]*?” — /, '') : empathy)}</p>
      <div class="concern-part"><h4>🔮 사주로 보면</h4><p class="body">${esc(insight)}</p><p class="body">${esc(approach)}</p></div>
      <div class="concern-part"><h4>🗓️ 올해의 흐름</h4><p class="body">${esc(timing)}</p></div>
      ${c.steps?.length ? `<div class="concern-part"><h4>✅ 지금 해볼 수 있는 일</h4>
        <ol class="concern-steps">${c.steps.map(t => `<li>${esc(t)}</li>`).join('')}</ol></div>` : ''}
      ${c.question ? `<div class="concern-ask"><span>스스로에게 물어보기</span><p>${esc(c.question)}</p>
        <small>답이 떠오르면 위의 성찰 질문 칸이나 성찰 일기에 적어두세요.</small></div>` : ''}
      <button type="button" class="concern-miss" data-concern-miss>🙁 풀이가 내 고민과 잘 안 맞아요 · 알려주기</button>
      <div class="concern-cta">
        <p>${info.icon} ${esc(c.pitch)}</p>
        ${open ? `<button class="btn btn-ghost" data-goreport="${c.product}">심화 리포트 보기 ↗</button>`
               : c.product === 'COMPAT' ? `<button class="btn btn-primary" data-goreport="COMPAT">궁합 볼 상대 고르기 ↗</button>`
               : `<button class="btn btn-primary" data-buy="${c.product}">${pr ? won(pr.price) : ''} · ${info.name()} 심화 리포트 열기</button>`}
      </div>
    </div>`;
}
/** 결과 화면 위쪽 안내: 적은 고민의 풀이가 아래에 있다고 알려주고 바로 이동 */
function concernNotice(p) {
  if (!p.saju.concern) return '';
  return `<button type="button" class="concern-notice" data-scroll="concern">📝 적어주신 고민(${esc(p.saju.concern.topic || p.saju.concern.label)})에 대한 풀이가 이 페이지 아래에 있어요 <b>바로 보기 ↓</b></button>`;
}
document.addEventListener('click', e => {
  const s = e.target.closest('[data-scroll]');
  if (s) document.getElementById(s.dataset.scroll)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
});

/* ════════════════════════════════════════════
   탭 4. 올해 운세
   ════════════════════════════════════════════ */
function renderYear() {
  const root = document.getElementById('year-root');
  const p = current();
  if (!p) { root.innerHTML = emptyState('✨', '올해 운세를 보려면 사주가 필요해요', '출생 정보를 입력하면 올해의 기운과 나의 관계를 풀이해드려요.'); return; }

  const y = p.saju.thisYear, core = p.saju.core;
  const yEl = EL[y.yearElement], meEl = EL[core.dayMasterElement];

  root.innerHTML = `
  <div style="display:flex;flex-direction:column;gap:28px">
    <div class="crumb-row"><span class="crumb">내 사주 / ${esc(p.name)} / ${y.year}년 운세</span><span class="tag">✦ ${y.year}년 운세</span></div>
    <div class="intro">
      <span class="eyebrow">${y.year}년 · ${y.pillar}년 전반적인 운세</span>
      <h1 class="title">${y.year}년, ${esc(p.name)} 님에게 오는 흐름</h1>
      <p class="lead">올해를 이끄는 기운과 나를 나타내는 일간의 관계로 한 해의 흐름을 읽어요.</p>
    </div>

    <div class="reading">
      <article class="card reading-main">
        <div class="year-hero">
          <span class="ganji">${y.pillarHanja}</span>
          <span><span class="ganji-sub">${y.year}년 ${y.pillar}년</span><br>${avatar(y.yearElement, 'lg')}</span>
        </div>
        <div>
          <span class="chapter-label">CHAPTER · ${y.relation} (${y.relationDesc})</span>
          <h2 class="chapter-title">${esc(y.title)}</h2>
        </div>
        <div class="summary-cards">
          <div class="summary-card"><div class="k">주요 키워드</div><div class="v">${esc(y.keywords)}</div></div>
          <div class="summary-card"><div class="k">주의 포인트</div><div class="v">${esc(y.caution)}</div></div>
        </div>
        <p class="reading-body">${esc(y.body)}</p>
        <div class="action-box">
          <span style="font-size:18px">✎</span>
          <div><div class="t">올해의 실천 문구</div><div class="c">${esc(y.action)}</div></div>
        </div>
        <p class="notice">풀이는 자기 성찰을 위한 참고예요. 중요한 결정은 현실의 정보와 함께 판단해주세요.</p>
      </article>

      <aside class="side">
        <div class="card">
          <h4>✦ 풀이 근거</h4>
          <p style="font-size:14px;color:var(--body)">올해 천간 <b>${y.pillarHanja[0]}</b>은 <b>${yEl.ko}(${yEl.name})</b>, 나의 일간 <b>${core.dayMasterHanja}</b>은 <b>${meEl.ko}(${meEl.name})</b>의 기운이에요.</p>
          <div class="relation-map" style="margin-top:14px">
            ${RELATIONS.map(([n, d]) => `<div class="rel ${n === y.relation ? 'on' : ''}"><span>${n}</span><span>${d}</span></div>`).join('')}
          </div>
        </div>
        <div class="card reflect" data-reflect="year"></div>
        <button class="link" style="text-align:left" data-go="list">← 내 사주 목록으로</button>
      </aside>
    </div>
  </div>`;
  mountReflect(root.querySelector('[data-reflect]'), p, 'year');
  root.firstElementChild.insertAdjacentHTML('beforeend', premiumSlot('YEAR'));
  mountPremium(root, p, 'YEAR');
}

/* ════════════════════════════════════════════
   직장운 · 결혼운 공통
   ════════════════════════════════════════════ */
function topicFrame(p, o) {
  return `
  <div style="display:flex;flex-direction:column;gap:28px">
    <div class="crumb-row"><span class="crumb">내 사주 / ${esc(p.name)} / ${o.crumb}</span><span class="tag">${o.tag}</span></div>
    <div class="intro">
      <span class="eyebrow">${o.eyebrow}</span>
      <h1 class="title">${o.title}</h1>
      <p class="lead">${o.lead}</p>
    </div>
    <div class="reading">
      <article class="card reading-main">${o.main}
        <p class="notice">풀이는 자기 성찰을 위한 참고예요. 중요한 결정은 현실의 정보와 함께 판단해주세요.</p>
      </article>
      <aside class="side">${o.side}
        <div class="card reflect" data-reflect="${o.type}"></div>
        <button class="link" style="text-align:left" data-go="list">← 내 사주 목록으로</button>
      </aside>
    </div>
  </div>`;
}

function emptyTopic(icon, what) {
  return emptyState(icon, `${what}을 보려면 사주가 필요해요`, '출생 정보를 입력하면 사주의 십성 분포로 풀이해드려요.');
}

/* ════════════════════════════════════════════
   탭 5. 직장운
   ════════════════════════════════════════════ */
/* ════════════════════════════════════════════
   탭 5. 직장 · 학업 (직장운 · 학업운)
   ════════════════════════════════════════════ */
function renderWork() {
  const wrap = document.getElementById('career-root');
  const views = [['career', '🧭 직장운'], ['study', '📚 학업운']];
  wrap.innerHTML = `<div class="sub-tabs love-tabs" role="tablist">${views.map(([k, l]) =>
    `<button type="button" data-work="${k}" aria-selected="${state.workView === k}">${l}</button>`).join('')}</div>
    <div id="work-body" style="margin-top:28px"></div>`;
  (state.workView === 'study' ? renderStudy : renderCareer)();
}
document.addEventListener('click', e => {
  const b = e.target.closest('[data-work]');
  if (b) { state.workView = b.dataset.work; writeStore('saju.workView', state.workView); history.replaceState(null, '', '#work-' + state.workView); renderWork(); }
});

function renderStudy() {
  const root = document.getElementById('work-body');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('📚', '학업운'); return; }
  root.innerHTML = simpleFrame(p, { code: 'STUDY', crumb: '학업운', tag: '📚 학업운',
    eyebrow: '직장 · 학업 · 공부와 시험', title: `${esc(p.name)} 님의 학업운`,
    lead: '일간의 오행으로 나에게 맞는 공부 스타일을, 인성(배움)과 관성(시험)으로 시험운을 읽어요.',
    main: `<div class="topic-hero study-hero art-hero">${ART.book()}
        <span><span class="lbl">나의 공부 스타일</span><br><span class="val">${EL[p.saju.core.dayMasterElement].ko}(${EL[p.saju.core.dayMasterElement].hanja})의 공부</span></span></div>` + reportHtml(p.saju.study) });
  mountPremium(root, p, 'STUDY');
}

function renderCareer() {
  const root = document.getElementById('work-body');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('🧭', '직장운'); return; }
  const c = p.saju.career, core = p.saju.core, y = p.saju.year;

  const tg = c.tenGods.map(t => `
    <div class="tg-row ${t.name === c.dominant ? 'on' : ''}">
      <b>${t.name}</b>
      <div class="bar" title="${esc(t.desc)}"><i data-w="${t.percent}"></i></div>
      <span class="pct">${t.percent}%</span>
    </div>`).join('');

  root.innerHTML = topicFrame(p, {
    type: 'career', crumb: '직장운', tag: '🧭 직장운',
    eyebrow: `직장운 · 가장 많은 기운 ${c.dominant}`,
    title: `${esc(p.name)} 님에게 잘 맞는 일의 결`,
    lead: '사주 여덟 글자가 나(일간)와 맺는 관계, 십성의 분포로 일하는 스타일과 잘 맞는 분야를 읽어요.',
    main: `
      <div class="topic-hero career-hero">
        <span class="big">🧭</span>
        <span><span class="lbl">나의 일하는 스타일</span><br><span class="val">${esc(c.type)}</span></span>
      </div>
      <div>
        <span class="chapter-label">CHAPTER · ${c.dominant} (${esc(c.dominantDesc)})</span>
        <h2 class="chapter-title">${esc(c.type)}</h2>
      </div>
      <div class="summary-cards">
        <div class="summary-card"><div class="k">주요 키워드</div><div class="v">${esc(c.keywords)}</div></div>
        <div class="summary-card"><div class="k">주의 포인트</div><div class="v">${esc(c.caution)}</div></div>
      </div>
      <p class="reading-body">${esc(c.body)}</p>
      <div>
        <div class="k notice" style="margin-bottom:10px">잘 맞는 분야</div>
        <div class="fields">${c.fields.map(f => `<span class="chip">${esc(f)}</span>`).join('')}</div>
      </div>
      <div class="action-box">
        <span style="font-size:18px">✎</span>
        <div><div class="t">${y.year}년 일의 흐름 · ${c.thisYearRelation}</div><div class="c">${esc(c.thisYear)}</div></div>
      </div>
      <div class="action-box">
        <span style="font-size:18px">✎</span>
        <div><div class="t">이번 주 실천 문구</div><div class="c">${esc(c.action)}</div></div>
      </div>`,
    side: `
      <div class="card">
        <h4>✦ 풀이 근거 · 십성 분포</h4>
        <p class="side-note" style="margin-bottom:14px">일간 <b>${core.dayMasterHanja}</b>(${EL[core.dayMasterElement].ko}) 기준으로 나머지 글자를 셌어요. 태어난 계절(월지)은 2배로 반영했어요.</p>
        <div class="tg-list">${tg}</div>
      </div>`,
  });
  requestAnimationFrame(() => root.querySelectorAll('.bar i').forEach(b => b.style.width = b.dataset.w + '%'));
  mountReflect(root.querySelector('[data-reflect]'), p, 'career');
  root.firstElementChild.insertAdjacentHTML('beforeend', premiumSlot('CAREER'));
  mountPremium(root, p, 'CAREER');
}

/* ════════════════════════════════════════════
   탭 6. 결혼운
   ════════════════════════════════════════════ */
const LEVEL_TEXT = { NONE: '드러나지 않음', BALANCED: '알맞음', MANY: '많음' };

function renderMarriage() {
  const root = document.getElementById('love-body');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('💍', '결혼운'); return; }
  const m = p.saju.marriage, core = p.saju.core;
  const who = p.gender === 'F' ? '여성' : '남성';

  root.innerHTML = topicFrame(p, {
    type: 'marriage', crumb: '결혼운', tag: '💞 결혼운',
    eyebrow: `결혼운 · 배우자 별 ${m.starName} ${LEVEL_TEXT[m.level]}`,
    title: `${esc(p.name)} 님의 결혼과 가정의 결`,
    lead: '배우자를 나타내는 기운(배우자 별)과 배우자 자리(일지)로 어떤 배우자를 만나고, 어떤 결혼 생활을 꾸리는지 읽어요.',
    main: `
      <div class="topic-hero marriage-hero art-hero">
        ${ART.rings()}
        <span><span class="lbl">배우자 별 · ${m.starName}</span><br><span class="val">${LEVEL_TEXT[m.level]}</span></span>
        <span><span class="lbl">배우자 자리 · 일지</span><br><span class="val">${m.palaceHanja} ${m.palace}</span></span>
      </div>
      <div>
        <span class="chapter-label">CHAPTER · 결혼의 흐름</span>
        <h2 class="chapter-title">${esc(m.title)}</h2>
      </div>
      <div class="summary-cards">
        <div class="summary-card"><div class="k">주요 키워드</div><div class="v">${esc(m.keywords)}</div></div>
        <div class="summary-card"><div class="k">주의 포인트</div><div class="v">${esc(m.caution)}</div></div>
      </div>
      <p class="reading-body">${esc(m.body)}</p>
      <div class="palace">
        <div class="pillar-char el-${m.palaceElement}">${m.palaceHanja}<small>${m.palace} · ${EL[m.palaceElement].ko}</small></div>
        <p class="reading-body"><b style="color:var(--accent);font-weight:500">결혼 생활 · 배우자 자리 ${m.palaceRelation}</b><br>${esc(m.palaceText)}</p>
      </div>
      <div class="action-box">
        <span style="font-size:18px">✎</span>
        <div><div class="t">결혼을 위한 준비 한 가지</div><div class="c">${esc(m.action)}</div></div>
      </div>`,
    side: `
      <div class="card">
        <h4>✦ 풀이 근거</h4>
        <p class="side-note">전통 명리에서 ${who}의 배우자 별은 <b>${m.starName}</b>(${esc(m.starDesc)})이에요. 사주에서 ${m.starName} 점수는 <b>${m.starScore}점</b>이라 <b>${LEVEL_TEXT[m.level]}</b>으로 읽었어요.</p>
        <hr class="divider" style="margin:14px 0">
        <p class="side-note">배우자 자리인 일지 <b>${m.palaceHanja}</b>(${EL[m.palaceElement].ko})는 나의 일간 <b>${core.dayMasterHanja}</b>(${EL[core.dayMasterElement].ko})에게 <b>${m.palaceRelation}</b>이에요.</p>
        <p class="notice" style="margin-top:12px">결혼 여부나 시기를 정하는 풀이가 아니라, 결혼과 가정에서 나를 이해하기 위한 참고예요.</p>
      </div>`,
  });
  mountReflect(root.querySelector('[data-reflect]'), p, 'marriage');
  root.firstElementChild.insertAdjacentHTML('beforeend', premiumSlot('MARRIAGE'));
  mountPremium(root, p, 'MARRIAGE');
}

/* ════════════════════════════════════════════
   리포트 그리기 (무료 · 유료 공용)
   ════════════════════════════════════════════ */
/** 별점이 있는 항목 (영역별 풀이): 아이콘 · 별점 · 키워드 카드 */
const AREA_ICON = { '일 · 공부': '💼', '재물': '💰', '관계': '💞', '컨디션': '🌿' };
function scoreCards(items) {
  return `<div class="area-grid">${items.map(i => `
    <div class="area-card">
      <div class="area-top"><span class="area-icon">${AREA_ICON[i.label] || '✦'}</span>
        <div><span class="area-name">${esc(i.label)}</span>
          <span class="area-stars" aria-label="5점 만점에 ${i.score}점">${'★'.repeat(i.score)}<i>${'★'.repeat(5 - i.score)}</i></span></div></div>
      <b class="area-key">${esc(i.tag)}</b>
      <div class="area-bar"><i style="width:${i.score * 20}%"></i></div>
      <p>${esc(i.text)}</p>
    </div>`).join('')}</div>`;
}

/** 십성 분포: 막대 그래프 + 가장 많은 / 가장 적은 기운 강조 */
const TG_ICON = { 비겁: '🔥', 식상: '🎨', 재성: '💰', 관성: '🏛', 인성: '📚' };
function tenGodHtml(items) {
  const pct = i => parseInt(i.tag, 10) || 0;
  const max = Math.max(...items.map(pct)), min = Math.min(...items.map(pct));
  return `<div class="tg-cols">${items.map(i => {
    const v = pct(i), top = v === max, low = v === min;
    return `<div class="tg-col ${top ? 'top' : ''}" title="${esc(i.text)}">
      <div class="tg-head"><span>${TG_ICON[i.label] || '✦'} <b>${esc(i.label)}</b></span><span class="tg-pct">${v}%</span></div>
      <div class="tg-mini"><i style="width:${v ? Math.max(Math.round(v / max * 100), 6) : 0}%"></i></div>
      <p>${top ? '<em>가장 많음 · </em>' : low ? '<em class="dim">가장 적음 · </em>' : ''}${esc(i.text.split('.')[0])}</p>
    </div>`;
  }).join('')}</div>`;
}

/** 앞으로 3년: 연도 타임라인 (올해 강조 · 일 운 지수) */
const YEAR_MOOD = { 비겁: ['🚀', '도전 · 독립의 해'], 식상: ['🎤', '실력을 보여주는 해'], 재성: ['💰', '성과를 거두는 해'],
                    관성: ['🏆', '책임 · 승진의 해'], 인성: ['📚', '공부 · 충전의 해'] };
const STUDY_MOOD = { 비겁: ['🤝', '함께 공부하는 해'], 식상: ['🎨', '결과물을 만드는 해'], 재성: ['🎯', '실용 공부의 해'],
                     관성: ['📝', '시험 · 합격의 해'], 인성: ['📚', '배움이 쏙쏙 들어오는 해'] };
function careerYearsHtml(items, moods = YEAR_MOOD, label = '일 운') {
  return `<div class="cy-line">${items.map((i, n) => {
    const rel = (i.tag || '').split('·').pop().trim(), [emoji, mood] = moods[rel] || ['✦', ''];
    return `<div class="cy-node ${n === 0 ? 'now' : ''}">
      <div class="cy-dot">${emoji}</div>
      <span class="cy-year">${esc(i.label)}${n === 0 ? ' <em>올해</em>' : ''}</span>
      <b class="cy-mood">${mood}</b>
      <span class="cy-tag">${esc(i.tag)}</span>
      <div class="cy-score"><small>${label}</small>${[1, 2, 3, 4, 5].map(k => `<i class="${k <= i.score ? 'on' : ''}"></i>`).join('')}</div>
      <p>${esc(i.text.replace(/^[^.]*해\.\s*/, ''))}</p>
    </div>`;
  }).join('')}</div>`;
}

/** 잘 맞는 일 TOP 4: 순위 메달 + 적합도 */
function fieldsHtml(items) {
  const medal = ['🥇', '🥈', '🥉', '🏅'], match = [96, 91, 87, 83];
  return `<div class="job-grid">${items.map((i, n) => `
    <div class="job-card ${n === 0 ? 'best' : ''}">
      <div class="job-top"><span class="job-medal">${medal[n] || '✦'}</span><b>${esc(i.label)}</b></div>
      <div class="job-match"><span>적합도</span><div class="job-bar"><i style="width:${match[n] || 80}%"></i></div><strong>${match[n] || 80}%</strong></div>
      <p>${esc(i.text)}</p>
    </div>`).join('')}</div>`;
}

/** 직장 안에서의 관계: 회사 캐릭터 + 상사 · 동료 · 후배 케미 카드 */
const OFFICE_ICON = { 상사와: '👔', 동료와: '🤝', 후배와: '🌱' };
function officeHtml(s) {
  const ch = (s.lead || '').replace('회사에서의 나는 ', '');
  const [emoji, ...name] = ch.split(' ');
  return `
    <div class="office-hero"><span class="office-emoji">${esc(emoji)}</span>
      <div><span class="eyebrow">회사에서의 나는</span><b>${esc(name.join(' '))}</b></div></div>
    <div class="office-grid">${s.items.map(i => `
      <div class="office-card">
        <div class="office-top"><span>${OFFICE_ICON[i.label] || '✦'}</span><b>${esc(i.label.replace('와', ''))}</b>
          <span class="chem" aria-label="케미 ${i.score}점"><small>케미</small>${'💗'.repeat(i.score)}<i>${'🤍'.repeat(5 - i.score)}</i></span></div>
        <p class="office-say">${esc(i.tag)}</p>
        <p class="office-text">${esc(i.text)}</p>
      </div>`).join('')}</div>`;
}

/** 강점 · 약점 · 보완: 키워드는 위에 칩으로, 풀이는 줄글로 */
function swHtml(items) {
  const of = t => items.filter(i => i.tag === t);
  const chips = (t, cls, icon, title) => of(t).length ? `<div class="sw-keys ${cls}"><span>${icon} ${title}</span>${of(t).map(i => `<em>${esc(i.label)}</em>`).join('')}</div>` : '';
  const prose = (t, title) => of(t).length ? `<h4>${title}</h4>${of(t).map(i => `<p class="body">${esc(i.text)}</p>`).join('')}` : '';
  return `
    <div class="sw-top">${chips('강점', '', '💪', '강점')}${chips('약점', 'weak', '🌧', '약점')}${chips('보완', 'fill', '🌱', '보완')}</div>
    <div class="sw-prose">${prose('강점', '나의 강점')}${prose('약점', '조심하면 좋은 점')}${prose('보완', '보완하면 좋은 기운')}</div>`;
}

/** 행운 포인트: 부족한 오행 캐릭터 + 색 견본 · 나침반 · 시계 카드 */
const LUCKY_COLOR = { 초록: '#4caf73', 연두: '#a5d86b', 빨강: '#e5484d', 주황: '#f28b30', 분홍: '#ff70cf', 노랑: '#f2c94c',
  베이지: '#d9c3a0', 갈색: '#9a6b45', 흰색: '#f5f5f5', 은색: '#c0c6cf', 회색: '#8a8f98', 검정: '#1b1b1f', 남색: '#2b3a8c', 파랑: '#3b82f6' };
const LUCKY_DIR = { 동쪽: 90, 남쪽: 180, 서쪽: 270, 북쪽: 0 };
function luckyHtml(items) {
  const get = l => items.find(i => i.label === l)?.text || '';
  const weak = current()?.saju.balance.weakest, el = EL[weak];
  const colors = get('색').split('·').map(c => c.trim()).filter(Boolean);
  const dirText = get('방향'), deg = Object.entries(LUCKY_DIR).find(([k]) => dirText.includes(k))?.[1];
  const list = t => t.split(/[·,]/).map(x => x.trim()).filter(Boolean);
  return `
  <div class="lucky">
    ${el ? `<div class="lucky-hero">${avatar(weak, 'lg')}<div><span class="eyebrow">올해 채우면 좋은 기운</span>
      <b>${el.ko}(${el.hanja}) · ${el.name}의 기운</b><p>${el.char}의 기운을 곁에 두면 사주의 균형이 맞춰져요.</p></div></div>` : ''}
    <div class="lucky-grid">
      <div class="lucky-card"><span class="lk">🎨 행운의 색</span>
        <div class="swatches">${colors.map(c => `<span><i style="background:${LUCKY_COLOR[c] || 'var(--accent)'}"></i>${esc(c)}</span>`).join('')}</div></div>
      <div class="lucky-card"><span class="lk">🧭 행운의 방향</span>
        <div class="compass-row">${deg != null ? `<div class="compass"><b style="transform:rotate(${deg}deg)"></b><em>N</em></div>` : '<div class="compass center"><b></b></div>'}
          <strong>${esc(dirText)}</strong></div></div>
      <div class="lucky-card"><span class="lk">⏰ 행운의 시간</span><strong class="big">${esc(get('시간'))}</strong>
        <p>중요한 연락이나 결정은 이 시간대에 해보세요.</p></div>
      <div class="lucky-card wide"><span class="lk">🌱 기운을 채우는 활동</span>
        <div class="chips">${list(get('활동')).map(t => `<span class="chip">${esc(t)}</span>`).join('')}</div></div>
      <div class="lucky-card wide"><span class="lk">🎁 곁에 두면 좋은 물건</span>
        <div class="chips">${list(get('물건')).map(t => `<span class="chip">${esc(t)}</span>`).join('')}</div></div>
    </div>
  </div>`;
}

/** 한 줄 목록: ☐ 는 실제로 체크할 수 있는 체크리스트(이 브라우저에 저장), 숫자는 단계 번호 */
function simpleList(r, s) {
  const key = `saju.check.${current()?.id}.${r.code}.${s.title}`;
  let done = [];
  try { const v = JSON.parse(readStore(key) || '[]'); done = Array.isArray(v) ? v : (v.done || []); } catch {}
  if (s.items.every(i => i.label === '☐')) {
    // 성찰 일기에서 모아 볼 수 있게 항목 문구도 함께 저장
    try { writeStore(key, JSON.stringify({ pid: current()?.id, code: r.code, title: s.title, items: s.items.map(i => i.text), done,
      at: (JSON.parse(readStore(key) || '{}').at) || null })); } catch {}
  }
  const check = s.items.every(i => i.label === '☐');
  return `<ul class="rep-items simple ${check ? 'checklist' : ''}" ${check ? `data-check-key="${esc(key)}"` : ''}>${s.items.map((i, n) => check
    ? `<li><label class="ck"><input type="checkbox" data-n="${n}" ${done.includes(n) ? 'checked' : ''}><span class="box"></span><p>${esc(i.text)}</p></label></li>`
    : `<li><span class="mark ${/^\d$/.test(i.label) ? 'num' : ''}">${esc(i.label)}</span><p>${esc(i.text)}</p></li>`).join('')}</ul>
    ${check ? `<p class="notice ck-count">${done.length} / ${s.items.length} 완료 · 체크한 내용은 이 기기에 저장돼요</p>` : ''}`;
}
document.addEventListener('change', e => {
  const box = e.target.closest('.checklist input[type=checkbox]');
  if (!box) return;
  const ul = box.closest('.checklist');
  const done = [...ul.querySelectorAll('input:checked')].map(x => +x.dataset.n);
  saveChecks(ul.dataset.checkKey, done);
  if (ul.closest('.practice')) { document.getElementById('practice-box').innerHTML = practiceCard(); return; }
  const cnt = ul.nextElementSibling;
  if (cnt?.classList.contains('ck-count')) cnt.textContent = `${done.length} / ${ul.querySelectorAll('input').length} 완료 · 체크한 내용은 이 기기에 저장돼요`;
});

/* ── 실천 체크 저장 · 모아 보기 ── */
function readChecks() {
  const out = [];
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (!k.startsWith('saju.check.')) continue;
      const v = JSON.parse(localStorage.getItem(k));
      if (v && !Array.isArray(v) && v.items) out.push({ key: k, ...v });
    }
  } catch {}
  return out;
}
function saveChecks(key, done) {
  let v = {};
  try { v = JSON.parse(readStore(key) || '{}'); } catch {}
  if (Array.isArray(v)) v = {};
  writeStore(key, JSON.stringify({ ...v, done, at: new Date().toISOString() }));
}

/** 성찰 일기 맨 위: 심화 리포트에서 체크한 실천 목록 */
function practiceCard() {
  const name = { MARRIAGE: '💍 결혼운', FAMILY: '🏡 가족운' };
  const list = readChecks()
    .filter(c => state.profiles.some(p => String(p.id) === String(c.pid)))
    .filter(c => !state.journalFilter || String(c.pid) === String(state.journalFilter))
    .sort((a, b) => (b.at || '').localeCompare(a.at || ''));
  if (!list.length) return '';
  const total = list.reduce((n, c) => n + c.items.length, 0), done = list.reduce((n, c) => n + c.done.length, 0);
  return `
    <article class="card practice">
      <div class="practice-head">
        <div><span class="eyebrow">나의 실천 체크</span><h3>심화 리포트에서 고른 실천들</h3></div>
        <div class="practice-total"><b>${done}</b> / ${total}<small>완료</small></div>
      </div>
      ${list.map(c => {
        const p = state.profiles.find(x => String(x.id) === String(c.pid));
        const pct = Math.round(c.done.length / c.items.length * 100);
        return `<div class="practice-group">
          <div class="pg-head"><b>${name[c.code] || c.code}</b><span class="tag">${esc(p.name)}</span>
            <span class="pg-pct">${c.done.length}/${c.items.length}</span></div>
          <div class="pg-bar"><i style="width:${pct}%"></i></div>
          <ul class="rep-items simple checklist" data-check-key="${esc(c.key)}">${c.items.map((t, n) => `
            <li><label class="ck"><input type="checkbox" data-n="${n}" ${c.done.includes(n) ? 'checked' : ''}><span class="box"></span><p>${esc(t)}</p></label></li>`).join('')}</ul>
        </div>`;
      }).join('')}
    </article>`;
}

function reportHtml(r) {
  return r.sections.map(s => `
    <section class="rep-sec">
      <h3>${esc(s.title)}</h3>
      ${s.lead && s.title !== '직장 안에서의 관계' ? `<p class="rep-lead">${esc(s.lead)}</p>` : ''}
      ${s.paragraphs.map(t => `<p class="body">${esc(t)}</p>`).join('')}
      ${s.title === '올해의 행운 포인트' ? luckyHtml(s.items) : s.title.startsWith('십성 분포') ? tenGodHtml(s.items) : s.title === '강점과 약점' ? swHtml(s.items) : s.title === '직장 안에서의 관계' ? officeHtml(s) : s.title === '앞으로 3년 일의 흐름' ? careerYearsHtml(s.items) : s.title === '앞으로 3년 학업의 흐름' ? careerYearsHtml(s.items, STUDY_MOOD, '학업 운') : s.title === '잘 맞는 일 TOP 4' || s.title === '잘 맞는 공부 분야 TOP 4' ? fieldsHtml(s.items) : s.items.some(i => i.score) ? scoreCards(s.items) : s.items.length && s.items.every(i => i.label.length <= 1) ? simpleList(r, s) : s.items.length ? `<ul class="rep-items">${s.items.map(i => `
        <li><div class="rep-item-head"><b>${esc(i.label)}</b>${i.tag ? `<span class="tag">${esc(i.tag)}</span>` : ''}</div>
        <p>${esc(i.text)}</p></li>`).join('')}</ul>` : ''}
    </section>`).join('');
}

/* ════════════════════════════════════════════
   유료 심화 리포트
   ════════════════════════════════════════════ */
const REPORT_INFO = {
  YEAR: { icon: '✨', name: () => `${current()?.saju.year.year ?? ''}년 운세`, go: ['year'],
          preview: ['올해 총론 심화', '이번 달부터 12개월 월별 흐름', '분기별 실천 플랜', '영역별 풀이 (일 · 재물 · 관계 · 컨디션)', '나의 오행으로 본 올해', '올해의 행운 포인트'] },
  CAREER: { icon: '🧭', name: () => '직장운', go: ['career', 'career'],
          preview: ['일하는 스타일 심화', '십성 분포로 본 일의 힘', '강점 · 약점 · 보완할 기운', '직장 안에서의 관계', '앞으로 3년 일의 흐름', '잘 맞는 일 TOP 4'] },
  LOVE: { icon: '💗', name: () => '연애운', go: ['marriage', 'love'],
          preview: ['한눈에 보는 연애운', '나의 연애 스타일 심화', '이성 인연의 별 · 나의 매력 포인트', '인연이 들어오기 좋은 달 TOP 3', '앞으로 3년 연애의 흐름', '잘 맞는 상대 · 조심할 점 · 연애운 체크리스트'] },
  MARRIAGE: { icon: '💍', name: () => '결혼운', go: ['marriage', 'marriage'],
          preview: ['한눈에 보는 결혼운', '나의 배우자상과 결혼 시기', '앞으로 6년 결혼의 흐름', '결혼 생활 스타일', '가정 경제와 역할 분담', '부부 갈등을 줄이는 법 · 결혼 준비 체크리스트'] },
  COMPAT: { icon: '💞', name: () => '궁합', go: ['marriage', 'compat'],
          preview: ['두 사람의 기운 심화', '생활 리듬 (배우자 자리)', '서로 채워주는 오행', '갈등을 줄이는 대화법', '함께하면 좋은 활동'] },
  REUNION: { icon: '🌙', name: () => '재회운', go: ['marriage', 'reunion'],
          preview: ['다시 이어지는 관계의 조건', '관계에서 반복되기 쉬운 패턴', '앞으로 3년 재회의 흐름', '마음 정리 가이드 5단계'] },
  STUDY: { icon: '📚', name: () => '학업운', go: ['career', 'study'],
          preview: ['한눈에 보는 학업운', '공부 스타일 심화', '나에게 맞는 공부법', '시험 · 자격증 운', '앞으로 3년 학업의 흐름', '잘 맞는 공부 분야 TOP 4 · 슬럼프 관리'] },
  FAMILY: { icon: '🏡', name: () => '가족운', go: ['family'],
          preview: ['어린 시절 (년주)', '부모 · 형제 자리 심화', '형제 · 동료의 기운', '자녀 자리 (시주)', '가족과 건강한 거리 두기', '앞으로 3년 가족의 흐름'] },
};
const REPORT_CODES = ['YEAR', 'CAREER', 'STUDY', 'LOVE', 'MARRIAGE', 'COMPAT', 'REUNION', 'FAMILY'];

/** 리포트가 있는 화면으로 이동 */
function goReport(code) {
  const [tab, sub] = REPORT_INFO[code].go;
  if (sub && tab === 'marriage') state.loveView = sub;
  if (sub && tab === 'career') state.workView = sub;
  showTab(tab);
  setTimeout(() => document.querySelector(`[data-premium="${code}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60);
}
document.addEventListener('click', e => {
  const g = e.target.closest('[data-goreport]');
  if (g) goReport(g.dataset.goreport);
});

function buyButtons(p, code) {
  const one = product(code), bundle = product('BUNDLE');
  const bundleUseful = BUNDLE_CODES().some(c => !owns(p, c));
  return `
    <button class="btn btn-primary" data-buy="${code}">${one ? won(one.price) : ''} · 심화 리포트 열기</button>
    ${bundleUseful && bundle ? `<button class="btn btn-ghost" data-buy="BUNDLE">7종 묶음 ${won(bundle.price)}</button>` : ''}`;
}

/** 각 탭 아래에 붙는 심화 리포트 자리 */
function premiumSlot(code) {
  return `<div class="premium" data-premium="${code}"></div>`;
}

async function mountPremium(root, p, code, partner) {
  const el = root.querySelector(`[data-premium="${code}"]`);
  if (!el) return;
  const info = REPORT_INFO[code], pr = product(code);
  const partnerName = code === 'COMPAT' ? state.profiles.find(x => String(x.id) === String(partner))?.name : null;
  if (code === 'COMPAT' ? !ownsCompat(p, partner) : !owns(p, code)) {
    el.innerHTML = `
      <article class="card premium-card locked">
        <div class="premium-head">
          <span class="eyebrow">✦ 프리미엄 심화 리포트</span>
          <h2>${info.icon} ${esc(p.name)} 님${partnerName ? ' ♥ ' + esc(partnerName) + ' 님' : ''}의 ${info.name()} 심화 리포트</h2>
          <p class="lead">위의 기본 풀이보다 <b>3배 이상</b> 자세한 내용이 준비돼 있어요.${pr ? ' ' + esc(pr.includes) + '.' : ''}</p>
        </div>
        <ol class="premium-toc">${info.preview.map((t, i) => `<li><span>${String(i + 1).padStart(2, '0')}</span>${esc(t)}<em>🔒</em></li>`).join('')}</ol>
        <div class="premium-blur" aria-hidden="true">
          <p>${esc(p.name)} 님의 사주로 계산한 내용이 이곳에 펼쳐져요. 구매하면 이 프로필로 언제든 다시 볼 수 있어요. ${'✦ '.repeat(40)}</p>
          <p>${'별의 이야기가 이어집니다. '.repeat(10)}</p>
        </div>
        <div class="premium-cta">
          <div class="lock-actions">${buyButtons(p, code)}</div>
          <span class="notice">${code === 'COMPAT' ? `궁합 심화는 <b>상대 1명마다</b> 따로 구매해요. 한 번 구매하면 그 상대와의 궁합만 열려요.` : '한 번 구매하면 이 사주 프로필로 계속 볼 수 있어요.'}</span>
        </div>
      </article>`;
    return;
  }
  el.innerHTML = `<article class="card premium-card"><p class="notice">심화 리포트를 불러오는 중이에요…</p></article>`;
  try {
    const q = partner ? '?partnerId=' + encodeURIComponent(partner) : '';
    const r = await request(`/api/profiles/${p.id}/premium/${code}${q}`);
    el.innerHTML = `
      <article class="card premium-card">
        <div class="premium-head">
          <div class="crumb-row"><span class="eyebrow">✦ 프리미엄 심화 리포트</span><span class="tag open">✓ 구매 완료</span></div>
          <h2>${info.icon} ${esc(r.title)}</h2>
          <p class="lead">${esc(r.subtitle)}</p>
        </div>
        ${reportHtml(r)}
      </article>`;
  } catch (err) {
    el.innerHTML = `<article class="card premium-card"><p class="notice">심화 리포트를 불러오지 못했어요. (${esc(err.message)})</p></article>`;
  }
}

/** 내 결과 하단: 심화 리포트 8종 카드 (묶음은 궁합 뺀 7종) */
function paidSection(p) {
  const bundle = product('BUNDLE');
  const singleSum = BUNDLE_CODES().reduce((n, c) => n + (product(c)?.price || 0), 0);
  const all = BUNDLE_CODES().every(c => owns(p, c));
  const rec = p.saju.concern?.product;
  const cards = REPORT_CODES.map(code => {
    const info = REPORT_INFO[code], pr = product(code), open = owns(p, code);
    const compatPick = code === 'COMPAT' && !open;   // 궁합 단품은 상대를 골라야 해서 궁합 화면으로 보낸다
    return `
      <div class="card paid-card ${code === rec ? 'rec' : ''}">
        <span class="ch">${info.icon} ${code === rec ? '<b>내 고민 맞춤 추천</b>' : '심화 리포트'}</span>
        <h3>${info.name()}</h3>
        <p>${pr ? esc(pr.includes) : ''}</p>
        <div class="foot">
          ${open ? '<span class="tag open">✓ 구매 완료</span>' : `<span class="price">${pr ? won(pr.price) : ''}${code === 'COMPAT' ? '<small>/ 1명</small>' : ''}</span>`}
          ${open ? `<button class="btn btn-ghost btn-sm" data-goreport="${code}">리포트 보기 ↗</button>`
                 : compatPick ? `<button class="btn btn-primary btn-sm" data-goreport="COMPAT">상대 고르기 ↗</button>`
                 : `<button class="btn btn-primary btn-sm" data-buy="${code}">구매하기</button>`}
        </div>
      </div>`;
  }).join('');
  return `
    <div class="intro" style="margin-top:8px">
      <span class="eyebrow">프리미엄 심화 리포트</span>
      <h2 class="section-title">더 깊이 읽어보고 싶다면</h2>
      <p class="lead">각 탭의 기본 풀이는 무료예요. 심화 리포트는 같은 사주로 3배 이상 자세하게 풀어드려요.</p>
    </div>
    <div class="paid-grid">${cards}</div>
    ${!all && bundle ? `<div class="bundle-bar">
      <div><h3>심화 리포트 7종 한 번에 <small>(궁합은 상대마다 따로)</small></h3>
        <span class="save">따로 사면 ${won(singleSum)} → 묶음 ${won(bundle.price)} (${won(singleSum - bundle.price)} 할인)</span></div>
      <button class="btn btn-primary" data-buy="BUNDLE">7종 묶음 구매 ↗</button>
    </div>` : ''}`;
}

/* ════════════════════════════════════════════
   결제 (토스 테스트 결제창 · 모의 결제)
   ════════════════════════════════════════════ */
const payModal = document.getElementById('pay');
let payConfig = { mode: 'mock' };

async function loadPayConfig() {
  try { payConfig = await request('/api/payments/config'); } catch { payConfig = { mode: 'mock' }; }
  if (payConfig.mode === 'toss') loadTossSdk().catch(() => {});   // 결제 버튼을 누르기 전에 미리 불러두기
}

function loadTossSdk() {
  if (window.TossPayments) return Promise.resolve();
  return new Promise((ok, fail) => {
    const s = document.createElement('script');
    s.src = 'https://js.tosspayments.com/v2/standard';
    s.onload = ok;
    s.onerror = () => fail(new Error('토스 결제창을 불러오지 못했어요. 인터넷 연결을 확인해주세요.'));
    document.head.appendChild(s);
  });
}

function openPay(code) {
  // 결제는 로그인한 회원만: 로그인 창을 띄우고, 로그인하면 이어서 결제창을 연다
  if (!state.me?.loggedIn) return openAuth('login', '결제하려면 로그인이 필요해요. 로그인하면 지금 보던 사주도 계정에 함께 저장돼요.', () => openPay(code));
  const p = current(), pr = product(code);
  if (!p || !pr) return toast('먼저 사주를 선택해주세요');
  let partnerId = null, partnerName = '';
  if (code === 'COMPAT') {
    const onCompat = document.querySelector('.tab[aria-selected="true"]')?.dataset.tab === 'marriage' && state.loveView === 'compat';
    const partner = (state.partners || []).find(x => String(x.id) === String(state.partnerId));
    if (!onCompat || !partner) { goReport('COMPAT'); toast('궁합을 볼 상대를 먼저 골라주세요'); return; }
    partnerId = partner.id; partnerName = partner.name;
  }
  state.payFor = { profileId: p.id, code, partnerId };
  const already = pr.unlocks.filter(c => owns(p, c));
  document.getElementById('pay-title').textContent = pr.title;
  document.getElementById('pay-body').innerHTML = `
    <div style="display:flex;flex-direction:column;gap:10px">
      <div class="pay-row"><span class="k">상품</span><span>${esc(pr.title)}</span></div>
      <div class="pay-row"><span class="k">구성</span><span>${esc(pr.includes)}</span></div>
      <div class="pay-row"><span class="k">대상 사주</span><span>${esc(p.name)} · ${esc(p.birthDate.replaceAll('-', '.'))}</span></div>
      ${partnerName ? `<div class="pay-row"><span class="k">궁합 상대</span><span>${esc(partnerName)} (이 상대와의 궁합만 열려요)</span></div>` : ''}
      ${already.length ? `<p class="notice">이미 구매한 리포트(${already.map(c => REPORT_INFO[c].name()).join(', ')})가 포함돼 있어요.</p>` : ''}
      <div class="pay-total"><span class="k">결제 금액</span><b>${won(pr.price)}</b></div>
    </div>`;
  document.getElementById('pay-note').innerHTML = payConfig.mode === 'toss'
    ? `💳 <b>토스페이먼츠 ${payConfig.testMode ? '테스트 ' : ''}결제창</b>으로 이동해요.${payConfig.testMode ? ' 테스트 결제라 실제로 돈이 나가지 않아요.' : ''}`
    : '🧪 <b>모의 결제</b>예요. 실제 결제는 이루어지지 않고, 구매 기록만 저장돼서 리포트가 열려요.';
  document.getElementById('pay-agree').checked = false;
  document.getElementById('pay-error').textContent = '';
  document.getElementById('pay-confirm').textContent = `${won(pr.price)} 결제하기`;
  payModal.classList.add('open');
}
function closePay() { payModal.classList.remove('open'); state.payFor = null; }

document.addEventListener('click', e => {
  const b = e.target.closest('[data-buy]');
  if (b) openPay(b.dataset.buy);
});
document.getElementById('pay-close').onclick = closePay;
document.getElementById('pay-cancel').onclick = closePay;
payModal.addEventListener('click', e => { if (e.target === payModal) closePay(); });

/** 구매 후 돌아갈 화면 */
function returnPlace() {
  const tab = document.querySelector('.tab[aria-selected="true"]')?.dataset.tab || 'result';
  return { tab, love: state.loveView, partner: state.partnerId };
}

async function afterPurchase(code, place) {
  await Promise.all([loadProfiles(), loadShop()]);
  if (place) { state.loveView = place.love || state.loveView; state.partnerId = place.partner || state.partnerId; }
  if (code === 'BUNDLE' || !REPORT_INFO[code]) showTab(place?.tab || 'result');
  else goReport(code);
  toast('구매가 완료됐어요. 심화 리포트가 열렸어요 ✨');
}

document.getElementById('pay-confirm').onclick = async () => {
  const err = document.getElementById('pay-error');
  if (!document.getElementById('pay-agree').checked) { err.textContent = '구매 내용 확인에 동의해주세요.'; return; }
  const btn = document.getElementById('pay-confirm');
  btn.disabled = true;
  const { profileId, code, partnerId } = state.payFor;
  try {
    if (payConfig.mode === 'toss') {
      // 1) 서버가 주문번호 · 금액을 정해서 저장  2) 그 값으로 토스 결제창 띄우기
      const order = await request('/api/payments/orders', { method: 'POST', body: JSON.stringify({ profileId, productCode: code, partnerId, agree: true }) });
      await loadTossSdk();
      try { sessionStorage.setItem('saju.payReturn', JSON.stringify({ ...returnPlace(), code, profileId })); } catch {}
      const base = location.origin + location.pathname;
      const payment = TossPayments(order.clientKey).payment({ customerKey: order.customerKey });
      await payment.requestPayment({
        method: 'CARD',
        amount: { currency: 'KRW', value: order.amount },
        orderId: order.orderId,
        orderName: order.orderName,
        successUrl: base + '?pay=success',
        failUrl: base + '?pay=fail',
      });
      // 결제창에서 결제하면 successUrl 로 페이지가 바뀌므로 여기까지 오지 않음
    } else {
      await request('/api/purchases', { method: 'POST', body: JSON.stringify({ profileId, productCode: code, partnerId, agree: true }) });
      const place = returnPlace();
      closePay();
      await afterPurchase(code, place);
    }
  } catch (e2) {
    // 사용자가 결제창을 닫으면 USER_CANCEL 오류가 온다
    // 토스 SDK 오류는 code 가 함께 온다 → 원인 찾기 쉽게 코드도 보여준다
    console.error('[결제 오류]', e2);
    if (e2.code === 'LOGIN_REQUIRED') {   // 로그인이 풀린 경우 (서버 재시작 등)
      const again = state.payFor?.code; closePay(); state.me = { loggedIn: false }; renderAccount();
      openAuth('login', '로그인이 만료됐어요. 다시 로그인하면 이어서 결제할 수 있어요.', () => again && openPay(again));
      return;
    }
    err.textContent = e2.code === 'USER_CANCEL' ? '결제를 취소했어요.'
      : e2.code ? `${e2.message} (토스 오류 코드: ${e2.code})` : e2.message;
  } finally {
    btn.disabled = false;
  }
};

/** 토스 결제창에서 돌아왔을 때: ?pay=success&paymentKey=..&orderId=..&amount=.. */
async function handlePayReturn() {
  const q = new URLSearchParams(location.search);
  const pay = q.get('pay');
  if (!pay) return null;
  let back = null;
  try { back = JSON.parse(sessionStorage.getItem('saju.payReturn') || 'null'); sessionStorage.removeItem('saju.payReturn'); } catch {}
  history.replaceState(null, '', location.pathname);
  if (back?.profileId) setCurrent(back.profileId);
  if (pay === 'fail') {
    toast(q.get('code') === 'PAY_PROCESS_CANCELED' ? '결제를 취소했어요' : (q.get('message') || '결제에 실패했어요'));
    return back;
  }
  try {
    await request('/api/payments/confirm', { method: 'POST', body: JSON.stringify({
      paymentKey: q.get('paymentKey'), orderId: q.get('orderId'), amount: Number(q.get('amount')) }) });
    await afterPurchase(back?.code, back);
    return { done: true };
  } catch (e) {
    toast('결제 승인에 실패했어요: ' + e.message);
    return back;
  }
}

/* ════════════════════════════════════════════
   구매 내역 (하단 링크로 여는 창 · 취소 없음)
   ════════════════════════════════════════════ */
const buysModal = document.getElementById('buys');
async function openPurchases() {
  document.getElementById('auth').classList.remove('open');
  try { await loadShop(); } catch {}
  const box = document.getElementById('buys-list');
  if (!state.me?.loggedIn) {
    box.innerHTML = `<p class="notice">구매 내역은 로그인하면 볼 수 있어요. 로그인하면 다른 기기에서도 산 리포트를 그대로 볼 수 있어요.</p>
      <button class="btn btn-primary" data-auth-open="login">로그인 / 회원가입</button>`;
  } else if (!state.purchases.length) {
    box.innerHTML = `<p class="notice">아직 구매한 리포트가 없어요.</p>`;
  } else {
    const active = state.purchases.filter(x => x.status !== 'REFUNDED');
    const total = active.reduce((n, x) => n + x.price, 0);
    box.innerHTML = `<p class="notice">모두 <b>${active.length}건</b> · 합계 <b>${won(total)}</b> · 구매 후 7일 이내, 열어보지 않은 리포트는 환불할 수 있어요. <button class="f-link" data-help="refund">환불 규정</button></p>` +
      state.purchases.map(x => {
        const p = state.profiles.find(y => String(y.id) === String(x.profileId));
        const d = new Date(x.createdAt), refunded = x.status === 'REFUNDED';
        const openBtn = !refunded && p && REPORT_INFO[x.unlocks[0]]
          ? `<button class="btn btn-ghost btn-sm" data-popen="${x.profileId}" data-partner="${x.partnerId || ''}" data-code="${x.productCode === 'BUNDLE' ? 'YEAR' : x.productCode}">열기 ↗</button>` : '';
        const refundUi = refunded ? '<span class="tag">환불 완료</span>'
          : x.refundable ? `<span class="refund-ask" data-refund-box="${x.id}"><button class="refund-link" data-refund="${x.id}">환불 요청</button></span>`
          : '';
        return `<div class="buy-row ${refunded ? 'refunded' : ''}">
          <div><b>${esc(x.title)}</b> <span class="tag">${esc(p ? p.name : '삭제된 프로필')}${x.partnerId ? ' ♥ ' + esc(x.partnerName || '지운 상대') : ''}</span>
          <div class="meta">${fmtDate(d)} ${fmtTime(d)} · ${won(x.price)}${refunded ? ' · 환불됨' : x.opened ? ' · 열어봄' : ' · 아직 안 열어봄'}</div></div>
          <div class="acts">${openBtn}${refundUi}</div>
        </div>`;
      }).join('');
  }
  buysModal.classList.add('open');
}
function closePurchases() { buysModal.classList.remove('open'); }
document.getElementById('buys-close').onclick = closePurchases;
buysModal.addEventListener('click', async e => {
  if (e.target === buysModal) return closePurchases();
  // 환불: 한 번 더 확인 → 요청 (브라우저 confirm 창 대신 줄 안에서 확인)
  const ask = e.target.closest('[data-refund]');
  if (ask) {
    const box = ask.closest('[data-refund-box]');
    box.innerHTML = `<span>열어보지 않은 리포트를 환불할까요?</span><button class="refund-link yes" data-refund-yes="${ask.dataset.refund}">환불하기</button><button class="refund-link" data-refund-no>취소</button>`;
    return;
  }
  if (e.target.closest('[data-refund-no]')) return openPurchases();
  const yes = e.target.closest('[data-refund-yes]');
  if (yes) {
    yes.disabled = true;
    try {
      await request(`/api/purchases/${encodeURIComponent(yes.dataset.refundYes)}/refund`, { method: 'POST' });
      await Promise.all([loadProfiles(), loadShop()]);
      toast('환불했어요. 카드사에 따라 3~7일 안에 반영돼요');
      openPurchases();
      refreshTab();
    } catch (x) { toast(x.message); openPurchases(); }
    return;
  }
  const o = e.target.closest('[data-popen]');
  if (o) { closePurchases(); setCurrent(o.dataset.popen); if (o.dataset.partner) state.partnerId = o.dataset.partner; goReport(o.dataset.code); }
});
document.addEventListener('click', e => { if (e.target.closest('[data-buys]')) { e.preventDefault(); openPurchases(); } });

/* ════════════════════════════════════════════
   탭 6. 사랑운 (결혼운 · 궁합 · 재회운)
   ════════════════════════════════════════════ */
/* ── 사랑운 일러스트 (직접 그린 SVG) ── */
const ART = {
  rings: () => `<svg class="art" viewBox="0 0 220 140" aria-hidden="true">
    <defs><linearGradient id="rg1" x1="0" x2="1"><stop offset="0" stop-color="#ffd27a"/><stop offset="1" stop-color="#ff70cf"/></linearGradient>
      <linearGradient id="rg2" x1="0" x2="1"><stop offset="0" stop-color="#ff70cf"/><stop offset="1" stop-color="#b98cff"/></linearGradient>
      <filter id="gl"><feGaussianBlur stdDeviation="3"/></filter></defs>
    <circle cx="88" cy="78" r="38" fill="none" stroke="url(#rg1)" stroke-width="9" filter="url(#gl)" opacity=".5"/>
    <circle cx="132" cy="78" r="38" fill="none" stroke="url(#rg2)" stroke-width="9" filter="url(#gl)" opacity=".5"/>
    <circle cx="88" cy="78" r="38" fill="none" stroke="url(#rg1)" stroke-width="7"/>
    <circle cx="132" cy="78" r="38" fill="none" stroke="url(#rg2)" stroke-width="7"/>
    <path d="M88 40 l6 -12 l6 12 z" fill="#fff" opacity=".9"/><circle cx="94" cy="26" r="3" fill="#fff"/>
    ${[[30, 30, 3], [190, 36, 2.5], [200, 110, 2], [22, 112, 2.2], [110, 18, 1.8]].map(([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}" fill="#fff" opacity=".8"><animate attributeName="opacity" values=".2;1;.2" dur="${2 + r}s" repeatCount="indefinite"/></circle>`).join('')}
  </svg>`,
  moon: () => `<svg class="art" viewBox="0 0 220 140" aria-hidden="true">
    <defs><radialGradient id="mg" cx=".4" cy=".4"><stop offset="0" stop-color="#fff6d8"/><stop offset="1" stop-color="#f4c56a"/></radialGradient>
      <filter id="mgl"><feGaussianBlur stdDeviation="6"/></filter></defs>
    <circle cx="110" cy="62" r="34" fill="#f4c56a" filter="url(#mgl)" opacity=".45"/>
    <path d="M128 32 a34 34 0 1 0 0 60 a27 27 0 1 1 0 -60 z" fill="url(#mg)"/>
    <path d="M38 104 C 70 70, 150 140, 182 98" fill="none" stroke="#ff5a7a" stroke-width="2.5" stroke-dasharray="6 6" stroke-linecap="round">
      <animate attributeName="stroke-dashoffset" values="0;-24" dur="2s" repeatCount="indefinite"/></path>
    <path d="M38 92 l3 8 l8 1 l-6 5 l2 8 l-7 -4 l-7 4 l2 -8 l-6 -5 l8 -1 z" fill="#ff9bd8"/>
    <path d="M182 86 l3 8 l8 1 l-6 5 l2 8 l-7 -4 l-7 4 l2 -8 l-6 -5 l8 -1 z" fill="#b98cff"/>
    ${[[20, 26, 2], [200, 24, 2.4], [160, 128, 1.8], [64, 20, 1.6]].map(([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}" fill="#fff" opacity=".7"><animate attributeName="opacity" values=".2;1;.2" dur="${2 + r}s" repeatCount="indefinite"/></circle>`).join('')}
  </svg>`,
  book: () => `<svg class="art" viewBox="0 0 220 140" aria-hidden="true">
    <defs><linearGradient id="bk1" x1="0" x2="1"><stop offset="0" stop-color="#8f5bd6"/><stop offset="1" stop-color="#ff70cf"/></linearGradient>
      <filter id="bkg"><feGaussianBlur stdDeviation="5"/></filter></defs>
    <ellipse cx="110" cy="112" rx="70" ry="8" fill="#ff70cf" opacity=".18" filter="url(#bkg)"/>
    <path d="M110 46 C 90 34, 62 34, 44 40 L 44 106 C 62 100, 90 100, 110 112 Z" fill="url(#bk1)" opacity=".9"/>
    <path d="M110 46 C 130 34, 158 34, 176 40 L 176 106 C 158 100, 130 100, 110 112 Z" fill="url(#bk1)" opacity=".75"/>
    <path d="M110 46 L 110 112" stroke="#fff" stroke-width="1.5" opacity=".6"/>
    ${[56, 66, 76, 86].map(y => `<path d="M58 ${y} C 72 ${y - 4}, 90 ${y - 3}, 100 ${y + 2}" stroke="#fff" stroke-width="1.4" fill="none" opacity=".45"/><path d="M120 ${y + 2} C 130 ${y - 3}, 148 ${y - 4}, 162 ${y}" stroke="#fff" stroke-width="1.4" fill="none" opacity=".45"/>`).join('')}
    <path d="M110 18 l4 10 l10 1 l-8 6 l3 10 l-9 -6 l-9 6 l3 -10 l-8 -6 l10 -1 z" fill="#ffd27a">
      <animateTransform attributeName="transform" type="translate" values="0 0;0 -4;0 0" dur="2.4s" repeatCount="indefinite"/></path>
    ${[[30, 30, 2.4], [192, 26, 2], [200, 96, 1.8], [24, 90, 2]].map(([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}" fill="#fff" opacity=".7"><animate attributeName="opacity" values=".2;1;.2" dur="${2 + r}s" repeatCount="indefinite"/></circle>`).join('')}
  </svg>`,
  letter: () => `<svg class="art" viewBox="0 0 220 140" aria-hidden="true">
    <defs><linearGradient id="lt1" x1="0" x2="1"><stop offset="0" stop-color="#ffd1ec"/><stop offset="1" stop-color="#f6b3d8"/></linearGradient>
      <linearGradient id="lt2" x1="0" x2="1"><stop offset="0" stop-color="#ff70cf"/><stop offset="1" stop-color="#ff9a7a"/></linearGradient></defs>
    <g><animateTransform attributeName="transform" type="translate" values="0 0;0 -3;0 0" dur="3s" repeatCount="indefinite"/>
      <rect x="55" y="46" width="110" height="72" rx="8" fill="url(#lt1)"/>
      <path d="M55 52 L110 92 L165 52" fill="none" stroke="#e58fc0" stroke-width="3" stroke-linejoin="round"/>
      <path d="M110 100 C 98 91, 94 84, 98 78 C 102 72, 108 74, 110 79 C 112 74, 118 72, 122 78 C 126 84, 122 91, 110 100 z" fill="url(#lt2)"/></g>
    <path d="M168 34 C 162 29, 160 25, 163 22 C 165 19, 168 20, 168 23 C 168 20, 171 19, 173 22 C 176 25, 174 29, 168 34 z" fill="#ff70cf" opacity=".8">
      <animate attributeName="opacity" values=".3;.9;.3" dur="2.2s" repeatCount="indefinite"/></path>
    <path d="M44 40 C 40 37, 39 34, 41 32 C 43 30, 45 31, 45 33 C 45 31, 47 30, 49 32 C 51 34, 50 37, 44 40 z" fill="#ffb3e0" opacity=".7">
      <animate attributeName="opacity" values=".9;.3;.9" dur="2.6s" repeatCount="indefinite"/></path>
    ${[[30, 96, 2], [196, 82, 2.2], [186, 116, 1.6], [70, 22, 1.8]].map(([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}" fill="#fff" opacity=".7"><animate attributeName="opacity" values=".2;1;.2" dur="${2 + r}s" repeatCount="indefinite"/></circle>`).join('')}
  </svg>`,
  heart: () => `<svg class="art-heart" viewBox="0 0 60 54" aria-hidden="true">
    <defs><linearGradient id="hg" x1="0" x2="1"><stop offset="0" stop-color="#ff70cf"/><stop offset="1" stop-color="#ff9a7a"/></linearGradient></defs>
    <path d="M30 50 C 8 34, 2 22, 8 12 C 14 2, 26 4, 30 14 C 34 4, 46 2, 52 12 C 58 22, 52 34, 30 50 z" fill="url(#hg)">
      <animateTransform attributeName="transform" type="scale" values="1;1.08;1" dur="1.4s" repeatCount="indefinite" additive="sum"/></path>
  </svg>`,
};

/** 궁합 상단: 두 사람의 오행 캐릭터 + 하트 */
/** 궁합 상단: 두 사람의 오행 캐릭터 + 하트. partner 는 궁합 상대(이름 · 일간 · 일간 오행) */
function compatHero(me, partner, score) {
  const a = EL[me.saju.core.dayMasterElement], b = EL[partner.dayMasterElement];
  const who = (name, dm, e) => `<div class="cp-who"><span class="cp-ava" style="border-color:${e.color}"><img src="${e.img}" alt="${e.char}" data-emoji="${e.emoji}"></span>
      <b>${esc(name)}</b><small>${esc(dm)}${e.ko} · ${e.char.split(' ').pop()}</small></div>`;
  return `<div class="cp-hero">${who(me.name, me.saju.core.dayMaster, a)}<div class="cp-mid">${ART.heart()}<span class="cp-score">${score}<small>점</small></span></div>${who(partner.name, partner.dayMaster, b)}</div>`;
}

function renderLove() {
  const wrap = document.getElementById('marriage-root');
  const views = [['love', '💗 연애운'], ['compat', '💞 궁합'], ['reunion', '🌙 재회운'], ['marriage', '💍 결혼운']];
  wrap.innerHTML = `<div class="sub-tabs love-tabs" role="tablist">${views.map(([k, l]) =>
    `<button type="button" data-love="${k}" aria-selected="${state.loveView === k}">${l}</button>`).join('')}</div>
    <div id="love-body" style="margin-top:28px"></div>`;
  ({ love: renderLoveFortune, marriage: renderMarriage, compat: renderCompat, reunion: renderReunion })[state.loveView]();
}
document.addEventListener('click', e => {
  const b = e.target.closest('[data-love]');
  if (b) { state.loveView = b.dataset.love; writeStore('saju.loveView', state.loveView); history.replaceState(null, '', '#love-' + state.loveView); renderLove(); }
});

function simpleFrame(p, o) {
  return `
  <div style="display:flex;flex-direction:column;gap:28px">
    <div class="crumb-row"><span class="crumb">내 사주 / ${esc(p.name)} / ${o.crumb}</span><span class="tag">${o.tag}</span></div>
    <div class="intro">
      <span class="eyebrow">${o.eyebrow}</span>
      <h1 class="title">${o.title}</h1>
      <p class="lead">${o.lead}</p>
    </div>
    <article class="card reading-main">${o.main}
      <p class="notice">풀이는 자기 성찰을 위한 참고예요. 중요한 결정은 현실의 정보와 함께 판단해주세요.</p>
    </article>
    ${premiumSlot(o.code)}
  </div>`;
}

async function renderCompat() {
  const root = document.getElementById('love-body');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('💞', '궁합'); return; }
  let partners = [];
  try { partners = await request(`/api/profiles/${p.id}/partners`); } catch {}
  state.partners = partners;
  if (state.partnerId && !partners.some(x => String(x.id) === String(state.partnerId))) state.partnerId = null;
  if (!state.partnerId && partners.length && !state.addingPartner) state.partnerId = String(partners[0].id);
  const sel = partners.find(x => String(x.id) === String(state.partnerId));
  const adding = state.addingPartner || !partners.length;
  const open = sel && (sel.free || sel.unlocked);

  const chips = partners.length ? `<div class="pt-chips" role="tablist" aria-label="궁합 상대">
      ${partners.map(x => `<button type="button" class="pt-chip" data-partner-pick="${x.id}" aria-selected="${!adding && sel && sel.id === x.id}">
        ${esc(x.name)}<small>${x.free ? '무료' : x.unlocked ? '✓ 열림' : '🔒'}</small></button>`).join('')}
      <button type="button" class="pt-chip add" data-partner-add aria-selected="${adding}">＋ 상대 추가</button></div>` : '';

  const form = `<form class="pt-form" id="pt-form" novalidate>
      <p class="notice">${partners.length
        ? '두 번째 상대부터는 <b>4,900원</b>으로 그 상대와의 궁합(기본 풀이 + 심화 리포트)을 열 수 있어요.'
        : '궁합을 볼 상대의 출생 정보를 적어주세요. <b>첫 번째 상대는 무료</b>로 궁합을 볼 수 있어요.'}</p>
      <div class="pt-grid">
        <label>상대 이름 또는 별명<input class="input" id="pt-name" maxlength="20" placeholder="예) 민준" autocomplete="off"></label>
        <label>생년월일 (8자리)<input class="input" id="pt-date" inputmode="numeric" maxlength="10" placeholder="예) 19950712" autocomplete="off"></label>
        <label>달력<select class="input" id="pt-cal"><option value="SOLAR">양력</option><option value="LUNAR">음력</option></select></label>
        <label>태어난 시간<input class="input" id="pt-time" type="time"></label>
      </div>
      <div class="pt-row">
        <label class="check"><input type="checkbox" id="pt-leap"> 윤달</label>
        <label class="check"><input type="checkbox" id="pt-time-unknown"> 시간 모름</label>
        <span class="pt-gender" role="group" aria-label="성별"><button type="button" data-ptg="F" aria-pressed="false">여성</button><button type="button" data-ptg="M" aria-pressed="false">남성</button></span>
      </div>
      <p class="notice">상대의 정보는 궁합에만 쓰이고, 결제와 이어져 있어 나중에 수정할 수 없어요. 상대의 동의를 받고 입력해주세요.</p>
      <p class="pay-error" id="pt-error" role="alert"></p>
      <div class="pt-actions">${partners.length ? '<button type="button" class="btn btn-line" data-partner-cancel>취소</button>' : ''}
        <button type="submit" class="btn btn-primary">${partners.length ? '상대 추가하기' : '무료 궁합 보기'}</button></div>
    </form>`;

  const locked = sel && !open ? `<div class="pt-locked">
      <span class="big">🔒</span>
      <h3>${esc(p.name)} 님과 ${esc(sel.name)} 님의 궁합</h3>
      <p>첫 번째 상대 다음부터는 상대마다 궁합을 열어야 볼 수 있어요. 한 번 열면 기본 풀이와 심화 리포트를 모두 볼 수 있어요.</p>
      <button class="btn btn-primary" data-buy="COMPAT">${won(product('COMPAT')?.price || 4900)} · 이 상대와의 궁합 열기</button>
    </div>` : '';

  root.innerHTML = simpleFrame(p, { code: 'COMPAT', crumb: '궁합', tag: '💞 궁합',
    eyebrow: '사랑운 · 두 사람의 기운', title: `${esc(p.name)} 님의 궁합`,
    lead: '두 사람의 일간(나를 나타내는 글자)과 일지(배우자 자리), 오행 균형을 비교해 관계의 결을 읽어요.',
    main: `${chips}${adding ? form : locked || '<div id="compat-basic"><p class="notice">궁합을 계산하는 중이에요…</p></div>'}
      ${sel && !adding ? `<p class="pt-del"><button type="button" class="refund-link" data-partner-del="${sel.id}">이 상대 지우기</button></p>` : ''}` });
  if (adding) { bindPartnerForm(root, p); root.querySelector('[data-premium]')?.remove(); return; }
  if (!open) { root.querySelector('[data-premium]')?.remove(); return; }
  try {
    const r = await request(`/api/profiles/${p.id}/compat?partnerId=${sel.id}`);
    const score = parseInt(r.subtitle, 10);
    root.querySelector('#compat-basic').innerHTML = `
      ${compatHero(p, sel, score)}
      <p class="cp-grade">${esc(r.subtitle.replace(/^\d+점 · /, ''))}</p>
      ${reportHtml(r)}`;
  } catch (err) {
    root.querySelector('#compat-basic').innerHTML = `<p class="notice">궁합을 불러오지 못했어요. (${esc(err.message)})</p>`;
  }
  mountPremium(root, p, 'COMPAT', sel.id);
}

/** 궁합 상대 입력 칸 */
function bindPartnerForm(root, p) {
  const f = root.querySelector('#pt-form');
  let gender = null;
  const dateEl = f.querySelector('#pt-date');
  dateEl.addEventListener('input', () => {
    const d = dateEl.value.replace(/\D/g, '').slice(0, 8);
    dateEl.value = d.length > 6 ? `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6)}` : d.length > 4 ? `${d.slice(0, 4)}.${d.slice(4)}` : d;
  });
  f.querySelector('#pt-time-unknown').onchange = e => { f.querySelector('#pt-time').disabled = e.target.checked; };
  f.querySelectorAll('[data-ptg]').forEach(b => b.onclick = () => {
    gender = b.dataset.ptg; f.querySelectorAll('[data-ptg]').forEach(x => x.setAttribute('aria-pressed', String(x === b)));
  });
  f.onsubmit = async e => {
    e.preventDefault();
    const err = f.querySelector('#pt-error');
    const name = f.querySelector('#pt-name').value.trim(), d = dateEl.value.replace(/\D/g, '');
    const cal = f.querySelector('#pt-cal').value, unknown = f.querySelector('#pt-time-unknown').checked, time = f.querySelector('#pt-time').value;
    err.textContent = !name ? '상대 이름을 적어주세요.' : d.length !== 8 ? '생년월일을 8자리로 적어주세요. 예) 19950712'
      : !unknown && !time ? '태어난 시간을 고르거나 "시간 모름"을 체크해주세요.' : !gender ? '성별을 골라주세요.' : '';
    if (err.textContent) return;
    try {
      const added = await request(`/api/profiles/${p.id}/partners`, { method: 'POST', body: JSON.stringify({
        name, calendar: cal, birthDate: `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6)}`, leapMonth: cal === 'LUNAR' && f.querySelector('#pt-leap').checked,
        birthTime: unknown ? null : time, gender }) });
      state.addingPartner = false; state.partnerId = String(added.id);
      toast(added.free ? '무료 궁합을 볼 수 있어요 💞' : '상대를 추가했어요. 궁합을 열면 볼 수 있어요');
      renderCompat();
    } catch (x) { err.textContent = x.message; }
  };
}

document.addEventListener('click', async e => {
  const pick = e.target.closest('[data-partner-pick]');
  if (pick) { state.partnerId = pick.dataset.partnerPick; state.addingPartner = false; return renderCompat(); }
  if (e.target.closest('[data-partner-add]')) { state.addingPartner = true; return renderCompat(); }
  if (e.target.closest('[data-partner-cancel]')) { state.addingPartner = false; return renderCompat(); }
  const del = e.target.closest('[data-partner-del]');
  if (del) {
    if (del.dataset.confirm !== '1') { del.dataset.confirm = '1'; del.textContent = '지우면 이 상대와의 궁합(구매 포함)을 다시 볼 수 없어요. 정말 지울까요?'; return; }
    try {
      await request(`/api/profiles/${current().id}/partners/${encodeURIComponent(del.dataset.partnerDel)}`, { method: 'DELETE' });
      state.partnerId = null; toast('상대를 지웠어요'); renderCompat();
    } catch (x) { toast(x.message); }
  }
});

/** 사랑운 · 연애운(애정운): 연애 스타일 + 올해 인연의 흐름 (무료) + 심화 리포트 */
function renderLoveFortune() {
  const root = document.getElementById('love-body');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('💗', '연애운'); return; }
  root.innerHTML = simpleFrame(p, { code: 'LOVE', crumb: '연애운', tag: '💗 연애운',
    eyebrow: '사랑운 · 연애운(애정운)', title: `${esc(p.name)} 님의 연애운`,
    lead: '나를 나타내는 글자(일간)로 연애 스타일을, 올해 들어오는 기운으로 인연의 흐름을 읽어요. 결혼운이 가정을 꾸리는 일을 본다면, 연애운은 사람을 만나고 마음을 주고받는 방식을 봐요.',
    main: `<div class="topic-hero love-hero art-hero">${ART.letter()}
        <span><span class="lbl">마음이 닿는 순간</span><br><span class="val">설렘의 편지</span></span></div>` + reportHtml(p.saju.love) });
  mountPremium(root, p, 'LOVE');
}

function renderReunion() {
  const root = document.getElementById('love-body');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('🌙', '재회운'); return; }
  root.innerHTML = simpleFrame(p, { code: 'REUNION', crumb: '재회운', tag: '🌙 재회운',
    eyebrow: '사랑운 · 다시 이어지는 인연', title: `${esc(p.name)} 님의 재회운`,
    lead: '이별을 대하는 나의 방식(일간)과 올해의 기운으로, 지나간 인연을 어떻게 바라보면 좋을지 읽어요.',
    main: `<div class="topic-hero reunion-hero art-hero">${ART.moon()}
        <span><span class="lbl">끊어진 듯 이어진</span><br><span class="val">붉은 실의 인연</span></span></div>` + reportHtml(p.saju.reunion) });
  mountPremium(root, p, 'REUNION');
}

/* ════════════════════════════════════════════
   탭 7. 가족운
   ════════════════════════════════════════════ */
function renderFamily() {
  const root = document.getElementById('family-root');
  const p = current();
  if (!p) { root.innerHTML = emptyTopic('🏡', '가족운'); return; }
  root.innerHTML = simpleFrame(p, { code: 'FAMILY', crumb: '가족운', tag: '🏡 가족운',
    eyebrow: '가족운 · 년주 · 월주 · 시주', title: `${esc(p.name)} 님의 가족운`,
    lead: '사주의 기둥마다 가족의 자리가 있어요. 년주는 어린 시절, 월주는 부모 · 형제, 시주는 자녀를 나타내요.',
    main: reportHtml(p.saju.family) });
  mountPremium(root, p, 'FAMILY');
}

/* ════════════════════════════════════════════
   탭 5. 내 사주 목록
   ════════════════════════════════════════════ */
function renderList() {
  const list = document.getElementById('profile-list');
  document.getElementById('profile-count').textContent = state.profiles.length;

  if (!state.profiles.length) {
    list.innerHTML = emptyState('🐰', '저장된 사주가 없어요', '첫 번째 사주 프로필을 만들어보세요.');
    return;
  }

  list.innerHTML = state.profiles.map(p => `
    <article class="card profile-card ${String(p.id) === String(state.currentId) ? 'current' : ''}">
      <div class="pc-head">
        <div class="pc-name">${avatar(p.saju.core.dayMasterElement, 'lg')}<b>${esc(p.name)}</b>
          ${String(p.id) === String(state.currentId) ? '<span class="tag">보고 있는 사주</span>' : ''}</div>
        <div class="pc-manage">
          <button data-edit="${p.id}">✎ 정보 수정</button>
          <button class="del" data-del="${p.id}">🗑 프로필 삭제</button>
        </div>
      </div>
      <div class="pc-info">
        <div><div class="k">생년월일</div><div class="v">${p.birthDate.replaceAll('-', '.')} (양력)</div></div>
        <div><div class="k">태어난 시간</div><div class="v">${p.birthTime ? p.birthTime.slice(0, 5) : '모름'}</div></div>
        <div><div class="k">출생 지역</div><div class="v">${esc(p.birthPlace || '-')}</div></div>
        <div><div class="k">성별</div><div class="v">${p.gender === 'F' ? '여성' : '남성'}</div></div>
      </div>
      <div class="pc-foot">
        <div class="pc-status">✓ 사주 결과 열람 가능
          <small>무료: 사주팔자 · 오행 균형 · 핵심 성향 · 유료 ${p.saju.owned.length}/3 열림</small></div>
        <button class="btn btn-ghost" data-open="${p.id}">결과 보기 ↗</button>
      </div>
    </article>`).join('');
}

document.getElementById('profile-list').addEventListener('click', e => {
  const open = e.target.closest('[data-open]');
  if (open) { setCurrent(open.dataset.open); showTab('result'); return; }
  const del = e.target.closest('[data-del]');
  if (del) openDeleteModal(del.dataset.del);
});

// 정보 수정 (목록 · 결과 화면 공통)
document.addEventListener('click', e => {
  const edit = e.target.closest('[data-edit]');
  if (!edit) return;
  const p = state.profiles.find(x => String(x.id) === edit.dataset.edit);
  if (p) { fillForm(p); showTab('input'); }
});

/* ════════════════════════════════════════════
   성찰 질문 카드 (내 결과 · 올해 운세)
   ════════════════════════════════════════════ */
async function mountReflect(el, p, type) {
  if (!el) return;
  const key = type + ':' + p.id;
  const offset = state.qOffset[key] || 0;
  const label = { year: `✦ ${p.saju.year.year}년 성찰 질문`, career: '✦ 일에 대한 성찰 질문',
                  marriage: '✦ 관계에 대한 성찰 질문' }[type] || '✦ 오늘의 성찰 질문';
  const draft = state.drafts[key] || '';

  if (!el.dataset.ready) {
    el.innerHTML = `
      <div class="reflect-head">
        <span class="eyebrow">${label}</span>
        <button type="button" class="link" data-next title="다른 질문 보기">다른 질문 ↻</button>
      </div>
      <p class="q fade">질문을 불러오는 중이에요…</p>
      <textarea class="input reflect-input" maxlength="1000" rows="4"
        placeholder="떠오르는 대로 적어보세요. 정답은 없어요."></textarea>
      <div class="reflect-foot">
        <span class="char-count">0 / 1000</span>
        <button type="button" class="btn btn-primary btn-sm" data-save disabled>성찰 일기에 저장</button>
      </div>
      <p class="reflect-saved"></p>`;
    el.dataset.ready = '1';

    const ta = el.querySelector('textarea'), save = el.querySelector('[data-save]'), cnt = el.querySelector('.char-count');
    const sync = () => {
      state.drafts[key] = ta.value;
      cnt.textContent = `${ta.value.length} / 1000`;
      save.disabled = !ta.value.trim();
    };
    ta.value = draft; sync();
    ta.addEventListener('input', sync);

    el.querySelector('[data-next]').addEventListener('click', () => {
      state.qOffset[key] = (state.qOffset[key] || 0) + 1;
      loadQuestion(el, p, type);
    });

    save.addEventListener('click', async () => {
      const answer = ta.value.trim();
      if (!answer || !el.dataset.code) return;
      save.disabled = true;
      try {
        await request(REFLECT_API, { method: 'POST', body: JSON.stringify({
          profileId: p.id, type, questionCode: el.dataset.code, answer }) });
        ta.value = ''; sync();
        await loadReflections();
        updateSavedLine(el, p);
        toast('성찰 일기에 저장했어요');
      } catch (err) {
        toast(err.message); save.disabled = false;
      }
    });
  }
  updateSavedLine(el, p);
  loadQuestion(el, p, type);
}

async function loadQuestion(el, p, type) {
  const key = type + ':' + p.id;
  const q = el.querySelector('.q');
  q.classList.add('fade');
  try {
    const data = await request(`${REFLECT_API}/question?profileId=${p.id}&type=${type}&offset=${state.qOffset[key] || 0}`);
    if (!el.isConnected) return;
    el.dataset.code = data.code;
    q.textContent = data.text;
  } catch (err) {
    console.error('[성찰 질문]', err);
    q.innerHTML = `질문을 불러오지 못했어요. <button type="button" class="link" data-retry>다시 불러오기 ↻</button>
      <span class="notice" style="display:block;margin-top:6px">(${esc(err.message)})</span>`;
    q.querySelector('[data-retry]').onclick = () => loadQuestion(el, p, type);
  } finally {
    q.classList.remove('fade');
  }
}

function updateSavedLine(el, p) {
  const n = state.reflections.filter(r => String(r.profileId) === String(p.id)).length;
  el.querySelector('.reflect-saved').innerHTML = n
    ? `${esc(p.name)} 님의 성찰 일기 <b style="color:var(--accent)">${n}개</b> · <button type="button" class="link" data-journal-of="${p.id}">모아보기 ↗</button>`
    : '적은 답은 내 사주 목록 › 성찰 일기에 모여요.';
}

document.addEventListener('click', e => {
  const j = e.target.closest('[data-journal-of]');
  if (!j) return;
  state.journalFilter = j.dataset.journalOf;
  state.listView = 'journal';
  showTab('list');
});

/* ════════════════════════════════════════════
   내 사주 목록 › 성찰 일기
   ════════════════════════════════════════════ */
const WEEK = ['일', '월', '화', '수', '목', '금', '토'];
const fmtDate = d => `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')} (${WEEK[d.getDay()]})`;
const fmtTime = d => `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;

function applyListView() {
  const v = state.listView;
  document.querySelectorAll('[data-view]').forEach(b => b.setAttribute('aria-selected', b.dataset.view === v));
  document.getElementById('view-profiles').classList.toggle('on', v === 'profiles');
  document.getElementById('view-journal').classList.toggle('on', v === 'journal');
  document.getElementById('list-title').textContent =
    { journal: '나의 성찰 일기' }[v] || '저장한 사주 프로필';
  document.getElementById('list-lead').textContent = {
    journal: '성찰 질문에 적은 답을 모아봤어요. 지난 기록을 읽으며 달라진 나를 발견해보세요.',
  }[v] || '나와 소중한 사람들의 사주를 저장하고, 언제든 다시 열어볼 수 있어요.';
}

document.querySelectorAll('[data-view]').forEach(b => b.addEventListener('click', () => {
  state.listView = b.dataset.view; applyListView();
}));

function renderJournal() {
  document.getElementById('journal-count').textContent = state.reflections.length;
  document.getElementById('practice-box').innerHTML = practiceCard();

  // 프로필 필터
  const sel = document.getElementById('journal-filter');
  if (state.journalFilter && !state.profiles.some(p => String(p.id) === String(state.journalFilter))) state.journalFilter = '';
  sel.innerHTML = '<option value="">전체 프로필</option>' + state.profiles.map(p =>
    `<option value="${p.id}">${esc(p.name)} (${state.reflections.filter(r => String(r.profileId) === String(p.id)).length})</option>`).join('');
  sel.value = state.journalFilter;

  const list = state.reflections.filter(r => !state.journalFilter || String(r.profileId) === String(state.journalFilter));
  const now = new Date();
  const thisMonth = list.filter(r => { const d = new Date(r.createdAt); return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth(); }).length;
  document.getElementById('journal-stats').innerHTML = list.length
    ? `모두 <b>${list.length}개</b>의 기록 · 이번 달 <b>${thisMonth}개</b>` : '';

  const box = document.getElementById('journal-list');
  if (!list.length) {
    box.innerHTML = `<div class="card empty-state"><span class="big">✎</span>
      <h3>아직 적은 성찰 일기가 없어요</h3>
      <p>내 결과나 올해 운세 탭의 성찰 질문에 답을 적어보세요.</p>
      <button class="btn btn-primary" data-go="result">성찰 질문 보러 가기 ↗</button></div>`;
    return;
  }

  let month = '';
  box.innerHTML = list.map(r => {
    const d = new Date(r.createdAt);
    const m = `${d.getFullYear()}년 ${d.getMonth() + 1}월`;
    const monthHead = m !== month ? `<div class="j-month">${m}</div>` : '';
    month = m;
    const p = state.profiles.find(x => String(x.id) === String(r.profileId));
    const editing = String(state.editingReflection) === String(r.id);
    const confirming = String(state.confirmReflection) === String(r.id);
    const edited = r.updatedAt && r.updatedAt.slice(0, 16) !== r.createdAt.slice(0, 16);
    const typeLabel = { year: '올해 운세', career: '직장운', marriage: '결혼운' }[r.type] || '나의 결';
    return `${monthHead}
    <article class="card j-card" data-rid="${r.id}">
      <div class="j-head">
        <span class="j-date">${fmtDate(d)} ${fmtTime(d)}</span>
        <span class="tag">${esc(p ? p.name : '삭제된 프로필')}</span>
        <span class="j-type">${typeLabel}</span>
      </div>
      <p class="j-q">${esc(r.question)}</p>
      ${editing
        ? `<textarea class="input reflect-input" maxlength="1000" data-edit-area>${esc(r.answer)}</textarea>`
        : `<p class="j-a">${esc(r.answer)}</p>`}
      <div class="j-foot">
        <span class="j-edited">${edited ? '수정됨 · ' + fmtDate(new Date(r.updatedAt)) : ''}</span>
        ${editing ? `<div class="j-actions">
            <button type="button" class="btn btn-primary btn-sm" data-jsave>저장</button>
            <button type="button" class="btn btn-line btn-sm" data-jcancel>취소</button></div>`
        : confirming ? `<div class="j-confirm">이 기록을 삭제할까요? 복구할 수 없어요.
            <button type="button" class="btn btn-danger btn-sm" data-jdel-ok>삭제</button>
            <button type="button" class="btn btn-line btn-sm" data-jcancel>취소</button></div>`
        : `<div class="j-actions">
            <button type="button" data-jedit>✎ 수정</button>
            <button type="button" class="del" data-jdel>🗑 삭제</button></div>`}
      </div>
    </article>`;
  }).join('');

  const area = box.querySelector('[data-edit-area]');
  if (area) { area.focus(); area.setSelectionRange(area.value.length, area.value.length); }
}

document.getElementById('journal-filter').addEventListener('change', e => {
  state.journalFilter = e.target.value; renderJournal();
});

document.getElementById('journal-list').addEventListener('click', async e => {
  const card = e.target.closest('[data-rid]');
  if (!card) return;
  const id = card.dataset.rid;
  if (e.target.closest('[data-jedit]')) { state.editingReflection = id; state.confirmReflection = null; renderJournal(); return; }
  if (e.target.closest('[data-jdel]')) { state.confirmReflection = id; state.editingReflection = null; renderJournal(); return; }
  if (e.target.closest('[data-jcancel]')) { state.editingReflection = null; state.confirmReflection = null; renderJournal(); return; }

  if (e.target.closest('[data-jsave]')) {
    const answer = card.querySelector('[data-edit-area]').value.trim();
    if (!answer) { toast('답을 한 글자 이상 적어주세요'); return; }
    try {
      await request(`${REFLECT_API}/${id}`, { method: 'PUT', body: JSON.stringify({ answer }) });
      state.editingReflection = null;
      await loadReflections(); renderJournal(); toast('성찰 일기를 수정했어요');
    } catch (err) { toast(err.message); }
    return;
  }
  if (e.target.closest('[data-jdel-ok]')) {
    try {
      await request(`${REFLECT_API}/${id}`, { method: 'DELETE' });
      state.confirmReflection = null;
      await loadReflections(); renderJournal(); toast('성찰 일기를 삭제했어요');
    } catch (err) { toast(err.message); }
  }
});

/* ════════════════════════════════════════════
   삭제 모달
   ════════════════════════════════════════════ */
const modal = document.getElementById('modal');
let deleteId = null;

function openDeleteModal(id) {
  const p = state.profiles.find(x => String(x.id) === String(id));
  if (!p) return;
  deleteId = id;
  document.getElementById('modal-target').innerHTML =
    `<b>${esc(p.name)}</b><span class="muted">${esc(birthText(p))}</span>`;
  modal.classList.add('open');
  document.getElementById('modal-cancel').focus();
}
function closeModal() { modal.classList.remove('open'); deleteId = null; }

document.getElementById('modal-cancel').onclick = closeModal;
modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });
document.addEventListener('keydown', e => {
  if (e.key !== 'Escape') return;
  if (buysModal.classList.contains('open')) closePurchases();
  else if (payModal.classList.contains('open')) closePay();
  else if (guide.classList.contains('open')) closeGuide();
  else if (modal.classList.contains('open')) closeModal();
});

/* ════════════════════════════════════════════
   이용 안내 모달
   ════════════════════════════════════════════ */
const guide = document.getElementById('guide');
function openGuide(section = 'guide') {
  showGuideSection(section);
  guide.classList.add('open');
  document.body.style.overflow = 'hidden';
  document.getElementById('guide-close').focus();
}
function closeGuide() { guide.classList.remove('open'); document.body.style.overflow = ''; }
function showGuideSection(name) {
  guide.querySelectorAll('[data-gtab]').forEach(b => b.setAttribute('aria-selected', b.dataset.gtab === name));
  guide.querySelectorAll('[data-gsec]').forEach(s => s.classList.toggle('on', s.dataset.gsec === name));
  guide.querySelector('.guide-body').scrollTop = 0;
}
document.addEventListener('click', e => {
  const h = e.target.closest('[data-help]');
  if (h) { openGuide(h.dataset.help); return; }
  const g = e.target.closest('[data-gtab]');
  if (g) showGuideSection(g.dataset.gtab);
});
document.getElementById('guide-close').onclick = closeGuide;
guide.addEventListener('click', e => { if (e.target === guide) closeGuide(); });

document.getElementById('modal-confirm').onclick = async () => {
  const btn = document.getElementById('modal-confirm');
  btn.disabled = true;
  try {
    await api('/' + deleteId, { method: 'DELETE' });
    if (String(deleteId) === String(state.currentId)) setCurrent(null);
    closeModal();
    await Promise.all([loadProfiles(), loadReflections(), loadShop()]);
    renderList();
    renderJournal();
    toast('프로필을 삭제했어요');
  } catch (err) {
    toast(err.message);
  } finally {
    btn.disabled = false;
  }
};

/* ════════════════════════════════════════════
   입력 폼 (추가 · 수정 공용)
   ════════════════════════════════════════════ */
const form = document.getElementById('profile-form');
const $ = id => document.getElementById(id);

/* 생년월일: 숫자만 입력해도 1994.08.17 처럼 점을 찍어준다 (달력 스크롤 대신 직접 입력) */
$('f-date').addEventListener('input', e => {
  const d = e.target.value.replace(/\D/g, '').slice(0, 8);
  e.target.value = d.length > 6 ? `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6)}` : d.length > 4 ? `${d.slice(0, 4)}.${d.slice(4)}` : d;
});
function dateValue() {
  const d = $('f-date').value.replace(/\D/g, '');
  return d.length === 8 ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6)}` : '';
}
function setCalendar(v) {
  state.calendar = v;
  $('f-cal').querySelectorAll('button').forEach(b => b.setAttribute('aria-pressed', b.dataset.value === v));
  $('f-leap-wrap').hidden = v !== 'LUNAR';
  if (v !== 'LUNAR') $('f-leap').checked = false;
  $('f-date-help').textContent = v === 'LUNAR' ? '음력 날짜를 입력하면 양력으로 바꿔서 계산해요' : '숫자 8자리만 입력해도 돼요 (19940817)';
}
$('f-cal').addEventListener('click', e => { const b = e.target.closest('button'); if (b) setCalendar(b.dataset.value); });

function setConcern(v) {
  state.concern = state.concern === v ? null : v;
  $('f-concern').querySelectorAll('button').forEach(b => b.setAttribute('aria-pressed', b.dataset.value === state.concern));
}
$('f-concern').addEventListener('click', e => { const b = e.target.closest('button'); if (b) setConcern(b.dataset.value); });
$('f-concern-text').addEventListener('input', e => { $('f-concern-count').textContent = e.target.value.length; });

/* 태어난 시간: 숫자만 입력해도 14:30 처럼 콜론을 찍어준다 */
$('f-time').addEventListener('input', e => {
  const d = e.target.value.replace(/\D/g, '').slice(0, 4);
  e.target.value = d.length > 2 ? `${d.slice(0, 2)}:${d.slice(2)}` : d;
  timeHint();
});
/** 오전/오후 + 12시간 입력 → 24시간 "HH:MM" (오전 12시 = 0시, 오후 12시 = 12시) */
function timeValue() {
  const d = $('f-time').value.replace(/\D/g, '');
  if (d.length !== 4) return null;
  let h = +d.slice(0, 2); const m = +d.slice(2);
  if (h < 1 || h > 12 || m > 59) return null;
  if (state.ampm === 'AM') h = h === 12 ? 0 : h;
  else h = h === 12 ? 12 : h + 12;
  return `${String(h).padStart(2, '0')}:${d.slice(2)}`;
}
function setAmpm(v) {
  state.ampm = v;
  $('f-ampm').querySelectorAll('button').forEach(b => b.setAttribute('aria-pressed', b.dataset.value === v));
  timeHint();
}
/** 입력한 시간을 '새벽 · 낮 · 밤' 말로 풀어서 보여준다 (오전 12시 = 자정, 오후 12시 = 정오 헷갈림 방지) */
function timeHint() {
  const el = $('f-time-help'), t = timeValue();
  if (!t) { el.textContent = '숫자 4자리로 입력해요 (2시 30분 → 0230)'; el.classList.remove('time-ok'); return; }
  const h = +t.slice(0, 2), m = t.slice(3);
  const word = h === 0 ? '밤 12시(자정)' : h < 6 ? `새벽 ${h}시` : h < 12 ? `아침 ${h}시` : h === 12 ? '낮 12시(정오)' : h < 18 ? `오후 ${h - 12}시` : `저녁 ${h - 12}시`;
  el.textContent = `→ ${word} ${m}분으로 계산해요 (24시간 ${t})` + (h === 0 || h === 12 ? ' · 오전 12시는 자정, 오후 12시는 정오예요' : '');
  el.classList.add('time-ok');
}
$('f-ampm').addEventListener('click', e => { const b = e.target.closest('button'); if (b && !$('f-time').disabled) setAmpm(b.dataset.value); });
$('f-time-unknown').addEventListener('change', e => {
  $('f-time').disabled = e.target.checked;
  if (e.target.checked) $('f-time').value = '';
});

$('f-gender').addEventListener('click', e => {
  const b = e.target.closest('button');
  if (!b) return;
  setGender(b.dataset.value);
});
function setGender(v) {
  state.gender = v;
  $('f-gender').querySelectorAll('button').forEach(b => b.setAttribute('aria-pressed', b.dataset.value === v));
}

function resetForm() {
  state.editingId = null;
  form.reset();
  $('f-time').disabled = false;
  setGender(null);
  setCalendar('SOLAR');
  setAmpm('AM');
  state.concern = null; setConcern(null);
  $('f-concern-count').textContent = 0;
  $('form-error').textContent = '';
  form.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
  $('form-eyebrow').textContent = '사주 프로필 추가';
  $('form-title').textContent = '태어난 순간을 알려주세요';
  $('submit-btn').textContent = '사주 결과 보기 ↗';
}

function fillForm(p) {
  resetForm();
  state.editingId = p.id;
  $('f-name').value = p.name;
  setCalendar(p.calendar === 'LUNAR' ? 'LUNAR' : 'SOLAR');
  $('f-date').value = (p.calendar === 'LUNAR' && p.lunarDate ? p.lunarDate : p.birthDate).replaceAll('-', '.');
  $('f-leap').checked = !!p.leapMonth;
  state.concern = null; if (p.concernType) setConcern(p.concernType);
  $('f-concern-text').value = p.concernText || '';
  $('f-concern-count').textContent = (p.concernText || '').length;
  if (p.birthTime) {
    const h = +p.birthTime.slice(0, 2), mm = p.birthTime.slice(3, 5);
    setAmpm(h < 12 ? 'AM' : 'PM');
    const h12 = h % 12 === 0 ? 12 : h % 12;
    $('f-time').value = `${String(h12).padStart(2, '0')}:${mm}`;
  } else $('f-time').value = '';
  timeHint();
  $('f-time-unknown').checked = !p.birthTime;
  $('f-time').disabled = !p.birthTime;
  $('f-place').value = p.birthPlace || '';
  setGender(p.gender);
  $('f-agree').checked = true;
  $('form-eyebrow').textContent = '사주 정보 수정';
  $('form-title').textContent = `${p.name} 님의 출생 정보 수정`;
  $('submit-btn').textContent = '수정하고 다시 보기 ↗';
}

$('cancel-btn').onclick = () => { const wasEditing = state.editingId; resetForm(); showTab(wasEditing ? 'list' : 'home'); };

form.addEventListener('submit', async e => {
  e.preventDefault();
  form.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));

  const fail = (msg, el) => { $('form-error').textContent = msg; if (el) { el.classList.add('invalid'); el.focus(); } };
  const name = $('f-name').value.trim();
  const date = dateValue();
  const unknown = $('f-time-unknown').checked;
  const time = timeValue();

  if (!name) return fail('이름을 입력해주세요.', $('f-name'));
  if (!$('f-date').value) return fail('생년월일을 입력해주세요.', $('f-date'));
  if (!date) return fail('생년월일은 1994.08.17 처럼 8자리로 입력해주세요.', $('f-date'));
  if (date < '1900-01-01') return fail('1900년 이후 생년월일만 계산할 수 있어요.', $('f-date'));
  if (state.calendar === 'SOLAR') {
    const [yy, mm, dd] = date.split('-').map(Number), dt = new Date(yy, mm - 1, dd);
    if (dt.getMonth() !== mm - 1 || dt.getDate() !== dd) return fail('없는 날짜예요. 생년월일을 다시 확인해주세요.', $('f-date'));
    if (dt > new Date()) return fail('생년월일은 오늘 이전이어야 해요.', $('f-date'));
  }
  if (!unknown && !$('f-time').value) return fail('태어난 시간을 입력하거나 "시간을 몰라요"를 선택해주세요.', $('f-time'));
  if (!unknown && !time) return fail('태어난 시간은 1 ~ 12시 사이로, 02:30 처럼 입력해주세요.', $('f-time'));
  if (!state.gender) return fail('성별을 선택해주세요.');
  if (!$('f-agree').checked) return fail('출생 정보 이용에 동의해주세요.');
  $('form-error').textContent = '';

  const body = JSON.stringify({
    name, birthDate: date,
    calendar: state.calendar, leapMonth: state.calendar === 'LUNAR' && $('f-leap').checked,
    concernType: state.concern || null,
    concernText: $('f-concern-text').value.trim() || null,
    birthTime: unknown ? null : time,
    gender: state.gender,
    birthPlace: $('f-place').value || null,
  });

  form.classList.add('loading');
  try {
    const editing = state.editingId;
    const saved = editing
      ? await api('/' + editing, { method: 'PUT', body })
      : await api('', { method: 'POST', body });
    await loadProfiles();
    setCurrent(saved.id);
    resetForm();
    showTab('result');
    toast(editing ? '정보를 수정하고 사주를 다시 계산했어요' : '사주를 저장했어요');
  } catch (err) {
    fail(err.message);
  } finally {
    form.classList.remove('loading');
  }
});

/* ════════════════════════════════════════════
   시작
   ════════════════════════════════════════════ */
(function stars() {
  const box = document.getElementById('stars');
  for (let i = 0; i < 60; i++) {
    const s = document.createElement('i');
    s.style.left = Math.random() * 100 + '%';
    s.style.top = Math.random() * 100 + '%';
    s.style.animationDelay = (Math.random() * 3) + 's';
    box.appendChild(s);
  }
})();

// 사주상 올해 (입춘 2/4 이전이면 작년)
(function yearLabel() {
  const now = new Date();
  const beforeIpchun = now.getMonth() === 0 || (now.getMonth() === 1 && now.getDate() < 4);
  const y = now.getFullYear() - (beforeIpchun ? 1 : 0);
  document.querySelectorAll('.this-year').forEach(el => el.textContent = y + '년');
})();

/* ════════════════════════════════════════════
   로그인 · 회원가입 (아이디 · 비밀번호)
   - 무료 풀이는 로그인 없이 이용, 결제 · 구매 내역은 로그인 필수
   - 로그인하면 이 브라우저로 저장한 사주 · 성찰 일기가 계정으로 옮겨진다
   - 로그인 정보는 서버 세션 + HttpOnly 쿠키 (자바스크립트에서 읽을 수 없음)
   ════════════════════════════════════════════ */
const authModal = document.getElementById('auth');
let authNext = null;

async function loadMe() {
  try { state.me = await request('/api/auth/me'); } catch { state.me = { loggedIn: false }; }
  renderAccount();
}
function renderAccount() {
  const btn = document.getElementById('nav-account');
  btn.classList.toggle('on', !!state.me?.loggedIn);
  document.getElementById('nav-account-text').textContent = state.me?.loggedIn ? `${state.me.username} 님` : '로그인';
}

const PW_RULE = '영문 + 숫자 8자 이상';
function pwProblem(pw, again) {
  if (pw.length < 8 || !/[A-Za-z]/.test(pw) || !/\d/.test(pw)) return '비밀번호는 영문과 숫자를 섞어 8자 이상으로 정해주세요.';
  if (again !== undefined && pw !== again) return '비밀번호 확인이 일치하지 않아요.';
  return '';
}
const field = (id, label, type = 'text', extra = '') =>
  `<div class="field"><label for="${id}">${label}</label><input class="input" id="${id}" type="${type}" ${extra}></div>`;

function openAuth(mode = 'login', reason = '', next = null) {
  authNext = next;
  const body = document.getElementById('auth-body');
  const title = document.getElementById('auth-title');
  authModal.classList.add('open');

  // 로그인한 상태: 내 계정 (구매 내역 · 비밀번호 변경 · 복구 코드 · 로그아웃)
  if (state.me?.loggedIn) {
    title.textContent = '내 계정';
    body.innerHTML = `<div class="auth-me">
        <span class="who">${esc(state.me.username)} 님</span>
        <p class="notice">로그인한 동안 저장한 사주 · 구매한 리포트 · 성찰 일기는 계정에 저장돼서, 다른 기기에서 로그인해도 그대로 보여요.</p>
      </div>
      <div class="modal-actions">
        <button class="btn btn-line" data-buys>구매 내역</button>
        <button class="btn btn-primary" id="auth-logout">로그아웃</button>
      </div>
      <details class="acct-more"><summary>비밀번호 바꾸기</summary>
        <form class="auth-form" id="pw-form" novalidate>
          ${field('c-cur', '지금 비밀번호', 'password', 'maxlength="64" autocomplete="current-password"')}
          ${field('c-new', '새 비밀번호', 'password', `maxlength="64" autocomplete="new-password" placeholder="${PW_RULE}"`)}
          ${field('c-new2', '새 비밀번호 확인', 'password', 'maxlength="64" autocomplete="new-password"')}
          <p class="pay-error" id="pw-error" role="alert"></p>
          <button class="btn btn-line" type="submit">비밀번호 바꾸기</button>
        </form></details>
      <details class="acct-more"><summary>복구 코드 새로 받기</summary>
        <form class="auth-form" id="rc-form" novalidate>
          <p class="notice">복구 코드를 잃어버렸다면 새로 받으세요. 예전 코드는 더 이상 쓸 수 없어요.</p>
          ${field('r-pw', '비밀번호 확인', 'password', 'maxlength="64" autocomplete="current-password"')}
          <p class="pay-error" id="rc-error" role="alert"></p>
          <button class="btn btn-line" type="submit">새 복구 코드 받기</button>
        </form></details>`;
    document.getElementById('auth-logout').onclick = logout;
    document.getElementById('pw-form').onsubmit = async e => {
      e.preventDefault();
      const err = document.getElementById('pw-error'), cur = $v('c-cur'), nw = $v('c-new');
      err.textContent = pwProblem(nw, $v('c-new2'));
      if (err.textContent) return;
      try {
        await request('/api/auth/change-password', { method: 'POST', body: JSON.stringify({ currentPassword: cur, newPassword: nw }) });
        closeAuth(); toast('비밀번호를 바꿨어요');
      } catch (x) { err.textContent = x.message; }
    };
    document.getElementById('rc-form').onsubmit = async e => {
      e.preventDefault();
      const err = document.getElementById('rc-error');
      try {
        const r = await request('/api/auth/recovery-code', { method: 'POST', body: JSON.stringify({ password: $v('r-pw') }) });
        showCode(r.recoveryCode, '새 복구 코드를 받았어요', null);
      } catch (x) { err.textContent = x.message; }
    };
    return;
  }

  // 아이디 찾기 · 비밀번호 재설정 (복구 코드)
  if (mode === 'recover') {
    title.textContent = '아이디 · 비밀번호 찾기';
    body.innerHTML = `
      <p class="notice">가입할 때 받은 <b>복구 코드</b>(예: K7QM-2XPA-9RTD-HV4N)로 아이디를 찾거나 비밀번호를 새로 정할 수 있어요.</p>
      <details class="acct-more" open><summary>아이디 찾기</summary>
        <form class="auth-form" id="fid-form" novalidate>
          ${field('f-code', '복구 코드', 'text', 'maxlength="40" autocomplete="off" autocapitalize="characters" spellcheck="false"')}
          <p class="pay-error" id="fid-error" role="alert"></p>
          <button class="btn btn-line" type="submit">아이디 찾기</button>
        </form></details>
      <details class="acct-more"><summary>비밀번호 재설정</summary>
        <form class="auth-form" id="rst-form" novalidate>
          ${field('p-id', '아이디', 'text', 'maxlength="20" autocomplete="username" autocapitalize="none" spellcheck="false"')}
          ${field('p-code', '복구 코드', 'text', 'maxlength="40" autocomplete="off" autocapitalize="characters" spellcheck="false"')}
          ${field('p-new', '새 비밀번호', 'password', `maxlength="64" autocomplete="new-password" placeholder="${PW_RULE}"`)}
          ${field('p-new2', '새 비밀번호 확인', 'password', 'maxlength="64" autocomplete="new-password"')}
          <p class="pay-error" id="rst-error" role="alert"></p>
          <button class="btn btn-primary" type="submit">비밀번호 새로 정하기</button>
        </form></details>
      <div class="auth-links"><button type="button" data-auth-mode="login">로그인으로 돌아가기</button></div>`;
    document.getElementById('fid-form').onsubmit = async e => {
      e.preventDefault();
      const err = document.getElementById('fid-error');
      err.textContent = '';
      try {
        const r = await request('/api/auth/find-id', { method: 'POST', body: JSON.stringify({ recoveryCode: $v('f-code') }) });
        err.innerHTML = `<span style="color:var(--text)">아이디는 <b>${esc(r.username)}</b> 이에요.</span>`;
        document.getElementById('p-id').value = r.username;
      } catch (x) { err.textContent = x.message; }
    };
    document.getElementById('rst-form').onsubmit = async e => {
      e.preventDefault();
      const err = document.getElementById('rst-error'), nw = $v('p-new');
      err.textContent = pwProblem(nw, $v('p-new2'));
      if (err.textContent) return;
      try {
        const r = await request('/api/auth/reset-password', { method: 'POST',
          body: JSON.stringify({ username: $v('p-id').trim(), recoveryCode: $v('p-code'), newPassword: nw }) });
        showCode(r.recoveryCode, '비밀번호를 새로 정했어요. 이제 새 비밀번호로 로그인하세요.', () => openAuth('login', '', next));
      } catch (x) { err.textContent = x.message; }
    };
    bindAuthTabs(reason, next);
    return;
  }

  const signup = mode === 'signup';
  title.textContent = signup ? '회원가입' : '로그인';
  body.innerHTML = `
    <div class="auth-tabs" role="tablist">
      <button role="tab" data-auth-mode="login" aria-selected="${!signup}">로그인</button>
      <button role="tab" data-auth-mode="signup" aria-selected="${signup}">회원가입</button>
    </div>
    ${reason ? `<p class="auth-reason">${esc(reason)}</p>` : ''}
    <form class="auth-form" id="auth-form" novalidate>
      ${field('a-id', '아이디', 'text', `maxlength="20" autocomplete="username" autocapitalize="none" spellcheck="false" placeholder="${signup ? '영어 소문자 · 숫자 · _ 4~20자' : '아이디'}"`)}
      ${field('a-pw', '비밀번호', 'password', `maxlength="64" autocomplete="${signup ? 'new-password' : 'current-password'}" placeholder="${signup ? PW_RULE : '비밀번호'}"`)}
      ${signup ? field('a-pw2', '비밀번호 확인', 'password', 'maxlength="64" autocomplete="new-password"') + `
      <label class="check"><input type="checkbox" id="a-agree"> <span><button type="button" class="f-link" data-help="terms">이용약관</button> · <button type="button" class="f-link" data-help="privacy">개인정보 처리방침</button>에 동의해요 <span class="req">*</span></span></label>` : ''}
      <p class="notice">${signup ? '가입하면 아이디 · 비밀번호를 잊었을 때 쓰는 복구 코드를 드려요. 로그인 전에 저장한 사주와 성찰 일기는 계정으로 옮겨져요.' : '비밀번호를 5번 틀리면 10분 동안 로그인할 수 없어요.'}</p>
      <p class="pay-error" id="auth-error" role="alert"></p>
      <button class="btn btn-primary" id="auth-submit" type="submit">${signup ? '가입하고 시작하기' : '로그인'}</button>
    </form>
    ${signup ? '' : '<div class="auth-links"><button type="button" data-auth-mode="recover">아이디 · 비밀번호를 잊었어요</button></div>'}`;
  bindAuthTabs(reason, next);
  document.getElementById('auth-form').onsubmit = e => { e.preventDefault(); submitAuth(signup); };
  setTimeout(() => document.getElementById('a-id')?.focus(), 50);
}
const $v = id => document.getElementById(id)?.value || '';
function bindAuthTabs(reason, next) {
  document.getElementById('auth-body').querySelectorAll('[data-auth-mode]').forEach(b => b.onclick = () => openAuth(b.dataset.authMode, reason, next));
}

/** 복구 코드 보여주기 (가입 직후 · 재설정 후 · 새로 받기). 한 번만 보여주니 꼭 저장하게 안내 */
function showCode(code, headline, then) {
  document.getElementById('auth-title').textContent = '복구 코드';
  document.getElementById('auth-body').innerHTML = `
    <p class="auth-reason">${esc(headline)}</p>
    <p class="body" style="font-size:14px">아래 복구 코드는 <b>지금 한 번만</b> 보여드려요. 아이디나 비밀번호를 잊었을 때 꼭 필요하니 캡처하거나 메모해 두세요.</p>
    <div class="code-box" id="code-box">${esc(code)}</div>
    <div class="modal-actions">
      <button class="btn btn-line" id="code-copy">복사하기</button>
      <button class="btn btn-primary" id="code-done">저장했어요</button>
    </div>`;
  document.getElementById('code-copy').onclick = async () => {
    try { await navigator.clipboard.writeText(code); toast('복구 코드를 복사했어요'); } catch { toast('코드를 길게 눌러 복사해주세요'); }
  };
  document.getElementById('code-done').onclick = () => { if (then) then(); else closeAuth(); };
}
function closeAuth() { authModal.classList.remove('open'); authNext = null; }

async function submitAuth(signup) {
  const err = document.getElementById('auth-error');
  const username = $v('a-id').trim();
  const password = $v('a-pw');
  err.textContent = '';
  if (!username || !password) { err.textContent = '아이디와 비밀번호를 입력해주세요.'; return; }
  if (signup) {
    if (!/^[a-z0-9_]{4,20}$/.test(username)) { err.textContent = '아이디는 영어 소문자 · 숫자 · _ 로 4~20자예요.'; return; }
    err.textContent = pwProblem(password, $v('a-pw2'));
    if (err.textContent) return;
    if (!document.getElementById('a-agree').checked) { err.textContent = '이용약관과 개인정보 처리방침에 동의해주세요.'; return; }
  }
  const btn = document.getElementById('auth-submit');
  btn.disabled = true;
  try {
    const me = await request(signup ? '/api/auth/signup' : '/api/auth/login', {
      method: 'POST', body: JSON.stringify(signup ? { username, password, agree: true } : { username, password }) });
    state.me = me; renderAccount();
    const next = authNext;
    await Promise.all([loadProfiles(), loadReflections(), loadShop()]);
    if (!current() && state.profiles.length) setCurrent(state.profiles[0].id);
    refreshTab();
    const done = () => {
      closeAuth();
      toast(`${signup ? '가입했어요' : '로그인했어요'}${me.moved ? ` · 사주 ${me.moved}개를 계정에 저장했어요` : ''} ✨`);
      if (next) next();
    };
    if (signup && me.recoveryCode) showCode(me.recoveryCode, `${me.username} 님, 가입을 환영해요!`, done);
    else done();
  } catch (e) {
    err.textContent = e.message;
  } finally {
    btn.disabled = false;
  }
}

async function logout() {
  try { await request('/api/auth/logout', { method: 'POST' }); } catch {}
  // 로그아웃하면 이 브라우저는 새 손님 ID로 시작 (계정 데이터는 서버에 그대로 남아 있음)
  writeStore('saju.clientId', null);
  state.clientId = getClientId();
  state.me = { loggedIn: false }; setCurrent(null); state.partnerId = null;
  renderAccount(); closeAuth();
  try { await Promise.all([loadProfiles(), loadReflections(), loadShop()]); } catch {}
  showTab('home');
  toast('로그아웃했어요');
}

/** 지금 보고 있는 탭을 다시 그린다 (로그인 직후 데이터가 바뀌었을 때) */
function refreshTab() {
  const tab = document.querySelector('.tab[aria-selected="true"]')?.dataset.tab || 'home';
  showTab(tab);
}

document.getElementById('nav-account').onclick = () => openAuth('login');
document.getElementById('auth-close').onclick = closeAuth;
authModal.addEventListener('click', e => { if (e.target === authModal) closeAuth(); });
document.addEventListener('click', e => {
  const o = e.target.closest('[data-auth-open]');
  if (o) { document.getElementById('buys').classList.remove('open'); openAuth(o.dataset.authOpen); }
});

/* ════════════════════════════════════════════
   의견 보내기 (건의사항) → 관리자 페이지(/admin.html)에서 확인
   ════════════════════════════════════════════ */
const fbModal = document.getElementById('fb');
let fbCat = null, fbPage = null;
/** 종류마다 무엇을 적으면 되는지 안내 (빈 칸 안내문) */
const FB_HINT = {
  BUG: '어느 화면에서 무엇을 눌렀을 때 어떤 문제가 생겼나요?\n예) 사랑운 > 궁합에서 상대를 바꿨는데 결과가 그대로예요.',
  IDEA: '있었으면 하는 기능과 그 이유를 적어주세요.\n예) 오늘의 운세를 매일 알림으로 받고 싶어요.',
  READING: '어느 탭의 어떤 풀이가 어떻게 느껴졌나요?\n예) 연애운의 올해 흐름이 제 상황과 달랐어요.',
  ETC: '자유롭게 남겨주세요.',
};
function setFbCat(cat) {
  fbCat = cat;
  fbModal.querySelectorAll('[data-fbcat]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.fbcat === cat)));
  document.getElementById('fb-text').placeholder = FB_HINT[cat] || '먼저 위에서 의견 종류를 골라주세요.';
}
/** preset: { cat, text, page, hint } — "풀이가 안 맞아요"처럼 내용을 미리 채워서 열 때 */
function openFeedback(preset = {}) {
  fbPage = preset.page || null;
  setFbCat(preset.cat || null);
  // "풀이가 안 맞아요"로 열면 종류는 '풀이 내용'으로 고정하고 버튼은 숨긴다 (내용 틀이 풀이용이라서)
  fbModal.querySelector('.fb-cats').hidden = !!preset.fixed;
  document.getElementById('fb-title').textContent = preset.title || '의견 보내기';
  document.getElementById('fb-text').value = preset.text || '';
  document.getElementById('fb-count').textContent = `${(preset.text || '').length} / 1000`;
  document.getElementById('fb-error').textContent = '';
  fbModal.classList.add('open');
}
function closeFeedback() { fbModal.classList.remove('open'); }
document.getElementById('fb-open').onclick = () => openFeedback();
document.getElementById('fb-close').onclick = closeFeedback;
document.getElementById('fb-cancel').onclick = closeFeedback;
fbModal.addEventListener('click', e => {
  if (e.target === fbModal) return closeFeedback();
  const c = e.target.closest('[data-fbcat]');
  if (c) setFbCat(c.dataset.fbcat);
});
document.addEventListener('click', e => {
  if (e.target.closest('[data-feedback]')) { e.preventDefault(); openFeedback(); }
  // 고민 풀이 아래 "풀이가 안 맞아요": 내 고민과 받은 풀이를 미리 채워서 열기 → 관리자 페이지에서 보고 단어를 추가한다
  if (e.target.closest('[data-concern-miss]')) {
    const c = current()?.saju.concern;
    if (!c) return;
    const text = `[내가 적은 고민]\n${c.text || '(문장 없이 분류만 고름: ' + c.label + ')'}\n\n[받은 풀이]\n${c.topic || c.label} 주제로 풀이가 나왔어요.\n\n[어떤 점이 안 맞았나요? 원했던 답은?]\n`;
    openFeedback({ cat: 'READING', page: 'concern', title: '풀이가 안 맞았다면 알려주세요', text, fixed: true });
    const ta = document.getElementById('fb-text');
    setTimeout(() => { ta.focus(); ta.setSelectionRange(ta.value.length, ta.value.length); }, 50);
  }
});
document.getElementById('fb-text').addEventListener('input', e => {
  document.getElementById('fb-count').textContent = `${e.target.value.length} / 1000`;
  document.getElementById('fb-error').textContent = '';
});
document.getElementById('fb-send').onclick = async () => {
  const err = document.getElementById('fb-error'), message = document.getElementById('fb-text').value.trim();
  if (!fbCat) { err.textContent = '어떤 의견인지 골라주세요.'; return; }
  if (message.length < 5) { err.textContent = '내용을 5자 이상 적어주세요.'; return; }
  if (fbPage === 'concern' && /\[어떤 점이 안 맞았나요\? 원했던 답은\?\]\s*$/.test(message)) { err.textContent = '어떤 점이 안 맞았는지 한 줄만 적어주세요.'; return; }
  const btn = document.getElementById('fb-send'); btn.disabled = true;
  const tab = document.querySelector('.tab[aria-selected="true"]')?.dataset.tab || 'home';
  const page = fbPage || (tab === 'marriage' ? 'love-' + state.loveView : tab === 'career' ? 'work-' + state.workView : tab);
  try {
    await request('/api/feedback', { method: 'POST', body: JSON.stringify({ category: fbCat, message, page }) });
    closeFeedback(); toast('의견을 보냈어요. 고마워요! 💌');
  } catch (x) { err.textContent = x.message; }
  finally { btn.disabled = false; }
};

(async function init() {
  try { await Promise.all([loadMe(), loadProfiles(), loadReflections(), loadShop(), loadPayConfig()]); }
  catch (err) { toast('서버에 연결하지 못했어요'); }
  const back = await handlePayReturn();   // 토스 결제창에서 돌아온 경우
  if (back?.done) return;
  let start = back?.tab || location.hash.slice(1);
  if (back?.love) state.loveView = back.love;
  if (start === 'journal') { state.listView = start; start = 'list'; }
  if (start.startsWith('work-')) { state.workView = start.slice(5) === 'study' ? 'study' : 'career'; start = 'career'; }
  if (start.startsWith('love-')) { const v = start.slice(5); if (['love', 'marriage', 'compat', 'reunion'].includes(v)) state.loveView = v; start = 'marriage'; }
  showTab(['home', 'input', 'result', 'year', 'career', 'marriage', 'family', 'list'].includes(start) ? start : 'home');
})();
