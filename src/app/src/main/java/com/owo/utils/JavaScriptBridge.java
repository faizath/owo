package com.owo.utils;

import com.owo.controller.AuthController;
import com.owo.dao.PemesananDAO;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;

import javafx.application.Platform;
import javafx.concurrent.Task;
import netscape.javascript.JSObject;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The only surface JavaScript can call.
 *
 * <p><strong>Parameter types are constrained by the platform.</strong> WebView's JS→Java
 * bridge marshals values with a fixed LiveConnect-style table: numbers become numerics,
 * strings become {@code String}, and any JS object, array or function becomes
 * {@link JSObject}. There is no conversion to {@code java.util.Map} and no SAM adaptation
 * to {@code java.util.function.Consumer}. Every asynchronous method therefore takes
 * {@code (String argsJson, String callbackName)} — a payload produced by
 * {@code JSON.stringify}, and the name of a function on {@code window}.
 * {@code JavaScriptBridgeContractTest} enforces this.
 *
 * <p><strong>The session lives here, not in the client.</strong> No method accepts a user
 * id. Operations on user data derive identity from {@link #sessionUserId}, so a client
 * cannot ask for another user's data.
 */
public class JavaScriptBridge {

    /** Stable, machine-readable error identifiers so the UI can branch without string matching. */
    public static final String ERR_UNAUTHENTICATED = "ERR_UNAUTHENTICATED";
    public static final String ERR_INVALID_INPUT = "ERR_INVALID_INPUT";
    public static final String ERR_CREDENTIALS = "ERR_CREDENTIALS";
    public static final String ERR_NOT_FOUND = "ERR_NOT_FOUND";
    public static final String ERR_INTERNAL = "ERR_INTERNAL";

    private JSObject jsObject;

    /** Null when unauthenticated. The single source of truth for who is acting. */
    private volatile Integer sessionUserId;
    private volatile String sessionNama;
    private volatile String sessionEmail;

    /**
     * Bounded so a burst of calls cannot spawn unbounded threads. Daemon threads, so a
     * pending task never keeps the JVM alive after the window closes.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "owo-bridge-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    public void setJSObject(JSObject jsObject) {
        this.jsObject = jsObject;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // ------------------------------------------------------------ authentication

    public void register(String argsJson, String callbackName) {
        run(callbackName, () -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String nama = Json.optString(args, "nama", "");
            String email = Json.optString(args, "email", "");
            String password = Json.optString(args, "password", "");

            try {
                Akun akun = AuthController.register(nama, email, password);
                startSession(akun);
                return success("Registrasi berhasil", akunJson(akun));
            } catch (AuthController.AuthException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        });
    }

    public void login(String argsJson, String callbackName) {
        run(callbackName, () -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String email = Json.optString(args, "email", "");
            String password = Json.optString(args, "password", "");

            try {
                Akun akun = AuthController.login(email, password);
                startSession(akun);
                return success("Login berhasil", akunJson(akun));
            } catch (AuthController.AuthException e) {
                return error(e.getMessage(), ERR_CREDENTIALS);
            }
        });
    }

    public void logout(String callbackName) {
        endSession();
        respond(callbackName, success("Logout berhasil", null));
    }

    /** Lets the UI restore itself after a reload without re-authenticating. */
    public String getSession() {
        Integer userId = sessionUserId;
        if (userId == null) {
            return error("Belum masuk", ERR_UNAUTHENTICATED);
        }
        return success("Sesi aktif", Json.obj()
                .put("id", userId)
                .put("nama", sessionNama)
                .put("email", sessionEmail));
    }

    private void startSession(Akun akun) {
        sessionUserId = akun.getID();
        sessionNama = akun.getNama();
        sessionEmail = akun.getEmail();
        NotifikasiHelper.initialize(akun.getID());
    }

    private void endSession() {
        sessionUserId = null;
        sessionNama = null;
        sessionEmail = null;
        NotifikasiHelper.stop();
    }

    private Json.Obj akunJson(Akun akun) {
        return Json.obj()
                .put("id", akun.getID())
                .put("nama", akun.getNama())
                .put("email", akun.getEmail());
    }

    // -------------------------------------------------------------------- search

    public void searchFlights(String argsJson, String callbackName) {
        run(callbackName, () -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String origin = Json.optString(args, "origin", null);
            String destination = Json.optString(args, "destination", null);
            String kelas = Json.optString(args, "kelas", null);
            int passengers = Json.optInt(args, "passengers", 0);

            List<TiketPesawat> flights =
                    TiketDAO.searchTiketPesawat(origin, destination, kelas, passengers, true);

            Json.Arr items = Json.arr();
            for (TiketPesawat flight : flights) {
                items.add(flightJson(flight));
            }
            return success(flights.isEmpty() ? "Tidak ada penerbangan yang cocok" : "Penerbangan ditemukan",
                    items);
        });
    }

    public void searchHotels(String argsJson, String callbackName) {
        run(callbackName, () -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String location = Json.optString(args, "location", null);
            String hotelName = Json.optString(args, "hotelName", null);
            int guests = Json.optInt(args, "guests", 0);
            LocalDate checkIn = optDate(args, "checkin");
            LocalDate checkOut = optDate(args, "checkout");

            List<TiketHotel> hotels =
                    TiketDAO.searchTiketHotel(location, checkIn, checkOut, hotelName, guests, true);

            Json.Arr items = Json.arr();
            for (TiketHotel hotel : hotels) {
                items.add(hotelJson(hotel));
            }
            return success(hotels.isEmpty() ? "Tidak ada hotel yang cocok" : "Hotel ditemukan", items);
        });
    }

    private static LocalDate optDate(Map<String, Object> args, String key) {
        String raw = Json.optString(args, key, null);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new Json.JsonException("Field '" + key + "' is not an ISO date: " + raw);
        }
    }

    private Json.Obj flightJson(TiketPesawat flight) {
        return Json.obj()
                .put("id", flight.getId())
                .put("flightNumber", flight.getFlightNumber())
                .put("origin", flight.getOrigin())
                .put("destination", flight.getDestination())
                .put("maskapai", flight.getMaskapai())
                .put("kelas", flight.getKelas())
                .put("price", (double) flight.getHarga())
                .put("departure", SqlDates.format(flight.getWaktuKeberangkatan()))
                .put("tersedia", flight.isTersedia());
    }

    private Json.Obj hotelJson(TiketHotel hotel) {
        return Json.obj()
                .put("id", hotel.getId())
                .put("hotelName", hotel.getHotelName())
                .put("address", hotel.getAddress())
                .put("roomNumber", hotel.getRoomNumber())
                .put("price", (double) hotel.getHarga())
                .put("checkin", SqlDates.format(hotel.getCheckIn()))
                .put("checkout", SqlDates.format(hotel.getCheckOut()))
                .put("tersedia", hotel.isTersedia());
    }

    // ------------------------------------------------------------------ bookings

    /** Books an existing ticket by id. The client never supplies a price or a user id. */
    public void createBooking(String argsJson, String callbackName) {
        run(callbackName, () -> {
            Integer userId = sessionUserId;
            if (userId == null) {
                return error("Silakan masuk terlebih dahulu", ERR_UNAUTHENTICATED);
            }

            Map<String, Object> args = Json.parseObject(argsJson);
            int tiketId = Json.requireInt(args, "tiketId");

            Tiket tiket = TiketDAO.getTiketById(tiketId);
            if (tiket == null) {
                return error("Tiket tidak ditemukan", ERR_NOT_FOUND);
            }

            Pemesanan pemesanan = PemesananDAO.createPemesanan(userId, tiket);
            return success("Pemesanan dibuat", bookingJson(pemesanan));
        });
    }

    public void getUserBookings(String callbackName) {
        run(callbackName, () -> {
            Integer userId = sessionUserId;
            if (userId == null) {
                return error("Silakan masuk terlebih dahulu", ERR_UNAUTHENTICATED);
            }

            List<Pemesanan> bookings = PemesananDAO.getPemesananByCustomerId(userId);
            Json.Arr items = Json.arr();
            for (Pemesanan booking : bookings) {
                items.add(bookingJson(booking));
            }
            return success("Riwayat pemesanan dimuat", items);
        });
    }

    private Json.Obj bookingJson(Pemesanan pemesanan) {
        Json.Obj json = Json.obj()
                .put("id", pemesanan.getId())
                .put("status", pemesanan.getStatus())
                .put("tanggalPesan", SqlDates.format(pemesanan.getTanggalPesan()))
                .put("transactionId", "TXN" + pemesanan.getId());

        Tiket tiket = pemesanan.getTiket();
        if (tiket instanceof TiketPesawat flight) {
            json.put("tipe", "PESAWAT").put("tiket", flightJson(flight));
        } else if (tiket instanceof TiketHotel hotel) {
            json.put("tipe", "HOTEL").put("tiket", hotelJson(hotel));
        } else {
            json.put("tipe", "UNKNOWN").putNull("tiket");
        }
        return json;
    }

    // ------------------------------------------------------------------- utility

    public String getVersion() {
        return "OwO Booking System v1.0";
    }

    public void showNotification(String message) {
        Platform.runLater(() -> {
            if (jsObject != null) {
                // Passed as an argument, never concatenated into JavaScript source.
                jsObject.call("showNotification", message);
            }
        });
    }

    // ------------------------------------------------------------------ plumbing

    /** The body of a bridge operation: runs off the FX thread, returns a JSON response. */
    @FunctionalInterface
    interface Operation {
        String execute() throws Exception;
    }

    /**
     * Runs {@code operation} on the bridge executor and delivers its result to
     * {@code callbackName} on the FX thread.
     */
    private void run(String callbackName, Operation operation) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                try {
                    return operation.execute();
                } catch (Json.JsonException e) {
                    return error(e.getMessage(), ERR_INVALID_INPUT);
                } catch (Exception e) {
                    // The exception text may name tables and columns; log it, do not ship it.
                    System.err.println("Bridge operation failed: " + e);
                    return error("Terjadi kesalahan pada sistem", ERR_INTERNAL);
                }
            }
        };
        executeAsyncTask(task, callbackName);
    }

    private void executeAsyncTask(Task<String> task, String callbackName) {
        task.setOnSucceeded(e -> respond(callbackName, task.getValue()));
        task.setOnFailed(e -> {
            System.err.println("Bridge task failed: " + task.getException());
            respond(callbackName, error("Terjadi kesalahan pada sistem", ERR_INTERNAL));
        });
        executor.execute(task);
    }

    private void respond(String callbackName, String responseJson) {
        Platform.runLater(() -> {
            if (jsObject != null && callbackName != null && !callbackName.isEmpty()) {
                jsObject.call(callbackName, responseJson);
            }
        });
    }

    private static String success(String message, Object data) {
        Json.Obj response = Json.obj().put("success", true).put("message", message);
        if (data instanceof Json.Obj o) {
            response.put("data", o);
        } else if (data instanceof Json.Arr a) {
            response.put("data", a);
        } else {
            response.putNull("data");
        }
        return response.toString();
    }

    private static String error(String message, String code) {
        return Json.obj()
                .put("success", false)
                .put("message", message)
                .putNull("data")
                .put("code", code)
                .toString();
    }
}
