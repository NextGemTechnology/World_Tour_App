import React, { useState, useEffect, useRef } from "react";
import axios from "axios";
import { Plane, Train, Bus, Car, X, Send, Trash2, Sun, Moon, Plus } from "lucide-react";
import "../styles/AIChatModal.css";

const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080";

const generateSessionId = () => `session_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

function AIChatModal({ onClose }) {
  const [sessionId, setSessionId] = useState(generateSessionId);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [darkMode, setDarkMode] = useState(true);
  const [userInfo, setUserInfo] = useState(null);

  // Travel agent flow state machine
  const [agentState, setAgentState] = useState({
    step: "idle", // idle, waiting_for_mode, waiting_for_hotel, waiting_for_upi, payment_processing, payment_done
    destination: "",
    mode: "",
    options: [],
    hotels: [],
    selectedHotel: null,
    selectedOption: null,
    upi: "",
  });

  const chatEndRef = useRef(null);
  const modalRef = useRef(null);

  // Prevent page background from scrolling while AI panel is open
  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

  // Handle ESC key press to close modal
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const handleOverlayClick = (e) => {
    if (modalRef.current && !modalRef.current.contains(e.target)) {
      onClose();
    }
  };

  // Load theme preference
  useEffect(() => {
    const savedTheme = localStorage.getItem("ai-theme");
    if (savedTheme) setDarkMode(savedTheme === "dark");
  }, []);

  // Save theme preference
  useEffect(() => {
    localStorage.setItem("ai-theme", darkMode ? "dark" : "light");
  }, [darkMode]);

  // Fetch logged in user profile on mount
  useEffect(() => {
    const fetchUser = async () => {
      const token = localStorage.getItem("token") || localStorage.getItem("hotelToken") || localStorage.getItem("adminToken");
      if (token) {
        try {
          const res = await axios.get(`${API_BASE}/api/dashboard/profile`, {
            headers: { Authorization: `Bearer ${token}` }
          });
          if (res.data) {
            setUserInfo(res.data);
            setMessages([
              {
                role: "ai",
                text: `👋 Hello ${res.data.name}! I am your AI Travel Agent. Tell me where you'd like to go (e.g. "I want to go to Delhi"), and I will plan your transport, stay, and simulate payment.`
              }
            ]);
          }
        } catch (e) {
          console.error("AI couldn't fetch user profile", e);
          fallbackWelcome();
        }
      } else {
        fallbackWelcome();
      }
    };

    const fallbackWelcome = () => {
      setMessages([
        {
          role: "ai",
          text: "👋 Welcome Guest! Please login to start booking journeys. Where would you like to travel today? (E.g. \"I want to go to Delhi\")"
        }
      ]);
    };

    fetchUser();
  }, []);

  // Scroll to bottom when messages list updates
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const sendMessage = async (customInput) => {
    const text = customInput || input;
    if (!text.trim()) return;

    // Add user message to display list
    const userMsg = { role: "user", text };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");

    // Conversational State Machine Routing
    if (agentState.step === "waiting_for_upi") {
      handleUpiSubmit(text);
      return;
    }

    // Step 1: Check if user is asking to travel
    const travelMatch = text.match(/(?:go to|trip to|visit|travel to|plan to|want to go|planning for)\s+([a-zA-Z\s]+)/i);
    if (travelMatch) {
      const destination = travelMatch[1].trim();
      setLoading(true);
      setTimeout(() => {
        setMessages((prev) => [
          ...prev,
          {
            role: "ai",
            text: `Perfect! I see you want to travel to **${destination}**. Which mode of transportation would you prefer?`,
            type: "mode_selection"
          }
        ]);
        setAgentState((prev) => ({
          ...prev,
          step: "waiting_for_mode",
          destination
        }));
        setLoading(false);
      }, 800);
      return;
    }

    // Normal query fallback using backend AI/Ollama module
    setLoading(true);
    try {
      const token = localStorage.getItem("token") || localStorage.getItem("hotelToken") || localStorage.getItem("adminToken");
      const res = await axios.post(`${API_BASE}/api/ai/prompt`, {
        prompt: text,
        role: localStorage.getItem("role") || "GUEST",
        email: userInfo?.email || "",
        sessionId: sessionId
      }, {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      });

      if (Array.isArray(res.data)) {
        setMessages((prev) => [...prev, { role: "ai", type: "list", data: res.data }]);
      } else {
        setMessages((prev) => [...prev, { role: "ai", text: res.data?.message || res.data || "I am processing your request." }]);
      }
    } catch (e) {
      console.error(e);
      setMessages((prev) => [...prev, { role: "ai", text: "⚠️ Server response lookup failed. Try asking a destination: E.g. 'I want to go to Goa'." }]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectMode = async (mode) => {
    // Add user choice to chat
    setMessages((prev) => [...prev, { role: "user", text: `I prefer to go by ${mode}` }]);
    setLoading(true);

    const origin = userInfo?.city || "Indore";
    const dest = agentState.destination;

    // 1. Fetch available transport routes from database
    let fetchedOptions = [];
    try {
      let endpoint = "";
      if (mode === "Flight") endpoint = `/api/flights/search?source=${origin}&destination=${dest}`;
      else if (mode === "Bus") endpoint = `/api/flights/search?source=${origin}&destination=${dest}`; // reuse vehicle search
      else if (mode === "Train") endpoint = `/api/trains/search?source=${origin}&destination=${dest}`;

      if (endpoint) {
        const res = await axios.get(`${API_BASE}${endpoint}`);
        fetchedOptions = res.data?.data || [];
      }
    } catch (err) {
      console.error(err);
    }

    // Fallback Mock Options if Database has no exact schedules
    if (fetchedOptions.length === 0) {
      if (mode === "Flight") {
        fetchedOptions = [
          { id: 101, airline: "IndiGo", flightNumber: "6E-342", fare: 5200, departureTime: "08:00 AM", arrivalTime: "09:45 AM", availableSeats: 12 },
          { id: 102, airline: "Air India", flightNumber: "AI-102", fare: 6500, departureTime: "02:15 PM", arrivalTime: "04:00 PM", availableSeats: 8 }
        ];
      } else if (mode === "Train") {
        fetchedOptions = [
          { id: 201, trainName: "Rajdhani Express", trainNumber: "12431", fare: 1800, departureTime: "06:30 PM", arrivalTime: "08:15 AM", availableSeats: 25 },
          { id: 202, trainName: "Shatabdi Express", trainNumber: "12002", fare: 1200, departureTime: "06:00 AM", arrivalTime: "11:40 AM", availableSeats: 14 }
        ];
      } else if (mode === "Bus") {
        fetchedOptions = [
          { id: 301, operatorName: "Neeta Travels", busNumber: "MH-12-3211", fare: 800, departureTime: "09:00 PM", arrivalTime: "06:00 AM", availableSeats: 18 },
          { id: 302, operatorName: "Verma Travels", busNumber: "MP-09-5432", fare: 950, departureTime: "10:30 PM", arrivalTime: "07:30 AM", availableSeats: 20 }
        ];
      } else {
        fetchedOptions = [
          { id: 401, operatorName: "OLA Outstation", fare: 3500, duration: "8 hrs", availableSeats: 4 }
        ];
      }
    }

    // 2. Fetch Hotels at destination
    let fetchedHotels = [];
    try {
      const res = await axios.get(`${API_BASE}/api/dashboard/search?location=${dest}`);
      fetchedHotels = res.data?.data || res.data || [];
    } catch (err) {
      console.error(err);
    }

    if (fetchedHotels.length === 0) {
      fetchedHotels = [
        { id: 1, hotelName: "Hotel Radisson Luxury Stay", city: dest, price: 4500, rating: "4.8" },
        { id: 2, hotelName: "Hotel Blue Sapphire Comforts", city: dest, price: 2900, rating: "4.3" }
      ];
    }

    setTimeout(() => {
      setMessages((prev) => [
        ...prev,
        {
          role: "ai",
          text: `Found transportation choices for your journey:`,
          type: "options_list",
          data: fetchedOptions,
          mode
        },
        {
          role: "ai",
          text: `Here are stays in **${dest}**. Choose one to finalize your package:`,
          type: "hotel_selection",
          data: fetchedHotels
        }
      ]);

      setAgentState((prev) => ({
        ...prev,
        step: "waiting_for_hotel",
        mode,
        options: fetchedOptions,
        hotels: fetchedHotels
      }));
      setLoading(false);
    }, 1200);
  };

  const handleSelectPackage = (option, hotel) => {
    // If they just clicked stay option
    setAgentState((prev) => ({
      ...prev,
      selectedOption: option || prev.selectedOption,
      selectedHotel: hotel || prev.selectedHotel,
    }));

    const finalOption = option || agentState.selectedOption;
    const finalHotel = hotel || agentState.selectedHotel;

    if (!finalOption || !finalHotel) {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "Please pick both your travel ticket and hotel room option above." }
      ]);
      return;
    }

    const totalFare = (finalOption.fare || 2500) + (finalHotel.price || 3000);

    setMessages((prev) => [
      ...prev,
      {
        role: "ai",
        text: `🏨 Selected Stay: **${finalHotel.hotelName}** (₹${finalHotel.price})\n🚗 Selected Route: **${finalOption.airline || finalOption.trainName || finalOption.operatorName}** (₹${finalOption.fare})\n\n💰 Total Package cost: **₹${totalFare}**.\n\nPlease enter your UPI ID (e.g. username@upi) to checkout:`
      }
    ]);

    setAgentState((prev) => ({
      ...prev,
      selectedOption: finalOption,
      selectedHotel: finalHotel,
      step: "waiting_for_upi"
    }));
  };

  const handleUpiSubmit = (upiId) => {
    if (!upiId.includes("@")) {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "⚠️ Invalid UPI address. Please write a valid handle like yourname@bank:" }
      ]);
      return;
    }

    const totalFare = (agentState.selectedOption?.fare || 0) + (agentState.selectedHotel?.price || 0);

    setMessages((prev) => [
      ...prev,
      { role: "ai", text: `Payment request of **₹${totalFare}** sent to **${upiId}**. Waiting for approval...` }
    ]);

    setLoading(true);
    setAgentState((prev) => ({ ...prev, upi: upiId, step: "payment_processing" }));

    // Simulate UPI Push confirmation delay
    setTimeout(async () => {
      // Try to create real bookings in DB silently
      try {
        const token = localStorage.getItem("token");
        const userId = userInfo?.id || 1;

        if (token) {
          // Book Hotel
          await axios.post(`${API_BASE}/api/bookings/bookhotel`, {
            hotelId: agentState.selectedHotel.id,
            hotelName: agentState.selectedHotel.hotelName,
            checkIn: "2026-07-20",
            checkOut: "2026-07-22",
            amount: agentState.selectedHotel.price,
            userId: Number(userId)
          }, {
            headers: { Authorization: `Bearer ${token}` }
          });
        }
      } catch (err) {
        console.error("Silent DB booking write failed:", err);
      }

      setMessages((prev) => [
        ...prev,
        {
          role: "ai",
          text: `🎉 **UPI Payment Success!**\n\nYour trip package to **${agentState.destination}** has been confirmed.\n\n- Stays at: **${agentState.selectedHotel.hotelName}**\n- Transit via: **${agentState.selectedOption.airline || agentState.selectedOption.trainName || agentState.selectedOption.operatorName}**\n\nAll details have been sent to **${userInfo?.email || "guest@luxnestravel.com"}**. Check your dashboard for active tickets!`
        }
      ]);
      setAgentState((prev) => ({ ...prev, step: "payment_done" }));
      setLoading(false);
    }, 2800);
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleNewChat = () => {
    const newSess = generateSessionId();
    setSessionId(newSess);
    setMessages([
      {
        role: "ai",
        text: userInfo?.name
          ? `👋 Started a new chat! Hello ${userInfo.name}, where would you like to travel today?`
          : "👋 Started a new chat! Where would you like to travel today? (E.g. \"I want to go to Delhi\")"
      }
    ]);
    setAgentState({
      step: "idle",
      destination: "",
      mode: "",
      options: [],
      hotels: [],
      selectedHotel: null,
      selectedOption: null,
      upi: "",
    });
    setInput("");
  };

  const clearChat = () => {
    setMessages([{ role: "ai", text: "Chat history cleared. Tell me where you want to go next!" }]);
    setAgentState({ step: "idle", destination: "", mode: "", options: [], hotels: [], selectedHotel: null, selectedOption: null, upi: "" });
  };

  return (
    <div className={`ai-modal-overlay ${darkMode ? "dark" : "light"}`} onClick={handleOverlayClick}>
      <div className="ai-modal open" ref={modalRef} onClick={(e) => e.stopPropagation()}>
        
        {/* HEADER */}
        <div className="ai-header">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div className="ai-header-avatar-frame">
              <img src="/assets/ai-agent-logo.png" alt="AI Agent" className="ai-header-avatar-img" />
              <span className="ai-header-online-dot"></span>
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: "1.05rem", fontWeight: 700, letterSpacing: "-0.01em" }}>LuxNes AI Agent</h3>
              <span style={{ fontSize: "0.72rem", opacity: 0.85, fontWeight: 500, display: "block" }}>Smart Travel Assistant</span>
            </div>
          </div>
          <div className="ai-actions">
            <button onClick={handleNewChat} className="ai-new-chat-btn" title="Start New Chat">
              <Plus size={16} />
              <span>New Chat</span>
            </button>
            <button onClick={() => setDarkMode(!darkMode)} title="Toggle Theme">
              {darkMode ? <Sun size={18} /> : <Moon size={18} />}
            </button>
            <button onClick={clearChat} title="Clear Chat">
              <Trash2 size={18} />
            </button>
            <button onClick={onClose} title="Close Chat" className="ai-close-btn">
              <X size={20} />
            </button>
          </div>
        </div>

        {/* CHAT DISPLAY BODY */}
        <div className="ai-body">
          {messages.map((msg, i) => (
            <div key={i} className={`msg-wrapper ${msg.role === "user" ? "user" : "ai"}`}>
              {msg.role === "ai" && (
                <div className="ai-msg-avatar-wrap">
                  <img src="/assets/ai-agent-logo.png" alt="AI Agent" className="ai-msg-avatar" />
                </div>
              )}
              <div className={`message-bubble ${msg.role === "user" ? "user-bubble" : "ai-bubble"}`}>
                <p style={{ whiteSpace: "pre-line" }}>{msg.text}</p>

                {/* Transportation mode selector buttons */}
                {msg.type === "mode_selection" && (
                  <div className="travel-modes-picker">
                    <button onClick={() => handleSelectMode("Flight")}><Plane size={16} /> Flight</button>
                    <button onClick={() => handleSelectMode("Train")}><Train size={16} /> Train</button>
                    <button onClick={() => handleSelectMode("Bus")}><Bus size={16} /> Bus</button>
                    <button onClick={() => handleSelectMode("Cab")}><Car size={16} /> Cab</button>
                  </div>
                )}

                {/* Options cards list (transit routes) */}
                {msg.type === "options_list" && (
                  <div className="agent-cards-container">
                    {msg.data.map((opt) => (
                      <div key={opt.id} className={`agent-item-card ${agentState.selectedOption?.id === opt.id ? "selected" : ""}`}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                          <strong>{opt.airline || opt.trainName || opt.operatorName}</strong>
                          <span className="price-tag">₹{opt.fare}</span>
                        </div>
                        <p style={{ fontSize: "0.8rem", color: "#64748b" }}>
                          No: {opt.flightNumber || opt.trainNumber || "Cab service"} | Seats left: {opt.availableSeats}
                        </p>
                        <p style={{ fontSize: "0.8rem", color: "#64748b" }}>
                          Departs: {opt.departureTime || "Flexible"} | Duration: {opt.duration || "N/A"}
                        </p>
                        <button className="select-card-btn" onClick={() => handleSelectPackage(opt, null)}>
                          {agentState.selectedOption?.id === opt.id ? "Selected ✓" : "Select transit"}
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Stays list selection cards */}
                {msg.type === "hotel_selection" && (
                  <div className="agent-cards-container">
                    {msg.data.map((htl) => (
                      <div key={htl.id} className={`agent-item-card ${agentState.selectedHotel?.id === htl.id ? "selected" : ""}`}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                          <strong>{htl.hotelName}</strong>
                          <span className="price-tag">₹{htl.price}/n</span>
                        </div>
                        <p style={{ fontSize: "0.8rem", color: "#64748b" }}>City: {htl.city} | Rating: {htl.rating} ★</p>
                        <button className="select-card-btn" onClick={() => handleSelectPackage(null, htl)}>
                          {agentState.selectedHotel?.id === htl.id ? "Selected Stay ✓" : "Select Stay"}
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}

          {loading && (
            <div className="msg-wrapper ai">
              <div className="ai-msg-avatar-wrap">
                <img src="/assets/ai-agent-logo.png" alt="AI Agent" className="ai-msg-avatar" />
              </div>
              <div className="message-bubble ai-bubble typing">
                <span className="dot"></span>
                <span className="dot"></span>
                <span className="dot"></span>
              </div>
            </div>
          )}

          <div ref={chatEndRef} />
        </div>

        {/* INPUT FOOTER */}
        <div className="ai-footer">
          <textarea
            placeholder={
              agentState.step === "waiting_for_upi"
                ? "Enter your UPI address..."
                : "Where would you like to travel?"
            }
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyPress}
            rows={1}
          />
          <button className="send-btn" onClick={() => sendMessage()}>
            <Send size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default AIChatModal;