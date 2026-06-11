document.addEventListener('DOMContentLoaded', () => {
    // 初始化，加载下拉选项
    loadRegistrationAssets();
});

function switchRegisterTab(role, el) {
    const tabs = document.querySelectorAll('.auth-tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    el.classList.add('active');

    const panes = document.querySelectorAll('.tab-pane');
    panes.forEach(pane => pane.classList.remove('active'));
    document.getElementById(`tab-${role}`).classList.add('active');
}

async function loadRegistrationAssets() {
    try {
        // 1. 异步加载已激活机构
        const orgs = await api.auth.getActiveOrgs();
        const orgSelect = document.getElementById('user-org');
        orgSelect.innerHTML = '<option value="">-- 请选择所属单位 --</option>';
        orgs.forEach(org => {
            const opt = document.createElement('option');
            opt.value = org.id;
            opt.textContent = `${org.name} (${org.province})`;
            orgSelect.appendChild(opt);
        });

        // 2. 异步加载方向类别
        const categories = await api.topic.getCategories();
        const majorSelect = document.getElementById('expert-major');
        majorSelect.innerHTML = '<option value="">-- 请选择主要研究领域 --</option>';
        
        const minorBox = document.getElementById('expert-minors');
        minorBox.innerHTML = '';
        
        categories.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.id;
            opt.textContent = cat.name;
            majorSelect.appendChild(opt);

            const label = document.createElement('label');
            label.className = 'checkbox-item';
            label.innerHTML = `<input type="checkbox" name="expert-minor-checkbox" value="${cat.id}"> ${cat.name}`;
            minorBox.appendChild(label);
        });
    } catch (err) {
        showGlobalToast('加载注册选项失败，请刷新页面重试', 'error');
    }
}

function triggerRegisterUpload(prefix) {
    document.getElementById(`${prefix}-input`).click();
}

async function handleRegisterFileChange(inputElement, prefix) {
    const file = inputElement.files[0];
    if (!file) return;

    const progressBox = document.getElementById(`${prefix}-progress-box`);
    const progressBar = document.getElementById(`${prefix}-progress`);
    const nameEl = document.getElementById(`${prefix}-name`);
    const previewImg = document.getElementById(`${prefix}-preview`);
    const urlInput = document.getElementById(`${prefix}-url`);

    progressBox.style.display = 'block';
    progressBar.style.width = '0%';
    nameEl.textContent = `准备上传 ${file.name}...`;

    try {
        const downloadUrl = await api.file.upload(file, (percent) => {
            progressBar.style.width = `${percent}%`;
        });
        
        showGlobalToast('上传成功！', 'success');
        nameEl.textContent = `已成功上传: ${file.name}`;
        progressBox.style.display = 'none';
        
        urlInput.value = downloadUrl;
        if (file.type.startsWith('image/')) {
            previewImg.src = URL.createObjectURL(file);
            previewImg.style.display = 'block';
        } else {
            previewImg.style.display = 'none';
        }
    } catch (err) {
        showGlobalToast(`上传失败: ${err.message}`, 'error');
        nameEl.textContent = '上传失败，请重新点击上传';
        progressBox.style.display = 'none';
    }
}

async function handleUserRegister(event) {
    event.preventDefault();
    const username = document.getElementById('user-username').value.trim();
    const password = document.getElementById('user-password').value;
    const realName = document.getElementById('user-realname').value.trim();
    const mobile = document.getElementById('user-mobile').value.trim();
    const email = document.getElementById('user-email').value.trim();
    const orgId = document.getElementById('user-org').value;

    try {
        await api.auth.registerUser({ username, password, realName, mobile, email, orgId: parseInt(orgId) });
        showGlobalToast('个人申报注册成功！请等待单位管理员审核激活后登录。', 'success');
        setTimeout(() => { window.location.href = '/login.html'; }, 3000);
    } catch (err) {
        showGlobalToast(err.message || '注册失败', 'error');
    }
}

async function handleOrgRegister(event) {
    event.preventDefault();
    const orgName = document.getElementById('org-name').value.trim();
    const province = document.getElementById('org-province').value.trim();
    const address = document.getElementById('org-address').value.trim();
    const username = document.getElementById('org-username').value.trim();
    const password = document.getElementById('org-password').value;
    const realName = document.getElementById('org-realname').value.trim();
    const mobile = document.getElementById('org-mobile').value.trim();
    const email = document.getElementById('org-email').value.trim();
    const licenseUrl = document.getElementById('org-license-url').value;

    if (!licenseUrl) {
        showGlobalToast('请上传资质证明文件！', 'error');
        return;
    }

    try {
        await api.auth.registerOrg({ orgName, province, address, username, password, realName, mobile, email, licenseUrl });
        showGlobalToast('机构注册申请提交成功！请等待超级管理员审批后登录。', 'success');
        setTimeout(() => { window.location.href = '/login.html'; }, 3000);
    } catch (err) {
        showGlobalToast(err.message || '机构注册失败', 'error');
    }
}

async function handleExpertRegister(event) {
    event.preventDefault();
    const realName = document.getElementById('expert-realname').value.trim();
    const password = document.getElementById('expert-password').value;
    const mobile = document.getElementById('expert-mobile').value.trim();
    const majorDirection = document.getElementById('expert-major').value;
    const expertSignature = document.getElementById('expert-signature-url').value;

    if (!expertSignature) {
        showGlobalToast('请上传签字电子版！', 'error');
        return;
    }

    const minorCheckboxes = document.getElementsByName('expert-minor-checkbox');
    const minorList = [];
    minorCheckboxes.forEach(cb => { if (cb.checked) minorList.push(cb.value); });

    try {
        await api.auth.registerExpert({
            realName,
            password,
            mobile,
            majorDirection: parseInt(majorDirection),
            minorDirections: minorList.join(','),
            expertSignature
        });
        showGlobalToast('专家注册成功！请等待超级管理员审核启用后登录。', 'success');
        setTimeout(() => { window.location.href = '/login.html'; }, 3000);
    } catch (err) {
        showGlobalToast(err.message || '专家注册失败', 'error');
    }
}
