let currentUser = null;
let categoriesMap = {};

document.addEventListener('DOMContentLoaded', async () => {
    // 1. 认证与角色校验
    currentUser = tokenStorage.getUserInfo();
    const token = tokenStorage.getToken();
    if (!token || !currentUser || currentUser.role !== 'NORMAL_USER') {
        tokenStorage.clear();
        window.location.href = '/login.html';
        return;
    }

    // 2. 渲染顶部名片
    document.getElementById('profile-avatar').textContent = currentUser.realName.substring(0, 1);
    document.getElementById('profile-name').textContent = `${currentUser.realName} (申报个人)`;

    // 3. 加载主要类别字典
    try {
        const cats = await api.topic.getCategories();
        cats.forEach(c => categoriesMap[c.id] = c.name);
        initDeclarerForm(cats);
    } catch (e) {
        showToast('加载研究方向字典失败', 'error');
    }
});

function showToast(message, type = 'success') {
    if (typeof showGlobalToast === 'function') showGlobalToast(message, type);
}

function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

async function initDeclarerForm(categories) {
    try {
        const period = await api.topic.getActivePeriod();
        const isOpen = await api.topic.isOpen();
        
        const instructionsEl = document.getElementById('period-instructions');
        const authStatusEl = document.getElementById('declarer-auth-status');
        
        if (period) {
            instructionsEl.innerHTML = `<strong>【申报周期：${period.year} 年度】</strong><br>申报时间：${new Date(period.startTime).toLocaleString()} 至 ${new Date(period.endTime).toLocaleString()}<br><br>${period.instructions || '无特别注意事项'}`;
        } else {
            instructionsEl.textContent = '当前未设置申报周期，暂无法填报课题。';
        }

        if (currentUser.hasDeclarationAuth === 1) {
            authStatusEl.textContent = '已获得单位申报授权';
            authStatusEl.className = 'badge badge-success';
        } else {
            authStatusEl.textContent = '无单位申报授权';
            authStatusEl.className = 'badge badge-error';
            lockForm('您的账号尚未获得本单位管理员的申报授权。');
        }

        if (!isOpen && currentUser.hasDeclarationAuth === 1) {
            lockForm('当前申报通道已关闭。');
        }

        // 下拉框与多选渲染
        const mainSelect = document.getElementById('topic-category');
        const minorContainer = document.getElementById('topic-secondary-categories');
        
        categories.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.id;
            opt.textContent = cat.name;
            mainSelect.appendChild(opt);

            const lbl = document.createElement('label');
            lbl.className = 'checkbox-item';
            lbl.innerHTML = `<input type="checkbox" name="declarer-minor-cat" value="${cat.id}"> ${cat.name}`;
            minorContainer.appendChild(lbl);
        });

        renderDeclarerTable();
    } catch (e) {
        showToast('初始化申报环境失败: ' + e.message, 'error');
    }
}

function lockForm(msg) {
    const inputs = document.getElementById('declaration-form').querySelectorAll('input, select, textarea, button');
    inputs.forEach(i => i.disabled = true);
    showToast(msg, 'error');
}

