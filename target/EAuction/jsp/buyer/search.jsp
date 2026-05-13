<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.eauction.model.*,com.eauction.model.BidIncrementRule,java.util.*,java.time.format.*" %>
<%
    User currentUser      = (User) session.getAttribute("user");
    List<Product> results = (List<Product>) request.getAttribute("results");
    String keyword        = (String) request.getAttribute("searchKeyword");
    String category       = (String) request.getAttribute("searchCategory");
    String sort           = (String) request.getAttribute("searchSort");
    if (keyword  == null) keyword  = "";
    if (category == null) category = "";
    if (sort     == null) sort     = "endTime";
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yy, HH:mm");
    String ctxPath = request.getContextPath();

    String[] CAT_NAMES = {"Electronics","Furniture","Clothing","Vehicles","Art","Books","Sports"};
    String[] CAT_ICONS = {"💻","🛋️","👗","🚗","🎨","📚","⚽"};
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Search Results — E-Auction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-wrapper">

    <aside class="sidebar">
        <div class="sidebar-logo"><div class="logo-icon">🔨</div><h2>E-Auction</h2><p>Buyer Portal</p></div>
        <nav class="sidebar-nav">
            <a class="nav-item" href="${pageContext.request.contextPath}/buyer/dashboard"><span class="icon">◀</span> Back to Auctions</a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/profile" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;margin-bottom:8px;">👤 My Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn btn-ghost btn-sm" style="width:100%;justify-content:center;">🚪 Logout</a>
        </div>
    </aside>

    <main class="main-content">
        <div class="topbar">
            <div class="topbar-title">
                <h1>Search Results</h1>
                <p>Active auctions matching your query</p>
            </div>
            <div class="topbar-user">
                <div class="user-avatar"><%= currentUser.getInitial() %></div>
                <div><div class="user-name"><%= currentUser.getName() %></div><div class="user-role">Buyer</div></div>
            </div>
        </div>

        <!-- Refined search bar -->
        <form id="searchForm" action="${pageContext.request.contextPath}/search" method="GET"
              class="search-bar-wrap" onsubmit="return submitSearch('searchForm')">
            <input type="text" name="q" class="search-input"
                   value="<%= keyword %>" placeholder="🔍 Search auctions…">
            <select name="category" class="search-select">
                <option value="" <%= category.isEmpty() ? "selected":"" %>>All Categories</option>
                <% String[] cats = {"Electronics","Furniture","Clothing","Vehicles","Art","Books","Sports","Other"};
                   for (String c : cats) { %>
                <option value="<%= c %>" <%= c.equals(category) ? "selected":"" %>><%= c %></option>
                <% } %>
            </select>
            <select name="sort" class="search-select">
                <option value="endTime" <%= "endTime".equals(sort)?"selected":"" %>>Ending Soon</option>
                <option value="price"   <%= "price".equals(sort)?"selected":"" %>>Lowest Bid</option>
                <option value="name"    <%= "name".equals(sort)?"selected":"" %>>Name A–Z</option>
            </select>
            <button type="submit" class="btn btn-primary btn-sm">Search</button>
            <a href="${pageContext.request.contextPath}/buyer/dashboard" class="btn btn-ghost btn-sm">Clear</a>
        </form>

        <!-- Results summary -->
        <div class="search-results-header">
            <% if (!keyword.isEmpty() || !category.isEmpty()) { %>
                Found <span class="search-highlight"><%= results != null ? results.size() : 0 %></span> result(s)
                <% if (!keyword.isEmpty()) { %> for "<span class="search-highlight"><%= keyword %></span>"<% } %>
                <% if (!category.isEmpty()) { %> in <span class="search-highlight"><%= category %></span><% } %>
            <% } else { %>
                Showing all active auctions (<span class="search-highlight"><%= results != null ? results.size() : 0 %></span>)
            <% } %>
        </div>

        <!-- Results grid -->
        <% if (results == null || results.isEmpty()) { %>
            <div class="empty-state card" style="margin:24px 32px;border:1px solid #2E2E2E;">
                <div class="empty-icon">🔍</div>
                <p>No auctions found. Try a different search term or category.</p>
                <a href="${pageContext.request.contextPath}/buyer/dashboard" class="btn btn-primary" style="margin-top:16px;">Browse All Auctions</a>
            </div>
        <% } else { %>
        <div class="auctions-grid">
        <% for (Product p : results) {
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
                         alt="<%= p.getProductName() %>" style="width:100%;height:100%;object-fit:cover;">
                </div>
                <% } else { %>
                <div class="auction-card-img"><%= icon %></div>
                <% } %>
                <div class="auction-card-body">
                    <div class="auction-card-category"><%= p.getCategory() %></div>
                    <div class="auction-card-title"><%= p.getProductName() %></div>
                    <% if (p.getDescription() != null && !p.getDescription().isEmpty()) { %>
                    <div class="auction-card-desc"><%= p.getDescription().length()>80?p.getDescription().substring(0,80)+"…":p.getDescription() %></div>
                    <% } %>

                    <div class="auction-pricing">
                        <div><div class="price-label">Base</div><div class="price-value">₹<%= String.format("%,.2f",p.getBasePrice()) %></div></div>
                        <div class="price-current"><div class="price-label">Current Bid</div>
                            <div class="price-value" id="currentBid_<%= p.getProductId() %>">₹<%= String.format("%,.2f",p.getCurrentBid()) %></div>
                        </div>
                    </div>

                    <div class="auction-timer">⏱ <span id="timer_<%= p.getProductId() %>"></span></div>

                    <!-- Increment buttons -->
                    <div class="increment-panel" id="incrementPanel_<%= p.getProductId() %>">
                        <% for (double[] btn : rule.getButtons()) { %>
                        <button class="btn-increment"
                                onclick="incrementBidAndPlace(<%= p.getProductId() %>, <%= String.format("%.2f",btn[0]) %>, '<%= ctxPath %>')">
                            +₹<%= String.format("%,.0f",btn[0]) %>
                        </button>
                        <% } %>
                    </div>

                    <div class="bid-input-row" style="margin-top:8px;">
                        <input type="number" id="bidAmount_<%= p.getProductId() %>"
                               class="form-control" placeholder="Custom ₹"
                               min="<%= String.format("%.2f",minBid) %>" step="1">
                        <button id="bidBtn_<%= p.getProductId() %>" class="btn btn-primary"
                                onclick="placeBid(<%= p.getProductId() %>,'<%= ctxPath %>')">Bid</button>
                    </div>

                    <div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px;gap:8px;">
                        <span class="auto-bid-toggle" style="font-size:.72rem;"
                              onclick="openModal('autoBidModal_<%= p.getProductId() %>')">🤖 Auto-Bid</span>
                        <a href="${pageContext.request.contextPath}/auction/details?id=<%= p.getProductId() %>"
                           class="btn btn-ghost btn-sm">Details</a>
                    </div>
                </div>
            </div>

            <!-- Auto-bid modal -->
            <div class="modal-overlay" id="autoBidModal_<%= p.getProductId() %>">
                <div class="modal">
                    <div class="modal-header">
                        <h3>🤖 Auto-Bid — <%= p.getProductName() %></h3>
                        <button class="modal-close" onclick="closeModal('autoBidModal_<%= p.getProductId() %>')">×</button>
                    </div>
                    <div class="modal-body">
                        <div class="info-box" style="margin-bottom:14px;">Set your max. We'll bid the minimum needed on your behalf.</div>
                        <div class="form-group">
                            <label class="form-label">Maximum Bid (₹)</label>
                            <input type="number" id="autoBidMax_<%= p.getProductId() %>" class="form-control"
                                   placeholder="e.g. <%= String.format("%.0f",p.getCurrentBid()*1.5) %>"
                                   min="<%= String.format("%.2f",minBid) %>" step="1">
                        </div>
                        <div style="display:flex;gap:10px;">
                            <button id="autoBidBtn_<%= p.getProductId() %>" class="btn btn-primary" style="flex:1;justify-content:center;"
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

    </main>
</div>
<script src="${pageContext.request.contextPath}/js/main.js"></script>
<script>
    <% if (results != null && !results.isEmpty()) {
           StringBuilder ids = new StringBuilder("[");
           for (int i = 0; i < results.size(); i++) {
               ids.append(results.get(i).getProductId());
               if (i < results.size()-1) ids.append(",");
           }
           ids.append("]"); %>
    startAutoRefresh(<%= ids %>, '<%= ctxPath %>');
    <% } %>
    initNotifications('<%= ctxPath %>');
</script>
</body>
</html>
