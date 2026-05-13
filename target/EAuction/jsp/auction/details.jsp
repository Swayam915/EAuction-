<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.eauction.model.*,com.eauction.model.BidIncrementRule,java.util.*,java.time.format.*" %>
<%
    User    currentUser = (User)    session.getAttribute("user");
    Product product     = (Product) request.getAttribute("product");
    List<Bid> bids      = (List<Bid>) request.getAttribute("bids");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
    String ctxPath    = request.getContextPath();
    String endTimeStr = product.getAuctionEndTime() != null ? product.getAuctionEndTime().toString() : "";

    // Increment rules for the initial page render
    BidIncrementRule rule = BidIncrementRule.forCurrentBid(product.getCurrentBid());
    double minBid = product.getCurrentBid() + rule.getMinIncrement();

    String dashboardUrl = currentUser.isAdmin()  ? ctxPath + "/admin/dashboard"
                        : currentUser.isSeller() ? ctxPath + "/seller/dashboard"
                        : ctxPath + "/buyer/dashboard";
    boolean canBid = currentUser.isBuyer() && "active".equals(product.getStatus()) && product.isLive();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= product.getProductName() %> — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrapper">

    <!-- Sidebar -->
    <aside class="sidebar">
        <div class="sidebar-logo"><div class="logo-icon">🔨</div><h2>E-Auction</h2></div>
        <nav class="sidebar-nav">
            <a class="nav-item" href="<%= dashboardUrl %>"><span class="icon">◀</span> Back to Dashboard</a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/profile" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;margin-bottom:8px;">👤 My Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;">🚪 Logout</a>
        </div>
    </aside>

    <main class="main-content">
        <div class="topbar">
            <div class="topbar-title">
                <h1><%= product.getProductName() %></h1>
                <p><%= product.getCategory() %> · by <%= product.getSellerName() %></p>
            </div>
            <div class="topbar-actions">
                <span class="badge badge-<%= product.getStatus() %>" style="font-size:.9rem;padding:6px 16px;"><%= product.getStatus().toUpperCase() %></span>
                <!-- Notification bell (buyers only) -->
                <% if (currentUser.isBuyer()) { %>
                <div class="notif-bell-wrap" id="notifBell">
                    <button class="notif-bell-btn" onclick="toggleNotifPanel()" aria-label="Notifications">🔔
                        <span class="notif-badge" id="notifBadge" style="display:none;"></span>
                    </button>
                    <div class="notif-panel" id="notifPanel">
                        <div class="notif-panel-header">
                            <span>Notifications</span>
                            <button class="notif-mark-all" onclick="markAllNotifRead('<%= ctxPath %>')">Mark all read</button>
                        </div>
                        <div id="notifList"></div>
                    </div>
                </div>
                <% } %>
            </div>
        </div>

        <div class="details-grid">

            <!-- Product info + bid panel -->
            <div>
                <div class="card">
                    <div class="card-header"><h3>Auction Details</h3></div>
                    <div class="card-body">

                        <!-- Product image -->
                        <% if (product.getImage() != null && !product.getImage().isEmpty()) { %>
                        <img src="${pageContext.request.contextPath}/<%= product.getImage() %>"
                             alt="<%= product.getProductName() %>"
                             style="width:100%;max-height:220px;object-fit:cover;border-radius:6px;border:1px solid #2E2E2E;margin-bottom:16px;">
                        <% } %>

                        <p style="color:#8A8580;margin-bottom:20px;line-height:1.7;">
                            <%= product.getDescription() != null && !product.getDescription().isEmpty()
                                ? product.getDescription() : "No description provided." %>
                        </p>

                        <div class="detail-rows">
                            <div class="detail-row">
                                <span class="detail-label">Base Price</span>
                                <span class="detail-value gold">₹<%= String.format("%,.2f", product.getBasePrice()) %></span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Current Highest Bid</span>
                                <span class="detail-value gold lg" id="currentBid_<%= product.getProductId() %>">
                                    ₹<%= String.format("%,.2f", product.getCurrentBid()) %>
                                </span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Minimum Next Bid</span>
                                <span class="detail-value" id="minBidDisplay_<%= product.getProductId() %>">
                                    ₹<%= String.format("%,.2f", minBid) %>
                                </span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Auction Ends</span>
                                <span class="detail-value" style="color:#E67E22;">
                                    <%= product.getAuctionEndTime() != null ? product.getAuctionEndTime().format(fmt) : "—" %>
                                </span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Time Remaining</span>
                                <span id="timer_detail"></span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Total Bids</span>
                                <span class="detail-value"><%= bids != null ? bids.size() : 0 %></span>
                            </div>
                        </div>

                        <!-- ── BID PANEL (buyers only, live auctions) ── -->
                        <% if (canBid) { %>
                        <div class="bid-panel" style="margin-top:20px;">
                            <p class="bid-panel-label">Quick Bid — click an amount to bid instantly</p>

                            <!-- Feature 1 & 2: Preset increment buttons -->
                            <div class="increment-panel" id="incrementPanel_<%= product.getProductId() %>">
                                <% for (double[] btn : rule.getButtons()) {
                                       String label = "+₹" + String.format("%,.0f", btn[0]); %>
                                <button class="btn-increment"
                                        onclick="incrementBidAndPlace(<%= product.getProductId() %>, <%= String.format("%.2f", btn[0]) %>, '<%= ctxPath %>')">
                                    <%= label %> <small style="opacity:.6;font-weight:400;">(includes +₹<%= String.format("%,.0f", btn[0]) %>)</small>
                                </button>
                                <% } %>
                            </div>

                            <!-- Manual input row -->
                            <p class="bid-section-label" style="margin-top:12px;">Or enter custom amount</p>
                            <div class="bid-input-row">
                                <input type="number"
                                       id="bidAmount_<%= product.getProductId() %>"
                                       class="form-control"
                                       placeholder="₹<%= String.format("%,.0f", minBid) %> or more"
                                       min="<%= String.format("%.2f", minBid) %>"
                                       step="1">
                                <button id="bidBtn_<%= product.getProductId() %>"
                                        class="btn btn-primary"
                                        onclick="placeBid(<%= product.getProductId() %>,'<%= ctxPath %>')">
                                    Bid Now
                                </button>
                            </div>
                            <p class="bid-min-hint">Minimum bid: ₹<%= String.format("%,.2f", minBid) %></p>

                            <!-- Feature 3: Auto-bid toggle -->
                            <span class="auto-bid-toggle" onclick="openModal('autoBidModal_<%= product.getProductId() %>')">
                                🤖 Set up Auto-Bid (Proxy Bid)
                            </span>
                        </div>

                        <% } else if (currentUser.isBuyer() && !"active".equals(product.getStatus())) { %>
                        <div class="info-box" style="margin-top:20px;">
                            This auction is <strong><%= product.getStatus() %></strong> and no longer accepts bids.
                        </div>
                        <% } %>
                    </div>
                </div>
            </div>

            <!-- Bid history -->
            <div>
                <div class="card">
                    <div class="card-header"><h3>Bid History (<%= bids != null ? bids.size() : 0 %> bids)</h3></div>
                    <div class="card-body" style="padding:0 24px;">
                        <% if (bids == null || bids.isEmpty()) { %>
                            <div class="empty-state" style="padding:32px 0;"><div class="empty-icon">💬</div><p>No bids placed yet. Be the first!</p></div>
                        <% } else { boolean first = true; for (Bid b : bids) { %>
                        <div class="bid-item">
                            <div>
                                <div style="font-weight:600;font-size:.88rem;">
                                    <% if (first) { %><span class="bid-winner">🏆 Leading — </span><% first = false; } %>
                                    <%= b.getBuyerName() %>
                                </div>
                                <div class="muted-sm"><%= b.getBidTime() != null ? b.getBidTime().format(fmt) : "" %></div>
                            </div>
                            <div class="bid-amount">₹<%= String.format("%,.2f", b.getBidAmount()) %></div>
                        </div>
                        <% } } %>
                    </div>
                </div>
            </div>

        </div><!-- /details-grid -->
    </main>
