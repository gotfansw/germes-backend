// core helpers + cart + header/footer (stable cart)
window.GERMES = window.GERMES || {};
(function (G) {
  const BUILD_ID = "20260407a";
  const KEY = `germes_cart__${BUILD_ID}`;

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

  // ─── CART ─────────────────────────────────────────────────────────────────
  // Корзина полностью серверная (HttpSession на бэке).
  // localStorage используется только как кэш для badge-счётчика.
  // credentials: "include" обязателен — иначе браузер не шлёт JSESSIONID.
  // ──────────────────────────────────────────────────────────────────────────

  G.cart = {
    API: "https://germes-backend-production.up.railway.app",

    // Кэш последнего известного состояния корзины (только для badge)
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

    // Синхронное чтение из кэша (для badge)
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

    // Загрузить корзину с сервера и обновить кэш
    async fetch() {
      const res = await fetch(`${G.cart.API}/api/cart`, {
        method: "GET",
        credentials: "include"
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
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId, quantity })
      });

      if (!res.ok) throw new Error("cart_add_failed");

      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    async remove(itemId) {
      const res = await fetch(`${G.cart.API}/api/cart/item/${itemId}`, {
        method: "DELETE",
        credentials: "include"
      });
      if (!res.ok) throw new Error("cart_remove_failed");
      const cart = await res.json();
      G.cart._saveCache(cart);
      G.emitCartChange(cart);
      return cart;
    },

    clear() {
      G.cart._cache = null;
      try { localStorage.removeItem(KEY); } catch (_) {}
      const empty = { items: [] };
      G.emitCartChange(empty);
      return empty;
    },

    async placeOrder() {
      const cart = G.cart._loadCache();
      if (!cart || !(cart.items || []).length) throw new Error("cart_missing");

      const res = await fetch(`${G.cart.API}/api/orders/place/${cart.id}`, {
        method: "POST",
        credentials: "include"
      });

      if (!res.ok) throw new Error("order_failed");

      G.cart.clear();
      return await res.json().catch(() => ({ ok: true }));
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
            <div class="header__tagline">Расходные материалы для шиномонтажа<span>+7 (351) 223-97-57</span></div>
            <form class="search-form" onsubmit="return false;">
              <input class="search-form__input" type="text" placeholder="Поиск по сайту..." aria-label="Поиск" />
              <button class="search-form__btn" type="button">Поиск</button>
            </form>
            <a class="cart-btn" href="basket.html" aria-label="Корзина" title="Корзина">
              <span>${cartSvg}</span><span class="cart-btn__count" id="cartCount">0</span>
            </a>
          </div>
        </div>
      </header>
      <nav class="nav">
        <div class="container">
          <ul class="nav__list">
            <li class="nav__item">${link("home","index.html","Главная")}</li>
            <li class="nav__item">
              ${link("catalog","catalog.html","Каталог")}
              <div class="nav__dropdown">
                <a href="catalog.html#cat=1">Литые диски</a>
                <a href="catalog.html#cat=2">Штампованные диски</a>
                <a href="catalog.html#cat=3">Самоклеящиеся Pb</a>
                <a href="catalog.html#cat=4">Самоклеящиеся Zn</a>
                <a href="catalog.html">Все товары</a>
              </div>
            </li>
            <li class="nav__item">${link("delivery","delivery.html","Доставка")}</li>
            <li class="nav__item">${link("coop","#","Сотрудничество")}</li>
            <li class="nav__item">${link("contacts","contacts.html","Контакты")}</li>
            <li class="nav__item">${link("about","about.html","О нас")}</li>
          </ul>
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

    f.innerHTML = `
      <footer>
        <div class="container">
          <div class="footer__grid">
            <div>
              <div class="footer__col-title">Главная</div>
              <nav class="footer__links">
                <a href="catalog.html">Каталог</a><a href="#">Доставка</a><a href="#">Полезное</a><a href="#">Сотрудничество</a>
              </nav>
            </div>
            <div>
              <div class="footer__col-title">О нас</div>
              <p class="footer__about">Обособленное подразделение «Гермес» гарантирует вам не только качество товара, но и доступную цену!</p>
            </div>
            <div>
              <div class="footer__col-title">Контакты</div>
              <div class="footer__contacts">
                <div class="footer__contact-item">Тел: <a href="tel:+73512239757">+7 (351) 223-97-57</a></div>
                <div class="footer__contact-item">E-mail: <a href="mailto:89191239757@mail.ru">89191239757@mail.ru</a></div>
                <div class="footer__contact-item">г. Челябинск, ул. Молодогвардейцев, д. 7/3</div>
              </div>
            </div>
            <div>
              <div class="footer__col-title">Время работы</div>
              <div class="footer__schedule">
                <div>Пн-Чт с 9:00 до 18:00</div>
                <div>Пт с 9:00 до 17:00</div>
                <div>Сб-Вс выходной</div>
              </div>
            </div>
          </div>
          <div class="footer__bottom">© Компания "ГЕРМЕС" <span>Политика конфиденциальности</span></div>
        </div>
      </footer>
    `;

    G.updateBadge();
  };

  window.addEventListener("pageshow", () => G.updateBadge());
  window.addEventListener("storage", () => G.updateBadge());
  window.addEventListener("germes:cartchange", () => G.updateBadge());
})(window.GERMES);