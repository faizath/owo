/**
 * Screen controllers.
 *
 * Each screen exposes `init(context)`, called by the router after its markup is in the
 * document. Every one of these replaced an inline `<script>` block that simulated its
 * result locally — logging in with a `setTimeout`, checking in with `Math.random()`,
 * rendering a catalogue from `mockup.js` — without ever contacting the backend.
 *
 * Values that reach `innerHTML` pass through `App.escapeHtml` first: they come from the
 * database or from another user's input.
 */
(function () {
  'use strict';

  const S = window.OwOScreens = window.OwOScreens || {};
  const esc = function (v) { return window.App.escapeHtml(v); };

  // ------------------------------------------------------------------- helpers

  function byId(id) {
    return document.getElementById(id);
  }

  /** Shows a message where the screen has room for one, falling back to an alert. */
  function fail(message) {
    const banner = byId('screenError');
    if (banner) {
      banner.textContent = message;
      banner.classList.remove('hidden');
      return;
    }
    alert(message);
  }

  /** Adds a dismissible error banner to the top of a screen. */
  function ensureBanner(parent) {
    if (byId('screenError')) return;
    const banner = document.createElement('div');
    banner.id = 'screenError';
    banner.className = 'hidden';
    banner.style.cssText = 'margin:12px;padding:12px 16px;border-radius:8px;'
      + 'background:#f8d7da;color:#842029;border:1px solid #f5c2c7;font-size:14px;';
    parent.insertBefore(banner, parent.firstChild);
  }

  function busy(button, isBusy, busyLabel) {
    if (!button) return;
    if (isBusy) {
      button.dataset.label = button.textContent;
      button.textContent = busyLabel || 'Memproses...';
      button.disabled = true;
    } else {
      button.textContent = button.dataset.label || button.textContent;
      button.disabled = false;
    }
  }

  /** An ISO date string for an `<input type="date">`, offset from today. */
  function isoDate(offsetDays) {
    const d = new Date();
    d.setDate(d.getDate() + (offsetDays || 0));
    return d.toISOString().slice(0, 10);
  }

  /** Wires every element matching `selector` to navigate to `screen`. */
  function linkTo(selector, screen, context) {
    document.querySelectorAll(selector).forEach(function (el) {
      el.addEventListener('click', function (e) {
        e.preventDefault();
        window.App.navigate(screen, typeof context === 'function' ? context() : context);
      });
    });
  }

  /** Wires the shared page chrome: header nav, sidebar menu, logo. */
  function wireChrome() {
    document.querySelectorAll('.nav-item, .footer-link, .menu-item, .sidebar-title a')
      .forEach(function (el) {
        const label = (el.textContent || '').trim().toLowerCase();
        let target = null;

        if (label === 'pesawat' || label === 'hotel' || label === 'owo'
            || label === 'pemesanan') {
          target = 'Pemesanan';
        } else if (label === 'refund') {
          // Without a booking the refund screen shows what has been filed, which is
          // what this menu entry means. Filing starts from a booking in the history.
          target = 'RefundForm';
        } else if (label === 'tinjau refund') {
          target = 'TinjauRefund';
        }

        el.addEventListener('click', function (e) {
          e.preventDefault();
          if (label === 'log out') {
            window.App.logout();
          } else if (target) {
            window.App.navigate(target);
          }
        });
      });

    // The history screen is the entry point for check-in and refunds.
    document.querySelectorAll('.user-section, .user-avatar').forEach(function (el) {
      el.addEventListener('click', function () {
        window.App.navigate('RiwayatPemesanan');
      });
    });
  }

  // -------------------------------------------------------------------- Login

  S.LoginForm = {
    init: function () {
      const form = byId('loginForm');
      const button = form.querySelector('.login-button');
      const email = byId('email');
      const password = byId('password');

      ensureBanner(document.querySelector('.login-container') || document.body);

      function validate() {
        button.disabled = !(email.value.trim() && password.value.trim());
      }
      email.addEventListener('input', validate);
      password.addEventListener('input', validate);
      validate();

      form.addEventListener('submit', function (e) {
        e.preventDefault();
        busy(button, true, 'Masuk...');

        // This used to be a setTimeout followed by a redirect: any credentials worked,
        // and nothing was ever sent anywhere.
        window.OwOAPI.login(email.value.trim(), password.value)
          .then(function (user) {
            window.App.setSession(user);
            window.App.navigate('Pemesanan');
          })
          .catch(function (err) {
            busy(button, false);
            fail(err.message);
          });
      });

      // No password reset exists, so there is no "forgot password" link to wire. It used
      // to point at registration, which recovered nothing.
      linkTo('.login-link a, .register-link a', 'RegisterForm');
    }
  };

  // ----------------------------------------------------------------- Register

  S.RegisterForm = {
    init: function () {
      const form = byId('registerForm');
      const button = form.querySelector('.register-button');
      const nama = byId('name');
      const email = byId('email');
      const password = byId('password');

      ensureBanner(document.querySelector('.register-container') || document.body);

      form.addEventListener('submit', function (e) {
        e.preventDefault();
        busy(button, true, 'Mendaftar...');

        window.OwOAPI.register(nama.value.trim(), email.value.trim(), password.value)
          .then(function (user) {
            window.App.setSession(user);
            window.App.navigate('Pemesanan');
          })
          .catch(function (err) {
            busy(button, false);
            fail(err.message);
          });
      });

      linkTo('.login-link a, .signin-link a', 'LoginForm');
    }
  };

  // ---------------------------------------------------------------- Pemesanan

  S.Pemesanan = {
    init: function () {
      wireChrome();
      ensureBanner(document.querySelector('.container') || document.body);

      const flightForm = byId('flightForm');
      const hotelForm = byId('hotelForm');

      // Dates defaulted to a hardcoded day in 2025 and allowed dates in the past.
      const departureDate = byId('departureDate');
      if (departureDate) {
        departureDate.value = isoDate(1);
        departureDate.min = isoDate(0);
      }
      const checkIn = byId('checkin-input');
      const checkOut = byId('checkout-input');
      if (checkIn && checkOut) {
        checkIn.value = isoDate(1);
        checkIn.min = isoDate(0);
        checkOut.value = isoDate(3);
        checkOut.min = isoDate(1);
        checkIn.addEventListener('change', function () {
          checkOut.min = checkIn.value;
          if (checkOut.value <= checkIn.value) {
            const next = new Date(checkIn.value);
            next.setDate(next.getDate() + 1);
            checkOut.value = next.toISOString().slice(0, 10);
          }
        });
      }

      // Only one search panel is shown at a time; the tabs switch between them.
      document.querySelectorAll('.tab-button, .booking-tab').forEach(function (tab) {
        tab.addEventListener('click', function () {
          const wantsHotel = /hotel/i.test(tab.textContent || '');
          if (flightForm) flightForm.style.display = wantsHotel ? 'none' : '';
          if (hotelForm) hotelForm.style.display = wantsHotel ? '' : 'none';
          document.querySelectorAll('.tab-button, .booking-tab')
            .forEach(function (t) { t.classList.remove('active'); });
          tab.classList.add('active');
        });
      });

      if (flightForm) {
        // Bound once here. The form previously had a listener from an inline block and
        // another from pemesanan.js, so each search ran twice.
        flightForm.addEventListener('submit', function (e) {
          e.preventDefault();
          const button = byId('search-flight-button');
          busy(button, true, 'Mencari...');

          // `penumpang` is the name the bridge filters on. It used to be sent as
          // `passengers`, which nothing read, so the count was collected and dropped.
          const criteria = {
            origin: byId('departure') ? byId('departure').value : '',
            destination: byId('arrival') ? byId('arrival').value : '',
            kelas: byId('flightClass') ? byId('flightClass').value : '',
            penumpang: byId('passengers') ? parseInt(byId('passengers').value, 10) || 1 : 1
          };

          window.OwOAPI.searchFlights(criteria)
            .then(function (results) {
              busy(button, false);
              window.App.navigate('CekKetersediaanPesawat', {
                criteria: criteria,
                results: results,
                date: departureDate ? departureDate.value : ''
              });
            })
            .catch(function (err) {
              busy(button, false);
              fail(err.message);
            });
        });
      }

      if (hotelForm) {
        hotelForm.addEventListener('submit', function (e) {
          e.preventDefault();
          const button = byId('search-hotel-button');
          busy(button, true, 'Mencari...');

          // One booking is one room, so the guest count is what the room must hold.
          // The separate "jumlah kamar" input was removed rather than faked: booking
          // several rooms at once would be several bookings, which nothing supports.
          const criteria = {
            location: byId('destination-input') ? byId('destination-input').value : '',
            checkin: checkIn ? checkIn.value : '',
            checkout: checkOut ? checkOut.value : '',
            tamu: byId('guests-input') ? parseInt(byId('guests-input').value, 10) || 1 : 1
          };

          window.OwOAPI.searchHotels(criteria)
            .then(function (results) {
              busy(button, false);
              window.App.navigate('CekKetersediaanHotel', {
                criteria: criteria,
                results: results
              });
            })
            .catch(function (err) {
              busy(button, false);
              fail(err.message);
            });
        });
      }
    }
  };

  // ------------------------------------------------------- flight availability

  S.CekKetersediaanPesawat = {
    init: function (context) {
      const list = byId('flightList');
      const details = byId('searchDetails');
      const results = context.results || [];
      const criteria = context.criteria || {};

      document.querySelectorAll('.back-button').forEach(function (b) {
        b.addEventListener('click', function () { window.App.navigate('Pemesanan'); });
      });

      if (details) {
        details.textContent = (criteria.origin || 'Semua asal') + ' → '
          + (criteria.destination || 'Semua tujuan')
          + (context.date ? ' • ' + window.App.formatDate(context.date) : '');
      }

      if (!results.length) {
        // "No results" and "the search failed" used to be the same message.
        list.innerHTML = '<p style="padding:24px;text-align:center;color:#666;">'
          + 'Tidak ada penerbangan yang cocok dengan pencarian Anda.</p>';
        return;
      }

      list.innerHTML = results.map(function (f) {
        return '<div class="flight-card" data-id="' + esc(f.id) + '">'
          + '<div class="flight-main">'
          + '<div class="flight-airline">' + esc(f.maskapai) + ' • ' + esc(f.flightNumber) + '</div>'
          + '<div class="flight-route">' + esc(f.origin) + ' → ' + esc(f.destination) + '</div>'
          + '<div class="flight-time">' + esc(window.App.formatDateTime(f.departure)) + '</div>'
          + '<div class="flight-class">' + esc(f.kelas) + '</div>'
          + '</div>'
          + '<div class="flight-side">'
          + '<div class="flight-price">' + esc(window.App.formatRupiah(f.price)) + '</div>'
          + '<button type="button" class="select-button" data-id="' + esc(f.id) + '">Pilih</button>'
          + '</div>'
          + '</div>';
      }).join('');

      // Selection is by primary key. Matching on the airline name booked whichever
      // flight happened to share it.
      list.querySelectorAll('.select-button').forEach(function (button) {
        button.addEventListener('click', function () {
          const id = parseInt(button.dataset.id, 10);
          const chosen = results.filter(function (f) { return f.id === id; })[0];
          busy(button, true);

          // The party searched for is the party booked for; sending it again lets the
          // backend refuse a listing whose capacity changed since the search.
          window.OwOAPI.createBooking(id, criteria.penumpang || 1)
            .then(function (booking) {
              window.App.navigate('Pembayaran', { booking: booking, tiket: chosen });
            })
            .catch(function (err) {
              busy(button, false);
              alert(err.message);
            });
        });
      });
    }
  };

  // -------------------------------------------------------- hotel availability

  S.CekKetersediaanHotel = {
    init: function (context) {
      const list = byId('hotelList');
      const details = byId('searchDetails');
      const results = context.results || [];
      const criteria = context.criteria || {};

      document.querySelectorAll('.back-button').forEach(function (b) {
        b.addEventListener('click', function () { window.App.navigate('Pemesanan'); });
      });

      if (details) {
        details.textContent = (criteria.location || 'Semua lokasi')
          + (criteria.checkin ? ' • ' + window.App.formatDate(criteria.checkin) : '')
          + (criteria.checkout ? ' - ' + window.App.formatDate(criteria.checkout) : '');
      }

      if (!results.length) {
        list.innerHTML = '<p style="padding:24px;text-align:center;color:#666;">'
          + 'Tidak ada hotel yang cocok dengan pencarian Anda.</p>';
        return;
      }

      list.innerHTML = results.map(function (h) {
        return '<div class="hotel-card" data-id="' + esc(h.id) + '">'
          + '<div class="hotel-main">'
          + '<div class="hotel-name">' + esc(h.hotelName) + '</div>'
          + '<div class="hotel-address">' + esc(h.address) + '</div>'
          + '<div class="hotel-dates">' + esc(window.App.formatDate(h.checkin))
          + ' - ' + esc(window.App.formatDate(h.checkout)) + '</div>'
          + '<div class="hotel-room">Kamar ' + esc(h.roomNumber) + '</div>'
          + '</div>'
          + '<div class="hotel-side">'
          + '<div class="hotel-price">' + esc(window.App.formatRupiah(h.price)) + '</div>'
          + '<button type="button" class="select-button" data-id="' + esc(h.id) + '">Pilih</button>'
          + '</div>'
          + '</div>';
      }).join('');

      list.querySelectorAll('.select-button').forEach(function (button) {
        button.addEventListener('click', function () {
          const id = parseInt(button.dataset.id, 10);
          const chosen = results.filter(function (h) { return h.id === id; })[0];
          busy(button, true);

          window.OwOAPI.createBooking(id, criteria.tamu || 1)
            .then(function (booking) {
              window.App.navigate('Pembayaran', { booking: booking, tiket: chosen });
            })
            .catch(function (err) {
              busy(button, false);
              alert(err.message);
            });
        });
      });
    }
  };

  // --------------------------------------------------------------- Pembayaran

  S.Pembayaran = {
    init: function (context) {
      wireChrome();
      ensureBanner(document.querySelector('.container') || document.body);

      const booking = context.booking;
      if (!booking) {
        // The screen used to fall back to whatever booking was last left in storage,
        // so it charged for a stale one.
        fail('Tidak ada pemesanan yang dipilih.');
        return;
      }

      const tiket = booking.tiket || {};
      const price = Number(tiket.price) || 0;

      // Guest details come from the session rather than being asked for again: the
      // booking is made by the signed-in account and nothing stores a separate guest.
      const session = window.App.session || {};
      if (byId('guestName')) byId('guestName').textContent = session.nama || '-';
      if (byId('guestEmail')) byId('guestEmail').textContent = session.email || '-';
      if (byId('guestParty')) {
        byId('guestParty').textContent = (booking.jumlahPeserta || 1) + ' orang';
      }

      // The expiry year list was hardcoded and its first entries were already past,
      // so choosing them failed validation for no reason the user could see.
      const yearSelect = byId('expiryYear');
      if (yearSelect) {
        const thisYear = new Date().getFullYear();
        for (let y = thisYear; y <= thisYear + 10; y++) {
          const option = document.createElement('option');
          option.value = String(y);
          option.textContent = String(y);
          yearSelect.appendChild(option);
        }
      }

      const container = byId('bookingDetailsContainer');
      if (container) {
        const isFlight = booking.tipe === 'PESAWAT';
        container.innerHTML = '<div class="booking-summary">'
          + '<div class="summary-title">'
          + (isFlight ? esc(tiket.maskapai) + ' • ' + esc(tiket.flightNumber)
                      : esc(tiket.hotelName))
          + '</div>'
          + '<div class="summary-detail">'
          + (isFlight ? esc(tiket.origin) + ' → ' + esc(tiket.destination)
                      : esc(tiket.address))
          + '</div>'
          + '<div class="summary-detail">'
          + (isFlight ? esc(window.App.formatDateTime(tiket.departure))
                      : esc(window.App.formatDate(tiket.checkin)) + ' - '
                        + esc(window.App.formatDate(tiket.checkout)))
          + '</div>'
          + '<div class="summary-detail">Kode: ' + esc(booking.transactionId) + '</div>'
          + '</div>';
      }

      // Every figure comes from the ticket record rather than being invented here.
      const tax = Math.round(price * 0.1);
      if (byId('base-price')) byId('base-price').textContent = window.App.formatRupiah(price);
      if (byId('taxes-fees')) byId('taxes-fees').textContent = window.App.formatRupiah(tax);
      if (byId('total-price')) byId('total-price').textContent = window.App.formatRupiah(price + tax);
      if (byId('checkin-date')) byId('checkin-date').textContent = window.App.formatDate(tiket.checkin);
      if (byId('checkout-date')) byId('checkout-date').textContent = window.App.formatDate(tiket.checkout);

      const payButton = byId('payment-button');
      payButton.addEventListener('click', function (e) {
        e.preventDefault();

        const problem = validateCard();
        if (problem) {
          fail(problem);
          return;
        }

        busy(payButton, true, 'Memproses...');
        window.OwOAPI.confirmPayment(booking.id)
          .then(function (confirmed) {
            const modal = byId('successModal');
            if (modal) {
              modal.classList.add('show');
              modal.style.display = 'flex';
              modal.querySelectorAll('button, .modal-button').forEach(function (b) {
                b.addEventListener('click', function () {
                  window.App.navigate('RiwayatPemesanan');
                });
              });
              setTimeout(function () { window.App.navigate('RiwayatPemesanan'); }, 2500);
            } else {
              window.App.navigate('RiwayatPemesanan');
            }
          })
          .catch(function (err) {
            busy(payButton, false);
            fail(err.message);
          });
      });

      /** @returns a message describing the first problem, or null when the card is plausible */
      function validateCard() {
        const number = (byId('cardNumber') || {}).value || '';
        const name = ((byId('cardName') || {}).value || '').trim();
        const month = (byId('expiryMonth') || {}).value || '';
        const year = (byId('expiryYear') || {}).value || '';
        const cvv = (byId('cvv') || {}).value || '';

        if (!name) return 'Nama pada kartu wajib diisi.';

        // Validation used to check only that the fields were non-empty, so letters
        // were accepted as a card number.
        const digits = number.replace(/\s+/g, '');
        if (!/^\d{13,19}$/.test(digits)) return 'Nomor kartu harus 13-19 digit angka.';
        if (!luhn(digits)) return 'Nomor kartu tidak valid.';
        if (!/^\d{3,4}$/.test(cvv)) return 'CVV harus 3 atau 4 digit angka.';
        if (!month || !year) return 'Masa berlaku kartu wajib diisi.';

        const expiry = new Date(parseInt(year, 10), parseInt(month, 10), 0, 23, 59, 59);
        if (expiry < new Date()) return 'Kartu sudah kedaluwarsa.';

        return null;
      }

      function luhn(digits) {
        let sum = 0;
        let double = false;
        for (let i = digits.length - 1; i >= 0; i--) {
          let d = parseInt(digits.charAt(i), 10);
          if (double) {
            d *= 2;
            if (d > 9) d -= 9;
          }
          sum += d;
          double = !double;
        }
        return sum % 10 === 0;
      }
    }
  };

  // --------------------------------------------------------- RiwayatPemesanan

  /** Statuses that still count as an active booking rather than history. */
  const ACTIVE = ['PENDING', 'CONFIRMED', 'CHECKED_IN'];

  const STATUS_LABEL = {
    PENDING: 'Menunggu pembayaran',
    CONFIRMED: 'Aktif',
    CHECKED_IN: 'Sudah check-in',
    CANCELLED: 'Dibatalkan',
    REFUND_IN_PROGRESS: 'Refund diproses',
    REFUNDED: 'Refund selesai'
  };

  function bookingTitle(booking) {
    const t = booking.tiket || {};
    return booking.tipe === 'PESAWAT'
      ? (t.origin || '') + ' - ' + (t.destination || '')
      : (t.hotelName || 'Hotel');
  }

  function bookingWhen(booking) {
    const t = booking.tiket || {};
    return booking.tipe === 'PESAWAT'
      ? window.App.formatDateTime(t.departure)
      : window.App.formatDate(t.checkin) + ' - ' + window.App.formatDate(t.checkout);
  }

  function bookingWhere(booking) {
    const t = booking.tiket || {};
    return booking.tipe === 'PESAWAT'
      ? (t.maskapai || '') + ' • ' + (t.flightNumber || '')
      : (t.address || '');
  }

  /** True when today is the departure or check-in day. */
  function isCheckInDay(booking) {
    const t = booking.tiket || {};
    const raw = booking.tipe === 'PESAWAT' ? t.departure : t.checkin;
    if (!raw) return false;
    return String(raw).slice(0, 10) === isoDate(0);
  }

  S.RiwayatPemesanan = {
    init: function () {
      wireChrome();
      ensureBanner(document.querySelector('.container') || document.body);

      const active = byId('bookingList');
      const history = byId('historyList');
      active.innerHTML = '<p style="padding:16px;color:#666;">Memuat...</p>';

      window.OwOAPI.getUserBookings()
        .then(function (bookings) { render(bookings || []); })
        .catch(function (err) {
          active.innerHTML = '';
          fail(err.message);
        });

      function render(bookings) {
        const activeBookings = bookings.filter(function (b) {
          return ACTIVE.indexOf(b.status) >= 0;
        });
        const pastBookings = bookings.filter(function (b) {
          return ACTIVE.indexOf(b.status) < 0;
        });

        active.innerHTML = activeBookings.length
          ? activeBookings.map(card).join('')
          : '<p style="padding:16px;color:#666;">Belum ada pesanan aktif.</p>';

        renderHistory(pastBookings);

        // The status filter narrows the history list. It used to be a button with no
        // handler, inside markup whose tags were never closed.
        const filter = byId('historyFilter');
        if (filter) {
          filter.addEventListener('change', function () {
            renderHistory(filter.value
              ? pastBookings.filter(function (b) { return b.status === filter.value; })
              : pastBookings);
          });
        }

        wireActions(bookings);
      }

      function renderHistory(rows) {
        if (!history) return;
        history.innerHTML = rows.length
          ? rows.map(historyCard).join('')
          : '<p style="padding:16px;color:#666;">Belum ada histori pemesanan.</p>';
      }

      /**
       * One booking card. Built as a string and assigned in one pass, but a failure
       * inside a single card must not blank the whole list, so each is built defensively.
       */
      function card(b) {
        let refundButton = '';
        // Hidden once a refund is already under way, which the old page did not check.
        if (b.status === 'CONFIRMED' || b.status === 'CHECKED_IN') {
          refundButton = '<button type="button" class="action-button btn-refund" '
            + 'data-action="refund" data-id="' + esc(b.id) + '">Ajukan Refund</button>';
        }

        let checkInButton = '';
        if (b.status === 'CONFIRMED' && isCheckInDay(b)) {
          checkInButton = '<button type="button" class="action-button btn-checkin" '
            + 'data-action="checkin" data-id="' + esc(b.id) + '">Check In</button>';
        }

        let payButton = '';
        if (b.status === 'PENDING') {
          payButton = '<button type="button" class="action-button btn-checkin" '
            + 'data-action="pay" data-id="' + esc(b.id) + '">Bayar</button>';
        }

        return '<div class="booking-card">'
          + '<div class="booking-header">'
          + '<div class="booking-route">'
          + '<div class="route-icon ' + (b.tipe === 'PESAWAT' ? 'airplane' : 'hotel') + '"></div>'
          + '<span>' + esc(bookingTitle(b)) + ' • ' + esc(STATUS_LABEL[b.status] || b.status) + '</span>'
          + '</div>'
          + '<div class="booking-code">Kode Booking: ' + esc(b.transactionId) + '</div>'
          + '</div>'
          + '<div class="booking-details">'
          + '<div class="booking-info">'
          + '<div class="booking-date">' + esc(bookingWhen(b)) + '</div>'
          + '<div class="booking-location">' + esc(bookingWhere(b)) + '</div>'
          + '<div class="booking-party">' + esc(b.jumlahPeserta || 1) + ' orang</div>'
          + '</div>'
          + '<div class="booking-status status-active">' + esc(STATUS_LABEL[b.status] || b.status) + '</div>'
          + '</div>'
          + '<div class="booking-actions">' + payButton + checkInButton + refundButton + '</div>'
          + '</div>';
      }

      function historyCard(b) {
        return '<div class="history-card">'
          + '<div class="booking-header">'
          + '<div class="booking-route">'
          + '<div class="route-icon ' + (b.tipe === 'PESAWAT' ? 'airplane' : 'hotel') + '"></div>'
          + '<span>' + esc(bookingTitle(b)) + '</span>'
          + '</div>'
          + '<div class="booking-code">Kode Booking: ' + esc(b.transactionId) + '</div>'
          + '</div>'
          + '<div class="booking-details">'
          + '<div class="booking-info">'
          + '<div class="booking-date">' + esc(bookingWhen(b)) + '</div>'
          + '</div>'
          + '<div class="booking-status">' + esc(STATUS_LABEL[b.status] || b.status) + '</div>'
          + '</div>'
          + '</div>';
      }

      function wireActions(bookings) {
        function find(id) {
          return bookings.filter(function (b) { return b.id === id; })[0];
        }

        document.querySelectorAll('[data-action]').forEach(function (button) {
          const id = parseInt(button.dataset.id, 10);

          button.addEventListener('click', function () {
            const booking = find(id);

            if (button.dataset.action === 'refund') {
              window.App.navigate('RefundForm', { booking: booking });
              return;
            }

            if (button.dataset.action === 'pay') {
              window.App.navigate('Pembayaran', { booking: booking });
              return;
            }

            openCheckIn(booking);
          });
        });
      }

      /**
       * Confirms a check-in before performing it.
       *
       * Check-in is not reversible by the customer, so it gets a confirmation step
       * showing what is about to be checked into. The modal markup was already in the
       * screen but nothing opened it, and the action fired straight from the card.
       */
      function openCheckIn(booking) {
        const modal = byId('checkinModal');
        if (!modal || !booking) {
          performCheckIn(booking, null);
          return;
        }

        setText('modal-booking-code', booking.transactionId);
        setText('modal-booking-route', bookingTitle(booking));
        setText('modal-booking-date', bookingWhen(booking));
        setText('modal-booking-airline', bookingWhere(booking));
        // The passenger block used to be a hardcoded name and seat number.
        setText('modal-passenger-name',
          window.App.session ? window.App.session.nama : '');
        setText('modal-passenger-count', (booking.jumlahPeserta || 1) + ' orang');

        show('modal-loading', false);
        show('modal-success-message', false);
        show('modal-error-message', false);
        show('modal-action-buttons', true, 'flex');

        modal.style.display = 'flex';

        const confirm = modal.querySelector('.modal-btn-primary');
        const cancel = modal.querySelector('.modal-btn-secondary');
        const close = modal.querySelector('.close-button');

        function dismiss() {
          modal.style.display = 'none';
        }

        [cancel, close].forEach(function (el) {
          if (el) el.addEventListener('click', dismiss);
        });

        if (confirm) {
          confirm.addEventListener('click', function () {
            show('modal-action-buttons', false);
            show('modal-loading', true);
            performCheckIn(booking, modal);
          });
        }
      }

      function performCheckIn(booking, modal) {
        // Check-in used to be `Math.random() > 0.3`, with no backend call at all.
        window.OwOAPI.performCheckIn(booking.id)
          .then(function () {
            if (!modal) {
              window.App.showNotification('Check-in berhasil.');
              window.App.navigate('RiwayatPemesanan');
              return;
            }
            show('modal-loading', false);
            show('modal-success-message', true);
            setTimeout(function () {
              modal.style.display = 'none';
              window.App.navigate('RiwayatPemesanan');
            }, 1800);
          })
          .catch(function (err) {
            if (!modal) {
              fail(err.message);
              return;
            }
            show('modal-loading', false);
            setText('error-detail', err.message);
            show('modal-error-message', true);
            show('modal-action-buttons', true, 'flex');
          });
      }

      function setText(id, value) {
        const el = byId(id);
        // textContent, not innerHTML: these carry hotel names and reasons from the database.
        if (el) el.textContent = value == null ? '' : String(value);
      }

      function show(id, visible, mode) {
        const el = byId(id);
        if (el) el.style.display = visible ? (mode || 'block') : 'none';
      }
    }
  };

  // --------------------------------------------------------------- RefundForm

  /** Refund statuses that are still being decided or paid, versus finished. */
  const REFUND_OPEN = ['PENDING_REVIEW', 'APPROVED', 'PROCESSING'];

  const REFUND_LABEL = {
    PENDING_REVIEW: 'Menunggu peninjauan',
    APPROVED: 'Disetujui',
    PROCESSING: 'Sedang diproses',
    COMPLETED: 'Selesai',
    REJECTED: 'Ditolak',
    FAILED: 'Gagal'
  };

  S.RefundForm = {
    init: function (context) {
      wireChrome();
      ensureBanner(document.querySelector('.container') || document.body);

      const booking = context.booking;

      // Reached without a booking, the screen shows what the user has already filed
      // rather than refusing to render. Filing needs a booking; reviewing does not.
      if (!booking) {
        showSection('refund-status');
        loadRefundStatus();
        return;
      }

      const form = byId('refundForm') || byId('refund-form');
      const confirmButton = byId('confirmButton');
      const amountDisplay = byId('refund-amount-display');

      const customerName = byId('customerName');
      if (customerName && window.App.session) {
        customerName.value = window.App.session.nama;
      }

      document.querySelectorAll('.route-info')
        .forEach(function (el) { el.textContent = bookingTitle(booking); });

      // The amount is quoted by the server. It used to be computed in the page from a
      // price held in localStorage, which the user could edit.
      if (confirmButton) confirmButton.disabled = true;
      window.OwOAPI.quoteRefund(booking.id)
        .then(function (quote) {
          if (amountDisplay) {
            amountDisplay.textContent = window.App.formatRupiah(quote.jumlahRefund);
          }
          if (confirmButton) confirmButton.disabled = false;
        })
        .catch(function (err) {
          if (amountDisplay) amountDisplay.textContent = '-';
          fail(err.message);
        });

      if (!form) return;

      form.addEventListener('submit', function (e) {
        e.preventDefault();

        const reason = byId('refundReason');
        const namaPenerima = byId('namaPenerima');
        const rekeningTujuan = byId('rekeningTujuan');
        const terms = byId('termsCheckbox');

        if (!reason || !reason.value) {
          fail('Pilih alasan refund.');
          return;
        }
        if (!namaPenerima || !namaPenerima.value.trim()) {
          fail('Nama penerima wajib diisi.');
          return;
        }
        if (!rekeningTujuan || !rekeningTujuan.value.trim()) {
          fail('Rekening tujuan wajib diisi.');
          return;
        }
        if (terms && !terms.checked) {
          fail('Anda harus menyetujui syarat dan ketentuan.');
          return;
        }

        busy(confirmButton, true, 'Mengirim...');

        // Submission used to persist nothing at all: the page showed a success modal
        // and cleared its own input.
        window.OwOAPI.createRefund({
          pemesananId: booking.id,
          alasan: reason.value,
          namaPenerima: namaPenerima.value.trim(),
          rekeningTujuan: rekeningTujuan.value.trim()
        })
          .then(function (refund) {
            window.App.showNotification('Pengajuan refund ' + refund.id + ' diterima.');
            // Shows the refund that was just filed instead of navigating away, so the
            // submission has a visible result rather than only a transient modal.
            showSection('refund-status');
            loadRefundStatus(refund.id);
          })
          .catch(function (err) {
            busy(confirmButton, false);
            fail(err.message);
          });
      });
    }
  };

  // -------------------------------------------------------------- TinjauRefund

  /**
   * The administrator review queue.
   *
   * Approval and rejection existed with tested rules but nothing could reach them, so
   * every refund stayed in review for ever. Authority is enforced in Java; this screen
   * is only refused politely if a non-administrator reaches it.
   */
  S.TinjauRefund = {
    init: function () {
      wireChrome();
      ensureBanner(document.querySelector('.review-container') || document.body);

      const queue = byId('refundQueue');
      queue.innerHTML = '<p class="queue-empty">Memuat...</p>';

      load();

      function load() {
        window.OwOAPI.getPendingRefunds()
          .then(render)
          .catch(function (err) {
            queue.innerHTML = '';
            fail(err.message);
          });
      }

      function render(refunds) {
        const rows = refunds || [];
        if (!rows.length) {
          queue.innerHTML =
            '<p class="queue-empty">Tidak ada refund yang menunggu peninjauan.</p>';
          return;
        }

        queue.innerHTML = rows.map(function (r) {
          return '<div class="review-card" data-refund="' + esc(r.id) + '">'
            + '<div class="review-card-header">'
            + '<span class="review-id">' + esc(r.id) + '</span>'
            + '<span class="review-booking">Pemesanan TXN' + esc(r.pemesananId) + '</span>'
            + '</div>'
            + '<div class="review-grid">'
            + '<div><div class="review-label">Jumlah</div>'
            + '<div class="review-value review-amount">'
            + esc(window.App.formatRupiah(r.jumlahRefund)) + '</div></div>'
            + '<div><div class="review-label">Alasan</div>'
            + '<div class="review-value">' + esc(r.alasan) + '</div></div>'
            + '<div><div class="review-label">Nama Penerima</div>'
            + '<div class="review-value">' + esc(r.namaPenerima) + '</div></div>'
            + '<div><div class="review-label">Rekening Tujuan</div>'
            + '<div class="review-value">' + esc(r.rekeningTujuan) + '</div></div>'
            + '</div>'
            + '<div class="review-actions">'
            + '<button type="button" class="review-button btn-reject" '
            + 'data-decision="reject" data-id="' + esc(r.id) + '">Tolak</button>'
            + '<button type="button" class="review-button btn-approve" '
            + 'data-decision="approve" data-id="' + esc(r.id) + '">Setujui</button>'
            + '</div>'
            + '</div>';
        }).join('');

        queue.querySelectorAll('[data-decision]').forEach(function (button) {
          button.addEventListener('click', function () {
            const id = button.dataset.id;
            const approving = button.dataset.decision === 'approve';

            // Both buttons on the card are disabled, not just the one pressed, so a
            // double click cannot send an approval and a rejection for one refund.
            const card = queue.querySelector('[data-refund="' + id + '"]');
            if (card) {
              card.querySelectorAll('button').forEach(function (b) { b.disabled = true; });
            }
            busy(button, true, approving ? 'Menyetujui...' : 'Menolak...');

            const decide = approving ? window.OwOAPI.approveRefund : window.OwOAPI.rejectRefund;
            decide(id)
              .then(function () {
                window.App.showNotification(
                  'Refund ' + id + (approving ? ' disetujui.' : ' ditolak.'));
                load();
              })
              .catch(function (err) {
                if (card) {
                  card.querySelectorAll('button').forEach(function (b) { b.disabled = false; });
                }
                busy(button, false);
                fail(err.message);
              });
          });
        });
      }
    }
  };

  /** Switches which `.content-section` of the refund screen is visible. */
  function showSection(id) {
    document.querySelectorAll('.content-section').forEach(function (section) {
      section.classList.toggle('active', section.id === id);
    });
  }

  /**
   * Loads the signed-in user's refunds and renders them under the open/finished tabs.
   *
   * @param highlightId a refund to scroll into view, used right after filing one
   */
  function loadRefundStatus(highlightId) {
    const list = byId('refundStatusList');
    if (!list) return;
    list.innerHTML = '<p style="padding:16px;color:#666;">Memuat...</p>';

    window.OwOAPI.getUserRefunds()
      .then(function (refunds) {
        const all = refunds || [];
        let filter = 'processing';

        function paint() {
          const rows = all.filter(function (r) {
            const open = REFUND_OPEN.indexOf(r.status) >= 0;
            return filter === 'processing' ? open : !open;
          });

          list.innerHTML = rows.length ? rows.map(refundCard).join('')
            : '<p style="padding:16px;color:#666;">Tidak ada refund pada kategori ini.</p>';

          if (highlightId) {
            const card = list.querySelector('[data-refund="' + highlightId + '"]');
            if (card) card.scrollIntoView({ block: 'center' });
          }
        }

        document.querySelectorAll('.status-tab').forEach(function (tab) {
          tab.disabled = false;
          tab.addEventListener('click', function () {
            filter = tab.dataset.filter || 'processing';
            document.querySelectorAll('.status-tab')
              .forEach(function (t) { t.classList.remove('active'); });
            tab.classList.add('active');
            paint();
          });
        });

        paint();
      })
      .catch(function (err) {
        list.innerHTML = '';
        fail(err.message);
      });
  }

  function refundCard(r) {
    const open = REFUND_OPEN.indexOf(r.status) >= 0;
    return '<div class="refund-card" data-refund="' + esc(r.id) + '">'
      + '<div class="flight-info">'
      + '<div><div class="flight-route">Refund ' + esc(r.id) + '</div>'
      + '<div class="flight-details">' + esc(r.alasan) + '</div></div>'
      + '<div class="flight-details">'
      + '<span class="booking-code">TXN' + esc(r.pemesananId) + '</span></div>'
      + '</div>'
      + '<div class="refund-info-grid">'
      + '<div class="info-item"><div class="info-label">Jumlah</div>'
      + '<div class="info-value">' + esc(window.App.formatRupiah(r.jumlahRefund)) + '</div></div>'
      + '<div class="info-item"><div class="info-label">Status</div>'
      + '<div class="status-badge ' + (open ? 'processing' : 'completed') + '">'
      + '<div class="status-dot ' + (open ? 'processing' : 'completed') + '"></div>'
      + esc(REFUND_LABEL[r.status] || r.status) + '</div></div>'
      + '<div class="info-item"><div class="info-label">Penerima</div>'
      + '<div class="info-value">' + esc(r.namaPenerima) + '</div></div>'
      + '<div class="info-item"><div class="info-label">Rekening Tujuan</div>'
      + '<div class="info-value">' + esc(r.rekeningTujuan) + '</div></div>'
      + '</div>'
      + '</div>';
  }
})();
