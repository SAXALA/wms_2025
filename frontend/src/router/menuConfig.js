export const menu = [
    {
        base: '/procurement',
        title: '采购员工作台',
        icon: 'ShoppingCart',
        children: [
            { path: '/procurement/create', title: '采购申请创建' },
            { path: '/procurement/list', title: '我的申请列表' },
            { path: '/procurement/inventory', title: '库存查询' }
        ]
    },
    {
        base: '/warehouse',
        title: '仓库管理员',
        icon: 'Box',
        children: [
            { path: '/warehouse/inbound', title: '入库申请' },
            { path: '/warehouse/outbound', title: '出库申请' },
            { path: '/warehouse/tasks', title: '待执行任务' },
            { path: '/warehouse/inventory', title: '库存查询' }
        ]
    },
    {
        base: '/manager',
        title: '仓库经理',
        icon: 'Suitcase',
        children: [
            { path: '/manager/pending-approvals', title: '待我审批' },
            { path: '/manager/approval/:id?', title: '审批处理' },
            { path: '/manager/analytics', title: '报表仪表盘' },
            { path: '/manager/monitoring', title: '库存监控' }
        ]
    },
    {
        base: '/admin',
        title: '系统管理员',
        icon: 'Management',
        children: [
            { path: '/admin/users', title: '用户管理' },
            { path: '/admin/system', title: '系统监控' },
            { path: '/admin/logs', title: '操作日志' },
            { path: '/admin/downloads', title: '日志下载' }
        ]
    }
];
