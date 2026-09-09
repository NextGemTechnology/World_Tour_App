// Theme management utility supporting:
// 1. "dark-75"  -> 75% Dark (Default flagship deep slate/navy)
// 2. "light-50" -> 50% Light (Balanced soft slate/grey contrast)
// 3. "light-25" -> 25% Light (Luminous pure white cards & canvas)

const THEME_STORAGE_KEY = "app_theme";
export const THEMES = ["dark-75", "light-50", "light-25"];

export const THEME_CONFIG = {
  "dark-75": {
    id: "dark-75",
    label: "75% Dark",
    icon: "🌙",
    desc: "Deep Slate & Cyan Glow"
  },
  "light-50": {
    id: "light-50",
    label: "50% Light",
    icon: "🌓",
    desc: "Balanced Slate Grey"
  },
  "light-25": {
    id: "light-25",
    label: "25% Light",
    icon: "☀️",
    desc: "Pure White & Crisp Blue"
  }
};

export const getInitialTheme = () => {
  return "dark-75";
};

export const applyTheme = (theme) => {
  if (!THEMES.includes(theme)) theme = "dark-75";
  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem(THEME_STORAGE_KEY, theme);
  window.dispatchEvent(new CustomEvent("app-theme-changed", { detail: { theme } }));
  return theme;
};

export const cycleTheme = (currentTheme) => {
  const currentIndex = THEMES.indexOf(currentTheme);
  const nextIndex = (currentIndex + 1) % THEMES.length;
  const nextTheme = THEMES[nextIndex];
  return applyTheme(nextTheme);
};
