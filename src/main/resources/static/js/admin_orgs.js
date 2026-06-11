async function renderOrgsTable() {
    try {
        const list = await api.admin.getAllOrgs();
        const tbody = document.getElementById('super-orgs-body');
        tbody.innerHTML = '';

        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无注册申报机构数据</td></tr>';
            return;
        }

        list.forEach(org => {
            const tr = document.createElement('tr');
            
            let statusText = '';
            let statusBadge = '';
            if (org.status === 0) { statusText = '待资质审查'; statusBadge = 'badge-pending'; }
            else if (org.status === 1) { statusText = '资质已激活'; statusBadge = 'badge-success'; }
            else if (org.status === 2) { statusText = '资质被驳回'; statusBadge = 'badge-error'; }

            const licenseBtn = org.licenseUrl ? 
                `<button class="btn btn-outline btn-sm" onclick="api.file.download('${org.licenseUrl}', '${org.name}_资质证明')">查阅证书</button>` 
                : '未上传';

            let opt = org.status === 0 ? `
                <button class="btn btn-primary btn-sm" onclick="auditOrg(${org.id}, 1)">激活通过</button>
                <button class="btn btn-outline btn-sm" onclick="auditOrg(${org.id}, 2)" style="border-color:#ef4444; color:#ef4444;">资质驳回</button>
            ` : `<span style="font-size:0.8rem; color:var(--gray);">审查完毕</span>`;

            tr.innerHTML = `
                <td><strong>${org.name}</strong></td>
                <td>${org.province}</td>
                <td>${org.address}</td>
                <td>${licenseBtn}</td>
                <td><span class="badge ${statusBadge}">${statusText}</span></td>
                <td>
                    <input type="number" class="form-control" style="width: 80px; padding:0.25rem 0.5rem;" value="${org.quota}" onchange="adjustQuota(${org.id}, this)">
                </td>
                <td><div style="display:flex; gap:0.25rem;">${opt}</div></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showToast('拉取机构资质列表失败', 'error');
    }
}

async function auditOrg(orgId, status) {
    let rejectReason = '';
    if (status === 2) {
        rejectReason = prompt('请填写驳回该机构资质审批的说明原因：');
        if (rejectReason === null) return;
    }

    try {
        await api.admin.auditOrg({ orgId, status, rejectReason });
        showToast(status === 1 ? '该机构已成功激活通过！' : '已成功驳回该机构资质申请。', 'success');
        renderOrgsTable();
    } catch (e) {
        showToast('资质审核操作失败: ' + e.message, 'error');
    }
}

async function adjustQuota(orgId, input) {
    const quota = parseInt(input.value);
    if (isNaN(quota) || quota < 0) {
        showToast('限额上限名额必须为非负整数！', 'error');
        return;
    }

    try {
        await api.admin.adjustQuota(orgId, quota);
        showToast('已成功调整该机构的申报上限名额！', 'success');
        renderOrgsTable();
    } catch (e) {
        showToast('调整申报名额失败: ' + e.message, 'error');
    }
}
