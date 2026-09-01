const API = {
    base: '',

    async request(method, path, body) {
        const opts = {
            method,
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' }
        };
        if (body) opts.body = JSON.stringify(body);

        const res = await fetch(this.base + path, opts);
        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            throw new Error(data.error || `Request failed (${res.status})`);
        }
        return data;
    },

    auth: {
        register: (data) => API.request('POST', '/api/auth/register', data),
        login: (data) => API.request('POST', '/api/auth/login', data),
        logout: () => API.request('DELETE', '/api/auth/logout'),
        me: () => API.request('GET', '/api/auth/me'),
        forgotPassword: (email) => API.request('POST', '/api/auth/forgot-password', { email }),
        resetPassword: (token, password) => API.request('POST', '/api/auth/reset-password', { token, password })
    },

    trips: {
        list: (q, status) => {
            const params = new URLSearchParams();
            if (q) params.set('q', q);
            if (status) params.set('status', status);
            const qs = params.toString();
            return API.request('GET', '/api/trips' + (qs ? '?' + qs : ''));
        },
        get: (id) => API.request('GET', `/api/trips/${id}`),
        create: (data) => API.request('POST', '/api/trips', data),
        update: (id, data) => API.request('PUT', `/api/trips/${id}`, data),
        delete: (id) => API.request('DELETE', `/api/trips/${id}`),
        join: (inviteCode) => API.request('POST', '/api/trips/0/join', { inviteCode }),
        members: (id) => API.request('GET', `/api/trips/${id}/members`),
        timeline: (id) => API.request('GET', `/api/trips/${id}/timeline`),
        archive: (id) => API.request('POST', `/api/trips/${id}/archive`, {})
    },

    expenses: {
        list: (tripId, filters = {}) => {
            const params = new URLSearchParams({ tripId, ...filters });
            return API.request('GET', '/api/expenses?' + params);
        },
        create: (data) => API.request('POST', '/api/expenses', data),
        settlements: (tripId) => API.request('GET', `/api/expenses/settlements?tripId=${tripId}`)
    },

    itinerary: {
        list: (tripId) => API.request('GET', `/api/itinerary?tripId=${tripId}`),
        create: (data) => API.request('POST', '/api/itinerary', data)
    },

    checklist: {
        list: (tripId) => API.request('GET', `/api/checklist?tripId=${tripId}`),
        create: (data) => API.request('POST', '/api/checklist', data),
        assign: (data) => API.request('PUT', '/api/checklist', data)
    },

    memories: {
        list: (tripId) => API.request('GET', `/api/memories?tripId=${tripId}`),
        create: (data) => API.request('POST', '/api/memories', data),
        like: (tripId, memoryId) => API.request('PUT', '/api/memories', { tripId, memoryId })
    },

    analytics: {
        get: (tripId) => API.request('GET', `/api/analytics?tripId=${tripId}`)
    },

    notifications: {
        list: () => API.request('GET', '/api/notifications'),
        markRead: () => API.request('POST', '/api/notifications', {})
    },

    emergency: {
        get: (tripId) => API.request('GET', `/api/emergency?tripId=${tripId}`),
        save: (data) => API.request('POST', '/api/emergency', data)
    }
};
