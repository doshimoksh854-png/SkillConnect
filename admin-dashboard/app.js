// ===== SkillConnect Admin Dashboard — app.js =====

let allUsersCache = [];

// ─── Auth & Admin Check ──────────────────────────────────────

function handleLogin() {
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;
    const errorEl = document.getElementById('loginError');
    const btn = document.getElementById('btnLogin');

    if (!email || !password) { errorEl.textContent = 'Please enter email and password.'; return; }

    btn.disabled = true;
    btn.innerHTML = '<span class="material-icons">hourglass_top</span> Signing in...';
    errorEl.textContent = '';

    auth.signInWithEmailAndPassword(email, password)
        .then(cred => checkAdminAccess(cred.user))
        .catch(err => {
            errorEl.textContent = 'Invalid credentials. ' + err.message;
            btn.disabled = false;
            btn.innerHTML = '<span class="material-icons">login</span> Sign In';
        });
}

function checkAdminAccess(user) {
    const errorEl = document.getElementById('loginError');
    const btn = document.getElementById('btnLogin');

    const resetUI = (msg) => {
        errorEl.textContent = msg;
        auth.signOut();
        btn.disabled = false;
        btn.innerHTML = '<span class="material-icons">login</span> Sign In';
    };

    // Check if UID exists in the 'admins' collection
    db.collection('admins').doc(user.uid).get().then(doc => {
        if (doc.exists) {
            showDashboard(user);
        } else {
            // Auto-create first admin if collection is empty
            db.collection('admins').get().then(snap => {
                if (snap.empty) {
                    // First user to log in becomes admin
                    db.collection('admins').doc(user.uid).set({
                        email: user.email,
                        role: 'super_admin',
                        createdAt: Date.now()
                    }).then(() => showDashboard(user))
                      .catch(err => resetUI('Database Write Error: ' + err.message + ' (Check Rules)'));
                } else {
                    resetUI('⛔ Access denied. Not an admin account.');
                }
            }).catch(err => {
                resetUI('Database Read Error: ' + err.message + ' (Please deploy firestore.rules to Firebase Console)');
            });
        }
    }).catch(err => {
        resetUI('Database Access Error: ' + err.message + ' (Please deploy firestore.rules to Firebase Console)');
    });
}

function showDashboard(user) {
    document.getElementById('loginScreen').style.display = 'none';
    document.getElementById('dashboard').style.display = 'flex';
    document.getElementById('adminEmail').textContent = user.email;
    loadOverview();
}

function handleLogout() {
    auth.signOut().then(() => {
        document.getElementById('dashboard').style.display = 'none';
        document.getElementById('loginScreen').style.display = 'flex';
        document.getElementById('loginPassword').value = '';
        document.getElementById('loginError').textContent = '';
        const btn = document.getElementById('btnLogin');
        btn.disabled = false;
        btn.innerHTML = '<span class="material-icons">login</span> Sign In';
    });
}

// Auto-login check
auth.onAuthStateChanged(user => {
    if (user) checkAdminAccess(user);
});

// ─── Navigation ──────────────────────────────────────────────

function switchSection(name, el) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(s => s.style.display = 'none');
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    // Show target
    document.getElementById('sec-' + name).style.display = 'block';
    if (el) el.classList.add('active');
    document.getElementById('pageTitle').textContent = name.charAt(0).toUpperCase() + name.slice(1);

    // Load data
    switch (name) {
        case 'overview': loadOverview(); break;
        case 'users': loadUsers(); break;
        case 'bookings': loadBookings(); break;
        case 'payments': loadPayments(); break;
        case 'jobs': loadJobs(); break;
        case 'reviews': loadReviews(); break;
        case 'disputes': loadDisputes(); break;
        case 'verification': loadVerification(); break;
        case 'updates': /* static content, no load needed */ break;
    }
}

// ─── Overview ────────────────────────────────────────────────

