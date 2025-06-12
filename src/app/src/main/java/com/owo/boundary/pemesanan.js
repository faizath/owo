// Fungsi untuk menampilkan daftar hotel
function displayHotels(hotels) {
    const hotelListings = document.querySelector('.hotel-listings');
    if (!hotelListings) return;

    hotelListings.innerHTML = hotels.map(hotel => {
        // Map hotel names to their corresponding image files
        let imageName = '';
        switch(hotel.hotelName) {
            case 'Nandini Jungle':
                imageName = 'nandini.png';
                break;
            case 'Ramayana Suites':
                imageName = 'ramayana.png';
                break;
            case 'The Garcia Ubud':
                imageName = 'garcia.png';
                break;
            default:
                imageName = 'ramayana.png';
        }

        return `
            <div class="hotel-card">
                <div class="hotel-image">
                    <img src="../assets/${imageName}" 
                         alt="${hotel.hotelName}"
                         style="width: 100%; height: 100%; object-fit: cover;">
                </div>
                
                <div class="hotel-content">
                    <div class="hotel-header">
                        <h3 class="hotel-title">${hotel.hotelName}</h3>
                        <div class="heart-icon">♡</div>
                    </div>
                    
                    <div class="hotel-stars">
                        ${Array(5).fill('★').join('')}
                    </div>
                    
                    <p class="hotel-location">${hotel.address}</p>
                    
                    <div class="hotel-rating">
                        <span class="rating-score">4.8/5</span>
                        <span class="rating-badge">Location 4.7/5</span>
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
        `;
    }).join('');
}

