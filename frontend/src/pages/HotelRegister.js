import React, { useState } from "react";
import { registerHotel } from "../api/authApi";
import { useNavigate, Link } from "react-router-dom";
import { Eye, EyeOff, AlertCircle, User } from "lucide-react";
import "../styles/Login.css";

function HotelRegister() {
  const [formData, setFormData] = useState({
    hotel: "",
    email: "",
    password: "",
    city: "",
    address: "",
    price: "",
    roomAvailable: "",
    location: ""
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const errs = {};

    if (!formData.hotel.trim()) {
      errs.hotel = "Hotel name is required";
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const disposableDomains = ["mailinator.com", "tempmail.com", "temp-mail.org", "10minutemail.com", "guerrillamail.com", "sharklasers.com", "yopmail.com", "trashmail.com"];
    const domain = formData.email.trim().toLowerCase().split("@")[1] || "";

    if (!formData.email.trim()) {
      errs.email = "Email is required";
    } else if (!emailRegex.test(formData.email.trim()) || disposableDomains.includes(domain)) {
      errs.email = "Please enter a valid genuine email address (e.g. Gmail, Yahoo, Outlook, iCloud)";
    }

    if (!formData.password) {
      errs.password = "Password is required";
    } else if (formData.password.length < 6) {
      errs.password = "Password must be at least 6 characters";
    }

    if (!formData.city.trim()) {
      errs.city = "City is required";
    }

    if (!formData.address.trim()) {
      errs.address = "Street address is required";
    }

    if (!formData.location.trim()) {
      errs.location = "Landmark / Location is required";
    }

    if (!formData.price || isNaN(Number(formData.price)) || Number(formData.price) <= 0) {
      errs.price = "Enter a valid positive price per night";
    }

    if (!formData.roomAvailable || isNaN(Number(formData.roomAvailable)) || Number(formData.roomAvailable) < 1) {
      errs.roomAvailable = "Enter at least 1 available room";
    }

    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setErrorMsg("");

    if (!validateForm()) return;

    setLoading(true);
    try {
      await registerHotel({
        ...formData,
        hotel: formData.hotel.trim(),
        email: formData.email.trim(),
        city: formData.city.trim(),
        address: formData.address.trim(),
        location: formData.location.trim()
      });
      alert("✅ Hotel registered successfully! Please log in with your hotel credentials.");
      navigate("/hotel-login");
    } catch (err) {
      console.error("Hotel registration error:", err.response?.data);
      const backendMessage =
        err.response?.data?.error?.message ||
        err.response?.data?.message ||
        "Registration failed. Please verify your details.";
      setErrorMsg(backendMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page-wrapper">
      <div className="auth-card auth-card-wide">
        {/* Brand Header */}
        <div className="auth-header">
          <div className="auth-brand-badge">
            <img src="/assets/logo-badge.png" alt="worldtours.com Logo" className="auth-logo-img" />
            <span style={{ fontWeight: 800, fontSize: "1.1rem" }}>worldtours.com</span>
          </div>
          <h2>Register Hotel Partner</h2>
          <p>List your property and reach luxury travelers worldwide</p>
        </div>

        {errorMsg && (
          <div className="auth-alert-error" style={{ marginBottom: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <AlertCircle size={18} />
              <span>{errorMsg}</span>
            </div>
          </div>
        )}

        <form onSubmit={handleRegister} className="auth-form" noValidate>
          <div className="auth-grid-2col">
            {/* Hotel Name */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-name">
                Hotel Name <span className="form-label-required">*</span>
              </label>
              <input
                id="h-name"
                type="text"
                name="hotel"
                className={`auth-input ${fieldErrors.hotel ? "input-error" : ""}`}
                placeholder="e.g. Grand Palace Resort"
                value={formData.hotel}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.hotel && <span className="field-error-text">{fieldErrors.hotel}</span>}
            </div>

            {/* Email */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-email">
                Contact Gmail <span className="form-label-required">*</span>
              </label>
              <input
                id="h-email"
                type="email"
                name="email"
                className={`auth-input ${fieldErrors.email ? "input-error" : ""}`}
                placeholder="hotel@gmail.com"
                value={formData.email}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.email && <span className="field-error-text">{fieldErrors.email}</span>}
            </div>

            {/* Password */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-password">
                Password <span className="form-label-required">*</span>
              </label>
              <div className="password-input-wrapper">
                <input
                  id="h-password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  className={`auth-input ${fieldErrors.password ? "input-error" : ""}`}
                  placeholder="At least 6 characters"
                  value={formData.password}
                  onChange={handleChange}
                  disabled={loading}
                  required
                />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {fieldErrors.password && <span className="field-error-text">{fieldErrors.password}</span>}
            </div>

            {/* City */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-city">
                City <span className="form-label-required">*</span>
              </label>
              <input
                id="h-city"
                type="text"
                name="city"
                className={`auth-input ${fieldErrors.city ? "input-error" : ""}`}
                placeholder="e.g. Mumbai, Jaipur, Goa"
                value={formData.city}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.city && <span className="field-error-text">{fieldErrors.city}</span>}
            </div>

            {/* Street Address */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-address">
                Street Address <span className="form-label-required">*</span>
              </label>
              <input
                id="h-address"
                type="text"
                name="address"
                className={`auth-input ${fieldErrors.address ? "input-error" : ""}`}
                placeholder="e.g. 12 Marine Drive"
                value={formData.address}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.address && <span className="field-error-text">{fieldErrors.address}</span>}
            </div>

            {/* Location / Landmark */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-location">
                Location Landmark <span className="form-label-required">*</span>
              </label>
              <input
                id="h-location"
                type="text"
                name="location"
                className={`auth-input ${fieldErrors.location ? "input-error" : ""}`}
                placeholder="e.g. Near Airport / Beachfront"
                value={formData.location}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.location && <span className="field-error-text">{fieldErrors.location}</span>}
            </div>

            {/* Price Per Night */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-price">
                Base Price / Night (₹) <span className="form-label-required">*</span>
              </label>
              <input
                id="h-price"
                type="number"
                min="100"
                name="price"
                className={`auth-input ${fieldErrors.price ? "input-error" : ""}`}
                placeholder="e.g. 4500"
                value={formData.price}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.price && <span className="field-error-text">{fieldErrors.price}</span>}
            </div>

            {/* Rooms Available */}
            <div className="form-group-item">
              <label className="form-label" htmlFor="h-rooms">
                Available Rooms <span className="form-label-required">*</span>
              </label>
              <input
                id="h-rooms"
                type="number"
                min="1"
                name="roomAvailable"
                className={`auth-input ${fieldErrors.roomAvailable ? "input-error" : ""}`}
                placeholder="e.g. 20"
                value={formData.roomAvailable}
                onChange={handleChange}
                disabled={loading}
                required
              />
              {fieldErrors.roomAvailable && <span className="field-error-text">{fieldErrors.roomAvailable}</span>}
            </div>
          </div>

          <button type="submit" className="auth-submit-btn" disabled={loading} style={{ marginTop: "12px" }}>
            {loading ? "Registering Property..." : "Submit Partner Registration"}
          </button>
        </form>

        <div className="auth-footer-links">
          <div>
            Already registered as partner?{" "}
            <Link to="/hotel-login" className="auth-link-highlight">
              Hotel Login here
            </Link>
          </div>

          <div className="auth-role-switch-row" style={{ justifyContent: "center" }}>
            <Link to="/login" className="auth-role-switch-btn">
              <User size={13} /> Looking to book stays? Traveler Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default HotelRegister;
