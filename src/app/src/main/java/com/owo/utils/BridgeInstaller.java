package com.owo.utils;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

/**
 * Wires a {@link JavaScriptBridge} into a {@link WebEngine}.
 *
 * <p>Exists so the bridge's lifecycle methods can stay package-private. WebView exposes
 * every public method of the object passed to {@code setMember}, so anything public on
 * the bridge is callable by the page — including, without this indirection, the methods
 * that set the callback target and shut the executor down.
 */
public final class BridgeInstaller {

    private BridgeInstaller() {
    }

    /** Injects the bridge and the console shim, then signals the page. */
    public static void install(WebEngine engine, JavaScriptBridge bridge) {
        JSObject window = (JSObject) engine.executeScript("window");

        bridge.setJSObject(window);

        // Installed before the bridge is announced, so a failure in the page's own
        // startup is reported rather than silently leaving a blank screen.
        window.setMember("owoConsole", new JsConsole());
        engine.executeScript(JsConsole.installScript());

        window.setMember("owoBridge", bridge);
        engine.executeScript("window.dispatchEvent(new Event('owo:bridge-ready'))");
    }

    public static void shutdown(JavaScriptBridge bridge) {
        bridge.shutdown();
    }

    /**
     * The screen names the router will serve.
     *
     * <p>Exposed here rather than on the bridge because WebView publishes every public
     * method of the injected object, and here rather than copied because a second list is
     * exactly what drifts: a screen added to the allow-list and not to the copy stays
     * routable while escaping whatever the copy drives.
     */
    public static java.util.List<String> routableScreens() {
        return JavaScriptBridge.screens();
    }
}
