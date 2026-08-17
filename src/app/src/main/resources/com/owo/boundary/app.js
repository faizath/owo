/**
 * Shell: routing, session, notifications, and shared helpers.
 *
 * The application is a single document. It used to host every screen in a full-viewport
 * iframe and navigate by swapping the iframe's src, which put the screens in a window
 * where the injected bridge did not exist, destroyed all JavaScript state on every hop,
 * and re-ran the load handler each time so timers and listeners accumulated.
 */
(function () {
  'use strict';

  const app = {
    /** Screens reachable without signing in. */
    PUBLIC_SCREENS: ['LoginForm', 'RegisterForm'],

    current: null,
    session: null,
    /** Per-screen state, cleared on every navigation so screens cannot leak into each other. */
    context: {}
  };

  // ------------------------------------------------------------------ escaping

  /**
   * Escapes text for interpolation into markup.
   *
   * Screens build rows with template literals and assign them to innerHTML, so any value
   * originating from the database or a form must pass through here first.
   */
  function escapeHtml(value) {
    if (value === null || value === undefined) {
      return '';
    }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // ----------------------------------------------------------------- formatting

  /** Currency for display. id-ID is correct here; it is only wrong when crossing the bridge. */
  function formatRupiah(amount) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(Number(amount) || 0);
  }

  /** @param iso an ISO date or date-time produced by Java */
  function formatDate(iso) {
    if (!iso) return '-';
    const date = new Date(iso);
    if (isNaN(date.getTime())) return String(iso);
    return date.toLocaleDateString('id-ID', {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
    });
  }

  /** Time is dropped by a date-only format, which is how departure times went missing. */
  function formatTime(iso) {
    if (!iso || iso.indexOf('T') < 0) return '';
    const date = new Date(iso);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
  }

  function formatDateTime(iso) {
    const time = formatTime(iso);
    return time ? formatDate(iso) + ', ' + time : formatDate(iso);
  }

  // -------------------------------------------------------------- notifications

  function showNotification(message) {
    const container = document.getElementById('notificationContainer');
    if (!container) return;

    const notification = document.createElement('div');
    notification.className = 'notification';

    const text = document.createElement('p');
    text.className = 'notification-content';
    // textContent, not innerHTML: notification text comes from the database.
    text.textContent = message;
    notification.appendChild(text);

    container.appendChild(notification);
    setTimeout(function () { notification.classList.add('show'); }, 50);
    setTimeout(function () {
      notification.classList.remove('show');
      setTimeout(function () { notification.remove(); }, 300);
    }, 5000);
  }

  /** Java calls this by name to deliver a notification. */
  window.showNotification = showNotification;

  // ------------------------------------------------------------------- routing

  /**
   * Swaps the visible screen.
   *
   * @param name a screen name known to the Java side
   * @param context values the destination screen needs, replacing the previous screen's
   */
  function navigate(name, context) {
    if (!app.session && app.PUBLIC_SCREENS.indexOf(name) < 0) {
      name = 'LoginForm';
      context = undefined;
    }

    let screen;
    try {
      screen = window.OwOAPI.getScreen(name);
    } catch (e) {
      showFatal('Layar "' + name + '" tidak dapat dimuat: ' + e.message);
      return;
    }

    app.context = context || {};
    app.current = name;

    const root = document.getElementById('app');
    root.innerHTML = screen.html;
    root.className = 'screen screen-' + name;
    root.scrollTop = 0;

    updateChrome();

    const controller = window.OwOScreens[name];
    if (controller && typeof controller.init === 'function') {
      try {
        controller.init(app.context);
      } catch (e) {
        console.error('Screen ' + name + ' failed to initialise', e);
        showFatal('Layar tidak dapat ditampilkan: ' + e.message);
      }
    }
  }

  function showFatal(message) {
    const root = document.getElementById('app');
    root.className = 'screen';
    root.innerHTML = '';

    const box = document.createElement('div');
    box.className = 'fatal-error';
    box.textContent = message;
    root.appendChild(box);
  }

  /** Shows or hides the shell chrome according to the session. */
  function updateChrome() {
    const bar = document.getElementById('sessionBar');
    if (!bar) return;

    if (!app.session) {
      bar.classList.add('hidden');
      return;
    }
    bar.classList.remove('hidden');
    document.getElementById('sessionName').textContent = app.session.nama;
  }

  // ------------------------------------------------------------------- session

  function setSession(user) {
    app.session = user;
    updateChrome();
  }

  function logout() {
    window.OwOAPI.logout()
      .catch(function () { /* the session is being discarded either way */ })
      .then(function () {
        app.session = null;
        navigate('LoginForm');
      });
  }

  // --------------------------------------------------------------------- boot

  window.OwOScreens = window.OwOScreens || {};

  window.App = {
    navigate: navigate,
    setSession: setSession,
    logout: logout,
    showNotification: showNotification,
    escapeHtml: escapeHtml,
    formatRupiah: formatRupiah,
    formatDate: formatDate,
    formatTime: formatTime,
    formatDateTime: formatDateTime,
    get session() { return app.session; },
    get context() { return app.context; }
  };

  document.addEventListener('DOMContentLoaded', function () {
    const logoutButton = document.getElementById('logoutButton');
    if (logoutButton) {
      logoutButton.addEventListener('click', logout);
    }

    window.OwOAPI.whenReady().then(function () {
      // A session may already exist if the page reloaded without the JVM restarting.
      setSession(window.OwOAPI.getSession());
      navigate(app.session ? 'Pemesanan' : 'LoginForm');
    }).catch(function (e) {
      showFatal(e.message);
    });
  });
})();
