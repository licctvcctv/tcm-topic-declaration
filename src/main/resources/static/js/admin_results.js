// ==================== 3. 评审汇总与发布结果逻辑 ====================
async function renderResultsTable() {
    try {
        const list = await api.admin.getAllTopics();
        const tbody = document.getElementById('super-results-body');
        tbody.innerHTML = '';

        const finishedList = list.filter(t => t.status === 5 || t.status === 6);
        if (finishedList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无专家评审完结的课题成果</td></tr>';
            return;
        }

        for (const t of finishedList) {
            const tr = document.createElement('tr');
            const scoreText = t.averageScore !== null ? t.averageScore.toFixed(2) : '0.00';

            const reviews = await api.admin.getReviews(t.id);
            const recCount = reviews.filter(r => r.recommendResult === 1 && r.invitationStatus === 1).length;
            const validCount = reviews.filter(r => r.invitationStatus === 1).length;
            const passRate = validCount > 0 ? Math.round((recCount / validCount) * 100) : 0;
            
            const suggest = passRate >= 50 ? '建议立项' : '建议不予立项';

            let pubText = ''; let pubBadge = '';
            if (t.status === 5) { pubText = '评审中'; pubBadge = 'badge-pending'; }
            else if (t.status === 6 && (!t.finalPass || t.finalPass === 0)) { pubText = '未发布结果'; pubBadge = 'badge-draft'; }
            else if (t.status === 6 && t.finalPass === 1) { pubText = '已立项公布'; pubBadge = 'badge-success'; }
            else if (t.status === 6 && t.finalPass === 2) { pubText = '已公布不予立项'; pubBadge = 'badge-error'; }
            else if (t.status === 7) { pubText = '格式审核不通过'; pubBadge = 'badge-error'; }

            let opt = t.status === 6 && (!t.finalPass || t.finalPass === 0) ? `
                <button class="btn btn-primary btn-sm" onclick="openPublishModal(${t.id}, '${t.title}', ${scoreText}, '${suggest}')">发布立项</button>
                <button class="btn btn-outline btn-sm" onclick="openAddSecondModal(${t.id}, '${t.title}', ${t.categoryId})">追加二轮</button>
            `             : `<span style="font-size:0.85rem; color:var(--gray);">${t.finalPass === 1 ? '已立项' : (t.finalPass === 2 ? '已拒绝' : (t.status === 5 ? '评审中' : (t.status === 7 ? '已驳回' : '流程已结案')))}</span>`;

            tr.innerHTML = `
                <td><strong>${t.title}</strong></td>
                <td>${categoriesMap[t.categoryId] || t.categoryId}</td>
                <td><strong>${scoreText} 分</strong></td>
                <td><strong>${passRate}%</strong> (${recCount}/${validCount}人推荐)</td>
                <td><strong>${suggest}</strong></td>
                <td><span class="badge ${pubBadge}">${pubText}</span></td>
                <td><div style="display:flex; gap:0.25rem;">${opt}</div></td>
            `;
            tbody.appendChild(tr);
        }
    } catch(e) {
        showToast('拉取立项结果失败', 'error');
    }
}

async function openAddSecondModal(topicId, title, categoryId) {
    document.getElementById('second-topic-id').value = topicId;
    document.getElementById('second-topic-title').textContent = title;

    try {
        const experts = await api.admin.recommendExperts(topicId);
        const reviews = await api.admin.getReviews(topicId);
        const assignedIds = reviews.map(r => r.expertId);
        
        const available = experts.filter(exp => !assignedIds.includes(exp.id));
        const listContainer = document.getElementById('second-experts-list');
        listContainer.innerHTML = '';

        if (available.length === 0) {
            listContainer.innerHTML = '<p style="color:red; text-align:center; font-size:0.85rem;">暂无可用的其他备选专家（匹配同方向且单位回避）</p>';
            return;
        }

        available.forEach(exp => {
            const item = document.createElement('div');
            item.style.padding = '0.5rem'; item.style.borderBottom = '1px solid #f1f5f9';
            item.innerHTML = `
                <label style="display:flex; justify-content:space-between; align-items:center; cursor:pointer;">
                    <span>
                        <input type="radio" name="second-expert-radio" value="${exp.id}">
                        <strong>${exp.realName}</strong> (${categoriesMap[exp.majorDirection]})
                    </span>
                    <span style="font-size:0.75rem; color:var(--gray);">当前评审任务数: ${exp.limitDeclaration || 0}</span>
                </label>
            `;
            listContainer.appendChild(item);
        });
        openModal('add-second-expert-modal');
    } catch(e) {
        showToast('获取备选追加专家失败', 'error');
    }
}

async function confirmAddSecondExpert() {
    const topicId = document.getElementById('second-topic-id').value;
    const radios = document.getElementsByName('second-expert-radio');
    let expertId = null;
    radios.forEach(r => { if (r.checked) expertId = parseInt(r.value); });

    if (!expertId) {
        showToast('请选择一位追加的专家！', 'error');
        return;
    }

    try {
        const msg = await api.admin.addSecondExpert(topicId, expertId);
        showToast(msg, 'success');
        closeModal('add-second-expert-modal');
        switchSuperTab('results');
    } catch(e) {
        showToast('追加专家失败: ' + e.message, 'error');
    }
}

function openPublishModal(topicId, title, avgScore, suggest) {
    document.getElementById('publish-topic-id').value = topicId;
    document.getElementById('publish-topic-title').textContent = title;
    document.getElementById('publish-avg-score').textContent = avgScore;
    document.getElementById('publish-sys-suggest').textContent = suggest;
    document.getElementById('publish-opinion').value = '';
    document.getElementById('publish-announcement').value = '';
    openModal('publish-result-modal');
}

async function confirmPublishResult() {
    const topicId = document.getElementById('publish-topic-id').value;
    const opinion = document.getElementById('publish-opinion').value.trim();
    const announcement = document.getElementById('publish-announcement').value.trim();

    const passRadios = document.getElementsByName('publish-pass');
    let finalPass = 1;
    passRadios.forEach(r => { if (r.checked) finalPass = parseInt(r.value); });

    if (!opinion) {
        showToast('批复评语为必填项！', 'error');
        return;
    }

    try {
        const msg = await api.admin.publishFinalResult({ topicId: parseInt(topicId), finalPass, finalOpinion: opinion, announcement });
        showToast(msg, 'success');
        closeModal('publish-result-modal');
        switchSuperTab('results');
    } catch(e) {
        showToast('正式立项发布操作失败: ' + e.message, 'error');
    }
}

async function exportPassedCSV() {
    showToast('开始生成立项报表并下载...', 'success');
    await api.file.download(API_BASE + '/admin/topics/export-csv', 'TCM_Passed_Topics.csv');
}
