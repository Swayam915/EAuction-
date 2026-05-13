<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-box">
        <div class="auth-logo">
            <div class="logo-big">🔨</div>
            <h1>E-Auction</h1>
            <p>Online Auction Management System</p>
        </div>

        <%-- Success message from registration or logout --%>
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert alert-success">${success}</div>
        <% } %>
        <% if ("1".equals(request.getParameter("logout"))) { %>
            <div class="alert alert-success">You have been signed out successfully.</div>
        <% } %>

        <%-- Error message --%>
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error">${error}</div>
        <% } %>

        <form action="${pageContext.request.contextPath}/login" method="POST" novalidate>
            <div class="form-group">
                <label class="form-label" for="email">Email Address</label>
                <input type="email" id="email" name="email" class="form-control"
                       placeholder="you@example.com"
                       value="${emailValue}"
                       autocomplete="email" required>
            </div>
            <div class="form-group">
                <label class="form-label" for="password">Password</label>
                <div class="input-eye-wrap">
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="Enter password"
                           autocomplete="current-password" required>
                    <button type="button" class="eye-btn" onclick="togglePwd('password')" tabindex="-1" aria-label="Show password">👁</button>
                </div>
            </div>
            <button type="submit" class="btn btn-primary btn-block mt-8">
                Sign In
            </button>
        </form>

        <div class="auth-divider">or</div>

        <a href="${pageContext.request.contextPath}/register" class="btn btn-ghost btn-block">
            Create an Account
        </a>

        <div class="demo-box">
            <p class="demo-label">Demo Accounts</p>
            <p class="demo-row">🔑 <strong>Admin:</strong> admin@eauction.com / admin123</p>
            <p class="demo-row">🔑 <strong>Seller:</strong> seller@eauction.com / seller123</p>
            <p class="demo-row">🔑 <strong>Buyer:</strong> buyer@eauction.com / buyer123</p>
        </div>
    </div>
</div>
<script>
function togglePwd(id) {
    const el = document.getElementById(id);
    el.type = el.type === 'password' ? 'text' : 'password';
}
</script>
</body>
</html>
