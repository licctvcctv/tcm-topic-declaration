let currentUser = null;
let categoriesMap = {};

document.addEventListener('DOMContentLoaded', async () => {
    // 1. 安全检查
    currentUser = tokenStorage.getUserInfo();
    const token = tokenStorage.getToken();
    if (!token || !currentUser || currentUser.role !== 'SUPER_ADMIN') {
        tokenStorage.clear();
        window.location.href = '/login.html';
        return;
    }

    // 2. 顶部名片
    document.getElementById('profile-avatar').textContent = currentUser.realName.substring(0, 1);
    document.getElementById('profile-name').textContent = `${currentUser.realName} (系统管理员)`;

    // 3. 异步获取方向大类
    try {
        const cats = await api.topic.getCategories();
        cats.forEach(c => categoriesMap[c.id] = c.name);
    } catch(e){}

    // 4. 默认加载周期
    switchSuperTab('period');
});

function showToast(message, type = 'success') {
    showGlobalToast(message, type);
}

function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

function switchSuperTab(tab) {
    document.querySelectorAll('.super-tab-content').forEach(c => c.style.display = 'none');
    document.getElementById(`super-tab-${tab}`).style.display = 'block';

    const buttons = ['period', 'orgs', 'format', 'experts', 'results'];
    buttons.forEach(b => {
        const item = document.getElementById(`super-btn-${b}`);
        if (item) item.className = (b === tab) ? 'menu-item active' : 'menu-item';
    });

    // 触发子模块对应加载
    if (tab === 'period') { loadPeriodConfig(); }
    else if (tab === 'orgs') { renderOrgsTable(); }
    else if (tab === 'format') { renderFormatTable(); }
    else if (tab === 'experts') { renderExpertsTable(); }
    else if (tab === 'results') { renderResultsTable(); }
}

async function loadPeriodConfig() {
    try {
        const p = await api.admin.getPeriod();
        if (p) {
            document.getElementById('period-year').value = p.year;
            document.getElementById('period-start').value = p.startTime ? p.startTime.substring(0, 16) : '';
            document.getElementById('period-end').value = p.endTime ? p.endTime.substring(0, 16) : '';
            document.getElementById('period-instructions-input').value = p.instructions || '';
            document.getElementById('period-status').value = p.status;
        }
    } catch(e) {
        showToast('加载申报周期配置失败', 'error');
    }
}

async function savePeriodConfig(e) {
    e.preventDefault();
    const year = document.getElementById('period-year').value;
    const startTime = document.getElementById('period-start').value;
    const endTime = document.getElementById('period-end').value;
    const instructions = document.getElementById('period-instructions-input').value.trim();
    const status = document.getElementById('period-status').value;

    try {
        const msg = await api.admin.configPeriod({
            year: parseInt(year),
            startTime: startTime + ":00",
            endTime: endTime + ":00",
            instructions,
            status: parseInt(status)
        });
        showToast(msg, 'success');
        loadPeriodConfig();
    } catch(err) {
        showToast('保存周期失败: ' + err.message, 'error');
    }
}
