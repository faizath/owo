/**
 * OwO Bridge - JavaScript API for communicating with Java backend
 * This file provides convenient methods for the frontend to interact with Java services
 */

// Check if we're running in JavaFX WebView with bridge available
const isJavaFXContext = typeof window.owoBridge !== 'undefined';

// Main OwO API object
window.OwOAPI = {
    
    // Current user session
    currentUser: null,
    
    /**
     * Authentication Methods
     */
    auth: {
        register: function(userData, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - using mock data');
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock registration successful',
                    data: { id: 1, nama: userData.nama, email: userData.email }
                }));
                return;
            }
            
            console.log('Registering user:', userData);
            window.owoAPI.register(userData.nama, userData.email, userData.password, callback);
        },
        
        login: function(email, password, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - using mock data');
                const mockUser = { id: 1, nama: 'Mock User', email: email };
                window.OwOAPI.currentUser = mockUser;
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock login successful',
                    data: mockUser
                }));
                return;
            }
            
            console.log('Logging in user:', email);
            const responseCallback = function(response) {
                try {
                    const result = JSON.parse(response);
                    if (result.success && result.data) {
                        window.OwOAPI.currentUser = JSON.parse(result.data);
                        console.log('User logged in:', window.OwOAPI.currentUser);
                    }
                } catch (e) {
                    console.error('Error parsing login response:', e);
                }
                callback && callback(response);
            };
            
            window.owoAPI.login(email, password, responseCallback);
        },
        
        logout: function() {
            window.OwOAPI.currentUser = null;
            console.log('User logged out');
        },
        
        getCurrentUser: function() {
            return window.OwOAPI.currentUser;
        }
    },
    
    /**
     * Booking Methods
     */
    booking: {
        createHotelBooking: function(bookingData, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - mock hotel booking');
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock hotel booking created',
                    data: { id: Date.now(), transactionId: 'HTL' + Date.now(), ...bookingData }
                }));
                return;
            }
            
            // Ensure customerId is set
            if (!bookingData.customerId && window.OwOAPI.currentUser) {
                bookingData.customerId = window.OwOAPI.currentUser.id;
            }
            
            console.log('Creating hotel booking:', bookingData);
            window.owoAPI.createHotelBooking(bookingData, callback);
        },
        
        createFlightBooking: function(bookingData, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - mock flight booking');
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock flight booking created',
                    data: { id: Date.now(), transactionId: 'FLT' + Date.now(), ...bookingData }
                }));
                return;
            }
            
            // Ensure customerId is set
            if (!bookingData.customerId && window.OwOAPI.currentUser) {
                bookingData.customerId = window.OwOAPI.currentUser.id;
            }
            
            console.log('Creating flight booking:', bookingData);
            window.owoAPI.createFlightBooking(bookingData, callback);
        },
        
        getUserBookings: function(userId, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - mock bookings');
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock bookings retrieved',
                    data: []
                }));
                return;
            }
            
            userId = userId || (window.OwOAPI.currentUser ? window.OwOAPI.currentUser.id : 1);
            console.log('Getting bookings for user:', userId);
            window.owoAPI.getUserBookings(userId, callback);
        }
    },
    
    /**
     * Search Methods
     */
    search: {
        flights: function(searchCriteria, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - returning mockup flights');
                // Return mockup flights if available
                if (typeof window.mockupData !== 'undefined') {
                    const flights = window.mockupData.flights.filter(flight => {
                        const originCode = flight.origin.match(/\(([^)]+)\)/)?.[1];
                        const destCode = flight.destination.match(/\(([^)]+)\)/)?.[1];
                        const matchesRoute = (!searchCriteria.origin || originCode === searchCriteria.origin) &&
                                           (!searchCriteria.destination || destCode === searchCriteria.destination);
                        return matchesRoute;
                    });
                    callback && callback(JSON.stringify({
                        success: true,
                        message: 'Mock flights found',
                        data: flights
                    }));
                } else {
                    callback && callback(JSON.stringify({
                        success: true,
                        message: 'No flights found',
                        data: []
                    }));
                }
                return;
            }
            
            console.log('Searching flights:', searchCriteria);
            window.owoAPI.searchFlights(searchCriteria, callback);
        },
        
        hotels: function(searchCriteria, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - returning mockup hotels');
                // Return mockup hotels if available
                if (typeof window.mockupData !== 'undefined') {
                    const hotels = window.mockupData.hotels.filter(hotel => {
                        const matchesLocation = !searchCriteria.location || 
                                              hotel.address.toLowerCase().includes(searchCriteria.location.toLowerCase());
                        return matchesLocation;
                    });
                    callback && callback(JSON.stringify({
                        success: true,
                        message: 'Mock hotels found',
                        data: hotels
                    }));
                } else {
                    callback && callback(JSON.stringify({
                        success: true,
                        message: 'No hotels found',
                        data: []
                    }));
                }
                return;
            }
            
            console.log('Searching hotels:', searchCriteria);
            window.owoAPI.searchHotels(searchCriteria, callback);
        }
    },
    
    /**
     * Refund Methods
     */
    refund: {
        create: function(refundData, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - mock refund creation');
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock refund created',
                    data: { id: 'REF' + Date.now(), status: 'PENDING_REVIEW', ...refundData }
                }));
                return;
            }
            
            console.log('Creating refund:', refundData);
            window.owoAPI.createRefund(refundData, callback);
        }
    },
    
    /**
     * Check-in Methods
     */
    checkin: {
        perform: function(checkInData, callback) {
            if (!isJavaFXContext) {
                console.warn('Running in browser mode - mock check-in');
                callback && callback(JSON.stringify({
                    success: true,
                    message: 'Mock check-in successful',
                    data: { status: 'CHECKED_IN', checkInTime: new Date().toISOString(), ...checkInData }
                }));
                return;
            }
            
            console.log('Performing check-in:', checkInData);
            window.owoAPI.performCheckIn(checkInData, callback);
        }
    },
    
    /**
     * Utility Methods
     */
    utils: {
        showNotification: function(message) {
            if (isJavaFXContext) {
                window.owoAPI.showNotification(message);
            } else {
                console.log('Notification:', message);
                // You could implement browser notifications here
                if ('Notification' in window && Notification.permission === 'granted') {
                    new Notification('OwO Booking', { body: message });
                }
            }
        },
        
        getVersion: function() {
            if (isJavaFXContext) {
                return window.owoAPI.getVersion();
            } else {
                return 'OwO Browser Version 1.0';
            }
        },
        
        formatCurrency: function(amount) {
            return new Intl.NumberFormat('id-ID', {
                style: 'currency',
                currency: 'IDR',
                minimumFractionDigits: 0,
                maximumFractionDigits: 0
            }).format(amount);
        },
        
        formatDate: function(dateString) {
            if (!dateString) return 'N/A';
            try {
                const date = new Date(dateString);
                return new Intl.DateTimeFormat('id-ID', {
                    weekday: 'short',
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                }).format(date);
            } catch (e) {
                return dateString;
            }
        }
    },
    
    /**
     * Helper method to handle responses consistently
     */
    handleResponse: function(response, successCallback, errorCallback) {
        try {
            const result = JSON.parse(response);
            if (result.success) {
                console.log('Operation successful:', result.message);
                if (successCallback) {
                    const data = typeof result.data === 'string' ? JSON.parse(result.data) : result.data;
                    successCallback(data, result.message);
                }
            } else {
                console.error('Operation failed:', result.message);
                if (errorCallback) {
                    errorCallback(result.message);
                } else {
                    window.OwOAPI.utils.showNotification('Error: ' + result.message);
                }
            }
        } catch (e) {
            console.error('Error parsing response:', e, response);
            if (errorCallback) {
                errorCallback('Invalid response format');
            }
        }
    }
};

// Initialize the API
document.addEventListener('DOMContentLoaded', function() {
    console.log('OwO Bridge API initialized');
    console.log('JavaFX Context:', isJavaFXContext);
    console.log('Available API methods:', Object.keys(window.OwOAPI));
    
    // Set up auto user ID for testing if not in JavaFX context
    if (!isJavaFXContext) {
        window.OwOAPI.currentUser = { id: 1, nama: 'Test User', email: 'test@example.com' };
    }
});

// Export for module systems if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = window.OwOAPI;
} 