/* 관리자 페이지: 한눈에 보기 · 의견함 · 저장된 사주 · 구매 · 회원
 * 관리자 비밀번호는 sessionStorage(탭을 닫으면 사라짐)에만 두고, 요청마다 X-Admin-Key 헤더로 보낸다
 * 화면에 넣는 모든 글자는 esc()로 바꿔서 넣는다 (사용자가 적은 글로 스크립트가 실행되지 않게) */
const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const won = n => Number(n || 0).toLocaleString('ko-KR') + '원';
const fmt = t => t ? new Date(t).toLocaleString('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-';
const day = t => t ? new Date(t).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' }) : '-';
const ago = t => {
  if (!t) return '-';
  const m = Math.floor((Date.now() - new Date(t)) / 60000);
  return m < 1 ? '방금' : m < 60 ? `${m}분 전` : m < 1440 ? `${Math.floor(m / 60)}시간 전` : m < 10080 ? `${Math.floor(m / 1440)}일 전` : day(t);
};
const isToday = t => t && new Date(t).toDateString() === new Date().toDateString();

const CAT = { BUG: '🐞 불편 · 오류', IDEA: '💡 기능 제안', READING: '🔮 풀이 내용', ETC: '💬 기타' };
const concernTags = v => (v || '').split(',').filter(Boolean).map(k => `<span class="tag">${CONCERN[k] || esc(k)}</span>`).join(' ');
const CONCERN = { LOVE: '💞 연애 · 결혼', CAREER: '🧭 일 · 진로', STUDY: '📚 공부 · 시험', MONEY: '💰 돈 · 재물',
  RELATION: '🤝 인간관계', SELF: '🌙 나 자신 · 마음', FUTURE: '✨ 올해 · 앞날' };
const STATUS = { PAID: '결제 · 안 열어봄', OPENED: '열어봄', REFUNDED: '환불' };
const PARTNER = { COMPAT: '궁합 상대', REUNION: '재회 상대' };

let key = '';
try { key = sessionStorage.getItem('ats.adminKey') || ''; } catch {}
const data = { summary: {}, feedback: [], members: [], profiles: [], purchases: [] };
const ui = { fb: 'new', pf: 'mine', by: 'all', openMember: null, openProfile: null };

async function api(path, method = 'GET') {
  const res = await fetch('/api/admin' + path, { method, headers: { 'X-Admin-Key': key } });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) { const e = new Error(body.message || res.status + ' 오류'); e.status = res.status; throw e; }
  return body;
}

async function load() {
  const [s, f, m, p, b] = await Promise.all([api('/summary'), api('/feedback'), api('/members'), api('/profiles'), api('/purchases')]);
  Object.assign(data, { summary: s, feedback: f, members: m, profiles: p, purchases: b });
  $('updated').textContent = `${new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 기준`;
  $('nav-new').textContent = s.feedbackNew || '';
  renderDash(); renderFeedback(); renderProfiles(); renderBuys(); renderMembers();
}