</div>

<!-- ── Auto-bid Modal (Feature 3) ───────────────────────── -->
<% if (canBid) { %>
<div class="modal-overlay" id="autoBidModal_<%= product.getProductId() %>">
    <div class="modal">
        <div class="modal-header">
            <h3>🤖 Set Auto-Bid</h3>
            <button class="modal-close" onclick="closeModal('autoBidModal_<%= product.getProductId() %>')">×</button>
        </div>
        <div class="modal-body">
            <div class="info-box" style="margin-bottom:16px;">
                Auto-bidding lets the system bid on your behalf up to your maximum.
                We'll always bid the <em>minimum required</em> amount — not your maximum — to keep you in the lead.
            </div>
            <div class="form-group">
                <label class="form-label">Your Maximum Bid (₹)</label>
                <input type="number" id="autoBidMax_<%= product.getProductId() %>"
                       class="form-control"
                       placeholder="e.g. <%= String.format("%.0f", product.getCurrentBid() * 1.5) %>"
                       min="<%= String.format("%.2f", minBid) %>" step="1">
                <small style="color:#5A5550;margin-top:4px;display:block;">
                    Current bid: ₹<%= String.format("%,.2f", product.getCurrentBid()) %> — your max must be higher.
                </small>
            </div>
            <div style="display:flex;gap:10px;margin-top:4px;">
                <button id="autoBidBtn_<%= product.getProductId() %>"
                        class="btn btn-primary" style="flex:1;justify-content:center;"
                        onclick="saveAutoBid(<%= product.getProductId() %>,'<%= ctxPath %>')">
                    Set Auto-Bid
                </button>
                <button class="btn btn-ghost" onclick="closeModal('autoBidModal_<%= product.getProductId() %>')">Cancel</button>
            </div>
        </div>
    </div>
</div>
<% } %>

<script src="${pageContext.request.contextPath}/js/main.js"></script>
<script>
    startCountdown('<%= endTimeStr %>', 'timer_detail', <%= product.getProductId() %>);
    <% if (canBid) { %>
    initializeBidState(<%= product.getProductId() %>, <%= product.getCurrentBid() %>);
    setInterval(() => refreshBidStatus(<%= product.getProductId() %>, '<%= ctxPath %>'), 8000);
    <% } %>
    <% if (currentUser.isBuyer()) { %>
    initNotifications('<%= ctxPath %>');
    <% } %>
</script>
</body>
</html>
