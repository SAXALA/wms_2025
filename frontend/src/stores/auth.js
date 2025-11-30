import { reactive, computed, readonly } from 'vue';

const STORAGE_KEY = 'wms2025.auth';

const ROLE_ALIAS = {
    BUYER: ['PURCHASER'],
    PURCHASER: ['PURCHASER'],
    WAREHOUSE_CLERK: ['OPERATOR'],
    OPERATOR: ['OPERATOR'],
    MANAGER: ['MANAGER'],
    ADMIN: ['ADMIN']
};

const readStorage = () => {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return {};
        }
        return JSON.parse(raw) ?? {};
    } catch (error) {
        console.warn('[auth] failed to parse session storage', error);
        return {};
    }
};

const initial = readStorage();

const state = reactive({
    token: initial.token ?? '',
    username: initial.username ?? '',
    roles: Array.isArray(initial.roles) ? initial.roles : [],
    authorities: Array.isArray(initial.authorities) ? initial.authorities : []
});

const exposedState = readonly(state);
const isAuthenticated = computed(() => Boolean(state.token));

const normalizeRole = role => {
    if (!role) return '';
    return String(role).replace(/^ROLE_/i, '').toUpperCase();
};

const persist = () => {
    if (!state.token) {
        localStorage.removeItem(STORAGE_KEY);
        return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
        token: state.token,
        username: state.username,
        roles: state.roles,
        authorities: state.authorities
    }));
};

const setSession = session => {
    const token = session?.token ?? '';
    state.token = token;
    state.username = session?.username ?? '';
    const authorities = Array.isArray(session?.authorities ?? session?.roles)
        ? (session?.authorities ?? session?.roles)
        : [];
    state.authorities = authorities.map(item => String(item));
    state.roles = state.authorities
        .map(normalizeRole)
        .filter(Boolean);
    persist();
};

const clearSession = () => {
    state.token = '';
    state.username = '';
    state.roles = [];
    state.authorities = [];
    persist();
};

const hasRole = role => {
    const normalized = normalizeRole(role);
    if (!normalized) {
        return true;
    }
    if (state.roles.includes('ADMIN')) {
        return true;
    }
    const expected = ROLE_ALIAS[normalized] ?? [normalized];
    return expected.some(alias => state.roles.includes(alias));
};

const resolveDefaultRoute = () => {
    if (!isAuthenticated.value) {
        return '/login';
    }
    if (hasRole('ADMIN')) {
        return '/admin/users';
    }
    if (hasRole('MANAGER')) {
        return '/manager/pending-approvals';
    }
    if (hasRole('OPERATOR')) {
        return '/warehouse/tasks';
    }
    if (hasRole('PURCHASER')) {
        return '/procurement/create';
    }
    return '/login';
};

export function useAuth() {
    return {
        state: exposedState,
        isAuthenticated,
        hasRole,
        setSession,
        clearSession,
        resolveDefaultRoute
    };
}

export const getToken = () => state.token;