/* ---------- 한눈에 보기 ---------- */
function renderDash() {
  const s = data.summary, P = data.profiles, B = data.purchases;
  const mine = P.filter(x => !x.partnerKind);
  const todayP = mine.filter(x => isToday(x.createdAt)).length;
  const todayB = B.filter(x => isToday(x.createdAt) && x.status !== 'REFUNDED');
  const todayM = data.members.filter(x => isToday(x.createdAt)).length;
  const tiles = [
    ['pink', '👤', '회원', s.members, todayM ? `오늘 +${todayM}` : '', 'members'],
    ['sky', '🔮', '저장된 사주', mine.length, todayP ? `오늘 +${todayP} · 상대 ${P.length - mine.length}명 별도` : `궁합 · 재회 상대 ${P.length - mine.length}명 별도`, 'profiles'],
    ['mint', '💳', '구매 (사용 중)', s.purchases, todayB.length ? `오늘 +${todayB.length}` : '', 'buys'],
    ['gold', '💰', '매출', won(s.revenue), todayB.length ? `오늘 ${won(todayB.reduce((a, x) => a + x.price, 0))}` : '', 'buys'],
    ['red', '↩', '환불', s.refunded, s.refunded ? won(B.filter(x => x.status === 'REFUNDED').reduce((a, x) => a + x.price, 0)) : '', 'buys:REFUNDED'],
    ['pink', '💬', '새 의견', s.feedbackNew, `전체 ${data.feedback.length}개`, 'fb'],
  ];
  $('stats').innerHTML = tiles.map(([c, ic, k, v, sub, go]) => `
    <div class="stat${k === '새 의견' && v ? ' alert' : ''}" data-go="${go}" role="button" tabindex="0">
      <div class="ic ${c}">${ic}</div>
      <div><span>${k}</span><b>${esc(v)}</b>${sub ? `<span>${esc(sub)}</span>` : ''}</div>
      <span class="go">›</span></div>`).join('');

  $('recent-buys').innerHTML = B.length ? B.slice(0, 6).map(x => `
    <div class="mini-item" data-go="buys"><span class="tag ${x.status}">${STATUS[x.status]}</span>
      <span class="mini-text">${esc(x.product)} · ${esc(x.profileName || '-')}${x.partnerName ? ' ♡ ' + esc(x.partnerName) : ''}</span>
      <span class="muted">${won(x.price)} · ${ago(x.createdAt)}</span></div>`).join('')
    : '<p class="muted">아직 구매가 없어요.</p>';

  const byProduct = {};
  B.filter(x => x.status !== 'REFUNDED').forEach(x => { const r = byProduct[x.product] ||= { n: 0, sum: 0 }; r.n++; r.sum += x.price; });
  const rows = Object.entries(byProduct).sort((a, b) => b[1].sum - a[1].sum);
  const max = Math.max(1, ...rows.map(r => r[1].sum));
  $('bars-product').innerHTML = rows.length ? rows.map(([name, r]) => `
    <div class="bar-row" style="grid-template-columns:130px 1fr 96px"><span class="clip" title="${esc(name)}">${esc(name.replace(' 심화 리포트', '').replace(' 리포트', ''))}</span>
      <div class="bar"><i style="width:${r.sum / max * 100}%;background:var(--gold)"></i></div><b>${won(r.sum)} <span class="muted">${r.n}건</span></b></div>`).join('')
    : '<p class="muted">아직 매출이 없어요.</p>';

  $('recent').innerHTML = data.feedback.length ? data.feedback.slice(0, 5).map(x => `
    <div class="mini-item" data-go="fb"><span class="tag ${esc(x.category)}">${CAT[x.category] || esc(x.category)}</span>
      <span class="mini-text">${esc(firstLine(x))}</span><span class="muted">${ago(x.createdAt)}</span></div>`).join('')
    : '<p class="muted">아직 의견이 없어요.</p>';

  const cc = {};
  mine.forEach(x => (x.concernType ? x.concernType.split(',') : ['NONE']).forEach(k => { cc[k] = (cc[k] || 0) + 1; }));
  const crow = Object.entries(cc).sort((a, b) => b[1] - a[1]);
  const cmax = Math.max(1, ...crow.map(r => r[1]));
  $('bars-concern').innerHTML = crow.length ? crow.map(([k, n]) => `
    <div class="bar-row"><span>${k === 'NONE' ? '고민 안 고름' : CONCERN[k] || esc(k)}</span>
      <div class="bar"><i style="width:${n / cmax * 100}%"></i></div><b>${n}</b></div>`).join('')
    : '<p class="muted">아직 저장된 사주가 없어요.</p>';
}

/* ---------- 의견함 ---------- */
/** "풀이 안 맞아요" 글은 [내가 적은 고민] [받은 풀이] [원했던 답] 으로 나눠서 보여준다 */
function parseConcern(msg) {
  const get = label => { const m = msg.match(new RegExp('\\[' + label + '[^\\]]*\\]\\n?([\\s\\S]*?)(?=\\n\\[|$)')); return m ? m[1].trim() : ''; };
  const q = get('내가 적은 고민'), a = get('받은 풀이'), w = get('어떤 점이 안 맞았나요');
  return q || a || w ? { q, a, w } : null;
}
const firstLine = x => { const c = x.page === 'concern' && parseConcern(x.message); return c ? `고민 "${c.q}" → ${c.w || '(이유 안 적음)'}` : x.message; };

