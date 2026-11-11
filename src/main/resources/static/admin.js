/* 관리자 페이지: 건의사항 · 회원 · 요약
 * 관리자 비밀번호는 sessionStorage(탭을 닫으면 사라짐)에만 두고, 요청마다 X-Admin-Key 헤더로 보낸다 */
const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const CAT = { BUG: '🐞 불편 · 오류', IDEA: '💡 기능 제안', READING: '🔮 풀이 내용', ETC: '💬 기타' };
const fmt = t => t ? new Date(t).toLocaleString('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-';
let key = '';
try { key = sessionStorage.getItem('ats.adminKey') || ''; } catch {}
let feedback = [];

async function api(path, method = 'GET') {
  const res = await fetch('/api/admin' + path, { method, headers: { 'X-Admin-Key': key } });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) { const e = new Error(data.message || res.status + ' 오류'); e.status = res.status; throw e; }
  return data;
}

async function load() {
  const [s, f, m] = await Promise.all([api('/summary'), api('/feedback'), api('/members')]);
  feedback = f;
  $('stats').innerHTML = [
    ['회원', s.members], ['저장된 사주', s.profiles], ['구매 (사용 중)', s.purchases],
    ['환불', s.refunded], ['매출', Number(s.revenue).toLocaleString('ko-KR') + '원'], ['새 건의사항', s.feedbackNew],
  ].map(([k, v]) => `<div class="stat"><span>${k}</span><b>${esc(v)}</b></div>`).join('');
  renderFeedback();
  $('member-rows').innerHTML = m.length ? m.map(x => `<tr>
      <td style="color:var(--text)">${esc(x.username)}</td><td>${fmt(x.createdAt)}</td><td>${fmt(x.lastLoginAt)}</td>
      <td>${x.locked ? '<span class="tag">잠김</span>' : '정상'}</td>
      <td>${x.locked ? `<button data-unlock="${x.id}">잠금 풀기</button>` : ''}</td></tr>`).join('')
    : '<tr><td colspan="5" class="muted">아직 회원이 없어요.</td></tr>';
}

function renderFeedback() {
  const f = $('fb-filter').value;
  const list = f === 'new' ? feedback.filter(x => !x.done) : f === 'concern' ? feedback.filter(x => x.page === 'concern') : feedback;
  $('fb-rows').innerHTML = list.length ? list.map(x => `<tr class="${x.done ? 'done' : ''}">
      <td class="muted">${fmt(x.createdAt)}</td><td><span class="tag">${CAT[x.category] || esc(x.category)}</span></td>
      <td class="msg">${esc(x.message)}</td><td class="muted">${x.page === 'concern' ? '<span class="tag">고민 풀이</span>' : esc(x.page || '-')}</td><td class="muted">${esc(x.username || '손님')}</td>
      <td><div class="row"><button data-done="${x.id}" data-v="${!x.done}">${x.done ? '되돌리기' : '처리 완료'}</button>
        <button class="danger" data-del="${x.id}">삭제</button></div></td></tr>`).join('')
    : '<tr><td colspan="6" class="muted">표시할 건의사항이 없어요.</td></tr>';
}

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

$('login-form').onsubmit = e => { e.preventDefault(); key = $('key').value; $('login-err').textContent = ''; enter(); };
$('refresh').onclick = () => load().catch(e => alertBox(e));
$('logout').onclick = () => { key = ''; try { sessionStorage.removeItem('ats.adminKey'); } catch {} location.reload(); };
$('fb-filter').onchange = renderFeedback;
function alertBox(e) { $('login-err').textContent = e.message; if (e.status === 403) { $('app').hidden = true; $('login').hidden = false; } }

document.addEventListener('click', async e => {
  const d = e.target.closest('[data-done]'), del = e.target.closest('[data-del]'), un = e.target.closest('[data-unlock]');
  try {
    if (d) { await api(`/feedback/${d.dataset.done}/done?done=${d.dataset.v}`, 'POST'); await load(); }
    if (del) {
      if (del.dataset.confirm !== '1') { del.dataset.confirm = '1'; del.textContent = '정말 삭제'; return; }
      await api(`/feedback/${del.dataset.del}`, 'DELETE'); await load();
    }
    if (un) { await api(`/members/${un.dataset.unlock}/unlock`, 'POST'); await load(); }
  } catch (x) { alertBox(x); }
});

if (key) enter();
