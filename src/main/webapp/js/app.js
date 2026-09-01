const App = {
    user: null,
    currentTrip: null,
    currentTab: 'overview',
    draftKey: 'tripsync_draft',

    async init() {
        this.loadTheme();
        this.bindForms();
        try {
            this.user = await API.auth.me();
            this.showDashboard();
        } catch {
            this.showView('landing');
        }
        this.startNotificationPolling();
    },

    loadTheme() {
        const theme = localStorage.getItem('tripsync_theme') || 'light';
        document.documentElement.setAttribute('data-theme', theme);
    },

    toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('tripsync_theme', next);
    },

    showView(name) {
        document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
        const view = document.getElementById(name + 'View') || document.getElementById('landingView');
        view.classList.remove('hidden');
        this.updateNav();
    },

    updateNav() {
        const nav = document.getElementById('navActions');
        if (this.user) {
            nav.innerHTML = `
                <span class="text-muted">${this.user.name}</span>
                <button class="btn btn-outline btn-sm" onclick="App.toggleTheme()"><i class="bi bi-moon"></i></button>
                <button class="btn btn-outline btn-sm" onclick="App.showDashboard()"><i class="bi bi-grid"></i> Trips</button>
                <button class="btn btn-outline btn-sm" onclick="App.logout()">Logout</button>
            `;
        } else {
            nav.innerHTML = `
                <button class="btn btn-outline btn-sm" onclick="App.toggleTheme()"><i class="bi bi-moon"></i></button>
                <button class="btn btn-outline btn-sm" onclick="App.showView('login')">Sign In</button>
            `;
        }
    },

    bindForms() {
        document.getElementById('loginForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                this.user = await API.auth.login(Object.fromEntries(fd));
                this.toast('Welcome back!', 'success');
                this.showDashboard();
            } catch (err) { this.toast(err.message, 'error'); }
        });

        document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                this.user = await API.auth.register(Object.fromEntries(fd));
                this.toast('Account created!', 'success');
                this.showDashboard();
            } catch (err) { this.toast(err.message, 'error'); }
        });

        document.getElementById('forgotForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = new FormData(e.target).get('email');
            try {
                const result = await API.auth.forgotPassword(email);
                const el = document.getElementById('resetTokenDisplay');
                el.classList.remove('hidden');
                el.innerHTML = `<p>Reset token (dev mode):</p><code>${result.resetToken}</code>`;
                this.toast(result.message, 'success');
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    async logout() {
        await API.auth.logout();
        this.user = null;
        this.showView('landing');
    },

    async showDashboard() {
        this.showView('dashboard');
        await this.loadTrips();
    },

    async loadTrips(query) {
        try {
            const trips = await API.trips.list(query);
            this.renderTrips(trips);
        } catch (err) { this.toast(err.message, 'error'); }
    },

    searchTrips: debounce(function(q) { App.loadTrips(q); }, 300),

    renderTrips(trips) {
        const grid = document.getElementById('tripsGrid');
        if (!trips.length) {
            grid.innerHTML = `<div class="empty-state"><i class="bi bi-suitcase-lg"></i><p>No trips yet. Create one or join with an invite code!</p></div>`;
            return;
        }
        grid.innerHTML = trips.map(t => {
            const spent = t.totalSpent || 0;
            const budget = t.budget || 1;
            const pct = Math.min(100, (spent / budget) * 100);
            const barClass = pct >= 100 ? 'danger' : pct >= 80 ? 'warning' : '';
            return `
                <div class="trip-card" onclick="App.openTrip(${t.id})">
                    <div style="display:flex;justify-content:space-between;align-items:start">
                        <div><h3>${esc(t.name)}</h3><p class="destination"><i class="bi bi-geo-alt"></i> ${esc(t.destination)}</p></div>
                        <span class="badge badge-${(t.status||'planning').toLowerCase()}">${t.status}</span>
                    </div>
                    <div class="meta">
                        <span><i class="bi bi-calendar"></i> ${fmtDate(t.startDate)} – ${fmtDate(t.endDate)}</span>
                        <span><i class="bi bi-people"></i> ${t.memberCount || 0} members</span>
                    </div>
                    <div class="budget-bar"><div class="budget-fill ${barClass}" style="width:${pct}%"></div></div>
                    <div class="meta"><span>₹${spent} / ₹${budget}</span></div>
                </div>`;
        }).join('');
    },

    async openTrip(id) {
        try {
            this.currentTrip = await API.trips.get(id);
            this.currentTab = 'overview';
            this.renderTripDetail();
            this.showView('tripDetail');
        } catch (err) { this.toast(err.message, 'error'); }
    },

    renderTripDetail() {
        const t = this.currentTrip;
        document.getElementById('tripHeader').innerHTML = `
            <button class="btn btn-outline btn-sm" style="margin-bottom:1rem;color:#fff;border-color:rgba(255,255,255,.4)" onclick="App.showDashboard()">
                <i class="bi bi-arrow-left"></i> Back
            </button>
            <h2>${esc(t.name)}</h2>
            <p>${esc(t.destination)} · ${fmtDate(t.startDate)} – ${fmtDate(t.endDate)}</p>
            <div class="invite-code" onclick="App.copyInvite('${t.inviteCode}')" title="Click to copy">
                <i class="bi bi-link-45deg"></i> Invite: ${t.inviteCode}
            </div>`;

        document.querySelectorAll('.tab').forEach(tab => {
            tab.classList.toggle('active', tab.dataset.tab === this.currentTab);
        });
        this.loadTab();
    },

    switchTab(tab) {
        this.currentTab = tab;
        document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
        this.loadTab();
    },

    async loadTab() {
        const content = document.getElementById('tabContent');
        const id = this.currentTrip.id;

        try {
            switch (this.currentTab) {
                case 'overview': await this.renderOverview(content, id); break;
                case 'expenses': await this.renderExpenses(content, id); break;
                case 'itinerary': await this.renderItinerary(content, id); break;
                case 'checklist': await this.renderChecklist(content, id); break;
                case 'memories': await this.renderMemories(content, id); break;
                case 'timeline': await this.renderTimeline(content, id); break;
            }
        } catch (err) { content.innerHTML = `<p class="empty-state">${esc(err.message)}</p>`; }
    },

    async renderOverview(el, tripId) {
        const [analytics, members, settlements] = await Promise.all([
            API.analytics.get(tripId),
            API.trips.members(tripId),
            API.expenses.settlements(tripId)
        ]);

        const warning = analytics.budgetWarning ? '<div class="stat-card" style="border-color:var(--warning)"><div class="label">⚠ Budget Alert</div><div class="value" style="color:var(--warning);font-size:1rem">80%+ spent</div></div>' : '';

        el.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card"><div class="label">Budget</div><div class="value">₹${analytics.budget}</div></div>
                <div class="stat-card"><div class="label">Spent</div><div class="value">₹${analytics.totalSpent}</div></div>
                <div class="stat-card"><div class="label">Remaining</div><div class="value">₹${analytics.remaining}</div></div>
                ${warning}
            </div>
            <h3 style="margin-bottom:.75rem">Who Owes Whom</h3>
            ${settlements.length ? `<ul class="settlement-list">${settlements.map(s =>
                `<li><strong>${esc(s.fromUserName)}</strong> owes <strong>₹${s.amount}</strong> to <strong>${esc(s.toUserName)}</strong></li>`
            ).join('')}</ul>` : '<p class="empty-state">All settled up!</p>'}
            <h3 style="margin:1.5rem 0 .75rem">Members (${members.length})</h3>
            <div class="table-wrap"><table><thead><tr><th>Name</th><th>Role</th><th>Email</th></tr></thead>
            <tbody>${members.map(m => `<tr><td>${esc(m.userName)}</td><td>${m.role}</td><td>${esc(m.userEmail)}</td></tr>`).join('')}</tbody></table></div>`;
    },

    async renderExpenses(el, tripId) {
        const expenses = await API.expenses.list(tripId);
        el.innerHTML = `
            <div class="page-header"><h3>Expenses</h3>
                <button class="btn btn-primary btn-sm" onclick="App.showExpenseModal()"><i class="bi bi-plus"></i> Add Expense</button>
            </div>
            ${expenses.length ? `<div class="table-wrap"><table>
                <thead><tr><th>Title</th><th>Amount</th><th>Category</th><th>Paid By</th><th>Date</th></tr></thead>
                <tbody>${expenses.map(e => `<tr>
                    <td>${esc(e.title)}</td><td>₹${e.amount}</td><td>${e.category}</td>
                    <td>${esc(e.paidByName)}</td><td>${fmtDate(e.expenseDate)}</td>
                </tr>`).join('')}</tbody></table></div>` :
                '<div class="empty-state"><i class="bi bi-receipt"></i><p>No expenses yet</p></div>'}`;
    },

    async renderItinerary(el, tripId) {
        const days = await API.itinerary.list(tripId);
        el.innerHTML = `
            <div class="page-header"><h3>Itinerary</h3>
                <button class="btn btn-primary btn-sm" onclick="App.showItineraryModal()"><i class="bi bi-plus"></i> Add Day</button>
            </div>
            ${days.length ? days.map(d => `
                <div class="stat-card" style="margin-bottom:1rem">
                    <h4>Day ${d.dayNumber}${d.title ? ': ' + esc(d.title) : ''}</h4>
                    ${d.items && d.items.length ? `<ul>${d.items.map(i => `<li>${esc(i.title)}${i.location ? ' @ ' + esc(i.location) : ''}</li>`).join('')}</ul>` : '<p style="color:var(--text-muted)">No activities</p>'}
                </div>`).join('') :
                '<div class="empty-state"><i class="bi bi-map"></i><p>No itinerary yet</p></div>'}`;
    },

    async renderChecklist(el, tripId) {
        const items = await API.checklist.list(tripId);
        el.innerHTML = `
            <div class="page-header"><h3>Packing Checklist</h3>
                <button class="btn btn-primary btn-sm" onclick="App.showChecklistModal()"><i class="bi bi-plus"></i> Add Item</button>
            </div>
            ${items.length ? `<div class="table-wrap"><table>
                <thead><tr><th>Item</th><th>Category</th><th>My Status</th></tr></thead>
                <tbody>${items.map(i => `<tr>
                    <td>${esc(i.itemName)}</td><td>${esc(i.category || '-')}</td>
                    <td>
                        <button class="btn btn-sm btn-outline" onclick="App.assignChecklist(${tripId},${i.id},'BRINGING')">Bringing</button>
                        <button class="btn btn-sm btn-outline" onclick="App.assignChecklist(${tripId},${i.id},'NOT_BRINGING')">Not Bringing</button>
                    </td>
                </tr>`).join('')}</tbody></table></div>` :
                '<div class="empty-state"><i class="bi bi-backpack"></i><p>No checklist items</p></div>'}`;
    },

    async renderMemories(el, tripId) {
        const memories = await API.memories.list(tripId);
        el.innerHTML = `
            <div class="page-header"><h3>Memory Vault</h3>
                <button class="btn btn-primary btn-sm" onclick="App.showMemoryModal()"><i class="bi bi-plus"></i> Add Memory</button>
            </div>
            ${memories.length ? memories.map(m => `
                <div class="stat-card" style="margin-bottom:1rem">
                    <strong>${esc(m.userName)}</strong> · ${fmtDate(m.memoryDate || m.createdAt)}
                    <p>${esc(m.caption || '')}</p>
                    <button class="btn btn-sm btn-outline" onclick="App.likeMemory(${tripId},${m.id})">
                        <i class="bi bi-heart${m.likedByCurrentUser ? '-fill' : ''}"></i> ${m.likeCount}
                    </button>
                </div>`).join('') :
                '<div class="empty-state"><i class="bi bi-camera"></i><p>No memories yet</p></div>'}`;
    },

    async renderTimeline(el, tripId) {
        const logs = await API.trips.timeline(tripId);
        el.innerHTML = logs.length ?
            `<div class="timeline">${logs.map(l => `
                <div class="timeline-item">
                    <div class="time">${fmtDateTime(l.createdAt)}</div>
                    <div>${esc(l.description)}</div>
                </div>`).join('')}</div>` :
            '<div class="empty-state"><i class="bi bi-clock-history"></i><p>No activity yet</p></div>';
    },

    showCreateTripModal() {
        const draft = JSON.parse(localStorage.getItem(this.draftKey) || '{}');
        this.showModal('Create Trip', `
            <form id="createTripForm">
                <div class="form-group"><label>Trip Name</label><input name="name" required value="${esc(draft.name || '')}"></div>
                <div class="form-group"><label>Destination</label><input name="destination" required value="${esc(draft.destination || '')}"></div>
                <div class="form-group"><label>Start Date</label><input type="date" name="startDate" required value="${draft.startDate || ''}"></div>
                <div class="form-group"><label>End Date</label><input type="date" name="endDate" required value="${draft.endDate || ''}"></div>
                <div class="form-group"><label>Budget (₹)</label><input type="number" name="budget" required min="0" value="${draft.budget || ''}"></div>
                <div class="form-group"><label>Max Members</label><input type="number" name="maxMembers" value="${draft.maxMembers || 20}" min="2"></div>
                <div class="form-group"><label>Description</label><textarea name="description">${esc(draft.description || '')}</textarea></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create</button>
                </div>
            </form>`);

        const form = document.getElementById('createTripForm');
        form.addEventListener('input', () => {
            localStorage.setItem(this.draftKey, JSON.stringify(Object.fromEntries(new FormData(form))));
        });
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const data = Object.fromEntries(new FormData(form));
                data.budget = parseFloat(data.budget);
                data.maxMembers = parseInt(data.maxMembers);
                await API.trips.create(data);
                localStorage.removeItem(this.draftKey);
                this.closeModal();
                this.toast('Trip created!', 'success');
                this.loadTrips();
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    showJoinModal() {
        this.showModal('Join Trip', `
            <form id="joinTripForm">
                <div class="form-group"><label>Invite Code</label><input name="inviteCode" required placeholder="e.g. ABCD1234" style="text-transform:uppercase"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Join</button>
                </div>
            </form>`);
        document.getElementById('joinTripForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const code = new FormData(e.target).get('inviteCode');
            try {
                const trip = await API.trips.join(code);
                this.closeModal();
                this.toast('Joined trip!', 'success');
                this.openTrip(trip.id);
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    showExpenseModal() {
        this.showModal('Add Expense', `
            <form id="expenseForm">
                <div class="form-group"><label>Title</label><input name="title" required></div>
                <div class="form-group"><label>Amount (₹)</label><input type="number" name="amount" required min="0" step="0.01"></div>
                <div class="form-group"><label>Category</label>
                    <select name="category" required>
                        ${['FOOD','FUEL','HOTEL','SHOPPING','TICKETS','TOLL','MEDICAL','MISCELLANEOUS'].map(c => `<option value="${c}">${c}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group"><label>Date</label><input type="date" name="expenseDate" required value="${new Date().toISOString().slice(0,10)}"></div>
                <div class="form-group"><label>Notes</label><textarea name="notes"></textarea></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Add</button>
                </div>
            </form>`);
        document.getElementById('expenseForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target));
            data.tripId = this.currentTrip.id;
            data.amount = parseFloat(data.amount);
            try {
                await API.expenses.create(data);
                this.closeModal();
                this.toast('Expense added', 'success');
                this.loadTab();
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    showItineraryModal() {
        this.showModal('Add Itinerary Day', `
            <form id="itineraryForm">
                <div class="form-group"><label>Day Number</label><input type="number" name="dayNumber" required min="1" value="1"></div>
                <div class="form-group"><label>Title</label><input name="title" placeholder="e.g. Arrival Day"></div>
                <div class="form-group"><label>First Activity</label><input name="itemTitle" placeholder="e.g. Leave College"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Add</button>
                </div>
            </form>`);
        document.getElementById('itineraryForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target));
            data.tripId = this.currentTrip.id;
            data.dayNumber = parseInt(data.dayNumber);
            try {
                await API.itinerary.create(data);
                this.closeModal();
                this.toast('Day added', 'success');
                this.loadTab();
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    showChecklistModal() {
        this.showModal('Add Checklist Item', `
            <form id="checklistForm">
                <div class="form-group"><label>Item</label><input name="itemName" required placeholder="e.g. Power Bank"></div>
                <div class="form-group"><label>Category</label><input name="category" placeholder="e.g. Electronics"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Add</button>
                </div>
            </form>`);
        document.getElementById('checklistForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target));
            data.tripId = this.currentTrip.id;
            try {
                await API.checklist.create(data);
                this.closeModal();
                this.toast('Item added', 'success');
                this.loadTab();
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    showMemoryModal() {
        this.showModal('Add Memory', `
            <form id="memoryForm">
                <div class="form-group"><label>Caption</label><textarea name="caption"></textarea></div>
                <div class="form-group"><label>Location</label><input name="location"></div>
                <div class="form-group"><label>Date</label><input type="date" name="memoryDate" value="${new Date().toISOString().slice(0,10)}"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Save</button>
                </div>
            </form>`);
        document.getElementById('memoryForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = Object.fromEntries(new FormData(e.target));
            data.tripId = this.currentTrip.id;
            try {
                await API.memories.create(data);
                this.closeModal();
                this.toast('Memory saved', 'success');
                this.loadTab();
            } catch (err) { this.toast(err.message, 'error'); }
        });
    },

    async assignChecklist(tripId, itemId, status) {
        try {
            await API.checklist.assign({ tripId, itemId, status });
            this.toast('Updated', 'success');
            this.loadTab();
        } catch (err) { this.toast(err.message, 'error'); }
    },

    async likeMemory(tripId, memoryId) {
        try {
            await API.memories.like(tripId, memoryId);
            this.loadTab();
        } catch (err) { this.toast(err.message, 'error'); }
    },

    showModal(title, body) {
        document.getElementById('modal').innerHTML = `<h3>${title}</h3>${body}`;
        document.getElementById('modal').classList.remove('hidden');
        document.getElementById('modalOverlay').classList.remove('hidden');
    },

    closeModal() {
        document.getElementById('modal').classList.add('hidden');
        document.getElementById('modalOverlay').classList.add('hidden');
    },

    copyInvite(code) {
        navigator.clipboard.writeText(code);
        this.toast('Invite code copied!', 'success');
    },

    toast(msg, type = '') {
        const el = document.getElementById('toast');
        el.textContent = msg;
        el.className = 'toast ' + type;
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 3000);
    },

    startNotificationPolling() {
        if (!this.user) return;
        setInterval(async () => {
            try {
                const notes = await API.notifications.list();
                if (notes.length) this.toast(notes[0].message);
            } catch { /* silent */ }
        }, 30000);
    }
};

function esc(str) {
    if (!str) return '';
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

function fmtDate(d) {
    if (!d) return '';
    return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

function fmtDateTime(d) {
    if (!d) return '';
    return new Date(d).toLocaleString('en-IN');
}

function debounce(fn, ms) {
    let timer;
    return function(...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), ms);
    };
}

document.addEventListener('DOMContentLoaded', () => App.init());
