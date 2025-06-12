// Fungsi untuk menampilkan daftar hotel
function displayHotels(hotels) {
    const hotelListings = document.querySelector('.hotel-listings');
    if (!hotelListings) return;

    hotelListings.innerHTML = hotels.map(hotel => `
        <div class="hotel-card">
            <div class="hotel-image">
                <img src="../assets/${hotel.hotelName.toLowerCase().replace(/\s+/g, '-')}.png" alt="${hotel.hotelName}">
            </div>
            
            <div class="hotel-content">
                <div class="hotel-header">
                    <h3 class="hotel-title">${hotel.hotelName}</h3>
                    <div class="heart-icon">♡</div>
                </div>
                
                <div class="hotel-stars">
                    <span class="star">★</span>
                    <span class="star">★</span>
                    <span class="star">★</span>
                    <span class="star">★</span>
                    <span class="star">★</span>
                </div>
                
                <p class="hotel-location">${hotel.address}</p>
                
                <div class="hotel-rating">
                    <span class="rating-score">4.6/5</span>
                    <span class="rating-badge">Location 4.5/5</span>
                </div>
                
                <div class="hotel-footer">
                    <div class="hotel-price">
                        <span class="price-amount">${window.mockupData.formatCurrency(hotel.harga)}</span>
                        <span class="price-period">per malam</span>
                    </div>
                    <button class="book-button" onclick="bookHotel('${hotel.hotelName}')">Pesan Sekarang</button>
                </div>
            </div>
        </div>
    `).join('');
}

// Fungsi untuk menampilkan daftar penerbangan
function displayFlights(flights) {
    const flightListings = document.querySelector('.flight-listings');
    if (!flightListings) return;

    flightListings.innerHTML = flights.map(flight => `
        <div class="flight-card">
            <div class="flight-content">
                <div class="flight-header">
                    <h3 class="flight-title">${flight.maskapai}</h3>
                    <div class="heart-icon">♡</div>
                </div>
                
                <div class="flight-details">
                    <div class="flight-time">${window.mockupData.formatTime(flight.waktuKeberangkatan)}</div>
                    <div class="flight-duration">
                        <div class="flight-path"></div>
                        <span>2h 30m</span>
                    </div>
                    <div class="flight-time">${window.mockupData.formatTime(new Date(flight.waktuKeberangkatan.getTime() + 150 * 60000))}</div>
                </div>
                
                <div class="flight-info">
                    <div class="flight-info-item">
                        <span class="flight-info-label">Dari</span>
                        <span class="flight-info-value">${flight.origin}</span>
                    </div>
                    <div class="flight-info-item">
                        <span class="flight-info-label">Ke</span>
                        <span class="flight-info-value">${flight.destination}</span>
                    </div>
                    <div class="flight-info-item">
                        <span class="flight-info-label">Tanggal</span>
                        <span class="flight-info-value">${window.mockupData.formatDate(flight.waktuKeberangkatan)}</span>
                    </div>
                    <div class="flight-info-item">
                        <span class="flight-info-label">Kelas</span>
                        <span class="flight-info-value">${flight.kelas}</span>
                    </div>
                </div>
                
                <div class="flight-rating">
                    <span class="rating-score">4.8/5</span>
                    <span class="rating-badge">Direct Flight</span>
                </div>
                
                <div class="flight-footer">
                    <div class="flight-price">
                        <span class="price-amount">${window.mockupData.formatCurrency(flight.harga)}</span>
                        <span class="price-period">per orang</span>
                    </div>
                    <button class="book-button" onclick="bookFlight('${flight.maskapai}')">Pesan Sekarang</button>
                </div>
            </div>
        </div>
    `).join('');
}

