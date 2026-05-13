<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.eauction.model.*,java.util.*,java.time.format.*,java.time.LocalDateTime" %>
<%
    User currentUser = (User) session.getAttribute("user");
    List<Product> products = (List<Product>) request.getAttribute("products");
    DateTimeFormatter fmt      = DateTimeFormatter.ofPattern("dd MMM yy, HH:mm");
    DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    String minDateTime = LocalDateTime.now().format(inputFmt);
    long activeCount = 0;
    long closedCount = 0;
    if (products != null) {
        for (Product p : products) {
            if (p != null && "active".equalsIgnoreCase(p.getStatus())) activeCount++;
            if (p != null && "closed".equalsIgnoreCase(p.getStatus())) closedCount++;
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seller Dashboard — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrapper">

    <aside class="sidebar">
        <div class="sidebar-logo"><div class="logo-icon">🔨</div><h2>E-Auction</h2><p>Seller Portal</p></div>
        <nav class="sidebar-nav">
            <p class="nav-label">Menu</p>
            <a class="nav-item active"><span class="icon">📦</span> My Products</a>
            <a class="nav-item" onclick="openModal('addProductModal')"><span class="icon">➕</span> List New Product</a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/profile" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;margin-bottom:8px;">👤 My Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;">🚪 Logout</a>
        </div>
    </aside>

    <main class="main-content">
        <div class="topbar">
            <div class="topbar-title"><h1>Seller Dashboard</h1><p>Manage your auction listings</p></div>
            <div class="topbar-user">
                <div class="user-avatar"><%= currentUser.getInitial() %></div>
                <div><div class="user-name"><%= currentUser.getName() %></div><div class="user-role">Seller</div></div>
            </div>
        </div>

        <% if (request.getAttribute("error") != null) { %><div class="alert alert-error" style="margin:16px 32px 0;">${error}</div><% } %>
        <% if (request.getAttribute("success") != null) { %><div class="alert alert-success" style="margin:16px 32px 0;">${success}</div><% } %>

        <div class="stats-grid">
            <div class="stat-card"><div class="stat-label">Total Listed</div><div class="stat-value"><%= products!=null?products.size():0 %></div><div class="stat-icon">📦</div></div>
            <div class="stat-card"><div class="stat-label">Active Now</div><div class="stat-value"><%= activeCount %></div><div class="stat-icon">🟢</div></div>
            <div class="stat-card"><div class="stat-label">Closed</div><div class="stat-value"><%= closedCount %></div><div class="stat-icon">✅</div></div>
        </div>

        <div class="card">
            <div class="card-header">
                <h3>My Auction Listings</h3>
                <button class="btn btn-primary btn-sm" onclick="openModal('addProductModal')">+ List New Product</button>
            </div>
            <div class="card-body" style="padding:0;">
                <% if (products == null || products.isEmpty()) { %>
                    <div class="empty-state">
                        <div class="empty-icon">📦</div><p>No products listed yet.</p>
                        <button class="btn btn-primary" style="margin-top:16px;" onclick="openModal('addProductModal')">List Your First Product</button>
                    </div>
                <% } else { %>
                <table class="data-table">
                    <thead><tr><th>Product</th><th>Category</th><th>Base</th><th>Current Bid</th><th>End Time</th><th>Status</th><th>Action</th></tr></thead>
                    <tbody>
                    <% for (Product p : products) { %>
                    <tr>
                        <td>
                            <div style="display:flex;align-items:center;gap:10px;">
                            <% if (p.getImage()!=null&&!p.getImage().isEmpty()) { %>
                            <img src="${pageContext.request.contextPath}/<%= p.getImage() %>"
                                 style="width:36px;height:36px;object-fit:cover;border-radius:4px;border:1px solid #2E2E2E;" alt="">
                            <% } %>
                            <div>
                                <strong><%= p.getProductName() %></strong>
                                <% if (p.getDescription()!=null&&!p.getDescription().isEmpty()) { %>
                                <br><small class="muted-sm"><%= p.getDescription().length()>50?p.getDescription().substring(0,50)+"…":p.getDescription() %></small>
                                <% } %>
                            </div>
                            </div>
                        </td>
                        <td class="muted-sm"><%= p.getCategory()!=null?p.getCategory():"—" %></td>
                        <td>₹<%= String.format("%,.2f",p.getBasePrice()) %></td>
                        <td class="bid-col">₹<%= String.format("%,.2f",p.getCurrentBid()) %></td>
                        <td class="muted-sm"><%= p.getAuctionEndTime()!=null?p.getAuctionEndTime().format(fmt):"—" %></td>
                        <td><span class="badge badge-<%= p.getStatus() %>"><%= p.getStatus() %></span></td>
                        <td><a href="${pageContext.request.contextPath}/auction/details?id=<%= p.getProductId() %>" class="btn btn-sm btn-info">View Bids</a></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
                <% } %>
            </div>
        </div>
    </main>
</div>

<!-- Add Product Modal (with image upload) -->
<div class="modal-overlay" id="addProductModal">
    <div class="modal">
        <div class="modal-header">
            <h3>List New Product for Auction</h3>
            <button class="modal-close" onclick="closeModal('addProductModal')">×</button>
        </div>
        <div class="modal-body">
            <form action="${pageContext.request.contextPath}/seller/dashboard" method="POST"
                  enctype="multipart/form-data" onsubmit="return validateProductForm()">
                <input type="hidden" name="action" value="addProduct">

                <div class="form-group">
                    <label class="form-label">Product Name *</label>
                    <input type="text" name="productName" class="form-control" placeholder="e.g. Samsung Galaxy S24" maxlength="200" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Description</label>
                    <textarea name="description" class="form-control" rows="3" placeholder="Condition, features…" maxlength="2000"></textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Starting Price (₹) *</label>
                        <input type="number" id="basePrice" name="basePrice" class="form-control" min="1" step="0.01" placeholder="5000" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Category</label>
                        <select name="category" class="form-control">
                            <option value="Electronics">Electronics</option>
                            <option value="Furniture">Furniture</option>
                            <option value="Clothing">Clothing</option>
                            <option value="Vehicles">Vehicles</option>
                            <option value="Art">Art &amp; Antiques</option>
                            <option value="Books">Books</option>
                            <option value="Sports">Sports</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>
                </div>

                <!-- Feature 10: Image upload -->
                <div class="form-group">
                    <label class="form-label">Product Image <span class="optional">(optional, max 5 MB)</span></label>
                    <input type="file" name="productImage" class="form-control"
                           accept="image/jpeg,image/png,image/webp"
                           onchange="previewImage(this)">
                    <img id="imagePreview" class="image-preview-box" alt="Preview" src="">
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Auction Start *</label>
                        <input type="datetime-local" id="auctionStartTime" name="auctionStartTime" class="form-control" min="<%= minDateTime %>" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Auction End *</label>
                        <input type="datetime-local" id="auctionEndTime" name="auctionEndTime" class="form-control" min="<%= minDateTime %>" required>
                    </div>
                </div>
                <div style="display:flex;gap:12px;margin-top:8px;">
                    <button type="submit" class="btn btn-primary" style="flex:1;justify-content:center;">List for Auction</button>
                    <button type="button" class="btn btn-ghost" onclick="closeModal('addProductModal')">Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/main.js"></script>
<% if (request.getAttribute("error") != null) { %><script>openModal('addProductModal');</script><% } %>
</body>
</html>