function renderFeedback() {
  const F = data.feedback, q = $('fb-q').value.trim().toLowerCase();
  const filters = [['new', '처리 안 함', F.filter(x => !x.done).length], ['concern', '🔮 풀이 안 맞아요', F.filter(x => x.page === 'concern').length],
    ...Object.entries(CAT).map(([k, v]) => [k, v, F.filter(x => x.category === k).length]), ['done', '✓ 처리 완료', F.filter(x => x.done).length], ['all', '전체', F.length]];
  $('fb-filters').innerHTML = filters.map(([k, label, n]) => `<button class="chip" data-fbf="${k}" aria-pressed="${ui.fb === k}">${label}<em>${n}</em></button>`).join('');
  const list = F.filter(x => ui.fb === 'new' ? !x.done : ui.fb === 'done' ? x.done : ui.fb === 'concern' ? x.page === 'concern' : ui.fb === 'all' ? true : x.category === ui.fb)
    .filter(x => !q || (x.message + ' ' + (x.username || '')).toLowerCase().includes(q));
  $('fb-list').innerHTML = list.length ? list.map(x => {
    const c = x.page === 'concern' && parseConcern(x.message);
    const body = c ? `<dl class="qa">
        <div><dt>사용자가 적은 고민</dt><dd>${esc(c.q || '-')}</dd></div>
        <div><dt>받은 풀이</dt><dd>${esc(c.a || '-')}</dd></div>
        <div class="want"><dt>안 맞았던 점 · 원했던 답</dt><dd>${esc(c.w || '(적지 않음)')}</dd></div></dl>`
      : `<div class="fb-msg">${esc(x.message)}</div>`;
    return `<article class="fb ${x.done ? 'done' : 'new'}">
      <div class="fb-top"><span class="tag ${esc(x.category)}">${CAT[x.category] || esc(x.category)}</span>
        ${c ? '<span class="tag">고민 풀이</span>' : x.page ? `<span class="tag">화면: ${esc(x.page)}</span>` : ''}
        ${x.done ? '<span class="tag state-done">✓ 처리 완료</span>' : ''}
        <span class="muted who">${esc(x.username || '손님')} · ${fmt(x.createdAt)}</span></div>
      ${body}
      <div class="fb-actions"><button class="btn sm ${x.done ? '' : 'ok'}" data-done="${x.id}" data-v="${!x.done}">${x.done ? '되돌리기' : '✓ 처리 완료'}</button>
        <button class="btn sm danger" data-del="${x.id}">삭제</button></div></article>`;
  }).join('') : `<div class="empty"><b>🌙</b>${ui.fb === 'new' ? '처리할 의견이 없어요. 다 봤어요!' : '표시할 의견이 없어요.'}</div>`;
}

/* ---------- 저장된 사주 ---------- */
function renderProfiles() {
  const P = data.profiles, q = $('pf-q').value.trim().toLowerCase();
  const filters = [['mine', '내 사주', P.filter(x => !x.partnerKind).length], ['concern', '고민 적은 것', P.filter(x => !x.partnerKind && x.concernText).length],
    ['COMPAT', '💞 궁합 상대', P.filter(x => x.partnerKind === 'COMPAT').length], ['REUNION', '🌙 재회 상대', P.filter(x => x.partnerKind === 'REUNION').length], ['all', '전체', P.length]];
  $('pf-filters').innerHTML = filters.map(([k, label, n]) => `<button class="chip" data-pff="${k}" aria-pressed="${ui.pf === k}">${label}<em>${n}</em></button>`).join('');
  const list = P.filter(x => ui.pf === 'mine' ? !x.partnerKind : ui.pf === 'concern' ? !x.partnerKind && x.concernText : ui.pf === 'all' ? true : x.partnerKind === ui.pf)
    .filter(x => !q || [x.name, x.owner, x.concernText, x.partnerOfName].join(' ').toLowerCase().includes(q));
  $('pf-rows').innerHTML = list.length ? list.map(x => {
    const open = ui.openProfile === x.id;
    const who = x.partnerKind ? `<span class="tag">${PARTNER[x.partnerKind]}</span> <span class="muted">${esc(x.partnerOfName || '?')} 님의</span>` : esc(x.owner);
    const concern = x.partnerKind ? '<span class="muted">-</span>'
      : x.concernType || x.concernText ? `${x.concernType ? concernTags(x.concernType) + ' ' : ''}<span class="muted">${esc((x.concernText || '').slice(0, 24))}${(x.concernText || '').length > 24 ? '…' : ''}</span>`
      : '<span class="muted">-</span>';
    const row = `<tr class="click${open ? ' open' : ''}" data-pf="${x.id}">
      <td class="name">${esc(x.name)} <span class="muted">${x.gender === 'F' ? '여' : x.gender === 'M' ? '남' : ''}</span></td>
      <td>${who}</td><td>${x.birthYear ?? '-'}년 <span class="muted">${x.calendar === 'LUNAR' ? '음력' : '양력'}${x.timeKnown ? '' : ' · 시간 모름'}</span></td>
      <td class="clip">${concern}</td><td class="num">${x.purchases ? x.purchases + '건' : '<span class="muted">-</span>'}</td><td class="muted">${fmt(x.createdAt)}</td></tr>`;
    if (!open) return row;
    const buys = data.purchases.filter(b => b.profileName === x.name && b.owner === x.owner);
    return row + `<tr class="detail"><td colspan="6"><div class="detail-grid">
      <div><h4>적은 고민</h4>${x.concernText ? `<div class="quote">${esc(x.concernText)}</div>` : '<p class="muted">고민 글을 적지 않았어요.</p>'}</div>
      <div><h4>이 사주로 산 것</h4>${buys.length ? `<ul>${buys.map(b => `<li><span class="tag ${b.status}">${STATUS[b.status]}</span>${esc(b.product)}${b.partnerName ? ` <span class="muted">♡ ${esc(b.partnerName)}</span>` : ''} <span class="muted">${won(b.price)} · ${day(b.createdAt)}</span></li>`).join('')}</ul>` : '<p class="muted">구매 없음</p>'}</div>
    </div></td></tr>`;
  }).join('') : '<tr><td colspan="6" class="muted">표시할 사주가 없어요.</td></tr>';
}

