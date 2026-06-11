document.addEventListener('DOMContentLoaded', () => {
    // 如果已经登录，直接根据角色重定向到工作台
    const token = tokenStorage.getToken();
    const userInfo = tokenStorage.getUserInfo();
    if (token && userInfo) {
        redirectToDashboard(userInfo.role);
    }
});

function showGlobalToast(message, type = 'success') {
    const toastBox = document.getElementById('toast-box');
    if (toastBox) {
        toastBox.textContent = message;
        toastBox.className = `toast active ${type}`;
        setTimeout(() => toastBox.className = 'toast', 3000);
    }
}

async function handleLogin(event) {
    event.preventDefault();
    const usernameInput = document.getElementById('login-username');
    const passwordInput = document.getElementById('login-password');
    const submitBtn = event.target.querySelector('button[type="submit"]');

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    if (!username || !password) {
        showGlobalToast('用户名和密码不能为空', 'error');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitBtn.textContent = '正在登录，请稍候...';
        
        const response = await api.auth.login(username, password);
        
        showGlobalToast('登录成功！正在进入工作台...', 'success');
        setTimeout(() => {
            redirectToDashboard(response.user.role);
        }, 1000);
    } catch (err) {
        console.error(err);
        showGlobalToast(err.message || '用户名或密码不正确！', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = '立即登录';
    }
}

// 根据角色分发到不同的专属工作台页面
function redirectToDashboard(role) {
    switch (role) {
        case 'NORMAL_USER':
            window.location.href = '/user_dashboard.html';
            break;
        case 'ORG_ADMIN':
            window.location.href = '/org_dashboard.html';
            break;
        case 'EXPERT':
            window.location.href = '/expert_dashboard.html';
            break;
        case 'SUPER_ADMIN':
            window.location.href = '/admin_dashboard.html';
            break;
        default:
            showGlobalToast('未识别的系统角色: ' + role, 'error');
    }
}