// Update handleHotelSearch function
function handleHotelSearch(event) {
    event.preventDefault();
    
    const destination = document.querySelector('#hotelForm #destination-input')?.value;
    const checkin = document.querySelector('#hotelForm #checkin-input')?.value;
    const checkout = document.querySelector('#hotelForm #checkout-input')?.value;
    const guests = document.querySelector('#hotelForm #guests-input')?.value;

    if (!destination || !checkin || !checkout) {
        alert('Mohon lengkapi tujuan dan tanggal untuk pencarian hotel.');
        return;
    }

    const searchData = { destination, checkin, checkout, guests };
    console.log('Saving hotel search data:', searchData);
    sendDataToParent('save_data', 'hotel_search', searchData);

    // Get available hotels
    getDataFromParent('hotel_search', (hotels) => {
        if (hotels && hotels.length > 0) {
            showModal('hotel', () => {
                displayHotels(hotels);
                const hotelListings = document.querySelector('.hotel-listings');
                if (hotelListings) {
                    hotelListings.classList.add('show');
                    hotelListings.scrollIntoView({ behavior: 'smooth' });
                }
            });
        } else {
            showModal('hotel_not_found', () => {
                // Stay on the same page if no hotels found
            });
        }
    });
}

// Update handleFlightSearch function
function handleFlightSearch(event) {
    event.preventDefault();
    
    const from = document.querySelector('#flightForm #departure')?.value;
    const to = document.querySelector('#flightForm #arrival')?.value;
    const departure = document.querySelector('#flightForm #departureDate')?.value;
    const passengers = document.querySelector('#flightForm #passengers')?.value;
    const flightClass = document.querySelector('#flightForm #flightClass')?.value;

    if (!from || !to || !departure) {
        alert('Mohon lengkapi semua field untuk pencarian pesawat.');
        return;
    }

    const searchData = { from, to, departure, passengers, flightClass };
    console.log('Saving flight search data:', searchData);
    sendDataToParent('save_data', 'flight_search', searchData);

    // Get available flights
    getDataFromParent('flight_search', (flights) => {
        if (flights && flights.length > 0) {
            showModal('flight', () => {
                displayFlights(flights);
                const flightListings = document.querySelector('.flight-listings');
                if (flightListings) {
                    flightListings.classList.add('show');
                    flightListings.scrollIntoView({ behavior: 'smooth' });
                }
            });
        } else {
            showModal('flight_not_found', () => {
                // Stay on the same page if no flights found
            });
        }
    });
}

// Update getDataFromParent function
function getDataFromParent(type, callback, timeout = 5000) {
    console.log('Requesting data from parent:', type);
    
    if (window.parent && window.parent !== window) {
        let responseReceived = false;
        
        const responseListener = function(event) {
            console.log('Response received:', event.data);
            if (event.data.action === 'data_response' && event.data.type === type) {
                responseReceived = true;
                callback(event.data.data);
                window.removeEventListener('message', responseListener);
            }
        };
        
        window.addEventListener('message', responseListener);
        
        window.parent.postMessage({
            action: 'get_data',
            type: type
        }, '*');
        
        setTimeout(() => {
            if (!responseReceived) {
                console.warn('Timeout waiting for data from parent');
                window.removeEventListener('message', responseListener);
                callback(null);
            }
        }, timeout);
    } else {
        console.warn('Not in iframe, using mockup data');
        if (type === 'flight_search') {
            // Get the search parameters from the form
            const from = document.querySelector('#flightForm #departure')?.value;
            const to = document.querySelector('#flightForm #arrival')?.value;
            const departure = document.querySelector('#flightForm #departureDate')?.value;
            const flightClass = document.querySelector('#flightForm #flightClass')?.value;

            console.log('Filtering flights with criteria:', { from, to, departure, flightClass });

            // Filter flights based on search criteria
            const filteredFlights = window.mockupData.flights.filter(flight => {
                const flightDate = flight.waktuKeberangkatan.toISOString().split('T')[0];
                
                // Extract airport codes from the flight data
                const originCode = flight.origin.match(/\(([^)]+)\)/)?.[1];
                const destCode = flight.destination.match(/\(([^)]+)\)/)?.[1];
                
                // Compare with search criteria
                const matchesRoute = originCode === from && destCode === to;
                const matchesDate = flightDate === departure;
                const matchesClass = !flightClass || 
                    flight.kelas.toLowerCase() === flightClass.toLowerCase() ||
                    (flightClass.toLowerCase() === 'economy' && flight.kelas === 'Ekonomi') ||
                    (flightClass.toLowerCase() === 'ekonomi' && flight.kelas === 'Ekonomi');

                console.log('Flight check:', {
                    flight: flight.origin + ' -> ' + flight.destination,
                    originCode,
                    destCode,
                    from,
                    to,
                    matchesRoute,
                    matchesDate,
                    matchesClass
                });

                return matchesRoute && matchesDate && matchesClass;
            });

            console.log('Filtered flights:', filteredFlights);
            callback(filteredFlights);
        } else {
            callback(window.mockupData.hotels);
        }
    }
}

