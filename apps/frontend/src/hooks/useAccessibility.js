import { useEffect, useRef, useCallback } from 'react';

export const useFocusTrap = (isActive) => {
  const containerRef = useRef();
  
  useEffect(() => {
    if (!isActive || !containerRef.current) return;
    
    const container = containerRef.current;
    const focusableElements = container.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];
    
    const handleTabKey = (e) => {
      if (e.key !== 'Tab') return;
      
      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault();
          firstElement.focus();
        }
      }
    };
    
    const handleEscapeKey = (e) => {
      if (e.key === 'Escape') {
        // 모달 닫기 등의 동작
        const closeButton = container.querySelector('[data-close]');
        if (closeButton) closeButton.click();
      }
    };
    
    document.addEventListener('keydown', handleTabKey);
    document.addEventListener('keydown', handleEscapeKey);
    
    // 첫 번째 요소에 포커스
    if (firstElement) {
      firstElement.focus();
    }
    
    return () => {
      document.removeEventListener('keydown', handleTabKey);
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [isActive]);
  
  return containerRef;
};

export const useAnnouncement = () => {
  const announce = useCallback((message, priority = 'polite') => {
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', priority);
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    
    document.body.appendChild(announcement);
    
    setTimeout(() => {
      document.body.removeChild(announcement);
    }, 1000);
  }, []);
  
  return announce;
};

export const useKeyboardShortcuts = (shortcuts) => {
  useEffect(() => {
    const handleKeyDown = (e) => {
      const key = e.key.toLowerCase();
      const modifiers = {
        ctrl: e.ctrlKey,
        shift: e.shiftKey,
        alt: e.altKey,
        meta: e.metaKey
      };
      
      shortcuts.forEach(({ keys, action, preventDefault = true }) => {
        const match = keys.every(keyCombo => {
          if (typeof keyCombo === 'string') {
            return key === keyCombo;
          }
          
          const { key: shortcutKey, ...shortcutModifiers } = keyCombo;
          return key === shortcutKey && 
            Object.entries(shortcutModifiers).every(([mod, required]) => 
              modifiers[mod] === required
            );
        });
        
        if (match) {
          if (preventDefault) e.preventDefault();
          action(e);
        }
      });
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [shortcuts]);
};
