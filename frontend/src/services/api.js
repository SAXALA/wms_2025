import axios from 'axios';
import router from '../router';
import { mockApi } from './mockData.js';
import { useAuth } from '../stores/auth.js';

const useMock = false;

const client = axios.create({
    baseURL: '/api',
    timeout: 10000
});

const auth = useAuth();

const unwrap = response => response?.data?.data ?? response?.data ?? null;

const formatDateTime = iso => {
    if (!iso) return '';
    try {
        return new Date(iso).toLocaleString();
    } catch (error) {
        console.warn('[api] failed to format time', error);
        return iso;
    }
};

const toProduct = product => ({
    id: product.id,
    name: product.name ?? `商品${product.id}`,
    sku: product.sku ?? `SKU-${product.id}`,
    unit: product.unit ?? '件',
    price: Number(product.price ?? 0)
});

const toLocation = location => ({
    id: location.id,
    code: location.code,
    name: location.name,
    description: location.description,
    active: location.active ?? location.isActive ?? true,
    createdAt: formatDateTime(location.createdAt),
    updatedAt: formatDateTime(location.updatedAt),
    displayLabel: [location.code, location.name].filter(Boolean).join(' · ')
});

const toInventoryRow = stock => {
    const locationId = stock.locationId ?? stock.location?.id;
    const locationCode = stock.locationCode ?? stock.location?.code;
    const locationName = stock.locationName ?? stock.location?.name;
    const locationLabel = [locationCode, locationName].filter(Boolean).join(' · ') || '主仓库';
    return {
        productId: stock.productId,
        sku: stock.sku ?? `SKU-${stock.productId}`,
        name: stock.name ?? `商品${stock.productId}`,
        unit: stock.unit ?? '件',
        warehouse: locationLabel,
        locationId,
        locationCode,
        locationName,
        locationLabel,
        quantity: stock.currentStock,
        safetyStock: stock.safetyStock,
        lockedStock: stock.lockedStock
    };
};

const toProcurementItem = item => ({
    id: item.id,
    productId: item.productId,
    quantity: item.quantity,
    expectedPrice: item.expectedPrice,
    name: `商品${item.productId}`,
    sku: `SKU-${item.productId}`
});

const toProcurementApplication = app => ({
    id: `PROCUREMENT:${app.id}`,
    rawId: app.id,
    displayId: `PO-${String(app.id).padStart(6, '0')}`,
    title: app.title,
    applicant: app.applicant,
    status: app.status,
    submittedAt: formatDateTime(app.createdAt),
    items: Array.isArray(app.items) ? app.items.map(toProcurementItem) : []
});

const toInventoryApplication = app => {
    const type = app.type === 'IN' ? 'INBOUND' : 'OUTBOUND';
    return {
        id: `${type}:${app.id}`,
        rawId: app.id,
        type,
        typeLabel: type === 'INBOUND' ? '入库申请' : '出库申请',
        status: app.status,
        applicant: app.applicant,
        submittedAt: formatDateTime(app.createdAt),
        reason: app.reason,
        items: Array.isArray(app.items)
            ? app.items.map(item => ({
                id: item.id,
                productId: item.productId,
                quantity: item.quantity,
                actualQuantity: item.actualQuantity,
                locationId: item.locationId,
                locationCode: item.locationCode,
                locationName: item.locationName
            }))
            : []
    };
};

const ensureArray = value => (Array.isArray(value) ? value : []);

const parseCompositeId = composite => {
    if (!composite) return { type: '', id: '' };
    const [type, raw] = String(composite).split(':');
    return { type, id: raw };
};

const buildInventoryPayload = (payload, type) => ({
    type,
    reason: payload.reference?.trim() || '由前端创建的库存申请',
    items: ensureArray(payload.lines)
        .map(line => {
            const productId = Number(line.productId ?? line.id ?? line.sku?.replace(/[^0-9]/g, ''));
            const quantity = Number(line.quantity ?? 0);
            const locationRaw = line.locationId ?? line.location?.id ?? line.location;
            const locationId = Number(locationRaw);
            const item = { productId, quantity };
            if (Number.isFinite(locationId) && locationId > 0) {
                item.locationId = locationId;
            }
            return item;
        })
        .filter(item => Number.isFinite(item.productId) && item.productId > 0 && item.quantity > 0)
});

