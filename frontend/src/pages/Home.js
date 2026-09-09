import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { searchByLocation, searchTourByDays } from "../api/publicApi";
import axios from "axios";
import HomeNavbar from "../components/HomeNavbar";
import Footer from "../components/Footer";
import { Plane, Train, Bus, Hotel, Search, Star, Award, Shield, Wifi, MapPin, Calendar } from "lucide-react";
import "../styles/HomePage.css";

function HomePage() {
  const navigate = useNavigate();
  const today = new Date().toISOString().split("T")[0];

  // Active Tab: hotel, flight, bus, train
  const [activeTab, setActiveTab] = useState("hotel");

  // Search form states
  const [source, setSource] = useState("");
  const [destination, setDestination] = useState("");
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");
  const [guests, setGuests] = useState("1");
  const [days, setDays] = useState("");

  const [loading, setLoading] = useState(false);
  const [tours, setTours] = useState([]);
  const [showResults, setShowResults] = useState(false);
  const [hotels, setHotels] = useState([]);

  useEffect(() => {
    fetchTopHotels();
  }, []);

  const fetchTopHotels = async () => {
    try {
      const res = await axios.get(`${process.env.REACT_APP_API_URL || "http://localhost:8080"}/api/public/tophotels`);
      const hotelsData = res.data?.data || res.data.body?.data || [];
      setHotels(hotelsData);
    } catch (err) {
      console.error("Error fetching hotels", err);
    }
  };

  const handleSearchClick = async (e) => {
    e.preventDefault();

    if (activeTab === "flight") {
      navigate(`/flights?source=${source}&destination=${destination}`);
      return;
    }
    if (activeTab === "bus") {
      navigate(`/buses?source=${source}&destination=${destination}`);
      return;
    }
    if (activeTab === "train") {
      navigate(`/trains?source=${source}&destination=${destination}`);
      return;
    }

    // Hotel / Tour Search
    if (!destination && !days) {
      alert("Please enter a destination or duration in days.");
      return;
    }

    try {
      setLoading(true);
      let res;
      if (destination) {
        res = await searchByLocation(destination);
      } else {
        res = await searchTourByDays(days);
      }

      if (res.error) {
        alert(res.error.message);
        setTours([]);
      } else {
        setTours(res.data || []);
        setShowResults(true);
      }
    } catch (err) {
      console.error(err);
      alert("Error fetching tours");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="home">
      <HomeNavbar />

      {/* Hero Section */}
      <section className="hero">
        <h1>Simplify Your Next Journey</h1>
        <p>Book luxury stays, flights, buses, and trains at the best guaranteed rates</p>

        {/* AI Travel Assistant Prompt Strip */}
        <div className="hero-ai-strip" onClick={() => window.dispatchEvent(new CustomEvent("open-ai-chat"))}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <img src="/assets/ai-agent-logo.png" alt="AI Agent" style={{ width: 26, height: 26, borderRadius: "50%", border: "1.5px solid #38bdf8" }} />
            <span className="hero-ai-badge">AI Travel Assistant</span>
          </div>
          <span className="hero-ai-text">Plan your multi-modal trip with hotels, transit & budget</span>
          <button className="hero-ai-btn" onClick={(e) => { e.stopPropagation(); window.dispatchEvent(new CustomEvent("open-ai-chat")); }}>Ask AI →</button>
        </div>

        {/* Tabbed Search Panel */}
        <div className="search-container">
          <div className="search-tabs">
            <button className={`tab-btn ${activeTab === "hotel" ? "active" : ""}`} onClick={() => setActiveTab("hotel")}>
              <Hotel size={16} /> Hotels & Tours
            </button>
            <button className={`tab-btn ${activeTab === "flight" ? "active" : ""}`} onClick={() => setActiveTab("flight")}>
              <Plane size={16} /> Flights
            </button>
            <button className={`tab-btn ${activeTab === "bus" ? "active" : ""}`} onClick={() => setActiveTab("bus")}>
              <Bus size={16} /> Buses
            </button>
            <button className={`tab-btn ${activeTab === "train" ? "active" : ""}`} onClick={() => setActiveTab("train")}>
              <Train size={16} /> Trains
            </button>
          </div>

          <form onSubmit={handleSearchClick}>
            {activeTab === "hotel" ? (
              <div className="search-row">
                <div className="search-input-group">
                  <label><MapPin size={14} style={{ marginRight: 4, verticalAlign: "middle" }} /> Destination</label>
                  <input
                    type="text"
                    placeholder="Where are you going?"
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                  />
                </div>
                <div className="search-input-group">
                  <label><Calendar size={14} style={{ marginRight: 4, verticalAlign: "middle" }} /> Check-In</label>
                  <input type="date" min={today} value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
                </div>
                <div className="search-input-group">
                  <label><Calendar size={14} style={{ marginRight: 4, verticalAlign: "middle" }} /> Check-Out</label>
                  <input type="date" min={checkIn || today} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
                </div>
                <div className="search-input-group">
                  <label>Guests</label>
                  <select value={guests} onChange={(e) => setGuests(e.target.value)}>
                    <option value="1">1 Guest</option>
                    <option value="2">2 Guests</option>
                    <option value="3">3 Guests</option>
                    <option value="4+">4+ Guests</option>
                  </select>
                </div>
              </div>
            ) : (
              <div className="search-row">
                <div className="search-input-group">
                  <label><MapPin size={14} style={{ marginRight: 4, verticalAlign: "middle" }} /> From</label>
                  <input
                    type="text"
                    placeholder="Origin City"
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                    required
                  />
                </div>
                <div className="search-input-group">
                  <label><MapPin size={14} style={{ marginRight: 4, verticalAlign: "middle" }} /> To</label>
                  <input
                    type="text"
                    placeholder="Destination City"
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                    required
                  />
                </div>
                <div className="search-input-group">
                  <label><Calendar size={14} style={{ marginRight: 4, verticalAlign: "middle" }} /> Departure Date</label>
                  <input type="date" min={today} required />
                </div>
              </div>
            )}

            <div style={{ display: "flex", justifyContent: "center", marginTop: 10 }}>
              <button className="search-action-btn" type="submit">
                <Search size={18} style={{ marginRight: 8, verticalAlign: "middle" }} />
                {loading ? "Searching..." : "Search Journeys"}
              </button>
            </div>
          </form>
        </div>
      </section>

      {/* Results Popup */}
      {showResults && (
        <div className="results-overlay">
          <div className="results-box">
            <button className="close-btn" onClick={() => setShowResults(false)}>✖</button>
            <h2 style={{ marginBottom: 20 }}>Tours matching your search</h2>
            <div className="card-container">
              {tours.length > 0 ? (
                tours.map((t, i) => (
                  <div key={i} className="card small">
                    <img src={t.imageUrl || "/assets/logo-badge.png"} alt={t.name} style={{ width: "100%", borderRadius: 10, height: 160, objectFit: "contain", background: "var(--bg-input, rgba(255,255,255,0.1))", padding: 10 }} />
                    <h3 style={{ marginTop: 12 }}>{t.name}</h3>
                    <p style={{ color: "#64748b", margin: "4px 0" }}>{t.destination}</p>
                    <p>{t.duration} Days</p>
                    <h3 style={{ color: "#38bdf8" }}>₹{t.price}</h3>
                    <button style={{ backgroundColor: "#38bdf8", color: "#0b132b", fontWeight: 700, padding: "8px 16px", border: "none", borderRadius: 6, cursor: "pointer", marginTop: 10 }} onClick={() => { setShowResults(false); navigate(`/tour-booking/${t.id}`); }}>
                      Book Details
                    </button>
                  </div>
                ))
              ) : (
                <p>No tour packages found. Try search using another location.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Featured Hotels */}
      <section className="featured-section">
        <h2 className="featured-title">Top Rated Hotels</h2>
        <div className="featured-container">
          {hotels.length > 0 ? (
            hotels.slice(0, 3).map((hotel, index) => (
              <div className="featured-card" key={hotel.id}>
                <img
                  className="featured-img"
                  src={hotel.imageUrl || "/assets/logo-badge.png"}
                  alt={hotel.hotelName}
                />
                <div className="featured-content">
                  <h3 className="hotel-name">{hotel.hotelName}</h3>
                  <p className="hotel-city">{hotel.city} | Rating: {hotel.rating || "4.5"} ⭐</p>
                  <p className="hotel-price">₹{hotel.price || "2500"} / night</p>
                  <button className="book-btn" onClick={() => navigate("/hotels")}>Book Room</button>
                </div>
              </div>
            ))
          ) : (
            <p className="no-data" style={{ gridColumn: "1/-1", textAlign: "center" }}>No hotels registered yet. Check back soon!</p>
          )}
        </div>
      </section>

      {/* Why Choose Us */}
      <section className="section" style={{ background: "transparent" }}>
        <h2>Why Travel With worldtours.com</h2>
        <div className="features">
          <div><Star style={{ color: "#38bdf8" }} /> Best Price Guarantee</div>
          <div><Award style={{ color: "#38bdf8" }} /> 1000+ Vetted Stays</div>
          <div><Shield style={{ color: "#38bdf8" }} /> Secure Payments</div>
          <div><Wifi style={{ color: "#38bdf8" }} /> 24/7 AI Assistance</div>
        </div>
      </section>

      {/* Special Offer */}
      <section className="offer">
        <h2>Special Launch Offer</h2>
        <p>Flat 20% OFF on your first booking using worldtours.com AI Travel Assistant</p>
        <button onClick={() => navigate("/register")}>Register & Claim</button>
      </section>

      {/* Reviews */}
      <section className="section">
        <h2>What Travelers Say</h2>
        <div className="card-container">
          <div className="card small">
            <p>"worldtours.com made booking my flights and hotels together extremely easy. The Razorpay verification was fast and transparent."</p>
            <h4>- Rahul S.</h4>
          </div>
          <div className="card small">
            <p>"The AI assistant planned my whole Delhi-Jaipur itinerary and suggested cheap buses. Absolute game-changer!"</p>
            <h4>- Priya M.</h4>
          </div>
        </div>
      </section>

      {/* Destination Showcase (Pure CSS Cards, Zero External Images) */}
      <section className="section">
        <h2>Explore Top Destinations</h2>
        <div className="gallery" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 16 }}>
          {[
            { title: "Taj Mahal", location: "Agra", icon: "🏛️", desc: "Wonders of India" },
            { title: "Jaipur Palace", location: "Rajasthan", icon: "🏰", desc: "Royal Heritage" },
            { title: "Goa Beaches", location: "Goa", icon: "🏖️", desc: "Sun & Ocean" },
            { title: "Kerala Backwaters", location: "Kerala", icon: "🌴", desc: "Serene Nature" }
          ].map((dest, i) => (
            <div key={i} style={{
              background: "var(--bg-card, rgba(15, 23, 42, 0.85))",
              border: "var(--glass-border, 1px solid rgba(255,255,255,0.12))",
              borderRadius: 16,
              padding: "24px 20px",
              textAlign: "center",
              boxShadow: "var(--card-shadow, 0 4px 14px rgba(0,0,0,0.15))"
            }}>
              <div style={{ fontSize: "2.4rem", marginBottom: 10 }}>{dest.icon}</div>
              <h3 style={{ fontSize: "1.1rem", marginBottom: 4, color: "var(--text-main, #ffffff)" }}>{dest.title}</h3>
              <p style={{ fontSize: "0.85rem", color: "var(--accent-cyan, #38bdf8)", fontWeight: 600 }}>📍 {dest.location}</p>
              <span style={{ fontSize: "0.78rem", color: "var(--text-muted, #94a3b8)", marginTop: 6, display: "block" }}>{dest.desc}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Branded NEXTGEM-TECHNOLOGY Footer */}
      <Footer />
    </div>
  );
}

export default HomePage;