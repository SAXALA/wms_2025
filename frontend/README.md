# WMS 2025 前端工作台

使用 Vue 3 + Vite + Element Plus + ECharts 构建的前端界面，覆盖采购员、仓库管理员、仓库经理与系统管理员四类角色的工作台。

## 开发启动

```bash
cd frontend
npm install
npm run dev
```

默认开发服务器运行于 `http://localhost:5173`，并通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`。

## 目录结构

- `src/router`：路由表及侧边菜单配置
- `src/views`：按角色划分的页面模块
- `src/services`：接口封装（默认启用 Mock 数据，可切换真实接口）
- `src/components`：通用组件（侧边栏、头部、状态标签等）
- `src/layouts`：主布局骨架

## Mock 切换

`src/services/api.js` 中通过 `useMock` 控制是否调用真实后端。如果接入后端接口，将其设置为 `false` 并确保代理地址正确。
