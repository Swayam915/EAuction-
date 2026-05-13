<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.eauction.model.*,java.util.*,java.time.format.*" %>
<%
    User currentUser   = (User) session.getAttribute("user");
    List<User>          allUsers    = (List<User>)          request.getAttribute("allUsers");
    List<Product>       allProducts = (List<Product>)       request.getAttribute("allProducts");
    List<AuctionResult> allResults  = (List<AuctionResult>) request.getAttribute("allResults");
    int    totalUsers    = (Integer) request.getAttribute("totalUsers");
    int    activeAuctions= (Integer) request.getAttribute("activeAuctions");
    double totalRevenue  = (Double)  request.getAttribute("totalRevenue");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yy, HH:mm");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrapper">

    <!-- ── Sidebar ───────────────────────────────────────────────────── -->
    <aside class="sidebar">
        <div class="sidebar-logo">
            <div class="logo-icon">🔨</div>
            <h2>E-Auction</h2>
            <p>Admin Panel</p>
        </div>
        <nav class="sidebar-nav">
            <p class="nav-label">Overview</p>
            <a class="nav-item active" id="navOverview" onclick="switchTab('tab-overview',this)">
                <span class="icon">📊</span> Dashboard
            </a>
            <p class="nav-label">Management</p>
            <a class="nav-item" id="navUsers" onclick="switchTab('tab-users',this)">
                <span class="icon">👥</span> Users
            </a>
            <a class="nav-item" id="navAuctions" onclick="switchTab('tab-auctions',this)">
                <span class="icon">🏷️</span> Auctions
            </a>
            <a class="nav-item" id="navResults" onclick="switchTab('tab-results',this)">
                <span class="icon">🏆</span> Results
            </a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/profile"
               class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;margin-bottom:8px;">
                👤 My Profile
            </a>
            <a href="${pageContext.request.contextPath}/logout"
               class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;">
                🚪 Logout
            </a>
        </div>
    </aside>

    <!-- ── Main content ───────────────────────────────────────────────── -->
    <main class="main-content">
        <div class="topbar">
            <div class="topbar-title">
                <h1>Admin Dashboard</h1>
                <p>System overview and management</p>
            </div>
            <div class="topbar-user">
                <div class="user-avatar"><%= currentUser.getInitial() %></div>
                <div>
                    <div class="user-name"><%= currentUser.getName() %></div>
                    <div class="user-role">Administrator</div>
                </div>
            </div>
        </div>

        <!-- ── TAB: Overview ──────────────────────────────────────────── -->
        <div id="tab-overview" class="tab-content active">
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-label">Total Users</div>
                    <div class="stat-value"><%= totalUsers %></div>
                    <div class="stat-icon">👥</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Active Auctions</div>
                    <div class="stat-value"><%= activeAuctions %></div>
                    <div class="stat-icon">🏷️</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Total Revenue</div>
                    <div class="stat-value">₹<%= String.format("%,.0f", totalRevenue) %></div>
                    <div class="stat-icon">💰</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Auctions Closed</div>
                    <div class="stat-value"><%= allResults != null ? allResults.size() : 0 %></div>
                    <div class="stat-icon">🏆</div>
                </div>
            </div>

            <div class="card">
                <div class="card-header"><h3>Recent Auction Activity</h3></div>
                <div class="card-body" style="padding:0;">
                    <% if (allProducts == null || allProducts.isEmpty()) { %>
                        <div class="empty-state"><div class="empty-icon">📭</div><p>No auctions yet.</p></div>
                    <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr><th>Product</th><th>Seller</th><th>Base</th><th>Current Bid</th><th>Status</th><th>Ends</th></tr>
                        </thead>
                        <tbody>
                        <% int cnt = 0; for (Product p : allProducts) { if (cnt++ >= 8) break; %>
                        <tr>
                            <td><strong><%= p.getProductName() %></strong><br>
                                <small style="color:#5A5550"><%= p.getCategory() %></small></td>
                            <td><%= p.getSellerName() %></td>
                            <td>₹<%= String.format("%,.2f", p.getBasePrice()) %></td>
                            <td class="bid-col">₹<%= String.format("%,.2f", p.getCurrentBid()) %></td>
                            <td><span class="badge badge-<%= p.getStatus() %>"><%= p.getStatus() %></span></td>
                            <td class="muted-sm"><%= p.getAuctionEndTime() != null ? p.getAuctionEndTime().format(fmt) : "—" %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    <% } %>
                </div>
            </div>
        </div>

        <!-- ── TAB: Users ─────────────────────────────────────────────── -->
        <div id="tab-users" class="tab-content">
            <div class="card">
                <div class="card-header">
                    <h3>All Users (<%= allUsers != null ? allUsers.size() : 0 %>)</h3>
                </div>
                <div class="card-body" style="padding:0;">
                    <% if (allUsers == null || allUsers.isEmpty()) { %>
                        <div class="empty-state"><div class="empty-icon">👥</div><p>No users found.</p></div>
                    <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr><th>#</th><th>Name</th><th>Email</th><th>Role</th><th>Phone</th><th>Status</th><th>Action</th></tr>
                        </thead>
                        <tbody>
                        <% for (User u : allUsers) {
                               if (u.isAdmin()) continue;
                               String actionTxt = u.isActive() ? "Disable" : "Enable";
                               String actionClass = u.isActive() ? "btn-danger" : "btn-success"; %>
                        <tr>
                            <td class="muted-sm"><%= u.getUserId() %></td>
                            <td><strong><%= u.getName() %></strong></td>
                            <td class="muted-sm"><%= u.getEmail() %></td>
                            <td><span class="badge badge-<%= u.getRole() %>"><%= u.getRole() %></span></td>
                            <td class="muted-sm"><%= u.getPhone() != null ? u.getPhone() : "—" %></td>
                            <td>
                                <% if (u.isActive()) { %>
                                    <span class="badge badge-active">Active</span>
                                <% } else { %>
                                    <span class="badge badge-cancelled">Inactive</span>
                                <% } %>
                            </td>
                            <td>
                                <form action="${pageContext.request.contextPath}/admin/action"
                                      method="POST" style="display:inline;"
                                      onsubmit="return confirm('<%= actionTxt %> this user?')">
                                    <input type="hidden" name="action" value="toggleUser">
                                    <input type="hidden" name="id"     value="<%= u.getUserId() %>">
                                    <button type="submit"
                                            class="btn btn-sm <%= actionClass %>">
                                        <%= actionTxt %>
                                    </button>
                                </form>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    <% } %>
                </div>
            </div>
        </div>

        <!-- ── TAB: Auctions ──────────────────────────────────────────── -->
        <div id="tab-auctions" class="tab-content">
            <div class="card">
                <div class="card-header">
                    <h3>All Auctions (<%= allProducts != null ? allProducts.size() : 0 %>)</h3>
                </div>
                <div class="card-body" style="padding:0;">
                    <% if (allProducts == null || allProducts.isEmpty()) { %>
                        <div class="empty-state"><div class="empty-icon">🏷️</div><p>No auctions found.</p></div>
                    <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr><th>#</th><th>Product</th><th>Seller</th><th>Base</th><th>Current Bid</th><th>Status</th><th>End Time</th><th>Actions</th></tr>
                        </thead>
                        <tbody>
                        <% for (Product p : allProducts) { %>
                        <tr>
                            <td class="muted-sm"><%= p.getProductId() %></td>
                            <td><strong><%= p.getProductName() %></strong><br>
                                <small style="color:#5A5550"><%= p.getCategory() %></small></td>
                            <td><%= p.getSellerName() %></td>
                            <td>₹<%= String.format("%,.2f", p.getBasePrice()) %></td>
                            <td class="bid-col">₹<%= String.format("%,.2f", p.getCurrentBid()) %></td>
                            <td><span class="badge badge-<%= p.getStatus() %>"><%= p.getStatus() %></span></td>
                            <td class="muted-sm"><%= p.getAuctionEndTime() != null ? p.getAuctionEndTime().format(fmt) : "—" %></td>
                            <td>
                                <% if ("active".equals(p.getStatus())) { %>
                                <form action="${pageContext.request.contextPath}/admin/action"
                                      method="POST" style="display:inline;"
                                      onsubmit="return confirm('Close this auction?')">
                                    <input type="hidden" name="action" value="closeAuction">
                                    <input type="hidden" name="id"     value="<%= p.getProductId() %>">
                                    <button class="btn btn-sm btn-danger">Close</button>
                                </form>
                                <form action="${pageContext.request.contextPath}/admin/action"
                                      method="POST" style="display:inline;">
                                    <input type="hidden" name="action" value="startSimulation">
                                    <input type="hidden" name="id"     value="<%= p.getProductId() %>">
                                    <button class="btn btn-sm" style="background:#E67E22;border-color:#E67E22;color:#FFF;">Simulate Bids</button>
                                </form>
                                <form action="${pageContext.request.contextPath}/admin/action"
                                      method="POST" style="display:inline;">
                                    <input type="hidden" name="action" value="stopSimulation">
                                    <input type="hidden" name="id"     value="<%= p.getProductId() %>">
                                    <button class="btn btn-sm" style="background:#E74C3C;border-color:#E74C3C;color:#FFF;">Stop Sim</button>
                                </form>
                                <% } else if ("pending".equals(p.getStatus())) { %>
                                <form action="${pageContext.request.contextPath}/admin/action"
                                      method="POST" style="display:inline;">
                                    <input type="hidden" name="action" value="activateAuction">
                                    <input type="hidden" name="id"     value="<%= p.getProductId() %>">
                                    <button class="btn btn-sm btn-success">Activate</button>
                                </form>
                                <form action="${pageContext.request.contextPath}/admin/action"
                                      method="POST" style="display:inline;"
                                      onsubmit="return confirm('Cancel this auction?')">
                                    <input type="hidden" name="action" value="cancelAuction">
                                    <input type="hidden" name="id"     value="<%= p.getProductId() %>">
                                    <button class="btn btn-sm btn-ghost">Cancel</button>
                                </form>
                                <% } else { %>
                                    <span class="muted-sm">—</span>
                                <% } %>
                                <a href="${pageContext.request.contextPath}/auction/details?id=<%= p.getProductId() %>"
                                   class="btn btn-sm btn-info">View</a>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    <% } %>
                </div>
            </div>
        </div>

        <!-- ── TAB: Results ───────────────────────────────────────────── -->
        <div id="tab-results" class="tab-content">
            <div class="card">
                <div class="card-header"><h3>Completed Auctions &amp; Results</h3></div>
                <div class="card-body" style="padding:0;">
                    <% if (allResults == null || allResults.isEmpty()) { %>
                        <div class="empty-state"><div class="empty-icon">🏆</div><p>No completed auctions yet.</p></div>
                    <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr><th>#</th><th>Product</th><th>Winner</th><th>Final Price</th><th>Closed On</th></tr>
                        </thead>
                        <tbody>
                        <% for (AuctionResult r : allResults) { %>
                        <tr>
                            <td class="muted-sm"><%= r.getResultId() %></td>
                            <td><strong><%= r.getProductName() %></strong></td>
                            <td>
                                <% if (r.getWinnerName() != null) { %>
                                    🏆 <%= r.getWinnerName() %>
                                <% } else { %>
                                    <span class="muted-sm">No bids</span>
                                <% } %>
                            </td>
                            <td class="bid-col">
                                <% if (r.getFinalPrice() > 0) { %>₹<%= String.format("%,.2f", r.getFinalPrice()) %><% } else { %>—<% } %>
                            </td>
                            <td class="muted-sm"><%= r.getResultDate() != null ? r.getResultDate().format(fmt) : "—" %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    <% } %>
                </div>
            </div>
        </div>

    </main>
</div>

<script src="${pageContext.request.contextPath}/js/main.js"></script>
<script>
// Keep sidebar nav item in sync with active tab
document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', function () {
        document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
        this.classList.add('active');
    });
});
// Open to the correct tab if hash is present
const hash = window.location.hash;
if (hash === '#users')    { switchTab('tab-users',    document.getElementById('navUsers')); }
if (hash === '#auctions') { switchTab('tab-auctions', document.getElementById('navAuctions')); }
if (hash === '#results')  { switchTab('tab-results',  document.getElementById('navResults')); }
</script>
</body>
</html>
