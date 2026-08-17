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

  /**
   * Shows a message on the current screen.
   *
   * This used to fall back to `alert` when a screen had no banner. WebView shows nothing
   * for `alert` unless an `onAlert` handler is installed, and nothing installs one — so on
   * the two screens that never called `ensureBanner`, every booking failure was silent.
   * The banner is created on demand instead, so there is no path that reports nothing.
   */
  function fail(message) {
    let banner = byId('screenError');
    if (!banner) {
      ensureBanner(document.getElementById('app') || document.body);
      banner = byId('screenError');
    }
    if (!banner) return;
    banner.textContent = message;
    banner.classList.remove('hidden');
    banner.scrollIntoView({ block: 'nearest' });
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

  /**
   * Turns a destructive button into a two-step confirmation.
   *
   * WebView's native `confirm()` resolves to false unless a confirm handler is installed
   * on the engine, and nothing installs one — a dialog-based confirmation would therefore
   * refuse every cancellation without showing anything. Asking for a second click needs
   * no platform dialog. The armed state lapses on its own so a stray first click does not
   * leave a live destructive button behind.
   */
  function confirmTwice(button, armedLabel, onConfirmed) {
    if (!button) return;
    const original = button.textContent;
    let armed = false;
    let timer = null;

    function disarm() {
      armed = false;
      button.textContent = original;
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
    }

    button.addEventListener('click', function (e) {
      e.preventDefault();
      if (!armed) {
        armed = true;
        button.textContent = armedLabel;
        timer = setTimeout(disarm, 6000);
        return;
      }
      disarm();
      onConfirmed();
    });
  }

  /**
   * Cancels a booking and returns to the history.
   *
   * Cancelling is what releases the claimed ticket back to the catalogue; simply leaving
   * the payment screen does not, which is why every state the transition table allows to
   * CANCELLED offers this.
   */
  function cancelBooking(bookingId, button) {
    busy(button, true, 'Membatalkan...');
    window.OwOAPI.cancelBooking(bookingId)
      .then(function () {
        window.App.showNotification('Pemesanan dibatalkan.');
        window.App.navigate('RiwayatPemesanan');
      })
      .catch(function (err) {
        busy(button, false);
        fail(err.message);
      });
  }

  /** One labelled cell in a result card's info grid. */
  function infoItem(label, value) {
    return '<div class="flight-info-item">'
      + '<span class="flight-info-label">' + esc(label) + '</span>'
      + '<span class="flight-info-value">' + esc(value) + '</span>'
      + '</div>';
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

  /**
   * Which section of the Informasi screen each chrome label opens.
   *
   * Nine labels in the header, sidebar and footer called preventDefault and returned, so
   * every policy link, the contact details and the company profile were decoration. The
   * content they promise is a real obligation of a booking system — what a refund pays,
   * when a cancellation is free, what is stored — so they lead to it rather than being
   * deleted.
   */
  const INFO_TOPICS = {
    'tentang kami': 'tentang',
    'profil perusahaan': 'profil',
    'kontak': 'kontak',
    'bantuan dan keluhan': 'bantuan',
    'syarat & ketentuan': 'syarat',
    'syarat dan ketentuan': 'syarat',
    'kebijakan privasi': 'privasi',
    'kebijakan pembatalan': 'pembatalan'
  };

  /** Wires the shared page chrome: header nav, sidebar menu, logo. */
  function wireChrome() {
    document.querySelectorAll('.nav-item, .footer-link, .menu-item, .sidebar-title a')
      .forEach(function (el) {
        const label = (el.textContent || '').trim().toLowerCase();
        let target = null;
        let context = null;

        if (label === 'pesawat' || label === 'hotel' || label === 'owo'
            || label === 'pemesanan') {
          target = 'Pemesanan';
        } else if (label === 'refund') {
          // Without a booking the refund screen shows what has been filed, which is
          // what this menu entry means. Filing starts from a booking in the history.
          target = 'RefundForm';
        } else if (label === 'tinjau refund') {
          target = 'TinjauRefund';
        } else if (label === 'pengaturan' || label === 'pengaturan akun') {
          target = 'PengaturanAkun';
        } else if (INFO_TOPICS[label]) {
          target = 'Informasi';
          context = { topik: INFO_TOPICS[label] };
        }

        el.addEventListener('click', function (e) {
          e.preventDefault();
          if (label === 'log out') {
            window.App.logout();
          } else if (target) {
            window.App.navigate(target, context);
          }
        });
      });

    // The header greeting was the literal text "Hi, Owo!" on every screen, so it read as
    // a signed-in name without ever being one.
    const session = window.App.session;
    if (session) {
      document.querySelectorAll('.user-greeting').forEach(function (el) {
        el.textContent = 'Hi, ' + session.nama + '!';
      });
    }

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

      // Class names are the fragment's own. They used to be invented here — flight-main,
      // flight-side, select-button — none of which the stylesheet defines, so every result
      // rendered as unstyled text with a default browser button. No test can see that;
      // ./gradlew screenshots can.
      list.innerHTML = results.map(function (f) {
        return '<div class="flight-card" data-id="' + esc(f.id) + '">'
          + '<div class="flight-content">'
          + '<div class="flight-header">'
          + '<div class="flight-title">' + esc(f.maskapai) + '</div>'
          + '<div class="flight-info-value">' + esc(f.flightNumber) + '</div>'
          + '</div>'
          + '<div class="flight-details">'
          + '<div class="flight-time">' + esc(f.origin) + '</div>'
          + '<div class="flight-duration"><div class="flight-path"></div></div>'
          + '<div class="flight-time">' + esc(f.destination) + '</div>'
          + '</div>'
          + '<div class="flight-info">'
          + infoItem('Keberangkatan', window.App.formatDateTime(f.departure))
          + infoItem('Kelas', f.kelas)
          + infoItem('Kapasitas', (f.kapasitas || 1) + ' orang')
          + '</div>'
          + '<div class="flight-footer">'
          + '<div class="flight-price">'
          + '<span class="price-amount">' + esc(window.App.formatRupiah(f.price)) + '</span>'
          + '<span class="price-period">per orang</span>'
          + '</div>'
          + '<button type="button" class="book-button" data-id="' + esc(f.id) + '">Pilih</button>'
          + '</div>'
          + '</div>'
          + '</div>';
      }).join('');

      // Selection is by primary key. Matching on the airline name booked whichever
      // flight happened to share it.
      list.querySelectorAll('.book-button').forEach(function (button) {
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
              fail(err.message);
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

      // As on the flight screen: these class names are the ones the fragment styles,
      // rather than a parallel set the stylesheet has never heard of.
      list.innerHTML = results.map(function (h) {
        return '<div class="hotel-card" data-id="' + esc(h.id) + '">'
          + '<div class="hotel-content">'
          + '<div class="hotel-header">'
          + '<div class="hotel-title">' + esc(h.hotelName) + '</div>'
          + '</div>'
          + '<div class="hotel-location">' + esc(h.address) + '</div>'
          + '<div class="hotel-amenities">'
          + '<span class="amenity">Kamar ' + esc(h.roomNumber) + '</span>'
          + '<span class="amenity">' + esc(h.kapasitas || 1) + ' tamu</span>'
          + '<span class="amenity">' + esc(window.App.formatDate(h.checkin)) + ' – '
          + esc(window.App.formatDate(h.checkout)) + '</span>'
          + '</div>'
          + '<div class="hotel-footer">'
          + '<div class="hotel-price">'
          + '<span class="price-amount">' + esc(window.App.formatRupiah(h.price)) + '</span>'
          + '<span class="price-period">per kamar</span>'
          + '</div>'
          + '<button type="button" class="book-button" data-id="' + esc(h.id) + '">Pilih</button>'
          + '</div>'
          + '</div>'
          + '</div>';
      }).join('');

      list.querySelectorAll('.book-button').forEach(function (button) {
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
              fail(err.message);
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

      // Every figure is asked for rather than worked out here. The page used to invent
      // tax as ten per cent of the ticket price and render a total the server never saw,
      // so nothing could tell whether the amount shown matched the amount charged.
      window.OwOAPI.quoteBooking(booking.id)
        .then(function (tagihan) {
          setMoney('base-price', tagihan.dasar);
          setMoney('taxes-fees', tagihan.pajak);
          setMoney('total-price', tagihan.total);
        })
        .catch(function (err) {
          // Paying without knowing the amount is worse than not being able to pay.
          busy(payButton, true, 'Biaya tidak tersedia');
          fail(err.message);
        });

      function setMoney(id, value) {
        if (byId(id)) byId(id).textContent = window.App.formatRupiah(value);
      }

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

      // A PENDING booking already holds its ticket, so leaving this screen unpaid used to
      // take the unit out of the catalogue for everybody with nothing able to give it back.
      confirmTwice(byId('cancel-booking-button'), 'Klik lagi untuk membatalkan', function () {
        cancelBooking(booking.id, byId('cancel-booking-button'));
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

      wireCheckInModal();

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

        // PENDING and CONFIRMED are exactly the states PemesananStatus allows to reach
        // CANCELLED; offering it anywhere else would only produce a refusal from Java.
        let cancelButton = '';
        if (b.status === 'PENDING' || b.status === 'CONFIRMED') {
          cancelButton = '<button type="button" class="action-button btn-cancel" '
            + 'data-action="cancel" data-id="' + esc(b.id) + '">Batalkan</button>';
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
          + '<div class="booking-actions">' + payButton + checkInButton + refundButton
          + cancelButton + '</div>'
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

          if (button.dataset.action === 'cancel') {
            // Cancelling is irreversible and releases the ticket, so it asks twice.
            confirmTwice(button, 'Klik lagi untuk membatalkan', function () {
              cancelBooking(id, button);
            });
            return;
          }

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
       * The booking the modal is currently confirming.
       *
       * The modal is one element that is never re-rendered, so its handlers are attached
       * once and read this. Attaching them per open left every previous opening's handler
       * in place: opening the modal for one booking, cancelling, then confirming another
       * checked in both.
       */
      let pendingCheckIn = null;

      function wireCheckInModal() {
        const modal = byId('checkinModal');
        if (!modal) return;

        function dismiss() {
          modal.style.display = 'none';
          pendingCheckIn = null;
        }

        [modal.querySelector('.modal-btn-secondary'), modal.querySelector('.close-button')]
          .forEach(function (el) {
            if (el) el.addEventListener('click', dismiss);
          });

        const confirm = modal.querySelector('.modal-btn-primary');
        if (confirm) {
          confirm.addEventListener('click', function () {
            if (!pendingCheckIn) return;
            show('modal-action-buttons', false);
            show('modal-loading', true);
            performCheckIn(pendingCheckIn, modal);
          });
        }
      }

      /**
       * Confirms a check-in before performing it.
       *
       * Check-in is not reversible by the customer, so it gets a confirmation step
       * showing what is about to be checked into. The modal markup was already in the
       * screen but nothing opened it, and the action fired straight from the card.
       */
      function openCheckIn(booking) {
        if (!booking) return;

        const modal = byId('checkinModal');
        if (!modal) {
          // No confirmation step available; the check-in itself still has to work.
          performCheckIn(booking, null);
          return;
        }

        pendingCheckIn = booking;

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

  /**
   * Refund statuses that are still being decided or paid, versus finished.
   *
   * FAILED belongs here: a failed disbursement is retried rather than re-decided, so it
   * is still outstanding from the customer's point of view.
   */
  const REFUND_OPEN = ['PENDING_REVIEW', 'APPROVED', 'PROCESSING', 'FAILED'];

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
   * What an administrator can do to a refund in each status.
   *
   * Deciding a refund is not the end of it: an approved refund still has to be paid out.
   * PROCESSING, COMPLETED and FAILED had no writer anywhere, so an approved refund sat
   * under "Sedang Diproses" for ever and "Selesai" could only ever hold rejections.
   */
  const REVIEW_ACTIONS = {
    PENDING_REVIEW: [
      { key: 'reject', label: 'Tolak', busy: 'Menolak...', className: 'btn-reject' },
      { key: 'approve', label: 'Setujui', busy: 'Menyetujui...', className: 'btn-approve' }
    ],
    APPROVED: [
      { key: 'process', label: 'Mulai Pencairan', busy: 'Memproses...', className: 'btn-approve' }
    ],
    PROCESSING: [
      { key: 'fail', label: 'Tandai Gagal', busy: 'Menyimpan...', className: 'btn-reject' },
      { key: 'complete', label: 'Tandai Selesai', busy: 'Menyimpan...', className: 'btn-approve' }
    ],
    FAILED: [
      { key: 'process', label: 'Coba Cairkan Lagi', busy: 'Memproses...', className: 'btn-approve' }
    ]
  };

  const REVIEW_CALL = {
    approve: function (id) { return window.OwOAPI.approveRefund(id); },
    reject: function (id) { return window.OwOAPI.rejectRefund(id); },
    process: function (id) { return window.OwOAPI.processRefund(id); },
    complete: function (id) { return window.OwOAPI.completeRefund(id); },
    fail: function (id) { return window.OwOAPI.failRefund(id); }
  };

  /**
   * The administrator work queue.
   *
   * Approval and rejection existed with tested rules but nothing could reach them, so
   * every refund stayed in review for ever. Authority is enforced in Java; this screen
   * is only refused politely if a non-administrator reaches it.
   */
  S.TinjauRefund = {
    init: function () {
      wireChrome();
      ensureBanner(document.querySelector('.review-container') || document.body);

      // Every other controller guards its container; this one dereferenced it directly,
      // so a markup change would have failed here with a null property read rather than
      // a message.
      const queue = byId('refundQueue');
      if (!queue) {
        fail('Antrean refund tidak dapat ditampilkan.');
        return;
      }
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
            '<p class="queue-empty">Tidak ada refund yang perlu ditindaklanjuti.</p>';
          return;
        }

        queue.innerHTML = rows.map(function (r) {
          const actions = REVIEW_ACTIONS[r.status] || [];
          return '<div class="review-card" data-refund="' + esc(r.id) + '">'
            + '<div class="review-card-header">'
            + '<span class="review-id">' + esc(r.id) + '</span>'
            + '<span class="review-booking">Pemesanan TXN' + esc(r.pemesananId)
            + ' • ' + esc(REFUND_LABEL[r.status] || r.status) + '</span>'
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
            + actions.map(function (a) {
              return '<button type="button" class="review-button ' + a.className + '" '
                + 'data-decision="' + a.key + '" data-busy="' + esc(a.busy) + '" '
                + 'data-id="' + esc(r.id) + '">'
                + esc(a.label) + '</button>';
            }).join('')
            + '</div>'
            + '</div>';
        }).join('');

        queue.querySelectorAll('[data-decision]').forEach(function (button) {
          button.addEventListener('click', function () {
            const id = button.dataset.id;
            const action = button.dataset.decision;
            const label = button.textContent;

            // Every button on the card is disabled, not just the one pressed, so a double
            // click cannot send two conflicting outcomes for one refund.
            const card = queue.querySelector('[data-refund="' + id + '"]');
            if (card) {
              card.querySelectorAll('button').forEach(function (b) { b.disabled = true; });
            }

            busy(button, true, button.dataset.busy || 'Memproses...');

            REVIEW_CALL[action](id)
              .then(function () {
                window.App.showNotification('Refund ' + id + ': ' + label.toLowerCase() + '.');
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

  // ----------------------------------------------------------------- Informasi

  S.Informasi = {
    init: function (context) {
      wireChrome();

      const panels = document.querySelectorAll('.info-panel');
      const buttons = document.querySelectorAll('.info-menu-item');
      if (!panels.length) return;

      // An unknown topic shows the first section rather than an empty page, so a link
      // added to the markup without a mapping still lands somewhere readable.
      show((context && context.topik) || 'tentang');

      buttons.forEach(function (button) {
        button.addEventListener('click', function () { show(button.dataset.topik); });
      });

      function show(topik) {
        let matched = false;
        panels.forEach(function (panel) {
          const isMatch = panel.dataset.topik === topik;
          panel.hidden = !isMatch;
          matched = matched || isMatch;
        });
        if (!matched) {
          panels[0].hidden = false;
          topik = panels[0].dataset.topik;
        }
        buttons.forEach(function (button) {
          button.classList.toggle('active', button.dataset.topik === topik);
        });
      }
    }
  };

  // ------------------------------------------------------------ PengaturanAkun

  S.PengaturanAkun = {
    init: function () {
      wireChrome();
      ensureBanner(document.querySelector('.akun-container') || document.body);

      const session = window.App.session || {};
      if (byId('akunEmail')) byId('akunEmail').textContent = session.email || '-';
      if (byId('akunNama')) byId('akunNama').value = session.nama || '';

      const namaButton = byId('akunSimpanNama');
      if (namaButton) {
        namaButton.addEventListener('click', function () {
          const nama = ((byId('akunNama') || {}).value || '').trim();
          if (!nama) {
            fail('Nama tidak boleh kosong.');
            return;
          }

          busy(namaButton, true, 'Menyimpan...');
          window.OwOAPI.updateProfile(nama)
            .then(function (akun) {
              busy(namaButton, false);
              // setSession, not an assignment: App.session is a getter, and it is what
              // repaints the session bar. Without this the header and the shell keep
              // showing the old name until the next login.
              window.App.setSession(akun);
              document.querySelectorAll('.user-greeting').forEach(function (el) {
                el.textContent = 'Hi, ' + akun.nama + '!';
              });
              reveal('akunNamaOk');
            })
            .catch(function (err) {
              busy(namaButton, false);
              fail(err.message);
            });
        });
      }

      const sandiButton = byId('akunSimpanSandi');
      if (sandiButton) {
        sandiButton.addEventListener('click', function () {
          const lama = (byId('akunSandiLama') || {}).value || '';
          const baru = (byId('akunSandiBaru') || {}).value || '';
          const ulang = (byId('akunSandiUlang') || {}).value || '';

          // Checked here as well as in Java: a mistyped confirmation is the user's own
          // slip, and there is nothing for the server to decide about it.
          if (baru !== ulang) {
            fail('Konfirmasi kata sandi tidak cocok.');
            return;
          }

          busy(sandiButton, true, 'Menyimpan...');
          window.OwOAPI.changePassword(lama, baru)
            .then(function () {
              busy(sandiButton, false);
              ['akunSandiLama', 'akunSandiBaru', 'akunSandiUlang'].forEach(function (id) {
                if (byId(id)) byId(id).value = '';
              });
              reveal('akunSandiOk');
            })
            .catch(function (err) {
              busy(sandiButton, false);
              fail(err.message);
            });
        });
      }

      function reveal(id) {
        const el = byId(id);
        if (el) el.classList.remove('hidden');
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

          wirePayeeEditing(list, all, paint);

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

  /**
   * Lets the payee be corrected while a refund is still awaiting review.
   *
   * A wrong account number is only discoverable after submission, and once a decision is
   * made the payee is part of the record — so this is offered on pending refunds only,
   * which is the same window the backend enforces.
   */
  function wirePayeeEditing(list, all, repaint) {
    list.querySelectorAll('[data-edit-payee]').forEach(function (button) {
      button.addEventListener('click', function () {
        const id = button.dataset.editPayee;
        const refund = all.filter(function (r) { return r.id === id; })[0];
        const card = list.querySelector('[data-refund="' + id + '"]');
        if (!refund || !card || card.querySelector('.payee-form')) return;

        // An inline form, not window.prompt. WebView has no prompt handler unless one is
        // installed, and nothing installs one — prompt() returns an empty string without
        // ever showing a dialog, so this button used to submit two blank fields and be
        // refused by the backend every time.
        const form = document.createElement('div');
        form.className = 'payee-form';
        form.innerHTML =
          '<label class="payee-label" for="payeeNama">Nama penerima</label>'
          + '<input class="payee-input" id="payeeNama" type="text" value="'
          + esc(refund.namaPenerima || '') + '">'
          + '<label class="payee-label" for="payeeRekening">Rekening tujuan</label>'
          + '<input class="payee-input" id="payeeRekening" type="text" value="'
          + esc(refund.rekeningTujuan || '') + '">'
          + '<div class="payee-actions">'
          + '<button type="button" class="payee-cancel" id="payeeCancel">Batal</button>'
          + '<button type="button" class="payee-save" id="payeeSave">Simpan</button>'
          + '</div>';
        card.appendChild(form);
        button.disabled = true;

        form.querySelector('#payeeCancel').addEventListener('click', function () {
          form.remove();
          button.disabled = false;
        });

        form.querySelector('#payeeSave').addEventListener('click', function () {
          const save = form.querySelector('#payeeSave');
          busy(save, true, 'Menyimpan...');

          window.OwOAPI.updateRefundPayee(
            id,
            form.querySelector('#payeeNama').value,
            form.querySelector('#payeeRekening').value
          )
            .then(function (updated) {
              refund.namaPenerima = updated.namaPenerima;
              refund.rekeningTujuan = updated.rekeningTujuan;
              window.App.showNotification('Detail pencairan diperbarui.');
              repaint();
            })
            .catch(function (err) {
              busy(save, false);
              fail(err.message);
            });
        });
      });
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
      + (r.status === 'PENDING_REVIEW'
          ? '<button type="button" class="payee-edit-button" data-edit-payee="'
            + esc(r.id) + '">Ubah detail pencairan</button>'
          : '')
      + '</div>';
  }
})();