// Fungsi untuk mengisi dropdown destinasi
function populateDestinations() {
    const departureSelect = document.querySelector('#flightForm #departure');
    const arrivalSelect = document.querySelector('#flightForm #arrival');
    const destinationInput = document.querySelector('#hotelForm #destination-input');
    const flightClassSelect = document.querySelector('#flightForm #flightClass');

    if (departureSelect && arrivalSelect) {
        const airports = window.mockupData.destinations.airports;
        const options = airports.map(airport => 
            `<option value="${airport.name}">${airport.name}</option>`
        ).join('');
        
        departureSelect.innerHTML = options;
        arrivalSelect.innerHTML = options;
    }

    if (destinationInput) {
        const cities = window.mockupData.destinations.cities;
        const options = cities.map(city => 
            `<option value="${city.name}">${city.name}, ${city.country}</option>`
        ).join('');
        
        destinationInput.innerHTML = options;
    }

    if (flightClassSelect) {
        const classes = window.mockupData.destinations.flightClasses;
        const options = classes.map(cls => 
            `<option value="${cls.code}">${cls.name}</option>`
        ).join('');
        
        flightClassSelect.innerHTML = options;
    }
}

function setupEventListeners() {
    // Populate destinations when the page loads
    populateDestinations();

    // Add event listeners for form submissions
    const flightForm = document.getElementById('flightForm');
    const hotelForm = document.getElementById('hotelForm');

    if (flightForm) {
        flightForm.addEventListener('submit', function(event) {
            event.preventDefault();
            
            const fromSelect = document.querySelector('#flightForm #departure');
            const toSelect = document.querySelector('#flightForm #arrival');
            const departure = document.querySelector('#flightForm #departureDate')?.value;
            const passengers = document.querySelector('#flightForm #passengers')?.value;
            const flightClass = document.querySelector('#flightForm #flightClass')?.value;

            if (!fromSelect?.value || !toSelect?.value || !departure) {
                alert('Mohon lengkapi semua field untuk pencarian pesawat.');
                return;
            }

            // Extract airport codes from the selected values
            const fromCode = fromSelect.value.match(/\(([^)]+)\)/)?.[1];
            const toCode = toSelect.value.match(/\(([^)]+)\)/)?.[1];

            if (!fromCode || !toCode) {
                alert('Format bandara tidak valid.');
                return;
            }

            const searchData = { 
                from: fromCode,
                to: toCode,
                departure, 
                passengers, 
                flightClass
            };
            
            console.log('Saving flight search data:', searchData);
            sendDataToParent('save_data', 'flight_search', searchData);

            // Get available flights
            getDataFromParent('flight_search', (flights) => {
                if (flights && flights.length > 0) {
                    showModal('flight', () => {
                        displayFlights(flights);
                        const flightListings = document.querySelector('.flight-listings');
                        if (flightListings) {
                            flightListings.classList.add('show');
                            flightListings.scrollIntoView({ behavior: 'smooth' });
                        }
                    });
                } else {
                    showModal('flight_not_found', () => {
                        // Stay on the same page if no flights found
                    });
                }
            });
        });
    }

    if (hotelForm) {
        hotelForm.addEventListener('submit', handleHotelSearch);
    }

    // Add event listeners for tabs
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            // Remove active class from all tabs
            tabs.forEach(t => t.classList.remove('active'));
            // Add active class to clicked tab
            this.classList.add('active');

            // Show/hide forms based on selected tab
            if (this.dataset.tab === 'flight') {
                flightForm.style.display = 'block';
                hotelForm.style.display = 'none';
            } else {
                flightForm.style.display = 'none';
                hotelForm.style.display = 'block';
            }
        });
    });
}

