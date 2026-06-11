let orgsMap = {};

// ==================== 1. 格式初审逻辑 ====================
async function renderFormatTable() {
    try {
        const list = await api.admin.getAllTopics();
        const tbody = document.getElementById('super-format-body');
        tbody.innerHTML = '';

        const formatList = list.filter(t => t.status === 1 || t.status === 2 || t.status === 3);
        if (formatList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">暂无需要格式初审的课题</td></tr>';
            return;
        }

        const orgs = await api.admin.getAllOrgs();
        orgs.forEach(o => orgsMap[o.id] = o.name);

        formatList.forEach(t => {
            const tr = document.createElement('tr');
            const orgName = orgsMap[t.orgId] || `单位ID:${t.orgId}`;
            const statusBadge = getStatusBadgeText(t.status);

            const dl1 = t.taskBookUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.taskBookUrl}', '${t.title}_任务书')">任务书</button>` : '';
            const dl2 = t.anonymousPageUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.anonymousPageUrl}', '${t.title}_活页')">活页</button>` : '';

            let opt = t.status === 2 ? `<button class="btn btn-primary btn-sm" onclick="openAuditModal(${t.id})">格式初审</button>` 
                                     : `<button class="btn btn-outline btn-sm" onclick="viewTopicDetailOnly(${t.id})">详情</button>`;

            tr.innerHTML = `
                <td><strong>${t.title}</strong></td>
                <td>${orgName}</td>
                <td>${categoriesMap[t.categoryId] || t.categoryId}</td>
                <td><div style="display:flex; gap:0.25rem;">${dl1} ${dl2}</div></td>
                <td>${statusBadge}</td>
                <td>${opt}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch(e) {
        showToast('加载初审课题失败', 'error');
    }
}

function getStatusBadgeText(status) {
    switch (status) {
        case 1: return '<span class="badge badge-pending">待单位审核</span>';
        case 2: return '<span class="badge badge-pending">待超管格式审核</span>';
        case 3: return '<span class="badge badge-error">退回修改</span>';
        case 4: return '<span class="badge badge-info">待分配专家</span>';
        case 5: return '<span class="badge badge-pending">专家评审中</span>';
        case 6: return '<span class="badge badge-draft">待发布结果</span>';
        case 7: return '<span class="badge badge-error">格式审核不通过</span>';
        case 8: return '<span class="badge badge-success">结果已发布</span>';
        default: return `<span class="badge badge-draft">未知(${status})</span>`;
    }
}

async function openAuditModal(id) {
    try {
        const t = await api.topic.getDetail(id);
        document.getElementById('detail-topic-id').value = t.id;
        document.getElementById('detail-title').textContent = t.title;
        document.getElementById('detail-category').textContent = categoriesMap[t.categoryId] || t.categoryId;
        document.getElementById('detail-org').textContent = orgsMap[t.orgId] || `单位ID:${t.orgId}`;
        document.getElementById('detail-audit-opinion').value = '';

        const dl1 = t.taskBookUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.taskBookUrl}', '${t.title}_任务书')">📥 任务书</button>` : '';
        const dl2 = t.anonymousPageUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.anonymousPageUrl}', '${t.title}_活页')">📥 活页</button>` : '';
        document.getElementById('detail-attachments').innerHTML = `<div style="display:flex; gap:0.5rem;">${dl1} ${dl2}</div>`;

        openModal('topic-detail-modal');
    } catch(e) {
        showToast('获取课题详情失败', 'error');
    }
}

async function submitTopicAudit(status) {
    const topicId = document.getElementById('detail-topic-id').value;
    const opinion = document.getElementById('detail-audit-opinion').value.trim();

    if (!opinion) {
        showToast('审核意见不能为空！', 'error');
        return;
    }

    try {
        await api.admin.formatAudit({ topicId: parseInt(topicId), approve: status, reason: opinion });
        showToast(status === 1 ? '格式审核通过，已转入指派评审专家环节！' : '该课题已被格式驳回修改。', 'success');
        closeModal('topic-detail-modal');
        switchSuperTab('format');
    } catch(e) {
        showToast('格式审核失败: ' + e.message, 'error');
    }
}

async function viewTopicDetailOnly(id) {
    await openAuditModal(id);
    document.getElementById('detail-audit-opinion').disabled = true;
    document.querySelector('#topic-detail-modal .modal-footer').style.display = 'none';
}

// 弹窗关闭恢复事件绑定
document.getElementById('topic-detail-modal').addEventListener('click', (e) => {
    if (e.target.classList.contains('close-btn') || e.target.id === 'topic-detail-modal') {
        document.getElementById('detail-audit-opinion').disabled = false;
        document.querySelector('#topic-detail-modal .modal-footer').style.display = 'flex';
    }
});


