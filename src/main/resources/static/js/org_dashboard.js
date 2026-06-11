let currentUser = null;
let categoriesMap = {};
let usersMap = {};

document.addEventListener('DOMContentLoaded', async () => {
    // 1. 认证安全校验
    currentUser = tokenStorage.getUserInfo();
    const token = tokenStorage.getToken();
    if (!token || !currentUser || currentUser.role !== 'ORG_ADMIN') {
        tokenStorage.clear();
        window.location.href = '/login.html';
        return;
    }

    // 2. 顶部名片
    document.getElementById('profile-avatar').textContent = currentUser.realName.substring(0, 1);
    document.getElementById('profile-name').textContent = `${currentUser.realName} (机构管理员)`;

    // 3. 全局缓存分类
    try {
        const cats = await api.topic.getCategories();
        cats.forEach(c => categoriesMap[c.id] = c.name);
    } catch(e){}

    // 4. 初始化数据
    initOrgDashboard();
});

function showToast(message, type = 'success') {
    if (typeof showGlobalToast === 'function') showGlobalToast(message, type);
}

function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

async function initOrgDashboard() {
    try {
        const instTopics = await api.institution.getTopics();
        const instUsers = await api.institution.getUsers();

        // 缓存员工映射
        instUsers.forEach(u => usersMap[u.id] = u.realName);

        document.getElementById('org-quota-assigned').textContent = instUsers.filter(u => u.hasDeclarationAuth === 1).length;
        document.getElementById('org-topics-submitted').textContent = instTopics.filter(t => [1, 2, 4, 5, 6, 8].includes(t.status)).length;

        // 获取机构限额 (通过已注册的机构列表匹配)
        try {
            const orgs = await api.auth.getActiveOrgs();
            const myOrg = orgs.find(o => o.id === currentUser.orgId);
            if (myOrg) {
                document.getElementById('org-name-title').textContent = `申报机构：${myOrg.name} (${myOrg.province})`;
                document.getElementById('org-quota-total').textContent = myOrg.quota;
            }
        } catch(err) {
            document.getElementById('org-name-title').textContent = '申报机构管理控制台';
        }

        switchTab('users');
    } catch (e) {
        showToast('获取机构统计失败: ' + e.message, 'error');
    }
}

function switchOrgPanel(tab) {
    document.querySelectorAll('.org-tab-content').forEach(c => c.style.display = 'none');
    document.getElementById(`org-tab-${tab}`).style.display = 'block';

    document.querySelectorAll('.sidebar-menu .menu-item').forEach(m => m.classList.remove('active'));
    const menuItem = document.getElementById(`org-menu-${tab}`);
    if (menuItem) menuItem.classList.add('active');

    if (tab === 'users') {
        renderUsersTable();
    } else {
        renderTopicsTable();
    }
}
function switchTab(tab) { switchOrgPanel(tab); }