async function loadOverview() {
    try {
        const [usersSnap, bookingsSnap, paymentsSnap, jobsSnap] = await Promise.all([
            db.collection('users').get(),
            db.collection('bookings').get(),
            db.collection('payments').get(),
            db.collection('jobs').get()
        ]);

        document.getElementById('statUsers').textContent = usersSnap.size;
        document.getElementById('statBookings').textContent = bookingsSnap.size;
        document.getElementById('statJobs').textContent = jobsSnap.docs.filter(d => d.data().status === 'open').length;

        let totalRev = 0;
        paymentsSnap.docs.forEach(d => {
            if (d.data().status === 'success') totalRev += (d.data().amount || 0);
        });
        document.getElementById('statPayments').textContent = '₹' + totalRev.toLocaleString();

        // Recent users
        const recentUsersDiv = document.getElementById('recentUsers');
        recentUsersDiv.innerHTML = '';
        usersSnap.docs.slice(0, 5).forEach(d => {
            const u = d.data();
            recentUsersDiv.innerHTML += `
                <div class="mini-item">
                    <span class="name">${esc(u.name || 'N/A')}</span>
                    <span class="meta">${esc(u.role || 'customer')}</span>
                </div>`;
        });
        if (usersSnap.empty) recentUsersDiv.innerHTML = '<div class="mini-item"><span class="meta">No users yet</span></div>';

        // Recent payments
        const recentPayDiv = document.getElementById('recentPayments');
        recentPayDiv.innerHTML = '';
        const sortedPayments = paymentsSnap.docs.sort((a, b) => (b.data().timestamp || 0) - (a.data().timestamp || 0));
        sortedPayments.slice(0, 5).forEach(d => {
            const p = d.data();
            recentPayDiv.innerHTML += `
                <div class="mini-item">
                    <span class="name">₹${(p.amount || 0).toLocaleString()} — ${esc(p.jobTitle || 'Service')}</span>
                    <span class="badge ${p.status === 'success' ? 'badge-success' : 'badge-danger'}">${esc(p.status || 'unknown')}</span>
                </div>`;
        });
        if (paymentsSnap.empty) recentPayDiv.innerHTML = '<div class="mini-item"><span class="meta">No payments yet</span></div>';

    } catch (err) {
        showToast('Error loading overview: ' + err.message);
    }
}

// ─── Users ───────────────────────────────────────────────────

async function loadUsers() {
    const roleFilter = document.getElementById('filterUserRole').value;
    try {
        let query = db.collection('users');
        if (roleFilter !== 'all') query = query.where('role', '==', roleFilter);

        const snap = await query.get();
        allUsersCache = snap.docs.map(d => ({ id: d.id, ...d.data() }));
        renderUsers(allUsersCache);
    } catch (err) {
        showToast('Error loading users: ' + err.message);
    }
}

function renderUsers(users) {
    const tbody = document.getElementById('usersBody');
    tbody.innerHTML = '';
    users.forEach(u => {
        const roleBadge = u.role === 'provider' ? 'badge-purple' : 'badge-info';
        tbody.innerHTML += `<tr>
            <td>${esc(u.name || 'N/A')}</td>
            <td>${esc(u.email || '')}</td>
            <td><span class="badge ${roleBadge}">${esc(u.role || 'customer')}</span></td>
            <td>${esc(u.phone || '-')}</td>
            <td>${u.rating ? '⭐ ' + Number(u.rating).toFixed(1) : '-'}</td>
            <td>
                <button class="btn-sm" onclick="viewUserDetail('${u.id}')">View</button>
                <button class="btn-sm btn-danger" onclick="disableUser('${u.id}', '${esc(u.name)}')">Disable</button>
            </td>
        </tr>`;
    });
    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--text-muted)">No users found</td></tr>';
    }
}

function filterUsersLocal() {
    const q = document.getElementById('searchUsers').value.toLowerCase();
    const filtered = allUsersCache.filter(u => (u.name || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q));
    renderUsers(filtered);
}

