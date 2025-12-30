export const generateCalendarColors = () => [
  '#3b82f6', // blue
  '#10b981', // emerald
  '#8b5cf6', // violet
  '#f59e0b', // amber
  '#ef4444', // red
  '#06b6d4', // cyan
  '#84cc16', // lime
  '#f97316', // orange
  '#ec4899', // pink
  '#6366f1', // indigo
];

export const getContrastColor = (backgroundColor) => {
  // Convert hex to RGB
  const hex = backgroundColor.replace('#', '');
  const r = parseInt(hex.substr(0, 2), 16);
  const g = parseInt(hex.substr(2, 2), 16);
  const b = parseInt(hex.substr(4, 2), 16);
  
  // Calculate luminance
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  
  return luminance > 0.5 ? '#000000' : '#ffffff';
};

export const lightenColor = (color, percent) => {
  const hex = color.replace('#', '');
  const r = parseInt(hex.substr(0, 2), 16);
  const g = parseInt(hex.substr(2, 2), 16);
  const b = parseInt(hex.substr(4, 2), 16);
  
  const newR = Math.min(255, Math.floor(r + (255 - r) * percent / 100));
  const newG = Math.min(255, Math.floor(g + (255 - g) * percent / 100));
  const newB = Math.min(255, Math.floor(b + (255 - b) * percent / 100));
  
  return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
};

export const COLOR_TO_THEME = {
  '#3b82f6': 'BLUE',     // blue
  '#6366f1': 'BLUE',     // indigo → BLUE로 묶기
  '#10b981': 'GREEN',    // emerald
  '#84cc16': 'GREEN',    // lime → GREEN
  '#f97316': 'ORANGE',   // orange
  '#f59e0b': 'ORANGE',   // amber → ORANGE
  '#ef4444': 'RED',      // red
  '#8b5cf6': 'PURPLE',   // violet
  '#ec4899': 'PURPLE',   // pink → PURPLE
  '#06b6d4': 'BLUE',     // cyan → BLUE 계열
};

export const THEME_TO_COLOR = {
  BLUE: '#3b82f6',
  GREEN: '#10b981',
  ORANGE: '#f97316',
  RED: '#ef4444',
  PURPLE: '#8b5cf6',
};