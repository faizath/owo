// Mockup classes that mirror Java Tiket classes
class Tiket {
    constructor(id, harga, tersedia) {
        this.id = id;
        this.harga = harga;
        this.tersedia = tersedia;
    }
}

class TiketPesawat extends Tiket {
    constructor(id, harga, tersedia, flightNumber, origin, destination, maskapai, kelas, waktuKeberangkatan) {
        super(id, harga, tersedia);
        this.flightNumber = flightNumber;
        this.maskapai = maskapai;
        this.origin = origin;
        this.destination = destination;
        this.kelas = kelas;
        this.waktuKeberangkatan = waktuKeberangkatan;
    }
}

class TiketHotel extends Tiket {
    constructor(id, harga, tersedia, checkIn, checkOut, hotelName, roomNumber, address) {
        super(id, harga, tersedia);
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.hotelName = hotelName;
        this.roomNumber = roomNumber;
        this.address = address;
    }
}

// Mockup data for destinations
const mockupDestinations = {
    airports: [
        { code: "CGK", name: "Jakarta (CGK)", city: "Jakarta" },
        { code: "DPS", name: "Bali / Denpasar (DPS)", city: "Bali" },
        { code: "SUB", name: "Surabaya (SUB)", city: "Surabaya" },
        { code: "KNO", name: "Kualanamu (KNO)", city: "Medan" }
    ],
    cities: [
        { name: "Jakarta", country: "Indonesia" },
        { name: "Bali", country: "Indonesia" },
        { name: "Surabaya", country: "Indonesia" },
        { name: "Medan", country: "Indonesia" }
    ],
    airlines: [
        { code: "GA", name: "Garuda Indonesia" },
        { code: "JT", name: "Lion Air" },
        { code: "QG", name: "Citilink" }
    ],
    flightClasses: [
        { code: "economy", name: "Ekonomi" },
        { code: "business", name: "Bisnis" },
        { code: "first", name: "First Class" }
    ]
};

// Mockup data for flights
const mockupFlights = [
    // Jakarta - Bali Routes
    new TiketPesawat(
        1,
        1850000,
        true,
        "GA-123",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Garuda Indonesia",
        "Ekonomi",
        new Date("2025-06-12T06:00:00")
    ),
    new TiketPesawat(
        2,
        1250000,
        true,
        "JT-456",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T07:15:00")
    ),
    new TiketPesawat(
        3,
        1350000,
        true,
        "QG-789",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Citilink",
        "Ekonomi",
        new Date("2025-06-12T08:20:00")
    ),
    new TiketPesawat(
        4,
        2500000,
        true,
        "GA-234",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Garuda Indonesia",
        "Bisnis",
        new Date("2025-06-12T09:30:00")
    ),
    new TiketPesawat(
        5,
        1800000,
        true,
        "JT-567",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T10:45:00")
    ),
    new TiketPesawat(
        6,
        3200000,
        true,
        "GA-345",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Garuda Indonesia",
        "First Class",
        new Date("2025-06-12T12:20:00")
    ),
    new TiketPesawat(
        7,
        2200000,
        true,
        "QG-678",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Citilink",
        "Bisnis",
        new Date("2025-06-12T13:15:00")
    ),
    new TiketPesawat(
        8,
        1600000,
        true,
        "JT-789",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T14:30:00")
    ),
    new TiketPesawat(
        9,
        2800000,
        true,
        "GA-456",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Garuda Indonesia",
        "Bisnis",
        new Date("2025-06-12T15:45:00")
    ),
    new TiketPesawat(
        10,
        1900000,
        true,
        "QG-890",
        "Jakarta (CGK)",
        "Bali (DPS)",
        "Citilink",
        "Ekonomi",
        new Date("2025-06-12T16:30:00")
    ),
    // Jakarta - Surabaya Routes
    new TiketPesawat(
        11,
        1500000,
        true,
        "GA-789",
        "Jakarta (CGK)",
        "Surabaya (SUB)",
        "Garuda Indonesia",
        "Ekonomi",
        new Date("2025-06-12T06:30:00")
    ),
    new TiketPesawat(
        12,
        1200000,
        true,
        "JT-012",
        "Jakarta (CGK)",
        "Surabaya (SUB)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T08:15:00")
    ),
    new TiketPesawat(
        13,
        2300000,
        true,
        "GA-345",
        "Jakarta (CGK)",
        "Surabaya (SUB)",
        "Garuda Indonesia",
        "Bisnis",
        new Date("2025-06-12T10:00:00")
    ),
    new TiketPesawat(
        14,
        1400000,
        true,
        "QG-678",
        "Jakarta (CGK)",
        "Surabaya (SUB)",
        "Citilink",
        "Ekonomi",
        new Date("2025-06-12T12:45:00")
    ),
    new TiketPesawat(
        15,
        1600000,
        true,
        "JT-901",
        "Jakarta (CGK)",
        "Surabaya (SUB)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T14:30:00")
    ),
    // Jakarta - Medan Routes
    new TiketPesawat(
        16,
        1800000,
        true,
        "GA-234",
        "Jakarta (CGK)",
        "Medan (KNO)",
        "Garuda Indonesia",
        "Ekonomi",
        new Date("2025-06-12T07:00:00")
    ),
    new TiketPesawat(
        17,
        1500000,
        true,
        "JT-567",
        "Jakarta (CGK)",
        "Medan (KNO)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T09:15:00")
    ),
    new TiketPesawat(
        18,
        2800000,
        true,
        "GA-890",
        "Jakarta (CGK)",
        "Medan (KNO)",
        "Garuda Indonesia",
        "Bisnis",
        new Date("2025-06-12T11:30:00")
    ),
    new TiketPesawat(
        19,
        1900000,
        true,
        "QG-123",
        "Jakarta (CGK)",
        "Medan (KNO)",
        "Citilink",
        "Ekonomi",
        new Date("2025-06-12T13:45:00")
    ),
    new TiketPesawat(
        20,
        2100000,
        true,
        "JT-456",
        "Jakarta (CGK)",
        "Medan (KNO)",
        "Lion Air",
        "Ekonomi",
        new Date("2025-06-12T15:00:00")
    )
];

// Mockup data for hotels
const mockupHotels = [
    new TiketHotel(
        1,
        3329114,
        true,
        new Date('2025-06-12'),
        new Date('2025-06-12'),
        "Nandini Jungle",
        "101",
        "Ubud, Bali, Indonesia"
    ),
    new TiketHotel(
        2,
        1158395,
        true,
        new Date('2025-06-12'),
        new Date('2025-06-12'),
        "Ramayana Suites",
        "201",
        "Ubud, Bali, Indonesia"
    ),
    new TiketHotel(
        3,
        1488858,
        true,
        new Date('2025-06-12'),
        new Date('2025-06-12'),
        "The Garcia Ubud",
        "301",
        "Ubud, Bali, Indonesia"
    )
];

// Helper functions to format data
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('id-ID', {
        style: 'currency',
        currency: 'IDR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(amount);
};

const formatDate = (date) => {
    return new Intl.DateTimeFormat('id-ID', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    }).format(date);
};

const formatTime = (date) => {
    return new Intl.DateTimeFormat('id-ID', {
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
};

// Export the mockup data and helper functions
window.mockupData = {
    flights: mockupFlights,
    hotels: mockupHotels,
    destinations: mockupDestinations,
    formatCurrency,
    formatDate,
    formatTime
}; 