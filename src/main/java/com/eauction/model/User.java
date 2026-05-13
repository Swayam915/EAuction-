package com.eauction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a system user (admin / seller / buyer).
 * Implements Serializable so it can be stored safely in an HttpSession.
 *
 * NOTE: The password field is populated only during login/register
 * processing; it is intentionally never exposed through JSP views.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           userId;
    private String        name;
    private String        email;
    private transient String password;   // never serialise the hash
    private String        phone;
    private String        address;
    private String        role;          // "admin" | "seller" | "buyer"
    private LocalDateTime registrationDate;
    private boolean       active;

    public User() {}

    public User(int userId, String name, String email,
                String phone, String address, String role) {
        this.userId  = userId;
        this.name    = name;
        this.email   = email;
        this.phone   = phone;
        this.address = address;
        this.role    = role;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public int getUserId()                        { return userId; }
    public void setUserId(int userId)             { this.userId = userId; }

    public String getName()                       { return name; }
    public void setName(String name)              { this.name = name; }

    public String getEmail()                      { return email; }
    public void setEmail(String email)            { this.email = email; }

    public String getPassword()                   { return password; }
    public void setPassword(String password)      { this.password = password; }

    public String getPhone()                      { return phone; }
    public void setPhone(String phone)            { this.phone = phone; }

    public String getAddress()                    { return address; }
    public void setAddress(String address)        { this.address = address; }

    public String getRole()                       { return role; }
    public void setRole(String role)              { this.role = role; }

    public LocalDateTime getRegistrationDate()    { return registrationDate; }
    public void setRegistrationDate(LocalDateTime d) { this.registrationDate = d; }

    public boolean isActive()                     { return active; }
    public void setActive(boolean active)         { this.active = active; }

    // ── Convenience ───────────────────────────────────────────────────────

    public boolean isAdmin()  { return "admin".equals(role); }
    public boolean isSeller() { return "seller".equals(role); }
    public boolean isBuyer()  { return "buyer".equals(role); }

    /** First letter of name, safe to use as avatar. */
    public char getInitial() {
        return (name != null && !name.isEmpty()) ? Character.toUpperCase(name.charAt(0)) : '?';
    }

    @Override
    public String toString() {
        return "User{id=" + userId + ", name='" + name + "', role='" + role + "'}";
    }
}
