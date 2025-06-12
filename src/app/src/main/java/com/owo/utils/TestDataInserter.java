package com.owo.utils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.owo.dao.TiketDAO;

public class TestDataInserter {
    
    public static void insertSampleData() throws SQLException {
        System.out.println("Inserting sample test data...");
        
        // Initialize database
        DBHelper.initializeDatabase();
        
        // Insert sample flight tickets
        insertSampleFlights();
        
        // Insert sample hotel tickets
        insertSampleHotels();
        
        System.out.println("Sample data insertion completed!");
    }
    
    private static void insertSampleFlights() throws SQLException {
        System.out.println("Inserting sample flights...");
        
        // Jakarta to Denpasar flights
        TiketDAO.createTiketPesawat(
            1500000f, true, "GA401", 
            "Jakarta (CGK)", "Denpasar (DPS)", 
            "Garuda Indonesia", "Economy", 
            LocalDateTime.of(2025, 5, 10, 7, 30)
        );
        
        TiketDAO.createTiketPesawat(
            2500000f, true, "GA403", 
            "Jakarta (CGK)", "Denpasar (DPS)", 
            "Garuda Indonesia", "Business", 
            LocalDateTime.of(2025, 5, 10, 14, 15)
        );
        
        TiketDAO.createTiketPesawat(
            1200000f, true, "JT750", 
            "Jakarta (CGK)", "Denpasar (DPS)", 
            "Lion Air", "Economy", 
            LocalDateTime.of(2025, 5, 10, 10, 45)
        );
        
        // Denpasar to Jakarta flights (return)
        TiketDAO.createTiketPesawat(
            1600000f, true, "GA412", 
            "Denpasar (DPS)", "Jakarta (CGK)", 
            "Garuda Indonesia", "Economy", 
            LocalDateTime.of(2025, 5, 12, 16, 20)
        );
        
        // Jakarta to Yogyakarta flights
        TiketDAO.createTiketPesawat(
            800000f, true, "QG701", 
            "Jakarta (CGK)", "Yogyakarta (JOG)", 
            "Citilink", "Economy", 
            LocalDateTime.of(2025, 5, 10, 8, 15)
        );
        
        // Jakarta to Surabaya flights
        TiketDAO.createTiketPesawat(
            900000f, true, "GA305", 
            "Jakarta (CGK)", "Surabaya (SBY)", 
            "Garuda Indonesia", "Economy", 
            LocalDateTime.of(2025, 5, 10, 13, 45)
        );
        
        System.out.println("Sample flights inserted successfully!");
    }
    
    private static void insertSampleHotels() throws SQLException {
        System.out.println("Inserting sample hotels...");
        
        // Hotels in Denpasar
        TiketDAO.createTiketHotel(
            750000f, true, 
            LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 15),
            "Nandini Jungle Resort & Spa", "101", 
            "Jl. Raya Susut, Banjar Susut Kaja, Denpasar, Bali"
        );
        
        TiketDAO.createTiketHotel(
            1200000f, true, 
            LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 15),
            "Ramayana Suites & Resort", "204", 
            "Jl. Bakung Sari, Kuta, Denpasar, Bali"
        );
        
        TiketDAO.createTiketHotel(
            950000f, true, 
            LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 15),
            "The Garcia Ubud Hotel & Resort", "305", 
            "Jl. Raya Ubud No.88, Ubud, Denpasar, Bali"
        );
        
        // Hotels in Jakarta
        TiketDAO.createTiketHotel(
            650000f, true, 
            LocalDate.of(2025, 5, 8), LocalDate.of(2025, 5, 12),
            "Hotel Indonesia Kempinski", "1205", 
            "Jl. M.H. Thamrin No.1, Menteng, Jakarta Pusat"
        );
        
        TiketDAO.createTiketHotel(
            850000f, true, 
            LocalDate.of(2025, 5, 8), LocalDate.of(2025, 5, 12),
            "The Ritz-Carlton Jakarta", "2108", 
            "Jl. DR. Ide Anak Agung Gde Agung, Kuningan, Jakarta Selatan"
        );
        
        // Hotels in Yogyakarta  
        TiketDAO.createTiketHotel(
            450000f, true, 
            LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 13),
            "Royal Malioboro Hotel", "308", 
            "Jl. Malioboro No.18, Yogyakarta"
        );
        
        // Hotels in Surabaya
        TiketDAO.createTiketHotel(
            550000f, true, 
            LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 14),
            "Hotel Majapahit Surabaya", "412", 
            "Jl. Tunjungan No.65, Genteng, Surabaya"
        );
        
        // Hotels in Malang
        TiketDAO.createTiketHotel(
            350000f, true, 
            LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 13),
            "Tugu Malang Hotel", "205", 
            "Jl. Tugu No.3, Klojen, Malang"
        );
        
        System.out.println("Sample hotels inserted successfully!");
    }
    
    public static void main(String[] args) {
        try {
            insertSampleData();
        } catch (SQLException e) {
            System.err.println("Error inserting sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 