import React, { useState, useEffect, useRef } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { User, LogOut, Settings, Shield, Building2, ChevronDown, Menu, X } from "lucide-react";
import { applyTheme } from "../utils/theme";
import "../styles/HomeNavbar.css";

function Navbar() {
  const [profileOpen, setProfileOpen] = useState(false);
  const [hotelDropdownOpen, setHotelDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("");

  const profileRef = useRef();
  const hotelRef = useRef();
  const navigate = useNavigate();
  const location = useLocation();

  // Initialize theme on mount
  useEffect(() => {
    applyTheme("dark-75");
  }, []);

  // Detect login status from localStorage
  useEffect(() => {
    const userEmail = localStorage.getItem("email");
    const userRole = localStorage.getItem("role");
    if (userEmail) {
      setEmail(userEmail);
      setRole((userRole || "USER").toUpperCase());
    } else {
      setEmail("");
      setRole("");
    }
  }, [location.pathname]);

  // Handle outside clicks to close dropdowns
  useEffect(() => {
    const handler = (e) => {
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setProfileOpen(false);
      }
      if (hotelRef.current && !hotelRef.current.contains(e.target)) {
        setHotelDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  // Close mobile menu on route change & handle scroll lock
  useEffect(() => {
    setMobileMenuOpen(false);
    setProfileOpen(false);
    setHotelDropdownOpen(false);
  }, [location.pathname]);

  // Lock background scrolling when mobile menu drawer is open
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.classList.add("menu-open-scroll-lock");
    } else {
      document.body.classList.remove("menu-open-scroll-lock");
    }
    return () => {
      document.body.classList.remove("menu-open-scroll-lock");
    };
  }, [mobileMenuOpen]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("hotelToken");
    localStorage.removeItem("adminToken");
    localStorage.removeItem("email");
    localStorage.removeItem("role");
    setEmail("");
    setRole("");
    setProfileOpen(false);
    navigate("/");
    window.location.reload();
  };

  const getDashboardPath = () => {
    if (role === "ADMIN") return "/admin";
    if (role === "HOTEL") return "/hotel-login-dashboard";
    return "/dashboard";
  };

  const isActive = (path) => location.pathname === path;

  return (
    <>
      <nav className="nav">
      <div className="nav-container">
        {/* LOGO & PRIMARY LINKS */}
        <div className="nav-left">
          <Link to="/" className="logo" aria-label="worldtours.com Home">
            <div className="app-logo-wrapper">
              <div className="app-logo-border-sweep"></div>
              <img
                src="/assets/logo-badge.png"
                alt="worldtours.com Logo"
                className="app-logo-img"
              />
            </div>
            <div className="brand-text-block">
              <span className="logo-text">worldtours.com</span>
              <span className="ngt-secret-badge" title="NextGem-Technology">
                NG-T
              </span>
            </div>
          </Link>
          <div className={`header-links ${mobileMenuOpen ? "mobile-active" : ""}`}>
            <Link to="/view-hotels" className={isActive("/view-hotels") ? "active" : ""}>
              View Hotels
            </Link>
            <Link to="/about-us" className={isActive("/about-us") ? "active" : ""}>
              About Us
            </Link>
            <Link to="/contact-us" className={isActive("/contact-us") ? "active" : ""}>
              Contact Us
            </Link>
            <Link to="/complaint" className={`nav-secondary ${isActive("/complaint") ? "active" : ""}`}>
              Complaints
            </Link>
            <Link to="/feedback" className={`nav-secondary ${isActive("/feedback") ? "active" : ""}`}>
              Feedback
            </Link>

            {/* MOBILE ONLY AUTH SECTION */}
            {mobileMenuOpen && !email && (
              <div className="mobile-auth-section">
                <div className="mobile-auth-group">
                  <span className="mobile-group-title">Traveler Account</span>
                  <div className="mobile-button-row">
                    <Link to="/login" className="btn-user-login">User Login</Link>
                    <Link to="/register" className="btn-user-register">Sign Up</Link>
                  </div>
                </div>
                <div className="mobile-auth-group">
                  <span className="mobile-group-title">Hotel Partner</span>
                  <div className="mobile-button-row">
                    <Link to="/hotel-login" className="btn-hotel-portal">Hotel Login</Link>
                    <Link to="/hotel-register" className="btn-hotel-register">Register Hotel</Link>
                  </div>
                </div>
                <div className="mobile-auth-group">
                  <span className="mobile-group-title">Administration</span>
                  <Link to="/admin-login" className="btn-admin-portal">Admin Login</Link>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* RIGHT SECTION: AUTHENTICATION OPTIONS */}
        <div className="nav-right">
          {email ? (
            /* LOGGED IN PROFILE DROPDOWN */
            <div className="dropdown" ref={profileRef}>
              <button
                className="profile-btn"
                onClick={() => setProfileOpen(!profileOpen)}
                aria-expanded={profileOpen}
              >
                <div className="profile-avatar">
                  <User size={16} />
                </div>
                <div className="profile-info">
                  <span className="profile-email">{email}</span>
                  <span className={`role-badge role-${role.toLowerCase()}`}>{role}</span>
                </div>
                <ChevronDown size={14} className={`dropdown-arrow ${profileOpen ? "open" : ""}`} />
              </button>

              {profileOpen && (
                <div className="dropdown-menu">
                  <div className="dropdown-header">
                    <span className="dropdown-header-label">Signed in as</span>
                    <strong className="dropdown-header-email">{email}</strong>
                    <span className="dropdown-header-role">Role: {role}</span>
                  </div>
                  <Link to={getDashboardPath()} className="dropdown-item">
                    <Settings size={15} /> My Dashboard
                  </Link>
                  <button onClick={handleLogout} className="dropdown-item logout-btn">
                    <LogOut size={15} /> Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            /* AUTHENTICATION OPTIONS (USER, HOTEL, ADMIN) */
            <div className="auth-nav-group">
              {/* 1. HOTEL PARTNER DROPDOWN */}
              <div className="dropdown" ref={hotelRef}>
                <button
                  className="hotel-partner-btn"
                  onClick={() => setHotelDropdownOpen(!hotelDropdownOpen)}
                  aria-expanded={hotelDropdownOpen}
                >
                  <Building2 size={15} />
                  <span>Hotel Partner</span>
                  <ChevronDown size={13} className={`dropdown-arrow ${hotelDropdownOpen ? "open" : ""}`} />
                </button>

                {hotelDropdownOpen && (
                  <div className="dropdown-menu hotel-menu">
                    <div className="dropdown-header">
                      <span className="dropdown-header-label">Hotel Management</span>
                    </div>
                    <Link to="/hotel-login" className="dropdown-item">
                      Hotel Login
                    </Link>
                    <Link to="/hotel-register" className="dropdown-item">
                      Register Hotel
                    </Link>
                  </div>
                )}
              </div>

              {/* 2. ADMIN LOGIN */}
              <Link to="/admin-login" className="admin-nav-btn" title="Restricted Admin Access">
                <Shield size={14} />
                <span>Admin</span>
              </Link>

              <div className="nav-divider"></div>

              {/* 3. USER LOGIN & SIGN UP */}
              <Link to="/login" className="user-login-btn">
                Log In
              </Link>
              <Link to="/register" className="user-signup-btn">
                Sign Up
              </Link>
            </div>
          )}

          {/* MOBILE HAMBURGER TOGGLE */}
          <button
            className="mobile-menu-toggle"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle navigation menu"
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>
    </nav>
    <div className="nav-spacer" aria-hidden="true" />
  </>
);
}

export default Navbar;
