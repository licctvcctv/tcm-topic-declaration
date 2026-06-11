const API_BASE = '/api';

// 获取和存储Token及用户信息
const tokenStorage = {
    getToken: () => localStorage.getItem('jwt_token'),
    setToken: (token) => localStorage.setItem('jwt_token', token),
    clear: () => {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_info');
    },
    getUserInfo: () => {
        const info = localStorage.getItem('user_info');
        return info ? JSON.parse(info) : null;
    },
    setUserInfo: (info) => localStorage.setItem('user_info', JSON.stringify(info))
};

// 基础网络请求封装
async function request(url, options = {}) {
    const headers = options.headers || {};
    const token = tokenStorage.getToken();
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    if (options.body && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json;charset=UTF-8';
        if (typeof options.body === 'object') {
            options.body = JSON.stringify(options.body);
        }
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(`${API_BASE}${url}`, config);
        
        // 401 token过期或未授权
        if (response.status === 401) {
            tokenStorage.clear();
            showGlobalToast('您的登录会话已过期，请重新登录', 'error');
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 1500);
            throw new Error('未授权，登录过期');
        }

        // 403 权限不足
        if (response.status === 403) {
            showGlobalToast('您没有权限进行此项操作！', 'error');
            throw new Error('无权限访问');
        }

        // 检查是不是下载文件的响应
        const contentType = response.headers.get('Content-Type');
        if (contentType && (contentType.includes('octet-stream') || contentType.includes('pdf') || contentType.includes('msword') || contentType.includes('csv'))) {
            return response;
        }

        const resData = await response.json();
        if (resData.code !== 200) {
            throw new Error(resData.message || '请求失败');
        }
        return resData.data;
    } catch (error) {
        console.error(`Request Error [${url}]:`, error);
        throw error;
    }
}

// 统一展示Toast的方法（支持多实例堆叠）
function showGlobalToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) {
        const div = document.createElement('div');
        div.id = 'toast-container';
        div.style.cssText = 'position:fixed;bottom:2rem;right:2rem;display:flex;flex-direction:column;gap:0.5rem;z-index:2000;';
        document.body.appendChild(div);
    }
    const container2 = document.getElementById('toast-container');
    const el = document.createElement('div');
    el.style.cssText = 'padding:0.75rem 1.5rem;border-radius:8px;color:white;font-size:0.9rem;font-weight:500;box-shadow:0 4px 12px rgba(0,0,0,0.15);transform:translateY(100px);opacity:0;transition:all 0.3s cubic-bezier(0.4,0,0.2,1);';
    el.style.background = type === 'error' ? '#ef4444' : 'var(--primary, #0d5c3a)';
    el.textContent = message;
    container2.appendChild(el);
    requestAnimationFrame(() => {
        el.style.transform = 'translateY(0)';
        el.style.opacity = '1';
    });
    setTimeout(() => {
        el.style.transform = 'translateY(100px)';
        el.style.opacity = '0';
        setTimeout(() => el.remove(), 300);
    }, 3000);
}