function viewUserDetail(userId) {
    const user = allUsersCache.find(u => u.id === userId);
    if (!user) return;
    alert(`User Details:\n\nName: ${user.name || 'N/A'}\nEmail: ${user.email || ''}\nRole: ${user.role || 'customer'}\nPhone: ${user.phone || '-'}\nRating: ${user.rating || 'N/A'}\nReviews: ${user.reviewCount || 0}`);
}

async function disableUser(userId, userName) {
    if (!confirm(`Are you sure you want to disable user "${userName}"?`)) return;
    try {
        await db.collection('users').doc(userId).update({ disabled: true });
        showToast(`User "${userName}" has been disabled.`);
        loadUsers();
    } catch (err) {
        showToast('Error: ' + err.message);
    }
}

// ─── Bookings ────────────────────────────────────────────────

async function loadBookings() {
    const statusFilter = document.getElementById('filterBookingStatus').value;
    try {
        let query = db.collection('bookings');
        if (statusFilter !== 'all') query = query.where('status', '==', statusFilter);

        const snap = await query.get();
        const tbody = document.getElementById('bookingsBody');
        tbody.innerHTML = '';

        snap.docs.forEach(d => {
            const b = d.data();
            const statusClass = getStatusBadge(b.status);
            const payClass = b.paymentStatus === 'paid' ? 'badge-success' : 'badge-warning';
            tbody.innerHTML += `<tr>
                <td>${esc(b.skillTitle || 'Service')}</td>
                <td>${esc(b.userName || b.userId || '-')}</td>
                <td>${esc(b.providerName || b.providerId || '-')}</td>
                <td>₹${(b.price || 0).toLocaleString()}</td>
                <td><span class="badge ${statusClass}">${esc(b.status || 'unknown')}</span></td>
                <td><span class="badge ${payClass}">${esc(b.paymentStatus || 'unpaid')}</span></td>
                <td>${formatDate(b.bookingDate)}</td>
            </tr>`;
        });

        if (snap.empty) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted)">No bookings found</td></tr>';
        }
    } catch (err) {
        showToast('Error loading bookings: ' + err.message);
    }
}

// ─── Payments ────────────────────────────────────────────────

async function loadPayments() {
    try {
        const snap = await db.collection('payments').orderBy('timestamp', 'desc').get();
        const tbody = document.getElementById('paymentsBody');
        tbody.innerHTML = '';

        let successCount = 0, failCount = 0, totalAmt = 0;

        snap.docs.forEach(d => {
            const p = d.data();
            const statusClass = p.status === 'success' ? 'badge-success' : 'badge-danger';
            if (p.status === 'success') { successCount++; totalAmt += (p.amount || 0); }
            else { failCount++; }

            tbody.innerHTML += `<tr>
                <td>${esc(p.jobTitle || 'Service')}</td>
                <td>${esc(p.customerName || '-')}</td>
                <td>${esc(p.providerName || '-')}</td>
                <td>₹${(p.amount || 0).toLocaleString()}</td>
                <td><span class="badge ${statusClass}">${esc(p.status || 'unknown')}</span></td>
                <td style="font-size:12px;color:var(--text-muted)">${esc(p.razorpayPaymentId || '-')}</td>
                <td>${formatDate(p.timestamp)}</td>
            </tr>`;
        });

        document.getElementById('paySuccess').textContent = successCount;
        document.getElementById('payFailed').textContent = failCount;
        document.getElementById('payTotal').textContent = '₹' + totalAmt.toLocaleString();

        if (snap.empty) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted)">No payments yet</td></tr>';
        }
    } catch (err) {
        showToast('Error loading payments: ' + err.message);
    }
}

// ─── Jobs ────────────────────────────────────────────────────