// Update displayFlights function
function displayFlights(flights) {
    const flightListings = document.querySelector('.flight-listings');
    if (!flightListings) return;

    flightListings.innerHTML = flights.map(flight => {
        // Format flight date and time
        const departureDate = new Date(flight.waktuKeberangkatan);
        const arrivalDate = new Date(flight.waktuKedatangan);
        
        const formatTime = (date) => {
            return date.toLocaleTimeString('id-ID', { 
                hour: '2-digit', 
                minute: '2-digit',
                hour12: false 
            });
        };

        const formatDate = (date) => {
            return date.toLocaleDateString('id-ID', { 
                weekday: 'short',
                day: 'numeric',
                month: 'short'
            });
        };

        // Calculate duration
        const duration = Math.round((arrivalDate - departureDate) / (1000 * 60)); // in minutes
        const hours = Math.floor(duration / 60);
        const minutes = duration % 60;
        const durationText = `${hours}j ${minutes}m`;

        // Format price
        const formattedPrice = new Intl.NumberFormat('id-ID', {
            style: 'currency',
            currency: 'IDR',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(flight.harga);

        return `
            <div class="flight-card">
                <div class="flight-info">
                    <div class="flight-time">
                        <div class="departure">
                            <span class="time">${formatTime(departureDate)}</span>
                            <span class="airport">${flight.origin}</span>
                            <span class="date">${formatDate(departureDate)}</span>
                        </div>
                        <div class="flight-duration">
                            <div class="duration-line"></div>
                            <span class="duration">${durationText}</span>
                        </div>
                        <div class="arrival">
                            <span class="time">${formatTime(arrivalDate)}</span>
                            <span class="airport">${flight.destination}</span>
                            <span class="date">${formatDate(arrivalDate)}</span>
                        </div>
                    </div>
                    <div class="flight-details">
                        <div class="airline">
                            <img src="assets/images/airlines/${flight.maskapai.toLowerCase()}.png" 
                                 alt="${flight.maskapai}"
                            <span>${flight.maskapai}</span>
                        </div>
                        <div class="flight-number">${flight.nomorPenerbangan}</div>
                        <div class="flight-class">${flight.kelas}</div>
                    </div>
                </div>
                <div class="flight-price">
                    <div class="price">${formattedPrice}</div>
                    <button class="book-btn" onclick="bookFlight('${flight.nomorPenerbangan}')">
                        Pilih
                    </button>
                </div>
            </div>
        `;
    }).join('');

    // Add event listeners to book buttons
    document.querySelectorAll('.book-btn').forEach(button => {
        button.addEventListener('click', (e) => {
            const flightCard = e.target.closest('.flight-card');
            const flightNumber = flightCard.querySelector('.flight-number').textContent;
            bookFlight(flightNumber);
        });
    });
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

    // Show loading modal first
    showModal('hotel', () => {
        // Get available hotels from mockup data
        const hotels = window.mockupData.hotels;
        console.log('Available hotels:', hotels);

        if (hotels && hotels.length > 0) {
            // Filter hotels based on search criteria
            const filteredHotels = hotels.filter(hotel => {
                // Format hotel dates to match search date format (YYYY-MM-DD)
                const hotelCheckIn = new Date(hotel.checkIn);
                const hotelCheckOut = new Date(hotel.checkOut);
                const searchCheckIn = new Date(checkin);
                const searchCheckOut = new Date(checkout);
                
                // Check if hotel location matches destination
                const hotelLocation = hotel.address.toLowerCase();
                const searchLocation = destination.toLowerCase();
                const matchesLocation = hotelLocation.includes(searchLocation);
                
                // Check if dates are available
                const matchesDates = hotelCheckIn <= searchCheckIn && hotelCheckOut >= searchCheckOut;
                
                console.log('Hotel check:', {
                    hotel: hotel.hotelName,
                    location: hotel.address,
                    searchLocation: destination,
                    matchesLocation,
                    hotelCheckIn: hotelCheckIn.toISOString().split('T')[0],
                    hotelCheckOut: hotelCheckOut.toISOString().split('T')[0],
                    searchCheckIn: searchCheckIn.toISOString().split('T')[0],
                    searchCheckOut: searchCheckOut.toISOString().split('T')[0],
                    matchesDates
                });

                return matchesLocation && matchesDates;
            });

            console.log('Filtered hotels:', filteredHotels);

            if (filteredHotels.length > 0) {
                displayHotels(filteredHotels);
                const hotelListings = document.querySelector('.hotel-listings');
                if (hotelListings) {
                    hotelListings.classList.add('show');
                    hotelListings.scrollIntoView({ behavior: 'smooth' });
                }
            } else {
                showModal('hotel_not_found', () => {
                    // Stay on the same page if no hotels found
                });
            }
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

    // Show loading modal first
    showModal('flight', () => {
        // Get available flights from mockup data
        const flights = window.mockupData.flights;
        console.log('Available flights:', flights);

        if (flights && flights.length > 0) {
            // Filter flights based on search criteria
            const filteredFlights = flights.filter(flight => {
                // Format flight date to match search date format (YYYY-MM-DD)
                const flightDate = new Date(flight.waktuKeberangkatan);
                const formattedFlightDate = flightDate.toISOString().split('T')[0];
                
                // Extract airport codes from the flight data
                const originCode = flight.origin.match(/\(([^)]+)\)/)?.[1];
                const destCode = flight.destination.match(/\(([^)]+)\)/)?.[1];
                
                // Compare with search criteria
                const matchesRoute = originCode === searchData.from && destCode === searchData.to;
                const matchesDate = formattedFlightDate === searchData.departure;
                const matchesClass = !searchData.flightClass || flight.kelas.toLowerCase() === searchData.flightClass.toLowerCase();

                console.log('Flight check:', {
                    flight: flight.origin + ' -> ' + flight.destination,
                    originCode,
                    destCode,
                    from: searchData.from,
                    to: searchData.to,
                    matchesRoute,
                    matchesDate,
                    matchesClass,
                    flightDate: formattedFlightDate,
                    searchDate: searchData.departure,
                    flightClass: flight.kelas,
                    searchClass: searchData.flightClass
                });

                return matchesRoute && matchesDate && matchesClass;
            });

            console.log('Filtered flights:', filteredFlights);

            if (filteredFlights.length > 0) {
                displayFlights(filteredFlights);
                const flightListings = document.querySelector('.flight-listings');
                if (flightListings) {
                    flightListings.classList.add('show');
                    flightListings.scrollIntoView({ behavior: 'smooth' });
                }
            } else {
                showModal('flight_not_found', () => {
                    // Stay on the same page if no flights found
                });
            }
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

// Function to populate destinations
function populateDestinations() {
    const departureSelect = document.getElementById('departure');
    const arrivalSelect = document.getElementById('arrival');
    const destinationSelect = document.getElementById('destination-input');
    const flightClassSelect = document.getElementById('flightClass');

    if (!departureSelect || !arrivalSelect || !destinationSelect || !flightClassSelect) {
        console.warn('Destination select elements not found');
        return;
    }

    // Clear existing options
    departureSelect.innerHTML = '<option value="">Pilih Bandara Keberangkatan</option>';
    arrivalSelect.innerHTML = '<option value="">Pilih Bandara Tujuan</option>';
    destinationSelect.innerHTML = '<option value="">Pilih Kota Tujuan</option>';
    flightClassSelect.innerHTML = '<option value="">Pilih Kelas</option>';

    // Add airports to departure and arrival selects
    window.mockupData.destinations.airports.forEach(airport => {
        const option = document.createElement('option');
        option.value = `${airport.city} (${airport.code})`;
        option.textContent = `${airport.city} (${airport.code})`;
        departureSelect.appendChild(option.cloneNode(true));
        arrivalSelect.appendChild(option);
    });

    // Add cities to hotel destination select
    window.mockupData.destinations.cities.forEach(city => {
        const option = document.createElement('option');
        option.value = city.name;
        option.textContent = `${city.name}, ${city.country}`;
        destinationSelect.appendChild(option);
    });

    // Add flight classes
    window.mockupData.destinations.flightClasses.forEach(flightClass => {
        const option = document.createElement('option');
        option.value = flightClass.name.toLowerCase();
        option.textContent = flightClass.name;
        flightClassSelect.appendChild(option);
    });
}

// Fungsi untuk setup event listeners
function setupEventListeners() {
    // Populate destinations when the page loads
    populateDestinations();

    // Add event listeners for form submissions
    const flightForm = document.getElementById('flightForm');
    const hotelForm = document.getElementById('hotelForm');

    if (flightForm) {
        flightForm.addEventListener('submit', handleFlightSearch);
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

// Fungsi untuk menampilkan modal
function showModal(type, callbackOnSuccess) {
    const modal = document.getElementById('modalOverlay');
    const title = document.getElementById('modalTitle');
    const message = document.getElementById('modalMessage');
    const buttons = document.getElementById('modalButtons');
    const icon = document.getElementById('modalIcon');

    if (!modal || !title || !message || !buttons || !icon) {
        console.warn('Modal elements not found, skipping modal');
        if (callbackOnSuccess) callbackOnSuccess();
        return;
    }

    modal.classList.add('active');

    switch(type) {
        case 'hotel':
            title.textContent = 'Mencari Hotel...';
            message.textContent = 'Mohon tunggu sebentar, kami sedang mencari hotel yang tersedia...';
            icon.textContent = '⏳';
            icon.className = 'modal-icon loading';
            break;
        case 'hotel_not_found':
            title.textContent = 'Kamar Tidak Tersedia';
            message.textContent = 'Maaf, tidak ada kamar yang tersedia untuk kriteria pencarian Anda. Silakan coba dengan tanggal atau lokasi yang berbeda.';
            icon.textContent = '❌';
            icon.className = 'modal-icon error';
            break;
        case 'flight':
            title.textContent = 'Mencari Penerbangan...';
            message.textContent = 'Mohon tunggu sebentar, kami sedang mencari penerbangan yang tersedia...';
            icon.textContent = '⏳';
            icon.className = 'modal-icon loading';
            break;
        case 'flight_not_found':
            title.textContent = 'Penerbangan Tidak Tersedia';
            message.textContent = 'Maaf, tidak ada penerbangan yang tersedia untuk kriteria pencarian Anda. Silakan coba dengan tanggal atau rute yang berbeda.';
            icon.textContent = '❌';
            icon.className = 'modal-icon error';
            break;
        case 'booking_success':
            title.textContent = 'Pesanan Berhasil';
            message.textContent = 'Pesanan Anda telah berhasil diproses. Silakan lakukan pembayaran untuk menyelesaikan transaksi.';
            icon.textContent = '✅';
            icon.className = 'modal-icon success';
            break;
    }

    buttons.innerHTML = '';

    if (type === 'hotel' || type === 'flight') {
        setTimeout(() => {
            if (callbackOnSuccess) {
                callbackOnSuccess();
            }
        }, 2000);
    } else {
        buttons.innerHTML = `
            <button class="modal-button primary" onclick="closeModal()">OK</button>
        `;
    }
}

function closeModal() {
    const modal = document.getElementById('modalOverlay');
    if (modal) modal.classList.remove('active');
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
function bookFlight(flightNumber) {
    // Get flight data from mockup data
    const flights = window.mockupData.flights;
    const flight = flights.find(f => f.nomorPenerbangan === flightNumber);
    
    if (!flight) {
        console.error('Flight not found:', flightNumber);
        return;
    }

    // Get form data
    const passengers = document.querySelector('#flightForm #passengers')?.value || '1';
    const flightClass = document.querySelector('#flightForm #flightClass')?.value || 'Economy';

    // Calculate total price
    const totalPrice = flight.harga * parseInt(passengers);

    // Format price
    const formattedPrice = new Intl.NumberFormat('id-ID', {
        style: 'currency',
        currency: 'IDR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(totalPrice);

    // Format dates
    const formatDate = (date) => {
        return new Date(date).toLocaleDateString('id-ID', {
            weekday: 'long',
            day: 'numeric',
            month: 'long',
            year: 'numeric'
        });
    };

    const formatTime = (date) => {
        return new Date(date).toLocaleTimeString('id-ID', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    };

    // Create booking data
    const bookingData = {
        flightNumber: flight.nomorPenerbangan,
        airline: flight.maskapai,
        origin: flight.origin,
        destination: flight.destination,
        departureDate: formatDate(flight.waktuKeberangkatan),
        departureTime: formatTime(flight.waktuKeberangkatan),
        arrivalDate: formatDate(flight.waktuKedatangan),
        arrivalTime: formatTime(flight.waktuKedatangan),
        passengers: passengers,
        flightClass: flightClass,
        totalPrice: totalPrice,
        formattedPrice: formattedPrice
    };

    // Save booking data
    console.log('Saving flight booking data:', bookingData);
    sendDataToParent('save_data', 'flight_booking', bookingData);

    // Show success message
    showModal('booking_success', () => {
        // Redirect to payment page or show payment form
        console.log('Redirecting to payment...');
        // You can add your payment redirection logic here
    });
} 