// Initialize event listeners when DOM is loaded
document.addEventListener('DOMContentLoaded', setupEventListeners);

// Update showModal function
function showModal(type, callbackOnSuccess) {
    const modal = document.getElementById('modalOverlay');
    const title = document.getElementById('modalTitle');
    const message = document.getElementById('modalMessage');
    const buttons = document.getElementById('modalButtons');

    if (!modal || !title || !message || !buttons) {
        console.warn('Modal elements not found, skipping modal');
        if (callbackOnSuccess) callbackOnSuccess();
        return;
    }

    modal.classList.add('active');

    switch(type) {
        case 'hotel':
            title.textContent = 'Mengecek Kamar...';
            message.textContent = 'Mohon tunggu sebentar...';
            break;
        case 'hotel_not_found':
            title.textContent = 'Kamar Tidak Tersedia';
            message.textContent = 'Maaf, tidak ada kamar yang tersedia untuk kriteria pencarian Anda. Silakan coba dengan tanggal atau lokasi yang berbeda.';
            break;
        case 'flight':
            title.textContent = 'Mengecek Penerbangan...';
            message.textContent = 'Mohon tunggu sebentar...';
            break;
        case 'flight_not_found':
            title.textContent = 'Penerbangan Tidak Tersedia';
            message.textContent = 'Maaf, tidak ada penerbangan yang tersedia untuk kriteria pencarian Anda. Silakan coba dengan tanggal atau rute yang berbeda.';
            break;
    }

    buttons.innerHTML = '';

    setTimeout(() => {
        if (type === 'hotel' || type === 'flight') {
            title.textContent = `${type === 'hotel' ? 'Kamar' : 'Penerbangan'} Tersedia!`;
            message.textContent = 'Hasil pencarian akan ditampilkan.';
            buttons.innerHTML = `<button class="modal-button primary" onclick="closeModal()">OK</button>`;
            
            setTimeout(() => {
                closeModal();
                if (callbackOnSuccess) {
                    callbackOnSuccess();
                }
            }, 1000);
        } else {
            buttons.innerHTML = `
                <button class="modal-button primary" onclick="closeModal()">OK</button>
            `;
        }
    }, 1500);
}

// Update bookHotel function
function bookHotel(hotelName) {
    console.log('Booking hotel:', hotelName);
    
    getDataFromParent('hotel_search', function(searchData) {
        const bookingData = {
            hotelName: hotelName,
            bookingType: 'hotel',
            ...(searchData || {})
        };
        
        console.log('Saving hotel booking:', bookingData);
        sendDataToParent('save_data', 'hotel_booking', bookingData);
        
        // Redirect to hotel availability page
        window.location.href = 'CekKetersediaanHotel.html';
    });
}

// Update bookFlight function
function bookFlight(flightName) {
    console.log('Booking flight:', flightName);
    
    getDataFromParent('flight_search', function(searchData) {
        const bookingData = {
            flightName: flightName,
            bookingType: 'flight',
            ...(searchData || {})
        };
        
        console.log('Saving flight booking:', bookingData);
        sendDataToParent('save_data', 'flight_booking', bookingData);
        
        // Redirect to flight availability page
        window.location.href = 'CekKetersediaanPesawat.html';
    });
} 