async function loadJobs() {
    const statusFilter = document.getElementById('filterJobStatus').value;
    try {
        let query = db.collection('jobs');
        if (statusFilter !== 'all') query = query.where('status', '==', statusFilter);

        const snap = await query.get();
        const tbody = document.getElementById('jobsBody');
        tbody.innerHTML = '';

        for (const d of snap.docs) {
            const j = d.data();
            const statusClass = getStatusBadge(j.status);

            // Count bids for this job
            const bidsSnap = await db.collection('bids').where('jobId', '==', d.id).get();

            tbody.innerHTML += `<tr>
                <td>${esc(j.title || 'Untitled')}</td>
                <td>${esc(j.customerName || '-')}</td>
                <td>${esc(j.category || '-')}</td>
                <td>₹${(j.budget || 0).toLocaleString()}</td>
                <td><span class="badge ${statusClass}">${esc(j.status || 'unknown')}</span></td>
                <td>${bidsSnap.size}</td>
                <td><button class="btn-sm btn-danger" onclick="deleteJob('${d.id}', '${esc(j.title)}')">Remove</button></td>
            </tr>`;
        }

        if (snap.empty) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted)">No jobs found</td></tr>';
        }
    } catch (err) {
        showToast('Error loading jobs: ' + err.message);
    }
}

async function deleteJob(jobId, title) {
    if (!confirm(`Delete job "${title}"? This cannot be undone.`)) return;
    try {
        await db.collection('jobs').doc(jobId).delete();
        showToast(`Job "${title}" deleted.`);
        loadJobs();
    } catch (err) {
        showToast('Error: ' + err.message);
    }
}

// ─── Reviews ─────────────────────────────────────────────────

async function loadReviews() {
    try {
        const snap = await db.collection('reviews').get();
        const tbody = document.getElementById('reviewsBody');
        tbody.innerHTML = '';

        snap.docs.forEach(d => {
            const r = d.data();
            const stars = '★'.repeat(Math.round(r.rating || 0)) + '☆'.repeat(5 - Math.round(r.rating || 0));
            tbody.innerHTML += `<tr>
                <td>${esc(r.reviewerName || '-')}</td>
                <td>${esc(r.skillTitle || '-')}</td>
                <td><span class="stars">${stars}</span> (${Number(r.rating || 0).toFixed(1)})</td>
                <td>${esc(r.comment || '-')}</td>
                <td>${formatDate(r.timestamp)}</td>
                <td><button class="btn-sm btn-danger" onclick="deleteReview('${d.id}')">Remove</button></td>
            </tr>`;
        });

        if (snap.empty) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--text-muted)">No reviews yet</td></tr>';
        }
    } catch (err) {
        showToast('Error loading reviews: ' + err.message);
    }
}

async function deleteReview(reviewId) {
    if (!confirm('Remove this review? This cannot be undone.')) return;
    try {
        await db.collection('reviews').doc(reviewId).delete();
        showToast('Review removed.');
        loadReviews();
    } catch (err) {
        showToast('Error: ' + err.message);
    }
}

// ─── Disputes ─────────────────────────────────────────────────

async function loadDisputes() {
    const statusFilter = document.getElementById('filterDisputeStatus').value;
    try {
        let query = db.collection('disputes');
        if (statusFilter !== 'all') query = query.where('status', '==', statusFilter);

        const snap = await query.get();
        const tbody = document.getElementById('disputesBody');
        tbody.innerHTML = '';

        snap.docs.forEach(d => {
            const r = d.data();
            const statusClass = getStatusBadge(r.status);
            tbody.innerHTML += `<tr>
                <td>${esc(r.reporterName || '-')} <span class="badge badge-info">${esc(r.reporterRole || '')}</span></td>
                <td>${esc(r.againstName || '-')}</td>
                <td>${esc(r.reason || '-')}</td>
                <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis">${esc(r.description || '-')}</td>
                <td><span class="badge ${statusClass}">${esc(r.status || 'open')}</span></td>
                <td>${formatDate(r.timestamp)}</td>
                <td>
                    ${r.status === 'open' ? `
                        <button class="btn-sm" onclick="updateDispute('${d.id}','under_review')">Review</button>
                        <button class="btn-sm btn-danger" onclick="updateDispute('${d.id}','dismissed')">Dismiss</button>
                    ` : r.status === 'under_review' ? `
                        <button class="btn-sm" onclick="updateDispute('${d.id}','resolved')">Resolve</button>
                        <button class="btn-sm btn-danger" onclick="updateDispute('${d.id}','dismissed')">Dismiss</button>
                    ` : '<span style="color:var(--text-muted)">—</span>'}
                </td>
            </tr>`;
        });

        if (snap.empty) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted)">No disputes found</td></tr>';
        }
    } catch (err) {
        showToast('Error loading disputes: ' + err.message);
    }
}

