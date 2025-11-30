const products = [
    { id: 1, name: '工业轴承 A1', sku: 'SKU-1001', unit: '箱', price: 1250 },
    { id: 2, name: '智能扫码枪', sku: 'SKU-1002', unit: '台', price: 980 },
    { id: 3, name: '防静电手套', sku: 'SKU-1003', unit: '包', price: 65 }
];

const inventory = [
    { sku: 'SKU-1001', name: '工业轴承 A1', warehouse: '华东仓', quantity: 520, safetyStock: 400 },
    { sku: 'SKU-1002', name: '智能扫码枪', warehouse: '华北仓', quantity: 115, safetyStock: 150 },
    { sku: 'SKU-1003', name: '防静电手套', warehouse: '华南仓', quantity: 890, safetyStock: 600 }
];

const purchaseRequests = [
    {
        id: 'PO-2025-001',
        applicant: '李雷',
        status: 'SUBMITTED',
        items: [
            { sku: 'SKU-1001', name: '工业轴承 A1', quantity: 60 },
            { sku: 'SKU-1003', name: '防静电手套', quantity: 200 }
        ],
        submittedAt: '2025-11-27 09:32'
    },
    {
        id: 'PO-2025-002',
        applicant: '韩梅梅',
        status: 'APPROVED',
        items: [{ sku: 'SKU-1002', name: '智能扫码枪', quantity: 15 }],
        submittedAt: '2025-11-26 14:18'
    }
];

const warehouseTasks = [
    { id: 'IN-0008', type: 'INBOUND', status: 'EXECUTING', createdAt: '2025-11-28 08:20', assignee: '张伟' },
    { id: 'OUT-0021', type: 'OUTBOUND', status: 'SUBMITTED', createdAt: '2025-11-27 15:42', assignee: '刘敏' }
];

const approvals = [
    { id: 'PO-2025-001', type: '采购申请', status: 'SUBMITTED', applicant: '李雷', submittedAt: '2025-11-27 09:32' },
    { id: 'IN-0008', type: '入库申请', status: 'SUBMITTED', applicant: '张伟', submittedAt: '2025-11-27 16:45' },
    { id: 'OUT-0021', type: '出库申请', status: 'SUBMITTED', applicant: '刘敏', submittedAt: '2025-11-27 18:05' }
];

const operationLogs = [
    { id: 1, operator: 'admin', module: '用户管理', action: '创建用户', details: '新增用户 buyer01', createdAt: '2025-11-27 10:11' },
    { id: 2, operator: 'manager', module: '审批中心', action: '审批通过', details: '采购单 PO-2025-002', createdAt: '2025-11-27 14:32' }
];

const roleOptions = [
    { code: 'BUYER', name: '采购员' },
    { code: 'WAREHOUSE_CLERK', name: '仓库管理员' },
    { code: 'MANAGER', name: '仓库经理' },
    { code: 'ADMIN', name: '系统管理员' }
];

const adminUsers = [
    {
        id: 1,
        username: 'buyer01',
        realName: '李雷',
        department: '采购部',
        status: 'ACTIVE',
        roles: ['BUYER'],
        createdAt: '2025-11-10'
    },
    {
        id: 2,
        username: 'manager',
        realName: '王强',
        department: '运营中心',
        status: 'ACTIVE',
        roles: ['MANAGER'],
        createdAt: '2025-11-08'
    }
];

export const mockApi = {
    listProducts() {
        return Promise.resolve(products);
    },
    listInventory(filters = {}) {
        return Promise.resolve(
            inventory.filter(item => {
                if (filters.keyword) {
                    const keyword = filters.keyword.trim().toLowerCase();
                    return (
                        item.name.toLowerCase().includes(keyword) ||
                        item.sku.toLowerCase().includes(keyword)
                    );
                }
                return true;
            })
        );
    },
    submitPurchase(payload) {
        console.log('Mock submit purchase', payload);
        return Promise.resolve({ success: true });
    },
    listPurchaseRequests() {
        return Promise.resolve(purchaseRequests);
    },
    listWarehouseTasks() {
        return Promise.resolve(warehouseTasks);
    },
    submitInbound(payload) {
        console.log('Mock inbound', payload);
        return Promise.resolve({ success: true });
    },
    submitOutbound(payload) {
        console.log('Mock outbound', payload);
        return Promise.resolve({ success: true });
    },
    listApprovals() {
        return Promise.resolve(approvals);
    },
    fetchApprovalDetail(id) {
        return Promise.resolve(
            approvals.find(item => item.id === id) ?? null
        );
    },
    processApproval() {
        return Promise.resolve({ success: true });
    },
    getDashboardStats() {
        return Promise.resolve({
            inbound: 245,
            outbound: 198,
            inventoryValue: 6.5,
            turnoverDays: 42
        });
    },
    getInventoryTrends() {
        return Promise.resolve([
            { date: '10-01', inbound: 35, outbound: 28 },
            { date: '10-10', inbound: 40, outbound: 25 },
            { date: '10-20', inbound: 28, outbound: 31 },
            { date: '10-30', inbound: 32, outbound: 30 },
            { date: '11-10', inbound: 44, outbound: 35 }
        ]);
    },
    listOperationLogs() {
        return Promise.resolve({
            records: operationLogs.map(item => ({
                ...item,
                createdAtText: item.createdAt
            })),
            total: operationLogs.length,
            page: 1,
            pageSize: operationLogs.length || 20
        });
    },
    exportOperationLogs() {
        const blob = new Blob(['mock operation logs'], { type: 'text/csv' });
        return Promise.resolve({ blob, fileName: 'operation-logs-mock.csv' });
    },
    listLogArchives() {
        return Promise.resolve([
            {
                fileName: 'operation-log-20251127.csv',
                date: '2025-11-27',
                recordCount: operationLogs.length,
                lastRecordAt: operationLogs[0]?.createdAt
            }
        ]);
    },
    downloadLogArchive() {
        const blob = new Blob(['mock daily log'], { type: 'text/csv' });
        return Promise.resolve({ blob, fileName: 'operation-log-mock.csv' });
    },
    previewLogArchive() {
        return Promise.resolve(operationLogs.map(item => ({
            ...item,
            createdAtText: item.createdAt
        })));
    },
    listAdminUsers(params = {}) {
        let result = [...adminUsers];
        if (params.keyword) {
            const kw = params.keyword.toLowerCase();
            result = result.filter(user =>
                user.username.toLowerCase().includes(kw) ||
                user.realName.toLowerCase().includes(kw)
            );
        }
        if (params.status && params.status !== 'ALL') {
            result = result.filter(user => user.status === params.status);
        }
        return Promise.resolve({ items: result, total: result.length });
    },
    saveAdminUser(payload) {
        if (payload.id) {
            const index = adminUsers.findIndex(item => item.id === payload.id);
            if (index >= 0) {
                adminUsers[index] = { ...adminUsers[index], ...payload };
            }
        } else {
            const id = adminUsers.length ? Math.max(...adminUsers.map(item => item.id)) + 1 : 1;
            adminUsers.push({ ...payload, id, createdAt: new Date().toISOString().slice(0, 10) });
        }
        return Promise.resolve({ success: true });
    },
    updateUserStatus(id, status) {
        const user = adminUsers.find(item => item.id === id);
        if (user) user.status = status;
        return Promise.resolve({ success: true });
    },
    assignRoles(id, roles) {
        const user = adminUsers.find(item => item.id === id);
        if (user) user.roles = [...roles];
        return Promise.resolve({ success: true });
    },
    listRoles() {
        return Promise.resolve(roleOptions);
    }
};