async function renderUsersTable() {
    try {
        const list = await api.institution.getUsers();
        const tbody = document.getElementById('org-users-body');
        tbody.innerHTML = '';

        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">暂无员工注册数据</td></tr>';
            return;
        }

        list.forEach(u => {
            const tr = document.createElement('tr');
            const statusText = u.status === 1 ? '已激活' : '待批准';
            const statusBadge = u.status === 1 ? 'badge-success' : 'badge-pending';
            const authChecked = u.hasDeclarationAuth === 1 ? 'checked' : '';

            let opt = u.status === 0 ? `<button class="btn btn-primary btn-sm" onclick="auditUser(${u.id}, 1)">批准</button>` 
                                     : `<button class="btn btn-outline btn-sm" onclick="auditUser(${u.id}, 2)">禁用</button>`;

            tr.innerHTML = `
                <td><strong>${u.realName}</strong></td>
                <td>${u.mobile}</td>
                <td><span class="badge ${statusBadge}">${statusText}</span></td>
                <td>
                    <input type="checkbox" ${authChecked} onchange="toggleDeclarationAuth(${u.id}, this)"> 勾选指派申报权限
                </td>
                <td>${opt}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showToast('加载员工列表失败', 'error');
    }
}

async function auditUser(userId, status) {
    try {
        await api.institution.auditUser({ userId, status });
        showToast(status === 1 ? '已批准该员工加入本单位！' : '该员工账户已禁用。', 'success');
        initOrgDashboard();
    } catch (e) {
        showToast('操作账号失败: ' + e.message, 'error');
    }
}

async function toggleDeclarationAuth(userId, checkbox) {
    const hasAuth = checkbox.checked ? 1 : 0;
    try {
        await api.institution.assignAuth({ userId, hasAuth });
        showToast(hasAuth === 1 ? '授权成功！' : '已取消授权！', 'success');
        initOrgDashboard();
    } catch (e) {
        showToast('授权修改失败: ' + e.message, 'error');
        checkbox.checked = !checkbox.checked;
    }
}

async function renderTopicsTable() {
    try {
        const list = await api.institution.getTopics();
        const tbody = document.getElementById('org-topics-body');
        tbody.innerHTML = '';

        // 只保留员工正式提交的课题
        const subList = list.filter(t => t.status > 0);

        if (subList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无提交的申报课题</td></tr>';
            return;
        }

        subList.forEach(t => {
            const tr = document.createElement('tr');
            
            const dl1 = t.taskBookUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.taskBookUrl}', '${t.title}_任务书')">任务书</button>` : '';
            const dl2 = t.anonymousPageUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.anonymousPageUrl}', '${t.title}_活页')">活页</button>` : '';
            
            const statusBadge = getStatusBadge(t.status);
            const author = usersMap[t.declarerId] || `账户ID:${t.declarerId}`;

            let resultHtml = '<span class="badge badge-draft">未公布</span>';
            let announcementHtml = '-';
            if (t.status === 7) {
                resultHtml = '<span class="badge badge-error">格式审核不通过</span>';
            } else if (t.status === 8 && t.finalPass === 1) {
                resultHtml = '<span class="badge badge-success">立项通过</span>';
                if (t.announcementContent) {
                    announcementHtml = `<span class="badge badge-info" style="cursor:pointer;" onclick="alert('${t.announcementContent.replace(/'/g, "\\'")}')">查看公告</span>`;
                }
            } else if (t.status === 8 && t.finalPass === 2) {
                resultHtml = '<span class="badge badge-error">立项不通过</span>';
            } else if (t.status === 6) {
                resultHtml = '<span class="badge badge-pending">待发布</span>';
            }

            let opt = t.status === 1 ? `<button class="btn btn-primary btn-sm" onclick="openAuditModal(${t.id}, '${author}')">初审</button>` 
                                     : `<button class="btn btn-outline btn-sm" onclick="viewDetails(${t.id}, '${author}')">查阅</button>`;

            tr.innerHTML = `
                <td><strong>${t.title}</strong></td>
                <td>${author}</td>
                <td>${categoriesMap[t.category] || t.category}</td>
                <td>${new Date(t.createTime).toLocaleDateString()}</td>
                <td><div style="display:flex; gap:0.25rem;">${dl1} ${dl2}</div></td>
                <td>${statusBadge}</td>
                <td>${resultHtml}</td>
                <td style="font-size:0.85rem;">${announcementHtml}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showToast('加载本单位课题失败', 'error');
    }
}

function getStatusBadge(status) {
    switch (status) {
        case 1: return '<span class="badge badge-pending">待单位审核</span>';
        case 2: return '<span class="badge badge-pending">待超管格式审核</span>';
        case 3: return '<span class="badge badge-error">退回修改</span>';
        case 4: return '<span class="badge badge-info">待分配专家</span>';
        case 5: return '<span class="badge badge-pending">专家评审中</span>';
        case 6: return '<span class="badge badge-draft">待发布结果</span>';
        case 7: return '<span class="badge badge-error">格式审核不通过</span>';
        case 8: return '<span class="badge badge-success">结果已发布</span>';
        default: return '<span class="badge badge-draft">未知</span>';
    }
}

async function openAuditModal(id, author) {
    try {
        const t = await api.topic.getDetail(id);
        document.getElementById('detail-topic-id').value = t.id;
        document.getElementById('detail-title').textContent = t.title;
        document.getElementById('detail-category').textContent = categoriesMap[t.category] || t.category;
        document.getElementById('detail-declarer').textContent = author;
        document.getElementById('detail-audit-opinion').value = '';

        const dl1 = t.taskBookUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.taskBookUrl}', '${t.title}_任务书')">📥 任务书</button>` : '';
        const dl2 = t.anonymousPageUrl ? `<button class="btn btn-outline btn-sm" onclick="api.file.download('${t.anonymousPageUrl}', '${t.title}_活页')">📥 活页</button>` : '';
        document.getElementById('detail-attachments').innerHTML = `<div style="display:flex; gap:0.5rem;">${dl1} ${dl2}</div>`;

        openModal('topic-detail-modal');
    } catch (e) {
        showToast('载入课题初审信息失败', 'error');
    }
}

async function submitAudit(status) {
    const topicId = document.getElementById('detail-topic-id').value;
    const opinion = document.getElementById('detail-audit-opinion').value.trim();

    if (!opinion) {
        showToast('审核意见不能为空！', 'error');
        return;
    }

    try {
        await api.institution.auditTopic({
            topicId: parseInt(topicId),
            approve: status,
            reason: opinion
        });
        showToast(status === 1 ? '初审通过，已成功推荐上报省局！' : '初审已退回给申报人修改。', 'success');
        closeModal('topic-detail-modal');
        initOrgDashboard();
    } catch (e) {
        showToast('初审操作失败: ' + e.message, 'error');
    }
}

async function viewDetails(id, author) {
    await openAuditModal(id, author);
    document.getElementById('detail-audit-opinion').value = '此课题已初审完成。历史初审意见可在历史记录中查阅。';
}
