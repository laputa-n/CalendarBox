export const measurePerformance = (name, fn) => {
  return async (...args) => {
    const start = performance.now();
    const result = await fn(...args);
    const end = performance.now();
    
    console.log(`${name} took ${end - start} milliseconds`);
    
    // Performance API 사용
    if (window.performance && window.performance.mark) {
      window.performance.mark(`${name}-start`);
      window.performance.mark(`${name}-end`);
      window.performance.measure(name, `${name}-start`, `${name}-end`);
    }
    
    return result;
  };
};

export const memoizeWithTTL = (fn, ttl = 5 * 60 * 1000) => {
  const cache = new Map();
  
  return (...args) => {
    const key = JSON.stringify(args);
    const now = Date.now();
    
    if (cache.has(key)) {
      const { value, timestamp } = cache.get(key);
      if (now - timestamp < ttl) {
        return value;
      }
      cache.delete(key);
    }
    
    const value = fn(...args);
    cache.set(key, { value, timestamp: now });
    
    return value;
  };
};

export const batchRequests = (fn, delay = 100) => {
  let timeoutId;
  let batch = [];
  
  return (...args) => {
    batch.push(args);
    
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    
    timeoutId = setTimeout(() => {
      fn(batch);
      batch = [];
    }, delay);
  };
};