/* ---------- 구매 · 매출 ---------- */
function renderBuys() {
  const B = data.purchases, q = $('by-q').value.trim().toLowerCase();
  const active = B.filter(x => x.status !== 'REFUNDED');
  $('buy-sum').innerHTML = `사용 중 <b>${active.length}건 · ${won(active.reduce((a, x) => a + x.price, 0))}</b> · 환불 ${B.length - active.length}건 · 아직 안 열어본 구매만 환불할 수 있어요.`;
  const filters = [['all', '전체', B.length], ['PAID', '결제 · 안 열어봄', B.filter(x => x.status === 'PAID').length],
    ['OPENED', '열어봄', B.filter(x => x.status === 'OPENED').length], ['REFUNDED', '환불', B.filter(x => x.status === 'REFUNDED').length]];
  $('by-filters').innerHTML = filters.map(([k, label, n]) => `<button class="chip" data-byf="${k}" aria-pressed="${ui.by === k}">${label}<em>${n}</em></button>`).join('');
  const list = B.filter(x => ui.by === 'all' || x.status === ui.by)
    .filter(x => !q || [x.product, x.owner, x.profileName, x.partnerName].join(' ').toLowerCase().includes(q));
  $('by-rows').innerHTML = list.length ? list.map(x => `<tr>
      <td class="name">${esc(x.product)}</td><td class="num">${won(x.price)}</td><td>${esc(x.owner)}</td>
      <td>${esc(x.profileName || '-')}${x.partnerName ? ` <span class="muted">♡ ${esc(x.partnerName)}</span>` : ''}</td>
      <td><span class="tag ${x.status}">${STATUS[x.status]}</span>${x.status === 'REFUNDED' ? ` <span class="muted">${day(x.refundedAt)}</span>` : x.status === 'OPENED' ? ` <span class="muted">${day(x.openedAt)}</span>` : ''}</td>
      <td class="muted">${fmt(x.createdAt)}</td></tr>`).join('')
    : '<tr><td colspan="6" class="muted">표시할 구매가 없어요.</td></tr>';
}

/* ---------- 회원 ---------- */
function renderMembers() {
  const q = $('mb-q').value.trim().toLowerCase();
  const list = data.members.filter(x => !q || x.username.toLowerCase().includes(q));
  $('member-rows').innerHTML = list.length ? list.map(x => {
    const open = ui.openMember === x.id;
    const row = `<tr class="click${open ? ' open' : ''}" data-mb="${x.id}">
      <td class="name">${esc(x.username)}</td><td class="num">${x.profiles}</td><td class="num">${x.purchases}</td><td class="num">${won(x.spent)}</td>
      <td class="muted">${fmt(x.createdAt)}</td><td class="muted">${ago(x.lastLoginAt)}</td>
      <td>${x.locked ? '<span class="tag REFUNDED">잠김</span>' : '<span class="tag state-done">정상</span>'}</td>
      <td>${x.locked ? `<button class="btn sm" data-unlock="${x.id}">잠금 풀기</button>` : ''}</td></tr>`;
    if (!open) return row;
    const ps = data.profiles.filter(p => p.owner === x.username && !p.partnerKind);
    const bs = data.purchases.filter(b => b.owner === x.username);
    return row + `<tr class="detail"><td colspan="8"><div class="detail-grid">
      <div><h4>저장한 사주 ${ps.length}개</h4>${ps.length ? `<ul>${ps.map(p => `<li>${esc(p.name)} <span class="muted">${p.birthYear ?? '-'}년생</span>${p.concernType ? ' ' + concernTags(p.concernType) : ''}</li>`).join('')}</ul>` : '<p class="muted">없음</p>'}</div>
      <div><h4>구매 ${bs.length}건</h4>${bs.length ? `<ul>${bs.map(b => `<li><span class="tag ${b.status}">${STATUS[b.status]}</span>${esc(b.product)} <span class="muted">${esc(b.profileName || '')}${b.partnerName ? ' ♡ ' + esc(b.partnerName) : ''} · ${won(b.price)} · ${day(b.createdAt)}</span></li>`).join('')}</ul>` : '<p class="muted">없음</p>'}</div>
    </div></td></tr>`;
  }).join('') : '<tr><td colspan="8" class="muted">아직 회원이 없어요.</td></tr>';
}

