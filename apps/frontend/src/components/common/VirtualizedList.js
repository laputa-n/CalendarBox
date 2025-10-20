import React from 'react';
import { useVirtualization } from '../../hooks/usePerformance';

export const VirtualizedList = ({ 
  items, 
  itemHeight, 
  containerHeight, 
  renderItem,
  className,
  style 
}) => {
  const {
    visibleItems,
    totalHeight,
    offsetY,
    handleScroll
  } = useVirtualization(items, itemHeight, containerHeight);

  return (
    <div
      className={className}
      style={{
        height: containerHeight,
        overflow: 'auto',
        ...style
      }}
      onScroll={handleScroll}
    >
      <div style={{ height: totalHeight, position: 'relative' }}>
        <div style={{ transform: `translateY(${offsetY}px)` }}>
          {visibleItems.map((item, index) => (
            <div
              key={item.id || item.index}
              style={{ height: itemHeight }}
            >
              {renderItem(item, item.index)}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};