<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-box auth-box-wide">
        <div class="auth-logo">
            <div class="logo-big">🔨</div>
            <h1>Create Account</h1>
            <p>Join the E-Auction Platform</p>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error">${error}</div>
        <% } %>

        <form action="${pageContext.request.contextPath}/register" method="POST" novalidate
              onsubmit="return validateRegForm()">

            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="name">Full Name *</label>
                    <input type="text" id="name" name="name" class="form-control"
                           placeholder="John Doe"
                           value="${nameValue}"
                           maxlength="100" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="email">Email Address *</label>
                    <input type="email" id="email" name="email" class="form-control"
                           placeholder="you@example.com"
                           value="${emailValue}"
                           autocomplete="email" required>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="password">Password *</label>
                    <div class="input-eye-wrap">
                        <input type="password" id="password" name="password" class="form-control"
                               placeholder="Min 8 chars, 1 letter + 1 digit"
                               autocomplete="new-password" required>
                        <button type="button" class="eye-btn" onclick="togglePwd('password')" tabindex="-1">👁</button>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="confirmPassword">Confirm Password *</label>
                    <div class="input-eye-wrap">
                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-control"
                               placeholder="Repeat password"
                               autocomplete="new-password" required>
                        <button type="button" class="eye-btn" onclick="togglePwd('confirmPassword')" tabindex="-1">👁</button>
                    </div>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="phone">Phone Number <span class="optional">(optional)</span></label>
                    <input type="tel" id="phone" name="phone" class="form-control"
                           placeholder="10-digit mobile"
                           value="${phoneValue}"
                           maxlength="10">
                </div>
                <div class="form-group">
                    <label class="form-label" for="role">Register As *</label>
                    <select id="role" name="role" class="form-control" required>
                        <option value="">-- Select Role --</option>
                        <option value="buyer"  <%= "buyer".equals(request.getAttribute("roleValue"))  ? "selected" : "" %>>Buyer</option>
                        <option value="seller" <%= "seller".equals(request.getAttribute("roleValue")) ? "selected" : "" %>>Seller</option>
                    </select>
                </div>
            </div>

            <div class="form-group">
                <label class="form-label" for="address">Address <span class="optional">(optional)</span></label>
                <input type="text" id="address" name="address" class="form-control"
                       placeholder="City, State"
                       value="${addressValue}"
                       maxlength="255">
            </div>

            <!-- Password strength indicator -->
            <div class="pw-strength-bar" id="pwStrengthBar">
                <div id="pwStrengthFill"></div>
            </div>
            <div id="pwStrengthLabel" class="pw-strength-label"></div>

            <button type="submit" class="btn btn-primary btn-block mt-8">
                Create Account
            </button>
        </form>

        <div class="auth-divider">already have an account?</div>
        <a href="${pageContext.request.contextPath}/login" class="btn btn-ghost btn-block">
            Sign In
        </a>
    </div>
</div>

<script>
function togglePwd(id) {
    const el = document.getElementById(id);
    el.type = el.type === 'password' ? 'text' : 'password';
}

// Password strength meter
document.getElementById('password').addEventListener('input', function () {
    const val = this.value;
    const fill  = document.getElementById('pwStrengthFill');
    const label = document.getElementById('pwStrengthLabel');
    let score = 0;
    if (val.length >= 8)                          score++;
    if (/[A-Z]/.test(val))                        score++;
    if (/[0-9]/.test(val))                        score++;
    if (/[^A-Za-z0-9]/.test(val))                 score++;

    const levels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    const colors = ['', '#e74c3c', '#e67e22', '#f1c40f', '#2ecc71'];
    fill.style.width  = (score * 25) + '%';
    fill.style.background = colors[score];
    label.textContent = score > 0 ? levels[score] : '';
    label.style.color = colors[score];
});

function validateRegForm() {
    const pwd  = document.getElementById('password').value;
    const cpwd = document.getElementById('confirmPassword').value;
    if (pwd.length < 8) {
        alert('Password must be at least 8 characters.');
        return false;
    }
    if (!/[A-Za-z]/.test(pwd) || !/[0-9]/.test(pwd)) {
        alert('Password must contain at least one letter and one digit.');
        return false;
    }
    if (pwd !== cpwd) {
        alert('Passwords do not match.');
        return false;
    }
    return true;
}
</script>
</body>
</html>
