/**
 * Promise wrapper over the Java bridge.
 *
 * Every asynchronous Java method takes (argsJson, callbackName): WebView cannot pass a
 * JavaScript function to Java, so the callback is registered on `window` under a
 * generated name and Java calls it back by that name.
 *
 * There is deliberately no mock fallback. The previous version decided at parse time
 * whether Java was present — before the bridge was injected, so the answer was always
 * "no" — and then silently served fabricated data that looked like success.
 */
(function () {
  'use strict';

  let callbackCounter = 0;

  /** Resolves once `window.owoBridge` exists. Java fires owo:bridge-ready after setMember. */
  function whenReady() {
    if (window.owoBridge) {
      return Promise.resolve(window.owoBridge);
    }
    return new Promise(function (resolve, reject) {
      const timer = setTimeout(function () {
        reject(new Error('Bridge Java tidak tersedia.'));
      }, 10000);

      window.addEventListener('owo:bridge-ready', function onReady() {
        clearTimeout(timer);
        window.removeEventListener('owo:bridge-ready', onReady);
        resolve(window.owoBridge);
      });
    });
  }

  /** Parses a bridge response, rejecting when it reports failure. */
  function unwrap(raw) {
    let response;
    try {
      response = JSON.parse(raw);
    } catch (e) {
      throw new Error('Respons dari server tidak dapat dibaca.');
    }
    if (!response.success) {
      const error = new Error(response.message || 'Operasi gagal.');
      error.code = response.code || 'ERR_UNKNOWN';
      throw error;
    }
    return response.data;
  }

  /** Invokes an async bridge method and resolves with its `data`. */
  function call(method, args) {
    return whenReady().then(function (bridge) {
      return new Promise(function (resolve, reject) {
        const name = '__owoCallback' + ++callbackCounter;

        window[name] = function (raw) {
          delete window[name];
          try {
            resolve(unwrap(raw));
          } catch (e) {
            reject(e);
          }
        };

        try {
          if (args === undefined) {
            bridge[method](name);
          } else {
            bridge[method](JSON.stringify(args), name);
          }
        } catch (e) {
          delete window[name];
          reject(new Error('Tidak dapat memanggil ' + method + ': ' + e.message));
        }
      });
    });
  }

  /** Invokes a synchronous bridge method that returns a response envelope. */
  function callSync(method, arg) {
    if (!window.owoBridge) {
      throw new Error('Bridge Java tidak tersedia.');
    }
    return unwrap(arg === undefined ? window.owoBridge[method]()
                                    : window.owoBridge[method](arg));
  }

  window.OwOAPI = {
    whenReady: whenReady,

    // Authentication
    register: function (nama, email, password) {
      return call('register', { nama: nama, email: email, password: password });
    },
    login: function (email, password) {
      return call('login', { email: email, password: password });
    },
    logout: function () {
      return call('logout');
    },
    /** @returns the session user, or null when not signed in */
    getSession: function () {
      try {
        return callSync('getSession');
      } catch (e) {
        return null;
      }
    },

    // Search
    searchFlights: function (criteria) {
      return call('searchFlights', criteria || {});
    },
    searchHotels: function (criteria) {
      return call('searchHotels', criteria || {});
    },

    // Bookings
    createBooking: function (tiketId, jumlahPeserta) {
      return call('createBooking', {
        tiketId: tiketId,
        jumlahPeserta: jumlahPeserta || 1
      });
    },
    getUserBookings: function () {
      return call('getUserBookings');
    },
    confirmPayment: function (pemesananId) {
      return call('confirmPayment', { pemesananId: pemesananId });
    },
    cancelBooking: function (pemesananId) {
      return call('cancelBooking', { pemesananId: pemesananId });
    },
    performCheckIn: function (pemesananId) {
      return call('performCheckIn', { pemesananId: pemesananId });
    },

    // Refunds
    quoteRefund: function (pemesananId) {
      return call('quoteRefund', { pemesananId: pemesananId });
    },
    createRefund: function (details) {
      return call('createRefund', details);
    },
    /** Every refund the signed-in user has filed. Scoped by the Java session. */
    getUserRefunds: function () {
      return call('getUserRefunds');
    },
    updateRefundPayee: function (refundId, namaPenerima, rekeningTujuan) {
      return call('updateRefundPayee', {
        refundId: refundId,
        namaPenerima: namaPenerima,
        rekeningTujuan: rekeningTujuan
      });
    },

    // Refund review. Refused unless the session's account row is an administrator.
    getPendingRefunds: function () {
      return call('getPendingRefunds');
    },
    approveRefund: function (refundId) {
      return call('approveRefund', { refundId: refundId });
    },
    rejectRefund: function (refundId) {
      return call('rejectRefund', { refundId: refundId });
    },
    // Disbursement: approving authorises the payment, these record it being made.
    processRefund: function (refundId) {
      return call('processRefund', { refundId: refundId });
    },
    completeRefund: function (refundId) {
      return call('completeRefund', { refundId: refundId });
    },
    failRefund: function (refundId) {
      return call('failRefund', { refundId: refundId });
    },

    // Screens
    getScreen: function (name) {
      return callSync('getScreen', name);
    },

    getVersion: function () {
      return window.owoBridge ? window.owoBridge.getVersion() : 'unavailable';
    }
  };
})();