async function renderDeclarerTable() {
    try {
        const list = await api.topic.getMyTopics();
        const tbody = document.getElementById('declarer-records-body');
        tbody.innerHTML = '';
        
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无申报记录</td></tr>';
            return;
        }

        list.forEach(t => {
            const tr = document.createElement('tr');
            const statusBadge = getStatusBadge(t.status);
            const scoreText = t.status === 7 ? '-' : (t.status >= 6 ? (t.averageScore !== null ? t.averageScore.toFixed(2) : '未打分') : (t.status >= 5 ? '评审中' : '-'));
            let finalResult = t.status === 8 && t.finalPass === 1 ? '立项通过' : (t.status === 8 && t.finalPass === 2 ? '立项不通过' : (t.status === 7 ? '格式审核不通过' : (t.status >= 6 ? '待发布' : '未发布')));
            if (t.status === 8 && t.finalPass === 1 && t.announcementContent) {
                finalResult += `<br><span class="badge badge-info" style="cursor:pointer;margin-top:4px;" onclick="alert('${t.announcementContent.replace(/'/g, "\\'")}')">查看公告</span>`;
            }

            let opts = t.status === 0 ? `<button class="btn btn-outline btn-sm" onclick="editTopic(${t.id})">编辑</button>` 
                                       : `<button class="btn btn-primary btn-sm" onclick="viewDetails(${t.id})">详情</button>`;

            tr.innerHTML = `
                <td><strong>${t.title}</strong></td>
                <td>${categoriesMap[t.categoryId] || t.categoryId}</td>
                <td>${new Date(t.createTime).toLocaleDateString()}</td>
                <td>${statusBadge}</td>
                <td>${scoreText}</td>
                <td>${finalResult}</td>
                <td>${opts}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showToast('加载申报记录失败', 'error');
    }
}

function getStatusBadge(status) {
    switch (status) {
        case 0: return '<span class="badge badge-draft">草稿暂存</span>';
        case 1: return '<span class="badge badge-pending">待单位审核</span>';
        case 2: return '<span class="badge badge-pending">待超管审核</span>';
        case 3: return '<span class="badge badge-error">退回修改</span>';
        case 4: return '<span class="badge badge-info">待分配专家</span>';
        case 5: return '<span class="badge badge-pending">专家评审中</span>';
        case 6: return '<span class="badge badge-draft">待发布结果</span>';
        case 7: return '<span class="badge badge-error">格式审核不通过</span>';
        case 8: return '<span class="badge badge-success">结果已发布</span>';
        default: return '<span class="badge badge-draft">未知</span>';
    }
}

function triggerFileUpload(prefix) {
    document.getElementById(`${prefix}-input`).click();
}

async function handleFileChange(input, prefix) {
    const file = input.files[0];
    if (!file) return;

    const progressBox = document.getElementById(`${prefix}-progress-box`);
    const progressBar = document.getElementById(`${prefix}-progress`);
    const nameEl = document.getElementById(`${prefix}-name`);
    const hiddenUrl = document.getElementById(prefix === 'task-book' ? 'task-book-url' : 'anonymous-page-url');

    progressBox.style.display = 'block';
    progressBar.style.width = '0%';
    nameEl.textContent = `上传中... ${file.name}`;

    try {
        const downloadUrl = await api.file.upload(file, (percent) => {
            progressBar.style.width = `${percent}%`;
        });
        showToast('上传成功！', 'success');
        nameEl.innerHTML = `已成功上传: <strong style="color:var(--primary);">${file.name}</strong>`;
        hiddenUrl.value = downloadUrl;
        progressBox.style.display = 'none';
    } catch (err) {
        showToast('上传失败: ' + err.message, 'error');
        nameEl.textContent = '上传失败，请重新上传';
        progressBox.style.display = 'none';
    }
}

async function editTopic(id) {
    try {
        const t = await api.topic.getDetail(id);
        document.getElementById('topic-id').value = t.id;
        document.getElementById('topic-title').value = t.title;
        document.getElementById('topic-category').value = t.categoryId;
        document.getElementById('topic-mobile').value = t.contactMobile;
        document.getElementById('task-book-url').value = t.taskBookUrl;
        document.getElementById('task-book-name').textContent = '已上传任务书 (点击重新上传)';
        document.getElementById('anonymous-page-url').value = t.anonymousPageUrl;
        document.getElementById('anonymous-name').textContent = '已上传活页 (点击重新上传)';

        const cbList = document.getElementsByName('declarer-minor-cat');
        const minors = t.secondaryCategories ? t.secondaryCategories.split(',').map(Number) : [];
        cbList.forEach(cb => cb.checked = minors.includes(parseInt(cb.value)));
        switchUserPanel('declare');
    } catch (e) {
        showToast('加载课题失败: ' + e.message, 'error');
    }
}

async function saveDeclarationDraft() { await submitForm(0); }
async function handleDeclarationSubmit(e) { e.preventDefault(); await submitForm(1); }

async function submitForm(isSubmit) {
    const id = document.getElementById('topic-id').value;
    const title = document.getElementById('topic-title').value.trim();
    const category = document.getElementById('topic-category').value;
    const mobile = document.getElementById('topic-mobile').value.trim();
    const taskBookUrl = document.getElementById('task-book-url').value;
    const anonymousPageUrl = document.getElementById('anonymous-page-url').value;

    if (isSubmit === 1 && (!taskBookUrl || !anonymousPageUrl)) {
        showToast('正式提交时必须上传任务书及盲评活页！', 'error');
        return;
    }

    const cbList = document.getElementsByName('declarer-minor-cat');
    const minors = [];
    cbList.forEach(cb => { if (cb.checked) minors.push(cb.value); });

    try {
        const msg = await api.topic.saveOrSubmit({
            id: id ? parseInt(id) : null,
            title,
            categoryId: parseInt(category),
            secondaryCategories: minors.join(','),
            contactMobile: mobile,
            taskBookUrl,
            anonymousPageUrl,
            isSubmit
        });
        showToast(msg, 'success');
        document.getElementById('declaration-form').reset();
        document.getElementById('topic-id').value = '';
        document.getElementById('task-book-url').value = '';
        document.getElementById('anonymous-page-url').value = '';
        document.getElementById('task-book-name').textContent = '点击上传《任务书》 (doc/docx/pdf)';
        document.getElementById('anonymous-name').textContent = '点击上传《活页》 (doc/docx/pdf)';
        renderDeclarerTable();
    } catch (e) {
        showToast('提交课题失败: ' + e.message, 'error');
    }
}

async function viewDetails(id) {
    try {
        const t = await api.topic.getDetail(id);
        document.getElementById('detail-title').textContent = t.title;
        document.getElementById('detail-category').textContent = categoriesMap[t.categoryId] || t.categoryId;
        
        let orgName = '单位信息正在获取...';
        try {
            const orgs = await api.auth.getActiveOrgs();
            const org = orgs.find(o => o.id === t.orgId);
            if (org) orgName = `${org.name} (${org.province})`;
        } catch(e){}
        document.getElementById('detail-org').textContent = orgName;
        
        document.getElementById('detail-mobile').textContent = t.contactMobile;
        document.getElementById('detail-audit-opinion').value = t.auditOpinion || '无审核反馈意见';

        const dl1 = t.taskBookUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.taskBookUrl}', '${t.title}_任务书')">📥 任务书</button>` : '';
        const dl2 = t.anonymousPageUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.anonymousPageUrl}', '${t.title}_活页')">📥 活页</button>` : '';
        document.getElementById('detail-attachments').innerHTML = `<div style="display:flex; gap:0.5rem;">${dl1} ${dl2}</div>`;

        openModal('topic-detail-modal');
    } catch (e) {
        showToast('获取课题详情失败', 'error');
    }
}

function switchUserPanel(panel) {
    document.querySelectorAll('.viewport .panel').forEach(p => {
        p.style.display = 'none';
        p.classList.remove('active');
    });
    document.querySelectorAll('.sidebar-menu .menu-item').forEach(m => {
        m.classList.remove('active');
    });

    if (panel === 'declare') {
        const p = document.getElementById('user-panel-declare');
        p.style.display = 'block';
        p.classList.add('active');
        document.getElementById('user-menu-declare').classList.add('active');
    } else if (panel === 'records') {
        const p = document.getElementById('user-panel-records');
        p.style.display = 'block';
        p.classList.add('active');
        document.getElementById('user-menu-records').classList.add('active');
        renderDeclarerTable();
    }
}
