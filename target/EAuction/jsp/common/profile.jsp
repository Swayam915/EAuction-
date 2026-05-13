<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.eauction.model.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String dashboardUrl;
    if (currentUser.isAdmin())        dashboardUrl = request.getContextPath() + "/admin/dashboard";
    else if (currentUser.isSeller())  dashboardUrl = request.getContextPath() + "/seller/dashboard";
    else                              dashboardUrl = request.getContextPath() + "/buyer/dashboard";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Profile — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrapper">
    <!-- Sidebar -->
    <aside class="sidebar">
        <div class="sidebar-logo">
            <div class="logo-icon">🔨</div>
            <h2>E-Auction</h2>
            <p><%= currentUser.getRole().substring(0,1).toUpperCase() + currentUser.getRole().substring(1) %> Portal</p>
        </div>
        <nav class="sidebar-nav">
            <p class="nav-label">Navigation</p>
            <a class="nav-item" href="<%= dashboardUrl %>">
                <span class="icon">◀</span> Back to Dashboard
            </a>
            <a class="nav-item active">
                <span class="icon">👤</span> My Profile
            </a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/logout"
               class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;">
                🚪 Logout
            </a>
        </div>
    </aside>

    <!-- Main -->
    <main class="main-content">
        <div class="topbar">
            <div class="topbar-title">
                <h1>My Profile</h1>
                <p>Update your information and change your password</p>
            </div>
            <div class="topbar-user">
                <div class="user-avatar"><%= currentUser.getInitial() %></div>
                <div>
                    <div class="user-name"><%= currentUser.getName() %></div>
                    <div class="user-role"><%= currentUser.getRole() %></div>
                </div>
            </div>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;">

            <!-- ── Update Profile ─────────────────────────────────── -->
            <div class="card">
                <div class="card-header"><h3>👤 Personal Information</h3></div>
                <div class="card-body">
                    <% if (request.getAttribute("profileSuccess") != null) { %>
                        <div class="alert alert-success">${profileSuccess}</div>
                    <% } %>
                    <% if (request.getAttribute("profileError") != null) { %>
                        <div class="alert alert-error">${profileError}</div>
                    <% } %>

                    <form action="${pageContext.request.contextPath}/profile" method="POST">
                        <input type="hidden" name="action" value="updateProfile">

                        <div class="form-group">
                            <label class="form-label">Full Name *</label>
                            <input type="text" name="name" class="form-control"
                                   value="<%= currentUser.getName() %>"
                                   maxlength="100" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Email Address</label>
                            <input type="email" class="form-control"
                                   value="<%= currentUser.getEmail() %>" disabled>
                            <small style="color:#5A5550;">E-mail cannot be changed.</small>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Phone Number</label>
                            <input type="tel" name="phone" class="form-control"
                                   value="<%= currentUser.getPhone() != null ? currentUser.getPhone() : "" %>"
                                   placeholder="10-digit mobile" maxlength="10">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Address</label>
                            <input type="text" name="address" class="form-control"
                                   value="<%= currentUser.getAddress() != null ? currentUser.getAddress() : "" %>"
                                   placeholder="City, State" maxlength="255">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Role</label>
                            <input type="text" class="form-control"
                                   value="<%= currentUser.getRole().toUpperCase() %>" disabled>
                        </div>
                        <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;">
                            Save Changes
                        </button>
                    </form>
                </div>
            </div>

            <!-- ── Change Password ────────────────────────────────── -->
            <div class="card">
                <div class="card-header"><h3>🔒 Change Password</h3></div>
                <div class="card-body">
                    <% if (request.getAttribute("passwordSuccess") != null) { %>
                        <div class="alert alert-success">${passwordSuccess}</div>
                    <% } %>
                    <% if (request.getAttribute("passwordError") != null) { %>
                        <div class="alert alert-error">${passwordError}</div>
                    <% } %>

                    <form action="${pageContext.request.contextPath}/profile" method="POST"
                          onsubmit="return validatePwdForm()">
                        <input type="hidden" name="action" value="changePassword">

                        <div class="form-group">
                            <label class="form-label">Current Password *</label>
                            <div class="input-eye-wrap">
                                <input type="password" id="currentPwd" name="currentPassword"
                                       class="form-control" required>
                                <button type="button" class="eye-btn"
                                        onclick="togglePwd('currentPwd')" tabindex="-1">👁</button>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">New Password *</label>
                            <div class="input-eye-wrap">
                                <input type="password" id="newPwd" name="newPassword"
                                       class="form-control"
                                       placeholder="Min 8 chars, 1 letter + 1 digit" required>
                                <button type="button" class="eye-btn"
                                        onclick="togglePwd('newPwd')" tabindex="-1">👁</button>
                            </div>
                            <div class="pw-strength-bar" id="pwBar"><div id="pwFill"></div></div>
                            <div id="pwLabel" class="pw-strength-label"></div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Confirm New Password *</label>
                            <div class="input-eye-wrap">
                                <input type="password" id="confirmPwd" name="confirmNewPassword"
                                       class="form-control" required>
                                <button type="button" class="eye-btn"
                                        onclick="togglePwd('confirmPwd')" tabindex="-1">👁</button>
                            </div>
                        </div>

                        <div class="info-box" style="margin-bottom:16px;">
                            <strong>Policy:</strong> minimum 8 characters, at least one letter and one digit.
                        </div>

                        <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;">
                            Change Password
                        </button>
                    </form>
                </div>
            </div>

        </div><!-- /grid -->
    </main>
</div>

<script>
function togglePwd(id) {
    const el = document.getElementById(id);
    el.type = el.type === 'password' ? 'text' : 'password';
}

document.getElementById('newPwd').addEventListener('input', function () {
    const val  = this.value;
    const fill  = document.getElementById('pwFill');
    const label = document.getElementById('pwLabel');
    let score = 0;
    if (val.length >= 8)            score++;
    if (/[A-Z]/.test(val))          score++;
    if (/[0-9]/.test(val))          score++;
    if (/[^A-Za-z0-9]/.test(val))   score++;
    const colors = ['','#e74c3c','#e67e22','#f1c40f','#2ecc71'];
    const labels = ['','Weak','Fair','Good','Strong'];
    fill.style.width      = (score * 25) + '%';
    fill.style.background = colors[score];
    label.textContent     = score > 0 ? labels[score] : '';
    label.style.color     = colors[score];
});

function validatePwdForm() {
    const np = document.getElementById('newPwd').value;
    const cp = document.getElementById('confirmPwd').value;
    if (np.length < 8 || !/[A-Za-z]/.test(np) || !/[0-9]/.test(np)) {
        alert('New password must be at least 8 characters with at least one letter and one digit.');
        return false;
    }
    if (np !== cp) { alert('New passwords do not match.'); return false; }
    return true;
}
</script>
</body>
</html>
