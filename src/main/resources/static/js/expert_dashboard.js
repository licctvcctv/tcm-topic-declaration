let currentUser = null;
let categoriesMap = {};

document.addEventListener('DOMContentLoaded', async () => {
    // 1. 安全校验
    currentUser = tokenStorage.getUserInfo();
    const token = tokenStorage.getToken();
    if (!token || !currentUser || currentUser.role !== 'EXPERT') {
        tokenStorage.clear();
        window.location.href = '/login.html';
        return;
    }

    // 2. 顶部名片
    document.getElementById('profile-avatar').textContent = currentUser.realName.substring(0, 1);
    document.getElementById('profile-name').textContent = `${currentUser.realName} (评审专家)`;

    // 3. 加载分类字典
    try {
        const cats = await api.topic.getCategories();
        cats.forEach(c => categoriesMap[c.id] = c.name);
    } catch(e){}

    // 4. 初始化专家面板
    document.getElementById('expert-name-title').textContent = `评审专家：${currentUser.realName} (主要学术方向: ${categoriesMap[currentUser.majorDirection] || currentUser.majorDirection})`;
    switchExpertPanel('invites');
});

function showToast(message, type = 'success') {
    if (typeof showGlobalToast === 'function') showGlobalToast(message, type);
}

function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

async function renderTasks() {
    try {
        const list = await api.expert.getTasks();
        
        // 1. 待回应邀请卡片列表
        const pending = list.filter(t => t.invitationStatus === 0);
        const invContainer = document.getElementById('expert-invitation-cards');
        const emptyEl = document.getElementById('expert-invitations-empty');
        
        if (pending.length > 0) {
            if (emptyEl) emptyEl.style.display = 'none';
            invContainer.style.display = 'grid';
            invContainer.innerHTML = '';
            pending.forEach(inv => {
                const card = document.createElement('div');
                card.className = 'stat-card';
                card.style.background = 'linear-gradient(145deg, #ffffff, #fffbeb)';
                card.style.border = '1px solid #fef3c7';
                card.innerHTML = `
                    <div>
                        <span class="label" style="color:#d97706;">新的课题盲评评审邀请</span>
                        <h4 style="margin: 0.5rem 0; font-family:'Noto Serif SC', serif; font-size:1.05rem; color:var(--primary);">课题序号: ${inv.topicId}</h4>
                        <p style="font-size:0.8rem; color:var(--gray);">研究方向: ${categoriesMap[inv.majorDirection] || '双盲方向'}</p>
                    </div>
                    <div style="display: flex; gap: 0.5rem; margin-top: 1rem; justify-content: flex-end;">
                        <button class="btn btn-outline btn-sm" onclick="respond(${inv.id}, 2)" style="border-color:#ef4444; color:#ef4444;">拒绝接受</button>
                        <button class="btn btn-primary btn-sm" onclick="respond(${inv.id}, 1)">接受邀请</button>
                    </div>
                `;
                invContainer.appendChild(card);
            });
        } else {
            invContainer.style.display = 'none';
            if (emptyEl) emptyEl.style.display = 'block';
        }

        // 2. 已接受任务列表
        const accepted = list.filter(t => t.invitationStatus === 1);
        const tbody = document.getElementById('expert-tasks-body');
        tbody.innerHTML = '';

        if (accepted.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无已接受的评审课题任务</td></tr>';
            return;
        }

        accepted.forEach(t => {
            const tr = document.createElement('tr');
            
            const statusText = t.status === 1 ? '已提交(锁定)' : '评审中(草稿)';
            const statusBadge = t.status === 1 ? 'badge-success' : 'badge-pending';
            
            const scoreVal = t.score !== null ? t.score.toFixed(2) : '-';
            const recText = t.status === 1 ? (t.recommendResult === 1 ? '推荐立项' : '不推荐') : '-';
            
            const downloadBtn = t.anonymousPageUrl ? 
                `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.anonymousPageUrl}', '双盲活页_${t.topicId}')">下载活页</button>` 
                : '<span style="color:red;">未上传活页</span>';

            let opt = t.status === 0 ? `<button class="btn btn-primary btn-sm" onclick="openReviewModal(${t.id})">录入打分意见</button>`
                                     : `<span style="font-size:0.8rem; color:var(--gray);">已锁定</span>`;

            tr.innerHTML = `
                <td><strong>课题# ${t.topicId}</strong></td>
                <td>${categoriesMap[t.majorDirection] || '双盲方向'}</td>
                <td>${downloadBtn}</td>
                <td><strong>${scoreVal}</strong></td>
                <td><strong>${recText}</strong></td>
                <td><span class="badge ${statusBadge}">${statusText}</span></td>
                <td>${opt}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showToast('获取评审任务失败: ' + e.message, 'error');
    }
}

async function respond(taskId, accept) {
    try {
        const msg = await api.expert.respond({ taskId, accept });
        showToast(msg, 'success');
        renderTasks();
    } catch (e) {
        showToast('回应评审邀请失败: ' + e.message, 'error');
    }
}

function openReviewModal(taskId) {
    document.getElementById('review-task-id').value = taskId;
    document.getElementById('review-score').value = '';
    document.getElementById('review-comments').value = '';
    openModal('review-opinion-modal');
}

async function saveOpinionDraft() { await submitOpinion(0); }
async function submitOpinionFinal() { await submitOpinion(1); }

async function submitOpinion(isSubmit) {
    const taskId = document.getElementById('review-task-id').value;
    const scoreVal = document.getElementById('review-score').value;
    const comments = document.getElementById('review-comments').value.trim();

    const recRadios = document.getElementsByName('recommend-radio');
    let recommendResult = 1;
    recRadios.forEach(r => { if (r.checked) recommendResult = parseInt(r.value); });

    if (!scoreVal || !comments) {
        showToast('评分和评审意见为必选项！', 'error');
        return;
    }

    const score = parseFloat(scoreVal);
    if (isNaN(score) || score < 0 || score > 100) {
        showToast('有效打分区间为 0.00 ~ 100.00 分！', 'error');
        return;
    }

    if (isSubmit === 1 && comments.length < 30) {
        showToast('正式提交的评审修改意见不得少于 30 个字！', 'error');
        return;
    }

    try {
        const msg = await api.expert.opinion({
            taskId: parseInt(taskId),
            score,
            recommendResult,
            comments,
            isSubmit
        });
        showToast(msg, 'success');
        closeModal('review-opinion-modal');
        renderTasks();
    } catch (e) {
        showToast('提交打分失败: ' + e.message, 'error');
    }
}

function switchExpertPanel(panel) {
    document.querySelectorAll('.viewport .panel').forEach(p => {
        p.style.display = 'none';
        p.classList.remove('active');
    });
    document.querySelectorAll('.sidebar-menu .menu-item').forEach(m => {
        m.classList.remove('active');
    });

    if (panel === 'invites') {
        const p = document.getElementById('expert-panel-invites');
        p.style.display = 'block';
        p.classList.add('active');
        document.getElementById('expert-menu-invites').classList.add('active');
        renderTasks();
    } else if (panel === 'tasks') {
        const p = document.getElementById('expert-panel-tasks');
        p.style.display = 'block';
        p.classList.add('active');
        document.getElementById('expert-menu-tasks').classList.add('active');
        renderTasks();
    }
}