// ==================== 2. 专家指派与打分明细逻辑 ====================
async function renderExpertsTable() {
    try {
        const list = await api.admin.getAllTopics();
        const tbody = document.getElementById('super-experts-body');
        tbody.innerHTML = '';

        const expertList = list.filter(t => t.status === 4 || t.status === 5);
        if (expertList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">暂无初审通过可指派专家的课题</td></tr>';
            return;
        }

        const orgs = await api.admin.getAllOrgs();
        orgs.forEach(o => orgsMap[o.id] = o.name);

        for (const t of expertList) {
            const tr = document.createElement('tr');
            const orgName = orgsMap[t.orgId] || `单位ID:${t.orgId}`;
            
            const reviews = await api.admin.getReviews(t.id);
            const acceptedCount = reviews.filter(r => r.invitationStatus === 1).length;
            const totalCount = reviews.length;

            let opt = totalCount === 0 ? 
                `<button class="btn btn-primary btn-sm" onclick="openAssignModal(${t.id}, '${orgName}', ${t.categoryId})">指派评审专家(3人)</button>` 
                : `<button class="btn btn-outline btn-sm" onclick="viewReviewOpinions(${t.id})">打分明细 (${acceptedCount}/${totalCount})</button>`;

            tr.innerHTML = `
                <td><strong>${t.title}</strong></td>
                <td>${categoriesMap[t.categoryId] || t.categoryId}</td>
                <td>${orgName}</td>
                <td><span class="badge ${totalCount >= 3 ? 'badge-success' : 'badge-draft'}">${acceptedCount} 接受 / 共指派 ${totalCount} 人</span></td>
                <td>${opt}</td>
            `;
            tbody.appendChild(tr);
        }
    } catch(e) {
        showToast('加载专家指派进度列表失败', 'error');
    }
}

async function openAssignModal(topicId, orgName, categoryId) {
    document.getElementById('assign-topic-id').value = topicId;
    document.getElementById('assign-topic-cat').textContent = categoriesMap[categoryId] || categoryId;
    document.getElementById('assign-topic-org').textContent = orgName;

    try {
        const experts = await api.admin.recommendExperts(topicId);
        const listContainer = document.getElementById('recommended-experts-list');
        listContainer.innerHTML = '';

        if (experts.length === 0) {
            listContainer.innerHTML = '<p style="color:red; font-size:0.85rem; text-align:center;">暂无可指派的领域专家（已被同单位回避过滤）</p>';
            return;
        }

        experts.forEach(exp => {
            const item = document.createElement('div');
            item.style.display = 'flex'; item.style.justifyContent = 'space-between';
            item.style.padding = '0.5rem'; item.style.borderBottom = '1px solid #f1f5f9';
            item.innerHTML = `
                <label style="display:flex; align-items:center; gap:0.5rem; font-size:0.9rem; cursor:pointer;">
                    <input type="checkbox" name="assign-expert-checkbox" value="${exp.id}">
                    <strong>${exp.realName}</strong> (方向: ${categoriesMap[exp.majorDirection]})
                </label>
                <span style="font-size:0.75rem; color:var(--gray);">专家ID: ${exp.id}</span>
            `;
            listContainer.appendChild(item);
        });
        openModal('assign-expert-modal');
    } catch(e) {
        showToast('拉取推荐专家失败', 'error');
    }
}

function autoPickThreeExperts() {
    const list = document.getElementsByName('assign-expert-checkbox');
    if (list.length < 3) {
        showToast('可用匹配专家不足3位，请手动勾选指派！', 'error');
        list.forEach(cb => cb.checked = true);
        return;
    }
    list.forEach(cb => cb.checked = false);
    const set = new Set();
    while (set.size < 3) {
        set.add(Math.floor(Math.random() * list.length));
    }
    set.forEach(idx => list[idx].checked = true);
    showToast('已自动为您随机挑选3位符合方向且自动避让同单位的专家！', 'success');
}

async function confirmAssignExperts() {
    const topicId = document.getElementById('assign-topic-id').value;
    const list = document.getElementsByName('assign-expert-checkbox');
    const expertIds = [];
    list.forEach(cb => { if (cb.checked) expertIds.push(parseInt(cb.value)); });

    if (expertIds.length !== 3) {
        showToast('指派专家的数量必须正好为 3 名！', 'error');
        return;
    }

    try {
        const msg = await api.admin.assignExperts({ topicId: parseInt(topicId), expertIds });
        showToast(msg, 'success');
        closeModal('assign-expert-modal');
        switchSuperTab('experts');
    } catch(e) {
        showToast('指派专家任务失败: ' + e.message, 'error');
    }
}

async function viewReviewOpinions(topicId) {
    try {
        const reviews = await api.admin.getReviews(topicId);
        const tbody = document.getElementById('review-list-body');
        tbody.innerHTML = '';

        reviews.forEach((r, idx) => {
            const tr = document.createElement('tr');
            const statusBadge = r.status === 1 ? 'badge-success' : 'badge-draft';
            
            let invStr = r.invitationStatus === 0 ? '待邀请回应' : (r.invitationStatus === 1 ? '已接受邀请' : '已拒绝');

            tr.innerHTML = `
                <td><strong>评审专家 #${idx+1}</strong></td>
                <td><strong>${r.score !== null ? r.score.toFixed(2) : '-'} 分</strong></td>
                <td>${r.recommendResult === 1 ? '推荐立项' : (r.recommendResult === 2 ? '不推荐' : '-')}</td>
                <td style="font-size:0.8rem; max-width:250px; word-break:break-all;">${r.opinion || '未填写意见'}</td>
                <td><span class="badge ${statusBadge}">${r.status === 1 ? '打分提交锁定' : '草稿填报中'}</span><br><span style="font-size:0.75rem; color:var(--gray);">${invStr}</span></td>
            `;
            tbody.appendChild(tr);
        });
        openModal('review-list-modal');
    } catch(e) {
        showToast('获取评审详情失败', 'error');
    }
}
