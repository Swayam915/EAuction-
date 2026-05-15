# 🔨 Online E-Auction Management System

A full-stack Java Servlet web application for real-time online auctions supporting buyers, sellers, and administrators with role-based dashboards, live bidding, and notifications.

---

## ✨ Features

- Role-based dashboards for Admin, Seller, and Buyer
- Real-time bidding with bid history
- Auto-bid (proxy bidding) support
- Product listings with image uploads and categories
- Search auctions by keyword or category
- In-app notifications for outbid alerts
- Admin panel to manage users, auctions, and bids
- Secure authentication using BCrypt hashing
- Automatic auction closing system
- Database auto-setup on startup

---

## 🛠 Tech Stack

| Layer         | Technology                   |
| ------------- | ---------------------------- |
| Language      | Java 17                      |
| Framework     | Java Servlets, JSP, JSTL     |
| Build Tool    | Apache Maven                 |
| Database      | H2                           |
| Security      | BCrypt                       |
| Server        | Apache Tomcat                |
| Frontend      | JSP, CSS, JavaScript         |

---

## 📁 Project Structure

```bash
eauction/
├── pom.xml
├── src/main/
│   ├── java/com/eauction/
│   └── webapp/
├── sql/
└── .gitignore