const api = {
    auth: {
        // 登录
        login: async (username, password) => {
            const data = await request('/auth/login', {
                method: 'POST',
                body: { username, password }
            });
            tokenStorage.setToken(data.token);
            tokenStorage.setUserInfo(data.user);
            return data;
        },
        // 注册机构
        registerOrg: (data) => request('/auth/register-org', {
            method: 'POST',
            body: data
        }),
        // 注册普通用户
        registerUser: (data) => request('/auth/register-user', {
            method: 'POST',
            body: data
        }),
        // 注册专家
        registerExpert: (data) => request('/auth/register-expert', {
            method: 'POST',
            body: data
        }),
        // 获取已激活机构列表（注册用）
        getActiveOrgs: () => request('/auth/active-orgs', { method: 'GET' }),
        // 登出
        logout: () => {
            tokenStorage.clear();
            window.location.href = '/login.html';
        }
    },

    topic: {
        // 保存/提交课题
        saveOrSubmit: (data) => request('/topics/save-submit', {
            method: 'POST',
            body: data
        }),
        // 获取我的课题
        getMyTopics: () => request('/topics/my', { method: 'GET' }),
        // 课题详情
        getDetail: (id) => request(`/topics/detail/${id}`, { method: 'GET' }),
        // 课题分类
        getCategories: () => request('/topics/categories', { method: 'GET' }),
        // 有效申报周期
        getActivePeriod: () => request('/topics/active-period', { method: 'GET' }),
        // 申报是否开启
        isOpen: () => request('/topics/is-open', { method: 'GET' })
    },

    institution: {
        // 获取本机构员工
        getUsers: () => request('/institution/users', { method: 'GET' }),
        // 审核本机构员工
        auditUser: (data) => request('/institution/users/audit', {
            method: 'POST',
            body: data
        }),
        // 分配申报权限
        assignAuth: (data) => request('/institution/users/assign-auth', {
            method: 'POST',
            body: data
        }),
        // 获取本机构课题
        getTopics: () => request('/institution/topics', { method: 'GET' }),
        // 审核本机构课题
        auditTopic: (data) => request('/institution/topics/audit', {
            method: 'POST',
            body: data
        })
    },

    expert: {
        // 获取分配给我的评审任务
        getTasks: () => request('/expert/tasks', { method: 'GET' }),
        // 接受或拒绝邀请
        respond: (data) => request('/expert/respond', {
            method: 'POST',
            body: data
        }),
        // 提交评审意见
        opinion: (data) => request('/expert/opinion', {
            method: 'POST',
            body: data
        })
    },

    admin: {
        // 获取待审核机构
        getPendingOrgs: () => request('/admin/orgs/pending', { method: 'GET' }),
        // 获取所有机构
        getAllOrgs: () => request('/admin/orgs', { method: 'GET' }),
        // 审核机构
        auditOrg: (data) => request('/admin/orgs/audit', {
            method: 'POST',
            body: data
        }),
        // 调整额度
        adjustQuota: (orgId, quota) => request(`/admin/orgs/quota?orgId=${orgId}&quota=${quota}`, {
            method: 'POST'
        }),
        // 全局配置申报周期
        configPeriod: (data) => request('/admin/period/config', {
            method: 'POST',
            body: data
        }),
        // 获取周期
        getPeriod: (year) => request(`/admin/period${year ? '?year=' + year : ''}`, { method: 'GET' }),
        // 格式初审
        formatAudit: (data) => request('/admin/topics/audit', {
            method: 'POST',
            body: data
        }),
        // 获取所有课题
        getAllTopics: () => request('/admin/topics', { method: 'GET' }),
        // 专家推荐
        recommendExperts: (topicId) => request(`/admin/experts/recommend?topicId=${topicId}`, { method: 'GET' }),
        // 指派专家
        assignExperts: (data) => request('/admin/experts/assign', {
            method: 'POST',
            body: data
        }),
        // 获取某课题的评审情况
        getReviews: (topicId) => request(`/admin/reviews?topicId=${topicId}`, { method: 'GET' }),
        // 发布最终结果
        publishFinalResult: (data) => request('/admin/topics/publish-result', {
            method: 'POST',
            body: data
        }),
        // 追加第二轮专家
        addSecondExpert: (topicId, expertId) => request(`/admin/topics/add-second-expert?topicId=${topicId}&expertId=${expertId}`, {
            method: 'POST'
        })
    },

    file: {
        // 真实文件上传，提供进度回调
        upload: (fileObject, onProgress) => {
            return new Promise((resolve, reject) => {
                const xhr = new XMLHttpRequest();
                const formData = new FormData();
                formData.append('file', fileObject);

                xhr.upload.addEventListener('progress', (e) => {
                    if (e.lengthComputable && typeof onProgress === 'function') {
                        const percent = Math.round((e.loaded / e.total) * 100);
                        onProgress(percent);
                    }
                });

                xhr.addEventListener('load', () => {
                    if (xhr.status === 200) {
                        try {
                            const res = JSON.parse(xhr.responseText);
                            if (res.code === 200) {
                                resolve(res.data);
                            } else {
                                reject(new Error(res.message || '上传失败'));
                            }
                        } catch (err) {
                            reject(new Error('解析上传响应出错'));
                        }
                    } else {
                        reject(new Error(`服务器响应错误: ${xhr.status}`));
                    }
                });

                xhr.addEventListener('error', () => reject(new Error('网络上传错误')));
                xhr.open('POST', `${API_BASE}/files/upload`);
                
                const token = tokenStorage.getToken();
                if (token) {
                    xhr.setRequestHeader('Authorization', `Bearer ${token}`);
                }
                
                xhr.send(formData);
            });
        },
        // 下载文件，实现拦截和安全下载
        download: async (fileUrl, defaultFilename) => {
            const token = tokenStorage.getToken();
            try {
                const response = await fetch(fileUrl, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (response.status === 403) {
                    showGlobalToast('您无权下载此文件（可能是评审已锁定或您不拥有该资源）！', 'error');
                    return;
                }
                
                if (!response.ok) {
                    showGlobalToast('下载文件失败', 'error');
                    return;
                }

                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                
                // 从 content-disposition 头部读取文件名
                const disposition = response.headers.get('content-disposition');
                let filename = defaultFilename || 'download_file';
                if (disposition && disposition.indexOf('filename=') !== -1) {
                    const matches = /filename="([^"]+)"/.exec(disposition);
                    if (matches != null && matches[1]) {
                        filename = decodeURIComponent(matches[1]);
                    }
                }
                
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            } catch (err) {
                console.error('Download error:', err);
                showGlobalToast('文件下载发生异常', 'error');
            }
        }
    }
};
