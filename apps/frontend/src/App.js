import React from 'react';
import { ErrorProvider } from './contexts/ErrorContext';
import { AuthProvider } from './contexts/AuthContext';
import { CalendarProvider } from './contexts/CalendarContext';
import { ScheduleProvider } from './contexts/ScheduleContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { FriendProvider } from './contexts/FriendContext';
import { AppRouter } from './components/AppRouter';


const App = () => {
  return (
    <div style={{
      fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
    }}>
        <ErrorProvider>
          <AuthProvider>
            <CalendarProvider>
              <ScheduleProvider>
                <NotificationProvider>
                  <FriendProvider>
                    <AppRouter />
                  </FriendProvider>
                </NotificationProvider>
              </ScheduleProvider>
            </CalendarProvider>
          </AuthProvider>
        </ErrorProvider>
    </div>
  );
};

export default App;