import axios from 'axios';
import { mockApi } from './mockData.js';

const useMock = true;

export const api = {
    listProducts() {
        if (useMock) return mockApi.listProducts();
        return axios.get('/api/products').then(res => res.data);
    },
    listInventory(params) {
        if (useMock) return mockApi.listInventory(params);
        return axios.get('/api/inventory', { params }).then(res => res.data);
    },
    submitPurchase(payload) {
        if (useMock) return mockApi.submitPurchase(payload);
        return axios.post('/api/purchase-requests', payload).then(res => res.data);
    },
    listPurchaseRequests() {
        if (useMock) return mockApi.listPurchaseRequests();
        return axios.get('/api/purchase-requests').then(res => res.data);
    },
    submitInbound(payload) {
        if (useMock) return mockApi.submitInbound(payload);
        return axios.post('/api/inbound-orders', payload).then(res => res.data);
    },
    submitOutbound(payload) {
        if (useMock) return mockApi.submitOutbound(payload);
        return axios.post('/api/outbound-orders', payload).then(res => res.data);
    },
    listWarehouseTasks() {
        if (useMock) return mockApi.listWarehouseTasks();
        return axios.get('/api/warehouse/tasks').then(res => res.data);
    },
    listApprovals() {
        if (useMock) return mockApi.listApprovals();
        return axios.get('/api/approvals').then(res => res.data);
    },
    fetchApprovalDetail(id) {
        if (useMock) return mockApi.fetchApprovalDetail(id);
        return axios.get(`/api/approvals/${id}`).then(res => res.data);
    },
    processApproval(id, payload) {
        if (useMock) return mockApi.processApproval(id, payload);
        return axios.post(`/api/approvals/${id}/decision`, payload).then(res => res.data);
    },
    getDashboardStats() {
        if (useMock) return mockApi.getDashboardStats();
        return axios.get('/api/analytics/overview').then(res => res.data);
    },
    getInventoryTrends() {
        if (useMock) return mockApi.getInventoryTrends();
        return axios.get('/api/analytics/inventory-trends').then(res => res.data);
    },
    listOperationLogs(params) {
        if (useMock) return mockApi.listOperationLogs(params);
        return axios.get('/api/admin/operation-logs', { params }).then(res => res.data);
    },
    listAdminUsers(params) {
        if (useMock) return mockApi.listAdminUsers(params);
        return axios.get('/api/admin/users', { params }).then(res => res.data);
    },
    saveAdminUser(payload) {
        if (useMock) return mockApi.saveAdminUser(payload);
        if (payload.id) {
            return axios.put(`/api/admin/users/${payload.id}`, payload).then(res => res.data);
        }
        return axios.post('/api/admin/users', payload).then(res => res.data);
    },
    updateUserStatus(id, status) {
        if (useMock) return mockApi.updateUserStatus(id, status);
        return axios.patch(`/api/admin/users/${id}/status`, { status }).then(res => res.data);
    },
    assignRoles(id, roles) {
        if (useMock) return mockApi.assignRoles(id, roles);
        return axios.post(`/api/admin/users/${id}/roles`, { roles }).then(res => res.data);
    },
    listRoles() {
        if (useMock) return mockApi.listRoles();
        return axios.get('/api/admin/roles').then(res => res.data);
    }
};
