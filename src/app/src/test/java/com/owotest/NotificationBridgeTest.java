package com.owotest;

import com.owo.utils.NotificationBridge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Queueing and lifecycle of {@link NotificationBridge}.
 *
 * <p>Delivery itself is deliberately <strong>not</strong> covered here. It runs through
 * {@code Platform.runLater} and calls {@code window.showNotification} on a real
 * {@code WebEngine}, neither of which exists without a JavaFX toolkit and a display. The
 * retry-then-drop path around {@code MAX_DELIVERY_ATTEMPTS} is only reachable once a script
 * call can actually fail, so it belongs to {@code BridgeLiveTest} (opt-in via
 * {@code -Powo.live=true}). What is testable headlessly is everything before the engine hand-off:
 * that a message raised before the page exists survives, and that connect/disconnect/shutdown
 * do not silently discard the queue.
 *
 * <p>Every behavioural test runs against its own instance built through the private
 * constructor. The singleton starts a one-second scheduler that lives for the whole JVM, so
 * mutating it — attaching an engine, calling {@code shutdown()} — would leak into every other
 * test class in the same fork.
 */
class NotificationBridgeTest {

    private final List<NotificationBridge> isolated = new ArrayList<>();

    @BeforeEach
    void setUp() {
        isolated.clear();
    }

    @AfterEach
    void tearDown() {
        // Each instance owns a scheduler thread; leaving them running leaks a thread per test.
        for (NotificationBridge bridge : isolated) {
            bridge.shutdown();
        }
    }

    private NotificationBridge freshBridge() throws Exception {
        Constructor<NotificationBridge> ctor = NotificationBridge.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        NotificationBridge bridge = ctor.newInstance();
        isolated.add(bridge);
        return bridge;
    }

    private static boolean schedulerIsShutdown(NotificationBridge bridge) throws Exception {
        // No accessor exposes this, and the point of shutdown() is precisely that the
        // scheduler stops; asserting that it returned quietly would assert nothing.
        Field field = NotificationBridge.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        return ((ExecutorService) field.get(bridge)).isShutdown();
    }

    @Test
    void getInstance_alwaysReturnsTheSameBridge() {
        // One queue, one scheduler. A second instance would poll a queue nobody feeds and
        // strand every message written to the other one.
        assertSame(NotificationBridge.getInstance(), NotificationBridge.getInstance());
    }

    @Test
    void aBridgeStartsDisconnectedWithAnEmptyQueue() throws Exception {
        NotificationBridge bridge = freshBridge();

        assertFalse(bridge.isConnected());
        assertEquals(0, bridge.getQueueSize());
    }

    @Test
    void sendNotification_beforeAnEngineIsAttached_isQueuedRatherThanLost() throws Exception {
        NotificationBridge bridge = freshBridge();

        // Login-time notifications are produced before the page has finished loading. If
        // the disconnected path dropped them, the first thing a user did would go unreported.
        bridge.sendNotification("Pemesanan berhasil dikonfirmasi");

        assertEquals(1, bridge.getQueueSize());
    }

    @Test
    void sendNotification_queuesEveryMessageWhileDisconnected() throws Exception {
        NotificationBridge bridge = freshBridge();

        bridge.sendNotification("Satu");
        bridge.sendNotification("Dua");
        bridge.sendNotification("Tiga");

        // The scheduler fires once a second while disconnected; it must not consume the
        // queue it cannot deliver to.
        assertEquals(3, bridge.getQueueSize());
    }

    @Test
    void sendNotification_withNothingToSay_isIgnored() throws Exception {
        NotificationBridge bridge = freshBridge();

        bridge.sendNotification(null);
        bridge.sendNotification("");
        bridge.sendNotification("   ");

        // A blank entry would still cost a delivery round-trip and would render as an
        // empty toast on the page.
        assertEquals(0, bridge.getQueueSize());
    }

    @Test
    void setWebEngine_withoutAnEngine_doesNotConsumeTheQueue() throws Exception {
        NotificationBridge bridge = freshBridge();
        bridge.sendNotification("Pemesanan berhasil dikonfirmasi");

        // setWebEngine marks the bridge connected before checking what it was handed, so a
        // null engine leaves it "connected" with nothing to deliver through. The queue must
        // survive that: draining into a null engine would lose the message outright.
        bridge.setWebEngine(null);

        assertEquals(1, bridge.getQueueSize());
    }

    @Test
    void disconnect_reportsDisconnectedAndKeepsQueuedMessages() throws Exception {
        NotificationBridge bridge = freshBridge();
        bridge.setWebEngine(null);
        bridge.sendNotification("Refund Anda telah disetujui");

        bridge.disconnect();

        // Logout must not throw away notifications; the next login attaches a new engine
        // and they are still owed to the user.
        assertFalse(bridge.isConnected());
        assertEquals(1, bridge.getQueueSize());
    }

    @Test
    void sendNotification_afterDisconnect_stillQueues() throws Exception {
        NotificationBridge bridge = freshBridge();
        bridge.setWebEngine(null);
        bridge.disconnect();

        bridge.sendNotification("Check-in dibuka");

        assertEquals(1, bridge.getQueueSize());
    }

    @Test
    void shutdown_stopsTheScheduler() throws Exception {
        NotificationBridge bridge = freshBridge();

        bridge.shutdown();

        // A scheduled task that outlives the application keeps a non-daemon thread alive
        // and the JVM with it.
        assertTrue(schedulerIsShutdown(bridge));
    }

    @Test
    void shutdown_twice_isHarmless() throws Exception {
        NotificationBridge bridge = freshBridge();

        bridge.shutdown();
        bridge.shutdown();

        // App.stop() and an explicit teardown can both reach this; the second call must not
        // throw or block for the five-second termination wait.
        assertTrue(schedulerIsShutdown(bridge));
    }
}
