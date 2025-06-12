package com.owo.utils;

import com.owo.controller.*;
import com.owo.entity.*;
import com.owo.dao.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import netscape.javascript.JSObject;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.function.Consumer;

public class JavaScriptBridge {
    private JSObject jsObject;
    private AuthController authController;
    private PemesananController pemesananController;
    private RefundController refundController;
    private CheckInController checkInController;
    
    // Storage for controllers that need initial data
    private Map<Integer, Pemesanan> pemesananMap;
    private Map<String, Refund> refundMap;

    public JavaScriptBridge() {
        this.pemesananMap = new HashMap<>();
        this.refundMap = new HashMap<>();
        this.authController = new AuthController();
        this.pemesananController = new PemesananController();
        this.refundController = new RefundController(pemesananMap, refundMap);
        this.checkInController = new CheckInController();
    }
    
    public void setJSObject(JSObject jsObject) {
        this.jsObject = jsObject;
    }
    
    // Authentication Methods
    public void register(String nama, String email, String password, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    // Create account using DAO directly since controller might not have register method
                    Akun akun = AkunDAO.createAkun(nama, email, password);
                    
                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Registration successful\", \"data\": \"{\\\"id\\\": %d, \\\"nama\\\": \\\"%s\\\", \\\"email\\\": \\\"%s\\\"}\"}",
                        akun.getID(), akun.getNama(), akun.getEmail()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Registration failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Registration failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    public void login(String email, String password, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    Akun akun = authController.login(email, password);
                    if (akun != null) {
                        String jsonResponse = String.format(
                            "{\"success\": true, \"message\": \"Login successful\", \"data\": \"{\\\"id\\\": %d, \\\"nama\\\": \\\"%s\\\", \\\"email\\\": \\\"%s\\\"}\"}",
                            akun.getID(), akun.getNama(), akun.getEmail()
                        );
                        return jsonResponse;
                    } else {
                        return "{\"success\": false, \"message\": \"Invalid credentials\"}";
                    }
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Login failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Login failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    // Booking Methods
    public void createHotelBooking(Map<String, Object> bookingData, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    int customerId = ((Number) bookingData.get("customerId")).intValue();
                    String hotelName = (String) bookingData.get("hotelName");
                    String checkin = (String) bookingData.get("checkin");
                    String checkout = (String) bookingData.get("checkout");
                    int guests = ((Number) bookingData.get("guests")).intValue();
                    int rooms = ((Number) bookingData.get("rooms")).intValue();
                    double price = ((Number) bookingData.get("price")).doubleValue();
                    String roomType = (String) bookingData.getOrDefault("roomType", "Standard Room");
                    String address = (String) bookingData.getOrDefault("address", "");

                    // Parse dates
                    LocalDate checkinDate = LocalDate.parse(checkin);
                    LocalDate checkoutDate = LocalDate.parse(checkout);

                    // Create hotel ticket
                    TiketHotel tiketHotel = TiketDAO.createTiketHotel((float) price, true, checkinDate, checkoutDate, hotelName, "101", address);

                    // Create booking
                    Pemesanan pemesanan = PemesananDAO.createPemesanan(customerId, tiketHotel);
                    
                    // Store in map for controller
                    pemesananMap.put(pemesanan.getId(), pemesanan);

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Hotel booking created successfully\", \"data\": \"{\\\"id\\\": %d, \\\"customerId\\\": \\\"%s\\\", \\\"tiketId\\\": %d, \\\"hotelName\\\": \\\"%s\\\", \\\"checkin\\\": \\\"%s\\\", \\\"checkout\\\": \\\"%s\\\", \\\"transactionId\\\": \\\"TXN%d\\\"}\"}",
                        pemesanan.getId(), pemesanan.getCustomerId(), tiketHotel.getId(), hotelName, checkin, checkout, pemesanan.getId()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Hotel booking failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Hotel booking failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    public void createFlightBooking(Map<String, Object> bookingData, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    int customerId = ((Number) bookingData.get("customerId")).intValue();
                    String flightNumber = (String) bookingData.get("flightNumber");
                    String origin = (String) bookingData.get("origin");
                    String destination = (String) bookingData.get("destination");
                    String maskapai = (String) bookingData.get("maskapai");
                    String kelas = (String) bookingData.get("kelas");
                    String departureStr = (String) bookingData.get("departure");
                    double price = ((Number) bookingData.get("price")).doubleValue();
                    int passengers = ((Number) bookingData.get("passengers")).intValue();

                    // Parse departure time
                    LocalDateTime departure = LocalDateTime.parse(departureStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    // Create flight ticket
                    TiketPesawat tiketPesawat = TiketDAO.createTiketPesawat((float) price, true, flightNumber, origin, destination, maskapai, kelas, departure);

                    // Create booking
                    Pemesanan pemesanan = PemesananDAO.createPemesanan(customerId, tiketPesawat);
                    
                    // Store in map for controller
                    pemesananMap.put(pemesanan.getId(), pemesanan);

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Flight booking created successfully\", \"data\": \"{\\\"id\\\": %d, \\\"customerId\\\": \\\"%s\\\", \\\"tiketId\\\": %d, \\\"flightNumber\\\": \\\"%s\\\", \\\"origin\\\": \\\"%s\\\", \\\"destination\\\": \\\"%s\\\", \\\"departure\\\": \\\"%s\\\", \\\"transactionId\\\": \\\"TXN%d\\\"}\"}",
                        pemesanan.getId(), pemesanan.getCustomerId(), tiketPesawat.getId(), flightNumber, origin, destination, departureStr, pemesanan.getId()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Flight booking failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Flight booking failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    // Booking Management Methods
    public void getUserBookings(int userId, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    List<Pemesanan> bookings = PemesananDAO.getPemesananByCustomerId(userId);
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    for (int i = 0; i < bookings.size(); i++) {
                        if (i > 0) sb.append(",");
                        Pemesanan p = bookings.get(i);
                        sb.append("{");
                        sb.append(String.format("\"id\": %d,", p.getId()));
                        sb.append(String.format("\"customerId\": \"%s\",", p.getCustomerId()));
                        sb.append(String.format("\"status\": \"%s\",", p.getStatus()));
                        sb.append(String.format("\"tanggalPesan\": \"%s\",", p.getTanggalPesan()));
                        sb.append(String.format("\"transactionId\": \"TXN%d\",", p.getId()));
                        sb.append(String.format("\"tiketId\": %d", p.getTiket().getId()));
                        sb.append("}");
                    }
                    sb.append("]");

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Bookings retrieved successfully\", \"data\": \"%s\"}",
                        sb.toString().replace("\"", "\\\"")
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Failed to retrieve bookings: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Failed to retrieve bookings: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    // Refund Methods
    public void createRefund(Map<String, Object> refundData, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    String refundId = (String) refundData.get("refundId");
                    int pemesananId = ((Number) refundData.get("pemesananId")).intValue();
                    String alasan = (String) refundData.get("alasan");
                    double jumlahRefund = ((Number) refundData.get("jumlahRefund")).doubleValue();

                    Refund refund = RefundDAO.createRefund(refundId, pemesananId, alasan, jumlahRefund);
                    
                    // Store in map for controller
                    refundMap.put(refund.getId(), refund);

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Refund created successfully\", \"data\": \"{\\\"id\\\": \\\"%s\\\", \\\"pemesananId\\\": %d, \\\"alasan\\\": \\\"%s\\\", \\\"jumlahRefund\\\": %.2f, \\\"status\\\": \\\"%s\\\"}\"}",
                        refund.getId(), pemesananId, alasan, jumlahRefund, refund.getStatus()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Refund creation failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Refund creation failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    // Check-in Methods
    public void performCheckIn(Map<String, Object> checkInData, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    int pemesananId = ((Number) checkInData.get("pemesananId")).intValue();

                    // Simulate check-in process by updating booking status
                    PemesananDAO.updateStatus(pemesananId, "CHECKED_IN");

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Check-in successful\", \"data\": \"{\\\"pemesananId\\\": %d, \\\"status\\\": \\\"CHECKED_IN\\\", \\\"checkInTime\\\": \\\"%s\\\"}\"}",
                        pemesananId, LocalDateTime.now().toString()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Check-in failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Check-in failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    // Search Methods
    public void searchFlights(Map<String, Object> searchCriteria, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    String origin = (String) searchCriteria.get("origin");
                    String destination = (String) searchCriteria.get("destination");
                    String kelas = (String) searchCriteria.get("kelas");
                    int passengers = searchCriteria.containsKey("passengers") ? 
                        ((Number) searchCriteria.get("passengers")).intValue() : 0;

                    List<TiketPesawat> flights = TiketDAO.searchTiketPesawat(origin, destination, kelas, passengers, true);

                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    for (int i = 0; i < flights.size(); i++) {
                        if (i > 0) sb.append(",");
                        TiketPesawat flight = flights.get(i);
                        sb.append("{");
                        sb.append(String.format("\"id\": %d,", flight.getId()));
                        sb.append(String.format("\"flightNumber\": \"%s\",", flight.getFlightNumber()));
                        sb.append(String.format("\"origin\": \"%s\",", flight.getOrigin()));
                        sb.append(String.format("\"destination\": \"%s\",", flight.getDestination()));
                        sb.append(String.format("\"maskapai\": \"%s\",", flight.getMaskapai()));
                        sb.append(String.format("\"kelas\": \"%s\",", flight.getKelas()));
                        sb.append(String.format("\"price\": %.2f,", flight.getHarga()));
                        sb.append(String.format("\"departure\": \"%s\"", flight.getWaktuKeberangkatan()));
                        sb.append("}");
                    }
                    sb.append("]");

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Flights found\", \"data\": %s}",
                        sb.toString()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Flight search failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Flight search failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    public void searchHotels(Map<String, Object> searchCriteria, Consumer<String> callback) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    String location = (String) searchCriteria.get("location");
                    String checkin = (String) searchCriteria.get("checkin");
                    String checkout = (String) searchCriteria.get("checkout");
                    String hotelName = (String) searchCriteria.get("hotelName");
                    int guests = searchCriteria.containsKey("guests") ? 
                        ((Number) searchCriteria.get("guests")).intValue() : 0;

                    LocalDate checkinDate = checkin != null ? LocalDate.parse(checkin) : null;
                    LocalDate checkoutDate = checkout != null ? LocalDate.parse(checkout) : null;

                    List<TiketHotel> hotels = TiketDAO.searchTiketHotel(location, checkinDate, checkoutDate, hotelName, guests, true);

                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    for (int i = 0; i < hotels.size(); i++) {
                        if (i > 0) sb.append(",");
                        TiketHotel hotel = hotels.get(i);
                        sb.append("{");
                        sb.append(String.format("\"id\": %d,", hotel.getId()));
                        sb.append(String.format("\"hotelName\": \"%s\",", hotel.getHotelName()));
                        sb.append(String.format("\"address\": \"%s\",", hotel.getAddress()));
                        sb.append(String.format("\"roomNumber\": \"%s\",", hotel.getRoomNumber()));
                        sb.append(String.format("\"price\": %.2f,", hotel.getHarga()));
                        sb.append(String.format("\"checkin\": \"%s\",", hotel.getCheckIn()));
                        sb.append(String.format("\"checkout\": \"%s\"", hotel.getCheckOut()));
                        sb.append("}");
                    }
                    sb.append("]");

                    String jsonResponse = String.format(
                        "{\"success\": true, \"message\": \"Hotels found\", \"data\": %s}",
                        sb.toString()
                    );
                    return jsonResponse;
                } catch (Exception e) {
                    return String.format("{\"success\": false, \"message\": \"Hotel search failed: %s\"}", e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (callback != null) {
                callback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            String errorResponse = String.format("{\"success\": false, \"message\": \"Hotel search failed: %s\"}", 
                task.getException().getMessage());
            if (callback != null) {
                callback.accept(errorResponse);
            }
        });

        new Thread(task).start();
    }
    
    // Utility Methods
    private String getValue(String[] parts, String key) {
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return "";
    }
    
    private String createSuccessResponse(String message, String data) {
        return String.format("{\"success\": true, \"message\": \"%s\", \"data\": %s}", message, data);
    }
    
    private String createErrorResponse(String message) {
        return String.format("{\"success\": false, \"message\": \"%s\", \"data\": null}", message);
    }
    
    private void executeAsyncTask(Task<String> task, String callback) {
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                if (jsObject != null && callback != null && !callback.isEmpty()) {
                    jsObject.call(callback, task.getValue());
                }
            });
        });
        
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                if (jsObject != null && callback != null && !callback.isEmpty()) {
                    String errorResponse = createErrorResponse("Operation failed: " + task.getException().getMessage());
                    jsObject.call(callback, errorResponse);
                }
            });
        });
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    // Direct methods for immediate operations (non-async)
    public String getVersion() {
        return "OwO Booking System v1.0";
    }
    
    public void showNotification(String message) {
        Platform.runLater(() -> {
            if (jsObject != null) {
                jsObject.call("showNotification", message);
            }
        });
    }
} 