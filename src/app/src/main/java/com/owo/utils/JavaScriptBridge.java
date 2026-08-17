package com.owo.utils;

import com.owo.controller.AuthController;
import com.owo.controller.CheckInController;
import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.Refund;
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
    public static final String ERR_FORBIDDEN = "ERR_FORBIDDEN";
    public static final String ERR_INTERNAL = "ERR_INTERNAL";

    private final PemesananController pemesananController = new PemesananController();
    private final CheckInController checkInController = new CheckInController(pemesananController);
    private final RefundController refundController = new RefundController(pemesananController);

    private JSObject jsObject;

    /** Null when unauthenticated. The single source of truth for who is acting. */
    private volatile Integer sessionUserId;
    private volatile String sessionNama;
    private volatile String sessionEmail;

    /**
     * Read from the account row at login. The page is told about it so it can show or hide
     * the review queue, but every administrator operation re-checks this field — the client
     * saying it is an administrator proves nothing.
     */
    private volatile boolean sessionAdmin;

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

    /**
     * Package-private on purpose: WebView exposes <em>every</em> public method of the
     * object handed to {@code setMember}, so lifecycle methods must not be public or the
     * page could rebind the callback target or stop the executor. {@link BridgeInstaller}
     * is the supported way to wire this up.
     */
    void setJSObject(JSObject jsObject) {
        this.jsObject = jsObject;
    }

    void shutdown() {
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
                .put("email", sessionEmail)
                .put("isAdmin", sessionAdmin));
    }

    private void startSession(Akun akun) {
        sessionUserId = akun.getID();
        sessionNama = akun.getNama();
        sessionEmail = akun.getEmail();
        sessionAdmin = akun.isAdmin();
        NotifikasiHelper.initialize(akun.getID());
    }

    private void endSession() {
        sessionUserId = null;
        sessionNama = null;
        sessionEmail = null;
        sessionAdmin = false;
        NotifikasiHelper.stop();
    }

    private Json.Obj akunJson(Akun akun) {
        return Json.obj()
                .put("id", akun.getID())
                .put("nama", akun.getNama())
                .put("email", akun.getEmail())
                .put("isAdmin", akun.isAdmin());
    }

    // -------------------------------------------------------------------- search

    public void searchFlights(String argsJson, String callbackName) {
        run(callbackName, () -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String origin = Json.optString(args, "origin", null);
            String destination = Json.optString(args, "destination", null);
            String kelas = Json.optString(args, "kelas", null);
            int penumpang = optPartySize(args, "penumpang");

            List<TiketPesawat> flights =
                    TiketDAO.searchTiketPesawat(origin, destination, kelas, true, penumpang);

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
            LocalDate checkIn = optDate(args, "checkin");
            LocalDate checkOut = optDate(args, "checkout");
            int tamu = optPartySize(args, "tamu");

            List<TiketHotel> hotels =
                    TiketDAO.searchTiketHotel(location, checkIn, checkOut, hotelName, true, tamu);

            Json.Arr items = Json.arr();
            for (TiketHotel hotel : hotels) {
                items.add(hotelJson(hotel));
            }
            return success(hotels.isEmpty() ? "Tidak ada hotel yang cocok" : "Hotel ditemukan", items);
        });
    }

    /**
     * Reads a party size, defaulting to one when the field is absent.
     *
     * <p>Bounded because it reaches the search as a filter and the booking as a stored
     * value; an absurd figure is a client error, not something to pass through.
     */
    private static int optPartySize(Map<String, Object> args, String key) {
        int value = Json.optInt(args, key, 1);
        if (value < 1 || value > MAX_PESERTA) {
            throw new Json.JsonException(
                    "Field '" + key + "' harus antara 1 dan " + MAX_PESERTA);
        }
        return value;
    }

    /** Upper bound on a party size the client may request. */
    private static final int MAX_PESERTA = 20;

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
                .put("kapasitas", flight.getKapasitas())
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
                .put("kapasitas", hotel.getKapasitas())
                .put("tersedia", hotel.isTersedia());
    }

    // ------------------------------------------------------------------ bookings

    /** Books an existing ticket by id. The client never supplies a price or a user id. */
    public void createBooking(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            int tiketId = Json.requireInt(args, "tiketId");
            int jumlahPeserta = optPartySize(args, "jumlahPeserta");

            Tiket tiket = TiketDAO.getTiketById(tiketId);
            if (tiket == null) {
                return error("Tiket tidak ditemukan", ERR_NOT_FOUND);
            }

            try {
                Pemesanan pemesanan =
                        pemesananController.createPemesanan(userId, tiket, jumlahPeserta);
                return success("Pemesanan dibuat", bookingJson(pemesanan));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    public void getUserBookings(String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            List<Pemesanan> bookings = pemesananController.getPemesananByCustomerId(userId);
            Json.Arr items = Json.arr();
            for (Pemesanan booking : bookings) {
                items.add(bookingJson(booking));
            }
            return success("Riwayat pemesanan dimuat", items);
        }));
    }

    /** Marks a booking paid. The transaction reference is generated here, not by the client. */
    public void confirmPayment(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            int pemesananId = Json.requireInt(args, "pemesananId");

            try {
                Pemesanan pemesanan = pemesananController.konfirmasiPemesanan(pemesananId, userId);
                return success("Pembayaran berhasil", bookingJson(pemesanan));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    public void cancelBooking(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            int pemesananId = Json.requireInt(args, "pemesananId");

            try {
                Pemesanan pemesanan = pemesananController.batalkanPemesanan(pemesananId, userId);
                return success("Pemesanan dibatalkan", bookingJson(pemesanan));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    public void performCheckIn(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            int pemesananId = Json.requireInt(args, "pemesananId");

            try {
                Pemesanan pemesanan = checkInController.checkIn(pemesananId, userId);
                return success("Check-in berhasil", bookingJson(pemesanan));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    /**
     * Quotes a refund without filing one, so the form can show the real figure rather
     * than computing its own from client-held data.
     */
    public void quoteRefund(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            int pemesananId = Json.requireInt(args, "pemesananId");

            try {
                Pemesanan pemesanan = pemesananController.getOwnedPemesanan(pemesananId, userId);
                double jumlah = refundController.hitungJumlahRefund(pemesanan);
                return success("Estimasi refund dihitung", Json.obj()
                        .put("pemesananId", pemesananId)
                        .put("hargaTiket", (double) pemesanan.getTiket().getHarga())
                        .put("biayaAdmin", RefundController.BIAYA_ADMIN)
                        .put("jumlahRefund", jumlah));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    public void createRefund(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            int pemesananId = Json.requireInt(args, "pemesananId");
            String alasan = Json.optString(args, "alasan", "");
            String namaPenerima = Json.optString(args, "namaPenerima", "");
            String rekeningTujuan = Json.optString(args, "rekeningTujuan", "");

            try {
                Refund refund = refundController.ajukanRefund(
                        pemesananId, userId, alasan, namaPenerima, rekeningTujuan);
                return success("Pengajuan refund diterima", refundJson(refund));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    /** Every refund the signed-in customer has filed. Scoped by the session, not by argument. */
    public void getUserRefunds(String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            List<Refund> refunds = refundController.getRefundsForCustomer(userId);
            Json.Arr items = Json.arr();
            for (Refund refund : refunds) {
                items.add(refundJson(refund));
            }
            return success("Daftar refund dimuat", items);
        }));
    }

    /** Corrects the payee on a refund that is still awaiting review. */
    public void updateRefundPayee(String argsJson, String callbackName) {
        run(callbackName, () -> withSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String refundId = Json.optString(args, "refundId", "");
            String namaPenerima = Json.optString(args, "namaPenerima", "");
            String rekeningTujuan = Json.optString(args, "rekeningTujuan", "");

            try {
                Refund refund = refundController.perbaruiDetailPencairan(
                        refundId, userId, namaPenerima, rekeningTujuan);
                return success("Detail pencairan diperbarui", refundJson(refund));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    // --------------------------------------------------------------------- review

    /** The administrator review queue. */
    public void getPendingRefunds(String callbackName) {
        run(callbackName, () -> withAdminSession(userId -> {
            List<Refund> refunds = refundController.getRefundsMenungguPeninjauan();
            Json.Arr items = Json.arr();
            for (Refund refund : refunds) {
                items.add(refundJson(refund));
            }
            return success("Antrean refund dimuat", items);
        }));
    }

    public void approveRefund(String argsJson, String callbackName) {
        run(callbackName, () -> withAdminSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String refundId = Json.optString(args, "refundId", "");

            try {
                Refund refund = refundController.setujuiRefund(refundId);
                return success("Refund disetujui", refundJson(refund));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    public void rejectRefund(String argsJson, String callbackName) {
        run(callbackName, () -> withAdminSession(userId -> {
            Map<String, Object> args = Json.parseObject(argsJson);
            String refundId = Json.optString(args, "refundId", "");

            try {
                Refund refund = refundController.tolakRefund(refundId);
                return success("Refund ditolak", refundJson(refund));
            } catch (PemesananController.PemesananException e) {
                return error(e.getMessage(), ERR_INVALID_INPUT);
            }
        }));
    }

    private Json.Obj refundJson(Refund refund) {
        return Json.obj()
                .put("id", refund.getId())
                .put("pemesananId", refund.getPemesananID())
                .put("alasan", refund.getAlasan())
                .put("status", refund.getStatus().name())
                .put("jumlahRefund", refund.getJumlahRefund())
                .put("namaPenerima", refund.getNamaPenerima())
                .put("rekeningTujuan", refund.getRekeningTujuan());
    }

    private Json.Obj bookingJson(Pemesanan pemesanan) {
        Json.Obj json = Json.obj()
                .put("id", pemesanan.getId())
                .put("status", pemesanan.getStatus())
                .put("tanggalPesan", SqlDates.format(pemesanan.getTanggalPesan()))
                .put("jumlahPeserta", pemesanan.getJumlahPeserta())
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

    /** Screens the router is allowed to load. Anything else is refused. */
    private static final List<String> SCREENS = List.of(
            "LoginForm", "RegisterForm", "Pemesanan", "CekKetersediaanPesawat",
            "CekKetersediaanHotel", "Pembayaran", "RiwayatPemesanan", "RefundForm",
            "TinjauRefund");

    /**
     * Returns a screen fragment as markup.
     *
     * <p>Java reads it from the classpath rather than the page fetching it, because a
     * packaged build is loaded over {@code jar:} where XHR does not work, and the plain
     * {@code file:} origin used in development is treated as opaque. This is also why the
     * name is matched against a fixed list instead of being used as a path.
     */
    public String getScreen(String name) {
        if (!SCREENS.contains(name)) {
            return error("Layar tidak dikenali", ERR_NOT_FOUND);
        }
        try (var in = getClass().getResourceAsStream("/com/owo/boundary/screens/" + name + ".html")) {
            if (in == null) {
                return error("Layar tidak ditemukan", ERR_NOT_FOUND);
            }
            String html = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return success("Layar dimuat", Json.obj().put("name", name).put("html", html));
        } catch (java.io.IOException e) {
            System.err.println("Cannot read screen " + name + ": " + e.getMessage());
            return error("Layar gagal dimuat", ERR_INTERNAL);
        }
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

    /** An operation that needs an authenticated user. */
    @FunctionalInterface
    interface SessionOperation {
        String execute(int userId) throws Exception;
    }

    /** Refuses the operation unless a session is active, then supplies its user id. */
    private String withSession(SessionOperation operation) throws Exception {
        Integer userId = sessionUserId;
        if (userId == null) {
            return error("Silakan masuk terlebih dahulu", ERR_UNAUTHENTICATED);
        }
        return operation.execute(userId);
    }

    /**
     * As {@link #withSession}, and additionally refuses anyone whose account row is not
     * flagged as an administrator. The flag comes from the database at login; nothing the
     * page sends can set it.
     */
    private String withAdminSession(SessionOperation operation) throws Exception {
        Integer userId = sessionUserId;
        if (userId == null) {
            return error("Silakan masuk terlebih dahulu", ERR_UNAUTHENTICATED);
        }
        if (!sessionAdmin) {
            return error("Anda tidak berwenang meninjau refund", ERR_FORBIDDEN);
        }
        return operation.execute(userId);
    }

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
