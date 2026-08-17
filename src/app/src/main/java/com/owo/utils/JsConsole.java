package com.owo.utils;

/**
 * Forwards the page's console output to the Java process.
 *
 * <p>A WebView has no visible developer console, so an uncaught JavaScript error leaves
 * no trace at all — the screen simply does not render. Routing console output and the
 * {@code error} event here makes those failures observable in the application log.
 */
public class JsConsole {

    public void log(String message) {
        System.out.println("[js] " + message);
    }

    public void warn(String message) {
        System.out.println("[js:warn] " + message);
    }

    public void error(String message) {
        System.err.println("[js:error] " + message);
    }

    /** The script that installs this object over the page's own console. */
    public static String installScript() {
        return """
            (function () {
              const forward = function (level) {
                const original = console[level] ? console[level].bind(console) : function () {};
                return function () {
                  const text = Array.prototype.map.call(arguments, function (a) {
                    if (a instanceof Error) { return a.message + '\\n' + a.stack; }
                    if (typeof a === 'object') {
                      try { return JSON.stringify(a); } catch (e) { return String(a); }
                    }
                    return String(a);
                  }).join(' ');
                  try { window.owoConsole[level](text); } catch (e) { /* nothing to do */ }
                  original.apply(null, arguments);
                };
              };
              console.log = forward('log');
              console.warn = forward('warn');
              console.error = forward('error');

              window.addEventListener('error', function (e) {
                window.owoConsole.error(
                  (e.message || 'error') + ' at ' + (e.filename || '?') + ':' + (e.lineno || 0));
              });
              window.addEventListener('unhandledrejection', function (e) {
                const reason = e.reason;
                window.owoConsole.error('unhandled rejection: '
                  + (reason && reason.message ? reason.message : String(reason)));
              });
            })();
            """;
    }
}
