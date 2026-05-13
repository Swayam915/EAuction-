<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%
    Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    String  errMsg     = (String)  request.getAttribute("javax.servlet.error.message");
    if (errMsg == null || errMsg.isBlank()) {
        errMsg = (statusCode != null && statusCode == 404)
            ? "The page you are looking for does not exist."
            : "Something went wrong. Please try again.";
    }
    String icon = (statusCode != null && statusCode == 404) ? "🔍" : "⚠️";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error <%= statusCode != null ? statusCode : "" %> — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-box" style="text-align:center;">
        <div style="font-size:4rem;margin-bottom:16px;"><%= icon %></div>
        <% if (statusCode != null) { %>
            <h1 style="color:#C9A84C;margin-bottom:6px;"><%= statusCode %></h1>
        <% } %>
        <p style="color:#8A8580;margin-bottom:28px;line-height:1.6;">
            <%= errMsg %>
        </p>
        <div style="display:flex;gap:12px;justify-content:center;flex-wrap:wrap;">
            <a href="javascript:history.back()" class="btn btn-ghost">← Go Back</a>
            <a href="${pageContext.request.contextPath}/login" class="btn btn-primary">Go to Home</a>
        </div>
    </div>
</div>
</body>
</html>
