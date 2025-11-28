import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/procurement/create' },
        {
            path: '/procurement/create',
            component: () => import('../views/procurement/ProcurementCreate.vue'),
            meta: { title: '采购申请创建', role: 'BUYER' }
        },
        {
            path: '/procurement/list',
            component: () => import('../views/procurement/ProcurementList.vue'),
            meta: { title: '我的采购申请', role: 'BUYER' }
        },
        {
            path: '/procurement/inventory',
            component: () => import('../views/shared/InventoryLookup.vue'),
            meta: { title: '库存查询', role: 'BUYER' }
        },
        {
            path: '/warehouse/inbound',
            component: () => import('../views/warehouse/InboundCreate.vue'),
            meta: { title: '入库申请创建', role: 'WAREHOUSE_CLERK' }
        },
        {
            path: '/warehouse/outbound',
            component: () => import('../views/warehouse/OutboundCreate.vue'),
            meta: { title: '出库申请创建', role: 'WAREHOUSE_CLERK' }
        },
        {
            path: '/warehouse/tasks',
            component: () => import('../views/warehouse/TaskList.vue'),
            meta: { title: '待执行任务', role: 'WAREHOUSE_CLERK' }
        },
        {
            path: '/warehouse/inventory',
            component: () => import('../views/shared/InventoryLookup.vue'),
            meta: { title: '库存查询', role: 'WAREHOUSE_CLERK' }
        },
        {
            path: '/manager/pending-approvals',
            component: () => import('../views/manager/ApprovalList.vue'),
            meta: { title: '待我审批', role: 'MANAGER' }
        },
        {
            path: '/manager/approval/:id?',
            component: () => import('../views/manager/ApprovalDetail.vue'),
            meta: { title: '审批处理', role: 'MANAGER' }
        },
        {
            path: '/manager/analytics',
            component: () => import('../views/manager/AnalyticsDashboard.vue'),
            meta: { title: '报表分析仪表盘', role: 'MANAGER' }
        },
        {
            path: '/manager/monitoring',
            component: () => import('../views/manager/InventoryMonitoring.vue'),
            meta: { title: '库存监控看板', role: 'MANAGER' }
        },
        {
            path: '/admin/users',
            component: () => import('../views/admin/UserManagement.vue'),
            meta: { title: '用户管理', role: 'ADMIN' }
        },
        {
            path: '/admin/system',
            component: () => import('../views/admin/SystemDashboard.vue'),
            meta: { title: '系统监控', role: 'ADMIN' }
        },
        {
            path: '/admin/logs',
            component: () => import('../views/admin/OperationLog.vue'),
            meta: { title: '操作日志查询', role: 'ADMIN' }
        },
        {
            path: '/admin/downloads',
            component: () => import('../views/admin/SystemLogDownload.vue'),
            meta: { title: '系统日志下载', role: 'ADMIN' }
        }
    ]
});

export default router;