async function updateDispute(disputeId, newStatus) {
    try {
        await db.collection('disputes').doc(disputeId).update({ status: newStatus });
        showToast(`Dispute ${newStatus}.`);
        loadDisputes();
    } catch (err) {
        showToast('Error: ' + err.message);
    }
}

// ─── Verification Requests ────────────────────────────────────

async function loadVerification() {
    try {
        const snap = await db.collection('verification_requests').get();
        const tbody = document.getElementById('verificationBody');
        tbody.innerHTML = '';

        snap.docs.forEach(d => {
            const v = d.data();
            const statusClass = getStatusBadge(v.status);
            tbody.innerHTML += `<tr>
                <td>${esc(v.email || '-')}</td>
                <td>${esc(v.legalName || '-')}</td>
                <td>${esc(v.idNumber || '-')}</td>
                <td>${esc(v.experience || '-')} yrs</td>
                <td><span class="badge ${statusClass}">${esc(v.status || 'pending')}</span></td>
                <td>${formatDate(v.timestamp)}</td>
                <td>
                    ${v.status === 'pending' ? `
                        <button class="btn-sm" onclick="approveVerification('${d.id}','${v.providerId}')">Approve</button>
                        <button class="btn-sm btn-danger" onclick="rejectVerification('${d.id}','${v.providerId}')">Reject</button>
                    ` : '<span style="color:var(--text-muted)">—</span>'}
                </td>
            </tr>`;
        });

        if (snap.empty) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted)">No verification requests yet</td></tr>';
        }
    } catch (err) {
        showToast('Error loading verification requests: ' + err.message);
    }
}

async function approveVerification(docId, providerId) {
    try {
        await db.collection('verification_requests').doc(docId).update({ status: 'approved' });
        await db.collection('users').doc(providerId).update({ verificationStatus: 'verified' });
        showToast('Provider verified! ✅');
        loadVerification();
    } catch (err) {
        showToast('Error: ' + err.message);
    }
}

async function rejectVerification(docId, providerId) {
    try {
        await db.collection('verification_requests').doc(docId).update({ status: 'rejected' });
        await db.collection('users').doc(providerId).update({ verificationStatus: 'rejected' });
        showToast('Verification rejected.');
        loadVerification();
    } catch (err) {
        showToast('Error: ' + err.message);
    }
}

// ─── Helpers ─────────────────────────────────────────────────

function esc(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function formatDate(ts) {
    if (!ts) return '-';
    const d = new Date(typeof ts === 'number' ? ts : ts.seconds * 1000);
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

function getStatusBadge(status) {
    switch ((status || '').toLowerCase()) {
        case 'completed': case 'success': case 'paid': case 'accepted': return 'badge-success';
        case 'pending': case 'open': case 'unpaid': return 'badge-warning';
        case 'cancelled': case 'failed': case 'rejected': return 'badge-danger';
        case 'awarded': case 'in_progress': return 'badge-info';
        default: return 'badge-purple';
    }
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// Enter key login
document.addEventListener('keydown', e => {
    if (e.key === 'Enter' && document.getElementById('loginScreen').style.display !== 'none') {
        handleLogin();
    }
});
