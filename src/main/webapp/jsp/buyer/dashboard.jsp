<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.eauction.model.*,com.eauction.model.BidIncrementRule,java.util.*,java.time.format.*" %>
<%
    User currentUser      = (User) session.getAttribute("user");
    List<Product>       activeProducts = (List<Product>)       request.getAttribute("activeProducts");
    List<Bid>           myBids         = (List<Bid>)           request.getAttribute("myBids");
    List<AuctionResult> wonAuctions    = (List<AuctionResult>) request.getAttribute("wonAuctions");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yy, HH:mm");
    String ctxPath = request.getContextPath();

    StringBuilder productIds = new StringBuilder("[");
    if (activeProducts != null) {
        for (int i = 0; i < activeProducts.size(); i++) {
            productIds.append(activeProducts.get(i).getProductId());
            if (i < activeProducts.size()-1) productIds.append(",");
        }
    }
    productIds.append("]");

    String[] CAT_NAMES = {"Electronics","Furniture","Clothing","Vehicles","Art","Books","Sports"};
    String[] CAT_ICONS = {"💻","🛋️","👗","🚗","🎨","📚","⚽"};
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Buyer Dashboard — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrapper">

    <aside class="sidebar">
        <div class="sidebar-logo"><div class="logo-icon">🔨</div><h2>E-Auction</h2><p>Buyer Portal</p></div>
        <nav class="sidebar-nav">
            <p class="nav-label">Browse</p>
            <a class="nav-item active" onclick="switchTab('tab-active',this)"><span class="icon">🏷️</span> Live Auctions</a>
            <a class="nav-item" onclick="switchTab('tab-mybids',this)"><span class="icon">📋</span> My Bids</a>
            <a class="nav-item" onclick="switchTab('tab-won',this)"><span class="icon">🏆</span> Won Auctions</a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/profile" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;margin-bottom:8px;">👤 My Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;">🚪 Logout</a>
        </div>
    </aside>

    <main class="main-content">
        <!-- Topbar with notification bell -->
        <div class="topbar">
            <div class="topbar-title">
                <h1>Live Auctions</h1>
                <p>Browse and bid on active auctions</p>
            </div>
            <div class="topbar-actions">
                <!-- Notification bell -->
                <div class="notif-bell-wrap" id="notifBell">
                    <button class="notif-bell-btn" onclick="toggleNotifPanel()" aria-label="Notifications">
                        🔔<span class="notif-badge" id="notifBadge" style="display:none;"></span>
                    </button>
                    <div class="notif-panel" id="notifPanel">
                        <div class="notif-panel-header">
                            <span>Notifications</span>
                            <button class="notif-mark-all" onclick="markAllNotifRead('<%= ctxPath %>')">Mark all read</button>
                        </div>
                        <div id="notifList"></div>
                    </div>
                </div>
                <div class="topbar-user">
                    <div class="user-avatar"><%= currentUser.getInitial() %></div>
                    <div><div class="user-name"><%= currentUser.getName() %></div><div class="user-role">Buyer</div></div>
                </div>
            </div>
        </div>

        <!-- Search bar (Feature 10) -->
        <form id="searchForm" action="${pageContext.request.contextPath}/search" method="GET"
              class="search-bar-wrap" onsubmit="return submitSearch('searchForm')">
            <input type="text" name="q" class="search-input" placeholder="🔍  Search auctions…">
            <select name="category" class="search-select">
                <option value="">All Categories</option>
                <option value="Electronics">Electronics</option>
                <option value="Furniture">Furniture</option>
                <option value="Clothing">Clothing</option>
                <option value="Vehicles">Vehicles</option>
                <option value="Art">Art &amp; Antiques</option>
                <option value="Books">Books</option>
                <option value="Sports">Sports</option>
                <option value="Other">Other</option>
            </select>
            <select name="sort" class="search-select">
                <option value="endTime">Ending Soon</option>
                <option value="price">Lowest Bid</option>
                <option value="name">Name A–Z</option>
            </select>
            <button type="submit" class="btn btn-primary btn-sm">Search</button>
        </form>

        <!-- Stats -->
        <div class="stats-grid">
            <div class="stat-card"><div class="stat-label">Live Auctions</div><div class="stat-value"><%= activeProducts != null ? activeProducts.size() : 0 %></div><div class="stat-icon">🏷️</div></div>
            <div class="stat-card"><div class="stat-label">My Bids</div><div class="stat-value"><%= myBids != null ? myBids.size() : 0 %></div><div class="stat-icon">📋</div></div>
            <div class="stat-card"><div class="stat-label">Auctions Won</div><div class="stat-value"><%= wonAuctions != null ? wonAuctions.size() : 0 %></div><div class="stat-icon">🏆</div></div>
        </div>

        <!-- TAB: Live Auctions -->
        <div id="tab-active" class="tab-content active">
            <% if (activeProducts == null || activeProducts.isEmpty()) { %>
                <div class="empty-state card" style="margin:24px 32px;border:1px solid #2E2E2E;">
                    <div class="empty-icon">🔍</div><p>No active auctions. Check back soon!</p>
                </div>
            <% } else { %>
            <div class="auctions-grid">
            <% for (Product p : activeProducts) {
                   String endTimeStr = p.getAuctionEndTime() != null ? p.getAuctionEndTime().toString() : "";
                   BidIncrementRule rule = BidIncrementRule.forCurrentBid(p.getCurrentBid());
                   double minBid = p.getCurrentBid() + rule.getMinIncrement();
                   String icon = "📦";
                   for (int ci = 0; ci < CAT_NAMES.length; ci++) { if (CAT_NAMES[ci].equals(p.getCategory())) { icon = CAT_ICONS[ci]; break; } }
            %>
                <div class="auction-card">
                    <% if (p.getImage() != null && !p.getImage().isEmpty()) { %>
                    <div class="auction-card-img" style="padding:0;overflow:hidden;">
                        <img src="${pageContext.request.contextPath}/<%= p.getImage() %>"
                             alt="<%= p.getProductName() %>"
                             style="width:100%;height:100%;object-fit:cover;">
                    </div>
                    <% } else { %>
                    <div class="auction-card-img"><%= icon %></div>
                    <% } %>
                    <div class="auction-card-body">
                        <div class="auction-card-category"><%= p.getCategory() != null ? p.getCategory() : "General" %></div>
                        <div class="auction-card-title"><%= p.getProductName() %></div>
                        <% if (p.getDescription() != null && !p.getDescription().isEmpty()) { %>
                        <div class="auction-card-desc"><%= p.getDescription().length() > 80 ? p.getDescription().substring(0,80)+"…" : p.getDescription() %></div>
                        <% } %>

                        <div class="auction-pricing">
                            <div><div class="price-label">Base</div><div class="price-value">₹<%= String.format("%,.2f", p.getBasePrice()) %></div></div>
                            <div class="price-current"><div class="price-label">Current Bid</div>
                                <div class="price-value" id="currentBid_<%= p.getProductId() %>">₹<%= String.format("%,.2f", p.getCurrentBid()) %></div>
                            </div>
                        </div>

                        <div class="auction-timer">⏱ <span id="timer_<%= p.getProductId() %>"></span></div>

                        <!-- Feature 1 & 2: Preset increment buttons -->
                        <p class="bid-section-label" style="margin-top:10px;">Quick Bid</p>
                        <div class="increment-panel" id="incrementPanel_<%= p.getProductId() %>">
                            <% for (double[] btn : rule.getButtons()) { %>
                            <button class="btn-increment"
                                    onclick="incrementBidAndPlace(<%= p.getProductId() %>, <%= String.format("%.2f", btn[0]) %>, '<%= ctxPath %>')">
                                +₹<%= String.format("%,.0f", btn[0]) %>
                            </button>
                            <% } %>
                        </div>

                        <!-- Manual input -->
                        <div class="bid-input-row" style="margin-top:8px;">
                            <input type="number" id="bidAmount_<%= p.getProductId() %>"
                                   class="form-control" placeholder="Custom ₹"
                                   min="<%= String.format("%.2f", minBid) %>" step="1">
                            <button id="bidBtn_<%= p.getProductId() %>"
                                    class="btn btn-primary"
                                    onclick="placeBid(<%= p.getProductId() %>,'<%= ctxPath %>')">Bid</button>
                        </div>

                        <!-- Feature 3: Auto-bid toggle -->
                        <div style="display:flex;justify-content:space-between;margin-top:8px;gap:8px;">
                            <span class="auto-bid-toggle" style="font-size:.72rem;"
                                  onclick="openModal('autoBidModal_<%= p.getProductId() %>')">🤖 Auto-Bid</span>
                            <a href="${pageContext.request.contextPath}/auction/details?id=<%= p.getProductId() %>"
                               class="btn btn-ghost btn-sm">View Details</a>
                        </div>
                    </div>
                </div>

                <!-- Auto-bid Modal for this product -->
                <div class="modal-overlay" id="autoBidModal_<%= p.getProductId() %>">
                    <div class="modal">
                        <div class="modal-header">
                            <h3>🤖 Auto-Bid — <%= p.getProductName() %></h3>
                            <button class="modal-close" onclick="closeModal('autoBidModal_<%= p.getProductId() %>')">×</button>
                        </div>
                        <div class="modal-body">
                            <div class="info-box" style="margin-bottom:14px;">We'll bid the minimum needed on your behalf, up to your maximum.</div>
                            <div class="form-group">
                                <label class="form-label">Your Maximum Bid (₹)</label>
                                <input type="number" id="autoBidMax_<%= p.getProductId() %>"
                                       class="form-control" placeholder="e.g. <%= String.format("%.0f", p.getCurrentBid()*1.5) %>"
                                       min="<%= String.format("%.2f", minBid) %>" step="1">
                            </div>
                            <div style="display:flex;gap:10px;">
                                <button id="autoBidBtn_<%= p.getProductId() %>"
                                        class="btn btn-primary" style="flex:1;justify-content:center;"
                                        onclick="saveAutoBid(<%= p.getProductId() %>,'<%= ctxPath %>')">Set Auto-Bid</button>
                                <button class="btn btn-ghost" onclick="closeModal('autoBidModal_<%= p.getProductId() %>')">Cancel</button>
                            </div>
                        </div>
                    </div>
                </div>

                <script>startCountdown('<%= endTimeStr %>','timer_<%= p.getProductId() %>',<%= p.getProductId() %>);</script>
            <% } %>
            </div>
            <% } %>
        </div>

        <!-- TAB: My Bids -->
        <div id="tab-mybids" class="tab-content">
            <div class="card">
                <div class="card-header"><h3>My Bid History</h3></div>
                <div class="card-body" style="padding:0;">
                    <% if (myBids == null || myBids.isEmpty()) { %>
                        <div class="empty-state"><div class="empty-icon">📋</div><p>No bids placed yet.</p></div>
                    <% } else { %>
                    <table class="data-table">
                        <thead><tr><th>Product</th><th>Bid Amount</th><th>Time Placed</th></tr></thead>
                        <tbody>
                        <% for (Bid b : myBids) { %>
                        <tr>
                            <td><a href="${pageContext.request.contextPath}/auction/details?id=<%= b.getProductId() %>" class="link-gold"><%= b.getProductName() != null ? b.getProductName() : "Product #"+b.getProductId() %></a></td>
                            <td class="bid-col-lg">₹<%= String.format("%,.2f", b.getBidAmount()) %></td>
                            <td class="muted-sm"><%= b.getBidTime() != null ? b.getBidTime().format(fmt) : "—" %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    <% } %>
                </div>
            </div>
        </div>

        <!-- TAB: Won Auctions -->
        <div id="tab-won" class="tab-content">
            <div class="card">
                <div class="card-header"><h3>Auctions Won 🏆</h3></div>
                <div class="card-body" style="padding:0;">
                    <% if (wonAuctions == null || wonAuctions.isEmpty()) { %>
                        <div class="empty-state"><div class="empty-icon">🏆</div><p>No wins yet — keep bidding!</p></div>
                    <% } else { %>
                    <table class="data-table">
                        <thead><tr><th>Product</th><th>Winning Bid</th><th>Closed On</th></tr></thead>
                        <tbody>
                        <% for (AuctionResult r : wonAuctions) { %>
                        <tr>
                            <td><strong style="color:#2ECC71;">🏆 <%= r.getProductName() %></strong></td>
                            <td class="bid-col-lg">₹<%= String.format("%,.2f", r.getFinalPrice()) %></td>
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
startAutoRefresh(<%= productIds %>, '<%= ctxPath %>');
initNotifications('<%= ctxPath %>');
document.querySelectorAll('.nav-item').forEach(item => item.addEventListener('click', function() {
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    this.classList.add('active');
}));
</script>
</body>
</html>
