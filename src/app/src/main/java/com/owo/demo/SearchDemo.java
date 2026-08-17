package com.owo.demo;

import com.owo.dao.TiketDAO;
import com.owo.entity.TiketPesawat;
import com.owo.entity.TiketHotel;
import com.owo.utils.DBHelper;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SearchDemo {
    
    public static void main(String[] args) {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("=== OWO Search Demo ===");
            System.out.println("SQLite JDBC driver loaded successfully");
            
            // Initialize database
            DBHelper.initializeDatabase();
            
            // Demonstrate flight search functionality
            demonstrateFlightSearch();
            
            // Demonstrate hotel search functionality
            demonstrateHotelSearch();
            
        } catch (ClassNotFoundException e) {
            System.err.println("Error: SQLite JDBC driver not found (" + e.getClass().getSimpleName()
                    + "): " + e.getMessage());
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database error (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void demonstrateFlightSearch() throws SQLException {
        System.out.println("\n=== FLIGHT SEARCH DEMONSTRATION ===");
        
        // 1. Search all available flights
        System.out.println("\n1. All Available Flights:");
        List<TiketPesawat> allFlights = TiketDAO.getAllAvailableFlights();
        System.out.println("Found " + allFlights.size() + " available flights");
        for (int i = 0; i < Math.min(3, allFlights.size()); i++) {
            TiketPesawat flight = allFlights.get(i);
            System.out.println("   • " + flight.getFlightNumber() + " - " + flight.getMaskapai() + 
                             " (" + flight.getOrigin() + " → " + flight.getDestination() + ") - " + 
                             flight.getKelas() + " - Rp " + String.format("%.0f", flight.getHarga()));
        }
        if (allFlights.size() > 3) {
            System.out.println("   ... and " + (allFlights.size() - 3) + " more flights");
        }
        
        // 2. Search flights by destination
        System.out.println("\n2. Flights to Bali:");
        List<TiketPesawat> baliFlights = TiketDAO.searchTiketPesawat(null, "Bali", null, true);
        System.out.println("Found " + baliFlights.size() + " flights to Bali");
        for (TiketPesawat flight : baliFlights.subList(0, Math.min(3, baliFlights.size()))) {
            System.out.println("   • " + flight.getFlightNumber() + " - " + flight.getMaskapai() + 
                             " (" + flight.getOrigin() + " → " + flight.getDestination() + ") - " + 
                             "Rp " + String.format("%.0f", flight.getHarga()));
        }
        
        // 3. Search flights by origin and destination
        System.out.println("\n3. Flights from Jakarta to Surabaya:");
        List<TiketPesawat> jakartaSurabaya = TiketDAO.searchTiketPesawat("Jakarta", "Surabaya", null, true);
        System.out.println("Found " + jakartaSurabaya.size() + " flights from Jakarta to Surabaya");
        for (TiketPesawat flight : jakartaSurabaya) {
            System.out.println("   • " + flight.getFlightNumber() + " - " + flight.getMaskapai() + 
                             " - " + flight.getKelas() + " - Rp " + String.format("%.0f", flight.getHarga()));
        }
        
        // 4. Search flights by class
        System.out.println("\n4. Business Class Flights:");
        List<TiketPesawat> businessFlights = TiketDAO.searchTiketPesawat(null, null, "Bisnis", true);
        System.out.println("Found " + businessFlights.size() + " business class flights");
        for (TiketPesawat flight : businessFlights.subList(0, Math.min(3, businessFlights.size()))) {
            System.out.println("   • " + flight.getFlightNumber() + " - " + flight.getMaskapai() + 
                             " (" + flight.getOrigin() + " → " + flight.getDestination() + ") - " + 
                             "Rp " + String.format("%.0f", flight.getHarga()));
        }
        
        // 5. Search flights by airline (using destination filter with airline name)
        System.out.println("\n5. Garuda Indonesia Flights:");
        List<TiketPesawat> garudaFlights = TiketDAO.searchTiketPesawat(null, null, null, true);
        int garudaCount = 0;
        for (TiketPesawat flight : garudaFlights) {
            if (flight.getMaskapai().toLowerCase().contains("garuda")) {
                if (garudaCount < 3) {
                    System.out.println("   • " + flight.getFlightNumber() + " - " + flight.getMaskapai() + 
                                     " (" + flight.getOrigin() + " → " + flight.getDestination() + ") - " + 
                                     flight.getKelas() + " - Rp " + String.format("%.0f", flight.getHarga()));
                }
                garudaCount++;
            }
        }
        System.out.println("Found " + garudaCount + " Garuda Indonesia flights");
    }
    
    private static void demonstrateHotelSearch() throws SQLException {
        System.out.println("\n=== HOTEL SEARCH DEMONSTRATION ===");
        
        // 1. Search all available hotels
        System.out.println("\n1. All Available Hotels:");
        List<TiketHotel> allHotels = TiketDAO.getAllAvailableHotels();
        System.out.println("Found " + allHotels.size() + " available hotels");
        for (TiketHotel hotel : allHotels) {
            System.out.println("   • " + hotel.getHotelName() + " - Room " + hotel.getRoomNumber() + 
                             " (" + hotel.getAddress() + ") - Rp " + String.format("%.0f", hotel.getHarga()));
        }
        
        // 2. Search hotels by location
        System.out.println("\n2. Hotels in Ubud:");
        List<TiketHotel> ubudHotels = TiketDAO.searchTiketHotel("Ubud", null, null, null, true);
        System.out.println("Found " + ubudHotels.size() + " hotels in Ubud");
        for (TiketHotel hotel : ubudHotels) {
            System.out.println("   • " + hotel.getHotelName() + " - Room " + hotel.getRoomNumber() + 
                             " - Rp " + String.format("%.0f", hotel.getHarga()));
        }
        
        // 3. Search hotels by name
        System.out.println("\n3. Hotels with 'Nandini' in name:");
        List<TiketHotel> nandiniHotels = TiketDAO.searchTiketHotel(null, null, null, "Nandini", true);
        System.out.println("Found " + nandiniHotels.size() + " hotels with 'Nandini' in name");
        for (TiketHotel hotel : nandiniHotels) {
            System.out.println("   • " + hotel.getHotelName() + " - Room " + hotel.getRoomNumber() + 
                             " (" + hotel.getAddress() + ") - Rp " + String.format("%.0f", hotel.getHarga()));
        }
        
        // 4. Search hotels by date range
        LocalDate searchDate = LocalDate.now();
        System.out.println("\n4. Hotels available from " + searchDate + ":");
        List<TiketHotel> dateFilteredHotels = TiketDAO.searchTiketHotel(null, searchDate, null, null, true);
        System.out.println("Found " + dateFilteredHotels.size() + " hotels available from " + searchDate);
        for (TiketHotel hotel : dateFilteredHotels) {
            System.out.println("   • " + hotel.getHotelName() + " - Check-in: " + hotel.getCheckIn() + 
                             ", Check-out: " + hotel.getCheckOut() + " - Rp " + String.format("%.0f", hotel.getHarga()));
        }
        
        // 5. Complex search: Hotels in Bali with specific criteria
        System.out.println("\n5. Hotels in Bali (complex search):");
        List<TiketHotel> baliHotels = TiketDAO.searchTiketHotel("Bali", null, null, null, true);
        System.out.println("Found " + baliHotels.size() + " hotels in Bali");
        for (TiketHotel hotel : baliHotels) {
            System.out.println("   • " + hotel.getHotelName() + " - Room " + hotel.getRoomNumber() + 
                             " (" + hotel.getAddress() + ") - Rp " + String.format("%.0f", hotel.getHarga()));
        }
    }
    
    /**
     * Utility method to format currency
     */
    private static String formatCurrency(float amount) {
        return String.format("Rp %.0f", amount);
    }
    
    /**
     * Print search tips for users
     */
    public static void printSearchTips() {
        System.out.println("\n=== SEARCH TIPS ===");
        System.out.println("Flight Search Parameters:");
        System.out.println("  - origin: Filter by departure city (e.g., 'Jakarta', 'CGK')");
        System.out.println("  - destination: Filter by arrival city (e.g., 'Bali', 'DPS')");
        System.out.println("  - kelas: Filter by flight class ('Ekonomi', 'Bisnis', 'First Class')");
        System.out.println("  - passengers: Number of passengers (for future seat availability)");
        System.out.println("  - tersediaOnly: true/false - only show available tickets");
        
        System.out.println("\nHotel Search Parameters:");
        System.out.println("  - location: Filter by location/address (e.g., 'Ubud', 'Bali')");
        System.out.println("  - checkIn: Minimum check-in date");
        System.out.println("  - checkOut: Maximum check-out date");
        System.out.println("  - hotelName: Filter by hotel name (e.g., 'Nandini')");
        System.out.println("  - guests: Number of guests (for future room capacity)");
        System.out.println("  - tersediaOnly: true/false - only show available tickets");
        
        System.out.println("\nNote: Use null or empty string to ignore a filter parameter");
    }
} 