const call = async (factory, fallback, label) => {
    try {
        const response = await factory();
        return unwrap(response);
    } catch (error) {
        if (fallback) {
            console.warn(`[api] ${label ?? 'request'} failed, using mock`, error);
            return fallback();
        }
        throw error;
    }
};

const extractFileName = headers => {
    const disposition = headers?.['content-disposition'] ?? headers?.get?.('content-disposition');
    if (!disposition) {
        return '';
    }
    const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
    if (utf8Match && utf8Match[1]) {
        return decodeURIComponent(utf8Match[1]);
    }
    const asciiMatch = /filename="?([^";]+)"?/i.exec(disposition);
    return asciiMatch && asciiMatch[1] ? asciiMatch[1] : '';
};

client.interceptors.request.use(config => {
    const token = auth.state.token;
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

client.interceptors.response.use(
    response => response,
    error => {
        if (error?.response?.status === 401) {
            auth.clearSession();
            if (router.currentRoute.value.path !== '/login') {
                router.replace({
                    path: '/login',
                    query: { redirect: router.currentRoute.value.fullPath }
                });
            }
        }
        return Promise.reject(error);
    }
);

export const api = {
    async login(payload) {
        if (useMock) return mockApi.login?.(payload);
        const data = await call(() => client.post('/auth/login', payload), null, 'login');
        if (!data?.token) {
            throw new Error('登录失败，请检查用户名或密码');
        }
        const authorities = ensureArray(data.roles).map(role => role?.toString());
        auth.setSession({
            token: data.token,
            username: data.username,
            authorities
        });
        return {
            token: data.token,
            username: data.username,
            authorities
        };
    },
    logout() {
        auth.clearSession();
    },
    async listProducts() {
        if (useMock) return mockApi.listProducts();
        const data = await call(() => client.get('/products'), () => mockApi.listProducts(), 'listProducts');
        return ensureArray(data).map(toProduct);
    },
    async listInventory(params) {
        if (useMock) return mockApi.listInventory(params);
        const data = await call(() => client.get('/inventory/stocks'), () => mockApi.listInventory(params), 'listInventory');
        const keyword = params?.keyword?.trim()?.toLowerCase();
        return ensureArray(data)
            .map(toInventoryRow)
            .filter(item => {
                if (!keyword) return true;
                return item.sku.toLowerCase().includes(keyword) || item.name.toLowerCase().includes(keyword);
            });
    },
    async submitPurchase(payload) {
        if (useMock) return mockApi.submitPurchase(payload);
        const items = ensureArray(payload.items).map(item => ({
            productId: item.id ?? item.productId,
            quantity: Number(item.quantity ?? 0),
            expectedPrice: item.expectedPrice ?? item.price ?? 0
        })).filter(item => item.productId && item.quantity > 0);
        if (!items.length) {
            throw new Error('采购申请至少需要一条有效物料');
        }
        const totalAmount = items.reduce((sum, item) => sum + Number(item.expectedPrice ?? 0) * item.quantity, 0);
        const body = {
            title: payload.remark?.trim() || `采购申请-${Date.now()}`,
            totalAmount,
            items
        };
        return call(() => client.post('/procurement/applications', body), () => mockApi.submitPurchase(payload), 'submitPurchase');
    },
    async listPurchaseRequests() {
        if (useMock) return mockApi.listPurchaseRequests();
        const data = await call(() => client.get('/procurement/applications'), () => mockApi.listPurchaseRequests(), 'listPurchaseRequests');
        return ensureArray(data).map(toProcurementApplication);
    },
    async submitInbound(payload) {
        if (useMock) return mockApi.submitInbound(payload);
        const body = buildInventoryPayload(payload, 'IN');
        if (!body.items.length) {
            throw new Error('入库申请至少需要一条有效物料');
        }
        return call(() => client.post('/inventory/in-applications', body), () => mockApi.submitInbound(payload), 'submitInbound');
    },
    async submitOutbound(payload) {
        if (useMock) return mockApi.submitOutbound(payload);
        const body = buildInventoryPayload(payload, 'OUT');
        if (!body.items.length) {
            throw new Error('出库申请至少需要一条有效物料');
        }
        return call(() => client.post('/inventory/out-applications', body), () => mockApi.submitOutbound(payload), 'submitOutbound');
    },
    async listLocations(params) {
        if (useMock) return [];
        const query = {};
        if (params?.includeInactive) {
            query.includeInactive = params.includeInactive;
        }
        const data = await call(() => client.get('/warehouse/locations', { params: query }), () => [], 'listLocations');
        return ensureArray(data).map(toLocation);
    },
    async createLocation(payload) {
        if (useMock) return { id: Date.now(), ...payload };
        const body = {
            code: payload.code,
            name: payload.name,
            description: payload.description
        };
        const data = await call(() => client.post('/warehouse/locations', body), null, 'createLocation');
        return data ? toLocation(data) : null;
    },
    async updateLocation(id, payload) {
        if (useMock) return { id, ...payload };
        const body = {
            code: payload.code,
            name: payload.name,
            description: payload.description,
            active: payload.active
        };
        const data = await call(() => client.put(`/warehouse/locations/${id}`, body), null, 'updateLocation');
        return data ? toLocation(data) : null;
    },
    async toggleLocationStatus(id, active) {
        if (useMock) return { id, active };
        const data = await call(() => client.patch(`/warehouse/locations/${id}/status`, { active }), null, 'toggleLocationStatus');
        return data ? toLocation(data) : null;
    },
    async deleteLocation(id) {
        if (useMock) return true;
        await call(() => client.delete(`/warehouse/locations/${id}`), null, 'deleteLocation');
        return true;
    },
    async listWarehouseTasks() {
        if (useMock) return mockApi.listWarehouseTasks();
        const [inApps, outApps] = await Promise.all([
            call(() => client.get('/inventory/in-applications'), () => [], 'listInboundTasks'),
            call(() => client.get('/inventory/out-applications'), () => [], 'listOutboundTasks')
        ]);
        return [...ensureArray(inApps).map(toInventoryApplication), ...ensureArray(outApps).map(toInventoryApplication)]
            .map(task => ({
                id: task.id,
                displayId: `${task.type === 'INBOUND' ? 'IN' : 'OUT'}-${String(task.rawId).padStart(6, '0')}`,
                type: task.type,
                typeLabel: task.typeLabel,
                status: task.status,
                applicant: task.applicant,
                createdAt: task.submittedAt
            }))
            .sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
    },
    async listApprovals() {
        if (useMock) return mockApi.listApprovals();
        const [procurement, inbound, outbound] = await Promise.all([
            call(() => client.get('/procurement/applications/pending'), () => [], 'pendingProcurements'),
            call(() => client.get('/inventory/in-applications'), () => [], 'pendingInbound'),
            call(() => client.get('/inventory/out-applications'), () => [], 'pendingOutbound')
        ]);
        const procurementItems = ensureArray(procurement).map(app => ({
            id: `PROCUREMENT:${app.id}`,
            displayId: `PO-${String(app.id).padStart(6, '0')}`,
            type: 'PROCUREMENT',
            typeLabel: '采购申请',
            applicant: app.applicant,
            submittedAt: formatDateTime(app.createdAt),
            status: app.status
        }));
        const inventoryItems = [...ensureArray(inbound), ...ensureArray(outbound)]
            .filter(app => app.status === 'PENDING_APPROVAL')
            .map(toInventoryApplication)
            .map(app => ({
                id: `${app.type}:${app.rawId}`,
                displayId: `${app.type === 'INBOUND' ? 'IN' : 'OUT'}-${String(app.rawId).padStart(6, '0')}`,
                type: app.type,
                typeLabel: app.typeLabel,
                applicant: app.applicant,
                submittedAt: app.submittedAt,
                status: app.status
            }));
        return [...procurementItems, ...inventoryItems].sort((a, b) => (b.submittedAt || '').localeCompare(a.submittedAt || ''));
    },
    async fetchApprovalDetail(compositeId) {
        if (useMock) return mockApi.fetchApprovalDetail(compositeId);
        const { type, id } = parseCompositeId(compositeId);
        if (!id) return null;
        if (type === 'PROCUREMENT') {
            const data = await call(() => client.get(`/procurement/applications/${id}`), () => null, 'fetchProcurementDetail');
            if (!data) return null;
            return { ...toProcurementApplication(data), type, id: compositeId };
        }
        const endpoint = type === 'INBOUND' ? '/inventory/in-applications' : '/inventory/out-applications';
        const list = await call(() => client.get(endpoint), () => [], 'fetchInventoryDetail');
        const match = ensureArray(list).find(item => String(item.id) === String(id));
        if (!match) return null;
        return { ...toInventoryApplication(match), id: compositeId };
    },
    async processApproval(compositeId, payload) {
        if (useMock) return mockApi.processApproval(compositeId, payload);
        const { type, id } = parseCompositeId(compositeId);
        if (!id) throw new Error('未知审批单号');
        const approved = payload.status === 'APPROVED';
        const comment = payload.remark ?? '';
        if (type === 'PROCUREMENT') {
            return call(() => client.put(`/procurement/applications/${id}/approve`, { approved, comment }), null, 'approveProcurement');
        }
        const endpoint = type === 'INBOUND' ? `/inventory/in-applications/${id}/approve` : `/inventory/out-applications/${id}/approve`;
        return call(() => client.put(endpoint, { approved, comment }), null, 'approveInventory');
    },
    async getDashboardStats() {
        if (useMock) return mockApi.getDashboardStats();
        const [summary, approvals] = await Promise.all([
            call(() => client.get('/reports/inventory-summary'), () => null, 'inventorySummary'),
            call(() => client.get('/reports/approval-stats'), () => null, 'approvalStats')
        ]);
        return {
            inbound: approvals?.inventoryInApprovals ?? 0,
            outbound: approvals?.inventoryOutApprovals ?? 0,
            inventoryValue: Number(summary?.totalInventoryValue ?? 0),
            turnoverDays: Number(approvals?.averageApprovalHours ?? 0).toFixed ? Number(Number(approvals?.averageApprovalHours ?? 0).toFixed(1)) : approvals?.averageApprovalHours ?? 0
        };
    },
    async getInventoryTrends() {
        if (useMock) return mockApi.getInventoryTrends();
        const data = await call(() => client.get('/reports/operation-trends', { params: { days: 30 } }), () => mockApi.getInventoryTrends(), 'inventoryTrends');
        return ensureArray(data?.points).map(point => ({
            date: point.date,
            inbound: point.inCount,
            outbound: point.outCount,
            approvals: point.approvalCount
        }));
    },
    async listOperationLogs(params = {}) {
        if (useMock) return mockApi.listOperationLogs(params);
        const query = {
            username: params?.username,
            module: params?.module,
            action: params?.action,
            page: Math.max((params?.page ?? 1) - 1, 0),
            size: params?.size ?? 20,
            startTime: params?.startTime,
            endTime: params?.endTime
        };
        const data = await call(() => client.get('/admin/logs', { params: query }), () => ({ content: [] }), 'listOperationLogs');
        const records = ensureArray(data?.content).map(item => ({
            id: item.id,
            operator: item.operator || '-',
            module: item.module,
            action: item.action,
            details: item.details,
            createdAt: item.createdAt,
            createdAtText: formatDateTime(item.createdAt)
        }));
        return {
            records,
            total: Number(data?.totalElements ?? records.length),
            page: Number(data?.pageNumber ?? query.page) + 1,
            pageSize: Number(data?.pageSize ?? query.size)
        };
    },
    async exportOperationLogs(params = {}) {
        if (useMock && typeof mockApi.exportOperationLogs === 'function') {
            return mockApi.exportOperationLogs(params);
        }
        const query = {
            username: params?.username,
            module: params?.module,
            action: params?.action,
            startTime: params?.startTime,
            endTime: params?.endTime
        };
        const response = await client.get('/admin/logs/export', { params: query, responseType: 'blob' });
        const fileName = extractFileName(response.headers) || `operation-logs-${Date.now()}.csv`;
        return { blob: response.data, fileName };
    },
    async listLogArchives(params = {}) {
        if (useMock && typeof mockApi.listLogArchives === 'function') {
            return mockApi.listLogArchives(params);
        }
        const query = { limit: params?.limit ?? 30 };
        const data = await call(() => client.get('/admin/logs/archives', { params: query }), () => [], 'listLogArchives');
        return ensureArray(data).map(item => ({
            fileName: item.fileName,
            date: item.date,
            recordCount: item.recordCount ?? 0,
            lastRecordAt: item.lastRecordAt,
            generatedAtText: formatDateTime(item.lastRecordAt)
        }));
    },
    async downloadLogArchive(date) {
        if (!date) {
            throw new Error('缺少日志日期');
        }
        if (useMock && typeof mockApi.downloadLogArchive === 'function') {
            return mockApi.downloadLogArchive(date);
        }
        const response = await client.get(`/admin/logs/archives/${date}/download`, { responseType: 'blob' });
        const fileName = extractFileName(response.headers) || `operation-log-${date}.csv`;
        return { blob: response.data, fileName };
    },
    async previewLogArchive(date, params = {}) {
        if (!date) {
            throw new Error('缺少日志日期');
        }
        if (useMock && typeof mockApi.previewLogArchive === 'function') {
            return mockApi.previewLogArchive(date, params);
        }
        const query = { size: params?.size ?? 50 };
        const data = await call(() => client.get(`/admin/logs/archives/${date}/preview`, { params: query }), () => [], 'previewLogArchive');
        return ensureArray(data).map(item => ({
            id: item.id,
            operator: item.operator || '-',
            module: item.module,
            action: item.action,
            details: item.details,
            createdAt: item.createdAt,
            createdAtText: formatDateTime(item.createdAt)
        }));
    },
    async listAdminUsers(params) {
        if (useMock) return mockApi.listAdminUsers(params);
        const query = {
            username: params?.keyword?.trim() || undefined,
            status: params?.status && params.status !== 'ALL' ? params.status : undefined,
            role: params?.role || undefined,
            page: Math.max((params?.page ?? 1) - 1, 0),
            size: params?.pageSize ?? 20
        };
        const data = await call(() => client.get('/admin/users', { params: query }), () => ({ content: [], totalElements: 0 }), 'listAdminUsers');
        const items = ensureArray(data?.content).map(user => ({
            id: user.id,
            username: user.username,
            realName: user.realName,
            department: user.department,
            status: user.status,
            roles: ensureArray(user.roles),
            createdAt: formatDateTime(user.createdAt),
            updatedAt: formatDateTime(user.updatedAt)
        }));
        return {
            items,
            total: data?.totalElements ?? items.length,
            page: (data?.page ?? 0) + 1,
            pageSize: data?.size ?? query.size
        };
    },
    async saveAdminUser(payload) {
        if (useMock) return mockApi.saveAdminUser(payload);
        if (payload.id) {
            const body = {
                realName: payload.realName,
                department: payload.department
            };
            return call(() => client.put(`/admin/users/${payload.id}`, body), null, 'updateAdminUser');
        }
        const body = {
            username: payload.username,
            password: payload.password,
            realName: payload.realName,
            department: payload.department,
            roles: ensureArray(payload.roles)
        };
        if (!body.password || body.password.length < 8) {
            throw new Error('初始密码至少 8 位');
        }
        if (!body.roles.length) {
            throw new Error('请至少选择一个角色');
        }
        return call(() => client.post('/admin/users', body), null, 'createAdminUser');
    },
    async updateUserStatus(id, status) {
        if (useMock) return mockApi.updateUserStatus(id, status);
        return call(() => client.put(`/admin/users/${id}/status`, { status }), null, 'updateUserStatus');
    },
    async assignRoles(id, roles) {
        if (useMock) return mockApi.assignRoles(id, roles);
        return call(() => client.post(`/admin/users/${id}/roles`, { roleCodes: ensureArray(roles) }), null, 'assignRoles');
    },
    async listRoles() {
        if (useMock) return mockApi.listRoles();
        const data = await call(() => client.get('/admin/roles'), () => mockApi.listRoles(), 'listRoles');
        return ensureArray(data).map(role => ({
            code: role.code,
            name: role.name ?? role.code,
            description: role.description
        }));
    }
};
