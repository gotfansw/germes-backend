// core helpers + cart + header/footer (stable cart)
window.GERMES = window.GERMES || {};
(function (G) {
  const BUILD_ID = "20260407a";
  const KEY = `germes_cart__${BUILD_ID}`;
  const SESSION_KEY = "germes_session_id";

  const getSessionId = () => {
    let sid = localStorage.getItem(SESSION_KEY);
    if (!sid) {
      sid = "sid_" + Math.random().toString(36).slice(2) + Date.now();
      localStorage.setItem(SESSION_KEY, sid);
    }
    return sid;
  };

  const safeJson = (value, fallback) => {
    try { return JSON.parse(value); } catch (_) { return fallback; }
  };

  const clamp = (n, min, max) => {
    const num = Number(n);
    if (!Number.isFinite(num)) return min;
    return Math.min(max, Math.max(min, Math.round(num)));
  };

  G.fmtRub = (v) => (
    Number.isFinite(Number(v))
      ? Number(v).toLocaleString("ru-RU", { maximumFractionDigits: 2 }) + " ₽"
      : "—"
  );

  G.byId = (id) => (G.data?.products || []).find((p) => p.id === Number(id)) || null;

  G.cart = {
    API: "https://germes-gotfa.amvera.run",

    _cache: null,

    _saveCache(cart) {
      G.cart._cache = cart;
      try {
        localStorage.setItem(KEY, JSON.stringify(cart));
      } catch (_) {}
    },

    _loadCache() {
      if (G.cart._cache) return G.cart._cache;
      try {
        const raw = localStorage.getItem(KEY);
        if (raw) G.cart._cache = JSON.parse(raw);
      } catch (_) {}
      return G.cart._cache || { items: [] };
    },

    read() {
      const cart = G.cart._loadCache();
      return {
        items: (cart.items || []).map((item) => ({
          id: Number(item.productId || item.id),
          qty: clamp(item.quantity || item.qty, 1, 999),
          itemId: Number(item.id || item.itemId || 0),
          productName: item.productName || "",
          price: Number(item.price || 0),
        })).filter((item) => Number.isFinite(item.id) && item.id > 0)
      };
    },

    count(cart) {
      return (cart || G.cart.read()).items.reduce((sum, item) => sum + item.qty, 0);
    },

    async fetch() {
      const res = await fetch(`${G.cart.API}/api/cart`, {
        method: "GET",
        credentials: "include",
        headers: { "X-Session-Id": getSessionId() }
      });
      if (!res.ok) throw new Error("cart_fetch_failed");
      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    async add(id, qty) {
      const productId = Number(id);
      const quantity = clamp(qty, 1, 999);
      if (!G.byId(productId)) throw new Error("invalid_product");

      const res = await fetch(`${G.cart.API}/api/cart/add`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-Session-Id": getSessionId()
        },
        body: JSON.stringify({ productId, quantity })
      });

      if (!res.ok) throw new Error("cart_add_failed");

      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    async set(itemId, qty) {
      const quantity = clamp(qty, 1, 999);
      const res = await fetch(`${G.cart.API}/api/cart/item/${itemId}`, {
        method: "PATCH",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-Session-Id": getSessionId()
        },
        body: JSON.stringify({ quantity })
      });
      if (!res.ok) throw new Error("cart_update_failed");
      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    async remove(itemId) {
      const res = await fetch(`${G.cart.API}/api/cart/item/${itemId}`, {
        method: "DELETE",
        credentials: "include",
        headers: { "X-Session-Id": getSessionId() }
      });
      if (!res.ok) throw new Error("cart_remove_failed");
      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    async clearRemote() {
      const res = await fetch(`${G.cart.API}/api/cart/clear`, {
        method: "DELETE",
        credentials: "include",
        headers: { "X-Session-Id": getSessionId() }
      });
      if (!res.ok) throw new Error("cart_clear_failed");
      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    clearLocal() {
      G.cart._cache = { items: [] };
      try { localStorage.removeItem(KEY); } catch (_) {}
      const empty = { items: [] };
      G.emitCartChange(empty);
      return empty;
    },

    async placeOrder(payload) {
      const res = await fetch(`${G.cart.API}/api/orders/place`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-Session-Id": getSessionId()
        },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(text || "order_failed");
      }
      return await res.json();
    },

    async updatePaymentStatus(orderId, paymentStatus) {
      const res = await fetch(`${G.cart.API}/api/orders/${orderId}/payment-status`, {
        method: "PATCH",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ paymentStatus })
      });

      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(text || "payment_status_failed");
      }

      const data = await res.json();
      if (paymentStatus === "PAID") {
        await G.cart.fetch().catch(() => G.cart.clearLocal());
      }
      return data;
    }
  };

  G.updateBadge = () => {
    const el = document.getElementById("cartCount");
    if (el) el.textContent = String(G.cart.count());
  };

  G.emitCartChange = (cart) => {
    G.updateBadge();
    try {
      window.dispatchEvent(new CustomEvent("germes:cartchange", { detail: cart || G.cart.read() }));
    } catch (_) {}
  };

  const cartSvg = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
    <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>`;

  G.renderShell = (active) => {
    const h = document.getElementById("appHeader");
    const f = document.getElementById("appFooter");
    if (!h || !f) return;

    const link = (key, href, text) => `<a href="${href}" class="nav__link${active === key ? " nav__link--active" : ""}">${text}</a>`;

    h.innerHTML = `
      <header class="header">
        <div class="container">
          <div class="header__inner">
            <a href="index.html" class="logo">
              <div class="logo__wrap">
                <div class="logo__mark">Germes <span style="font-size:22px;vertical-align:middle;">74</span></div>
                <div class="logo__sub">гермес74.рф</div>
              </div>
            </a>
            <div class="header__tagline">Расходные материалы для шиномонтажа<span>8 (351) 223-97-57 · 8-919-123-97-57</span></div>
            <form class="search-form" onsubmit="return false;">
              <div class="search-form__field">
                <input class="search-form__input" type="text" placeholder="Поиск по сайту..." aria-label="Поиск" autocomplete="off" />
                <div class="search-suggest" id="searchSuggest"></div>
              </div>
              <button class="search-form__btn" type="button">Поиск</button>
            </form>
            <a class="cart-btn" href="basket.html" aria-label="Корзина" title="Корзина">
              <span>${cartSvg}</span><span class="cart-btn__count" id="cartCount">0</span>
            </a>
          </div>
        </div>
      </header>
      <nav class="nav">
        <div class="container nav__container">
          <ul class="nav__list" id="navList">
            <li class="nav__item">${link("home","index.html","Главная")}</li>
            <li class="nav__item">
              ${link("catalog","catalog.html","Каталог")}
              <div class="nav__dropdown">
                <div class="nav__dropdown-section-title">Набивные</div>
                <a href="catalog.html?type=nabivnye&variant=litie">Набивные · литые</a>
                <a href="catalog.html?type=nabivnye&variant=shtamp">Набивные · штампованные</a>
                <div class="nav__dropdown-section-title">Самоклейка</div>
                <a href="catalog.html?type=samokley&variant=universal&material=pb">Самоклейка · Pb</a>
                <a href="catalog.html?type=samokley&variant=universal&material=fe">Самоклейка · Fe</a>
                <div class="nav__dropdown-section-title">Новые категории</div>
                <a href="catalog.html?categoryId=5">Груза на литые диски (алюм.)</a>
                <a href="catalog.html?categoryId=6">Груза на штампованные диски</a>
                <a href="catalog.html?categoryId=7">Самоклеящиеся свинцовые груза</a>
                <a href="catalog.html?categoryId=8">Самоклеящиеся груза Zn</a>
              </div>
            </li>
            <li class="nav__item">${link("delivery","delivery.html","Доставка")}</li>
            <li class="nav__item">${link("contacts","contacts.html","Контакты")}</li>
            <li class="nav__item">${link("about","about.html","О нас")}</li>
          </ul>
          <button class="nav__burger" id="navBurger" aria-label="Открыть меню">
            <span></span><span></span><span></span>
          </button>
        </div>
      </nav>
    `;

    const searchForm = h.querySelector(".search-form");
    const searchInput = h.querySelector(".search-form__input");
    const searchBtn = h.querySelector(".search-form__btn");

    const currentQuery = (() => {
      try {
        return new URLSearchParams(window.location.search).get("q") || "";
      } catch (_) {
        return "";
      }
    })();

    if (searchInput && currentQuery) searchInput.value = currentQuery;

    const suggestBox = h.querySelector("#searchSuggest");
    const getSuggestions = (raw) => {
      const q = G.normalizeQuery(raw);
      if (!q) return [];
      const names = (G.data?.products || [])
        .filter((p) => p.categoryId !== 4)
        .map((p) => G.cleanName(p.name));
      const fixed = Array.from(new Set([
        "Набивные",
        "Самоклейка",
        "Литые",
        "Штампованные",
        "Pb",
        ...names
      ]));
      return fixed.filter((item) => G.normalizeQuery(item).includes(q)).slice(0, 6);
    };

    const renderSuggestions = () => {
      if (!suggestBox) return;
      const items = getSuggestions(searchInput?.value || "");
      suggestBox.innerHTML = items.map((item) => `<button class="search-suggest__item" type="button">${item}</button>`).join("");
      suggestBox.classList.toggle("is-open", items.length > 0);
    };

    const goSearch = () => {
      const q = (searchInput?.value || "").trim();
      location.href = q ? `catalog.html?q=${encodeURIComponent(q)}` : "catalog.html";
    };

    searchBtn?.addEventListener("click", goSearch);
    searchForm?.addEventListener("submit", (e) => {
      e.preventDefault();
      goSearch();
    });
    searchInput?.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        goSearch();
      }
    });
    searchInput?.addEventListener("input", renderSuggestions);
    document.addEventListener("click", (e) => {
      const item = e.target.closest(".search-suggest__item");
      if (item && searchInput) {
        searchInput.value = item.textContent.trim();
        renderSuggestions();
        goSearch();
        return;
      }
      if (!e.target.closest(".search-form__field")) {
        suggestBox?.classList.remove("is-open");
      }
    });

    f.innerHTML = `
      <footer>
        <div class="container">
          <div class="footer__grid">
            <div class="footer__cell">
              <div class="footer__col-title">Навигация</div>
              <nav class="footer__links">
                <a href="index.html">Главная</a>
                <a href="catalog.html">Каталог</a>
                <a href="delivery.html">Доставка</a>
                <a href="contacts.html">Контакты</a>
                <a href="about.html">О нас</a>
              </nav>
            </div>
            <div class="footer__cell">
              <div class="footer__col-title">О нас</div>
              <p class="footer__about">Обособленное подразделение «Гермес» гарантирует вам не только качество товара, но и доступную цену!</p>
            </div>
            <div class="footer__cell">
              <div class="footer__col-title">Контакты</div>
              <div class="footer__contacts">
                <div class="footer__contact-item">
                  <strong>Телефон</strong>
                  <a href="tel:+73512239757">+7 (351) 223-97-57</a>
                </div>
                <div class="footer__contact-item">
                  <strong>Мобильный</strong>
                  <a href="tel:+79191239757">+7 (919) 123-97-57</a>
                </div>
                <div class="footer__contact-item">
                  <strong>E-mail</strong>
                  <a href="mailto:89191239757@mail.ru">89191239757@mail.ru</a>
                </div>
                <div class="footer__contact-item">
                  <strong>Адрес</strong>
                  г. Челябинск, ул. Молодогвардейцев, д.7к3, пом. 12
                </div>
              </div>
            </div>
            <div class="footer__cell">
              <div class="footer__col-title">Время работы</div>
              <div class="footer__hours">
                <div class="footer__hour-row">
                  <span class="footer__hour-day">Пн–Пт</span>
                  <span class="footer__hour-time">9:00 – 18:00</span>
                </div>
                <div class="footer__hour-row">
                  <span class="footer__hour-day">Сб–Вс</span>
                  <span class="footer__hour-time">Выходной</span>
                </div>
              </div>
            </div>
          </div>
          <div class="footer__bottom">
            <span>© Компания «ГЕРМЕС»</span>
            <a href="#">Политика конфиденциальности</a>
          </div>
        </div>
      </footer>
    `;

    G.updateBadge();

    // Бургер-меню для мобильных
    const burger  = document.getElementById("navBurger");
    const navList = document.getElementById("navList");
    if (burger && navList) {
      burger.addEventListener("click", function () {
        const isOpen = navList.classList.toggle("nav__list--open");
        burger.classList.toggle("nav__burger--open", isOpen);
        burger.setAttribute("aria-label", isOpen ? "Закрыть меню" : "Открыть меню");
      });
      // Закрыть меню при клике на ссылку
      navList.addEventListener("click", function (e) {
        if (e.target.tagName === "A") {
          navList.classList.remove("nav__list--open");
          burger.classList.remove("nav__burger--open");
        }
      });
      // Закрыть меню при клике вне
      document.addEventListener("click", function (e) {
        if (!burger.contains(e.target) && !navList.contains(e.target)) {
          navList.classList.remove("nav__list--open");
          burger.classList.remove("nav__burger--open");
        }
      });
    }
  };

  window.addEventListener("pageshow", () => G.updateBadge());
  window.addEventListener("storage", () => G.updateBadge());
  window.addEventListener("germes:cartchange", () => G.updateBadge());
})(window.GERMES);