/* ---------- 화면 이동 · 공통 ---------- */
function go(target) {
  const [page, sub] = target.split(':');
  document.querySelectorAll('.page').forEach(p => p.hidden = p.id !== 'page-' + page);
  document.querySelectorAll('.nav[data-page]').forEach(n => n.setAttribute('aria-current', String(n.dataset.page === page)));
  if (page === 'buys' && sub) { ui.by = sub; renderBuys(); }
  window.scrollTo(0, 0);
}
function toast(msg) { const t = $('toast'); t.textContent = msg; t.classList.add('show'); clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove('show'), 1800); }

async function enter() {
  try {
    await load();
    try { sessionStorage.setItem('ats.adminKey', key); } catch {}
    $('login').hidden = true; $('app').hidden = false;
  } catch (e) {
    $('login-err').textContent = e.message; key = '';
    try { sessionStorage.removeItem('ats.adminKey'); } catch {}
  }
}
function fail(e) {
  if (e.status === 403 || e.status === 401) { $('app').hidden = true; $('login').hidden = false; $('login-err').textContent = e.message; }
  else toast(e.message);
}

$('login-form').onsubmit = e => { e.preventDefault(); key = $('key').value; $('login-err').textContent = ''; enter(); };
$('logout').onclick = () => { key = ''; try { sessionStorage.removeItem('ats.adminKey'); } catch {} location.reload(); };
$('fb-q').oninput = renderFeedback; $('pf-q').oninput = renderProfiles; $('by-q').oninput = renderBuys; $('mb-q').oninput = renderMembers;

document.addEventListener('keydown', e => { if (e.key === 'Enter' && e.target.matches('.stat[data-go]')) go(e.target.dataset.go); });
document.addEventListener('click', async e => {
  const t = e.target;
  const nav = t.closest('.nav[data-page]'); if (nav) return go(nav.dataset.page);
  const g = t.closest('[data-go]'); if (g) return go(g.dataset.go);
  if (t.closest('[data-refresh]')) { try { await load(); toast('새로 불러왔어요'); } catch (x) { fail(x); } return; }
  const fbf = t.closest('[data-fbf]'); if (fbf) { ui.fb = fbf.dataset.fbf; return renderFeedback(); }
  const pff = t.closest('[data-pff]'); if (pff) { ui.pf = pff.dataset.pff; return renderProfiles(); }
  const byf = t.closest('[data-byf]'); if (byf) { ui.by = byf.dataset.byf; return renderBuys(); }
  const d = t.closest('[data-done]'), del = t.closest('[data-del]'), un = t.closest('[data-unlock]');
  try {
    if (d) { await api(`/feedback/${d.dataset.done}/done?done=${d.dataset.v}`, 'POST'); await load(); return toast(d.dataset.v === 'true' ? '처리 완료로 표시했어요' : '되돌렸어요'); }
    if (del) {
      if (del.dataset.confirm !== '1') { del.dataset.confirm = '1'; del.classList.add('armed'); del.textContent = '한 번 더 누르면 삭제'; return; }
      await api(`/feedback/${del.dataset.del}`, 'DELETE'); await load(); return toast('삭제했어요');
    }
    if (un) { await api(`/members/${un.dataset.unlock}/unlock`, 'POST'); await load(); return toast('잠금을 풀었어요'); }
  } catch (x) { return fail(x); }
  const pr = t.closest('[data-pf]'); if (pr) { ui.openProfile = ui.openProfile === +pr.dataset.pf ? null : +pr.dataset.pf; return renderProfiles(); }
  const mr = t.closest('[data-mb]'); if (mr) { ui.openMember = ui.openMember === +mr.dataset.mb ? null : +mr.dataset.mb; return renderMembers(); }
});

if (key) enter();
