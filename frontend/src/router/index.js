import { createRouter, createWebHistory } from 'vue-router';
import { useAuth } from '../stores/auth.js';

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: () => import('../views/system/LandingRedirect.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/login',
            component: () => import('../views/auth/LoginView.vue'),
            meta: { title: '登录', layout: 'blank', requiresAuth: false }
        },
        {
            path: '/procurement/create',
            component: () => import('../views/procurement/ProcurementCreate.vue'),
            meta: { title: '采购申请创建', requiresAuth: true, requiredRoles: ['PURCHASER'] }
        },
        {
            path: '/procurement/list',
            component: () => import('../views/procurement/ProcurementList.vue'),
            meta: { title: '我的采购申请', requiresAuth: true, requiredRoles: ['PURCHASER'] }
        },
        {
            path: '/procurement/inventory',
            component: () => import('../views/shared/InventoryLookup.vue'),
            meta: { title: '库存查询', requiresAuth: true, requiredRoles: ['PURCHASER'] }
        },
        {
            path: '/warehouse/inbound',
            component: () => import('../views/warehouse/InboundCreate.vue'),
            meta: { title: '入库申请创建', requiresAuth: true, requiredRoles: ['OPERATOR'] }
        },
        {
            path: '/warehouse/outbound',
            component: () => import('../views/warehouse/OutboundCreate.vue'),
            meta: { title: '出库申请创建', requiresAuth: true, requiredRoles: ['OPERATOR'] }
        },
        {
            path: '/warehouse/tasks',
            component: () => import('../views/warehouse/TaskList.vue'),
            meta: { title: '待执行任务', requiresAuth: true, requiredRoles: ['OPERATOR'] }
        },
        {
            path: '/warehouse/inventory',
            component: () => import('../views/shared/InventoryLookup.vue'),
            meta: { title: '库存查询', requiresAuth: true, requiredRoles: ['OPERATOR'] }
        },
        {
            path: '/manager/pending-approvals',
            component: () => import('../views/manager/ApprovalList.vue'),
            meta: { title: '待我审批', requiresAuth: true, requiredRoles: ['MANAGER'] }
        },
        {
            path: '/manager/approval/:id?',
            component: () => import('../views/manager/ApprovalDetail.vue'),
            meta: { title: '审批处理', requiresAuth: true, requiredRoles: ['MANAGER'] }
        },
        {
            path: '/manager/analytics',
            component: () => import('../views/manager/AnalyticsDashboard.vue'),
            meta: { title: '报表分析仪表盘', requiresAuth: true, requiredRoles: ['MANAGER'] }
        },
        {
            path: '/manager/monitoring',
            component: () => import('../views/manager/InventoryMonitoring.vue'),
            meta: { title: '库存监控看板', requiresAuth: true, requiredRoles: ['MANAGER'] }
        },
        {
            path: '/manager/locations',
            component: () => import('../views/manager/LocationManagement.vue'),
            meta: { title: '库位管理', requiresAuth: true, requiredRoles: ['MANAGER'] }
        },
        {
            path: '/admin/users',
            component: () => import('../views/admin/UserManagement.vue'),
            meta: { title: '用户管理', requiresAuth: true, requiredRoles: ['ADMIN'] }
        },
        {
            path: '/admin/system',
            component: () => import('../views/admin/SystemDashboard.vue'),
            meta: { title: '系统监控', requiresAuth: true, requiredRoles: ['ADMIN'] }
        },
        {
            path: '/admin/logs',
            component: () => import('../views/admin/OperationLog.vue'),
            meta: { title: '操作日志查询', requiresAuth: true, requiredRoles: ['ADMIN'] }
        },
        {
            path: '/admin/downloads',
            component: () => import('../views/admin/SystemLogDownload.vue'),
            meta: { title: '系统日志下载', requiresAuth: true, requiredRoles: ['ADMIN'] }
        }
    ]
});

router.beforeEach((to, from, next) => {
    const auth = useAuth();
    const requiresAuth = to.meta.requiresAuth !== false;

    if (requiresAuth && !auth.isAuthenticated.value) {
        return next({ path: '/login', query: { redirect: to.fullPath } });
    }

    if (!requiresAuth && to.path === '/login' && auth.isAuthenticated.value) {
        return next(auth.resolveDefaultRoute());
    }

    const requiredRoles = to.meta.requiredRoles;
    if (requiresAuth && Array.isArray(requiredRoles) && requiredRoles.length) {
        const permitted = requiredRoles.some(role => auth.hasRole(role));
        if (!permitted) {
            return next(auth.resolveDefaultRoute());
        }
    }

    next();
});

export default router;
