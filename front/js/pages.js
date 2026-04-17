(function (G) {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const clamp = (n, min, max) => Math.min(max, Math.max(min, Number(n) || min));
  const params = new URLSearchParams(location.search);
  const cleanName = (value) => (G.cleanName ? G.cleanName(value) : String(value || "").replace(/\s*\([^)]*\)/g, "").trim());

  const imgById = (id) => `img/products/${Number(id)}.jpg`;
  const parseProductBits = (product) => {
    const grams = /([0-9]+)\s*гр/i.exec(product.name)?.[1] || "—";
    const pack = /(?:\(|\s)([0-9]+)\s*шт/i.exec(product.name)?.[1] || (product.id === 13 ? "200" : "—");
    const variant = product.categoryId === 1 ? "Литые" : product.categoryId === 2 ? "Штампованные" : "Универсальная";
    return {
      grams,
      pack,
      variant,
      type: product.categoryId >= 3 ? "Самоклейка" : "Набивные",
      material: product.categoryId === 3 ? "Pb" : product.categoryId === 4 ? "Fe" : "Сталь",
    };
  };

  const getBadges = (product) => ({
    inStock: ![5, 10, 15, 20, 25, 30].includes(product.id),
    hot: [1, 4, 13, 28, 31].includes(product.id),
  });

  const normalizeText = (value) => String(value || "")
    .toLowerCase()
    .replace(/ё/g, "е")
    .replace(/[^a-zа-я0-9]+/gi, " ")
    .trim();

  const buildSearchText = (product) => {
    const category = (G.data?.categories || []).find((item) => item.id === product.categoryId)?.name || "";
    const bits = parseProductBits(product);
    return normalizeText([
      product.name,
      category,
      bits.type,
      bits.material,
      bits.grams !== "—" ? `${bits.grams} гр ${bits.grams}г` : "",
      bits.pack !== "—" ? `${bits.pack} шт ${bits.pack}шт` : "",
      `id ${product.id}`
    ].join(" "));
  };

  const matchesSearch = (product, query) => {
    if (!query) return true;
    const normalizedQuery = G.normalizeQuery ? G.normalizeQuery(query) : normalizeText(query);
    if (!normalizedQuery) return true;
    const haystack = buildSearchText(product);
    return normalizedQuery.split(/\s+/).filter(Boolean).every((token) => haystack.includes(token));
  };

  // ─────────────────────────────────────────────────────────────────
  // Загрузка данных с бэкенда
  // ─────────────────────────────────────────────────────────────────
  const API = "https://germes-backend-production.up.railway.app";

  async function loadDataFromBackend() {
    const [catsRes, prodsRes] = await Promise.all([
      fetch(`${API}/api/products/categories`),
      fetch(`${API}/api/products`)
    ]);
    if (!catsRes.ok || !prodsRes.ok) throw new Error("backend_error");
    const categories = await catsRes.json();
    const products   = await prodsRes.json();
    // Перезаписываем глобальные данные живыми данными с бэкенда
    G.data.categories = categories;
    G.data.products   = products;
    // Обновляем G.byId чтобы корзина тоже видела актуальные товары
    G.byId = (id) => (G.data.products || []).find((p) => p.id === Number(id)) || null;
    return products;
  }

  function initCatalog() {
    G.renderShell("catalog");

    const grid = $("#catalogGrid");
    const count = $("#catalogCount");
    const priceRange = $("#priceRange");
    const priceVal = $("#priceVal");
    if (!grid || !count) return;

    const state = {
      page: 1,
      perPage: 6,
      type: (params.get("type") || "").trim(),
      variant: (params.get("variant") || "").trim(),
      material: (params.get("material") || "").trim(),
      packWeight: (params.get("packWeight") || "").trim(),
      q: (params.get("q") || "").trim().toLowerCase(),
      maxPrice: Number(priceRange?.value || 10000),
    };

    const weightOptions = [
      { label: "100 г",  value: "100",  pack: 100 },
      { label: "250 г",  value: "250",  pack: 200 },
      { label: "500 г",  value: "500",  pack: 50  },
      { label: "1 кг",   value: "1kg",  pack: 25  },
    ];

    // allProducts теперь изменяемый — будет перезаписан после загрузки
    let allProducts = G.data?.products || [];
    const headerSearch = document.querySelector(".search-form__input");

    const syncSearchUrl = () => {
      const url = new URL(window.location.href);
      if (state.q) url.searchParams.set("q", state.q);
      else url.searchParams.delete("q");
      if (state.type) url.searchParams.set("type", state.type);
      else url.searchParams.delete("type");
      if (state.variant) url.searchParams.set("variant", state.variant);
      else url.searchParams.delete("variant");
      if (state.material) url.searchParams.set("material", state.material);
      else url.searchParams.delete("material");
      if (state.packWeight) url.searchParams.set("packWeight", state.packWeight);
      else url.searchParams.delete("packWeight");
      history.replaceState(null, "", `${url.pathname}${url.search}`);
      if (headerSearch) headerSearch.value = state.q;
    };

    document.querySelector(".search-form")?.addEventListener("submit", (e) => {
      e.preventDefault();
      state.q = (headerSearch?.value || "").trim().toLowerCase();
      state.page = 1;
      syncSearchUrl();
      render();
    });

    const syncFilterLinks = () => {
      $$('[data-type]').forEach((el) => {
        el.classList.toggle('filter-tag--active', state.type === (el.dataset.type || ""));
      });
      $$('[data-variant]').forEach((el) => {
        el.classList.toggle('filter-tag--active', state.variant === (el.dataset.variant || ""));
      });
      $$('[data-material]').forEach((el) => {
        el.classList.toggle('filter-tag--active', state.material === (el.dataset.material || ""));
      });
      $$('[data-weight]').forEach((el) => {
        el.classList.toggle('filter-tag--active', state.packWeight === (el.dataset.weight || ""));
      });
    };

    const applyFilters = () => {
      let items = [...allProducts];
      if (state.type === "nabivnye") items = items.filter((p) => p.categoryId === 1 || p.categoryId === 2);
      if (state.type === "samokley") items = items.filter((p) => p.categoryId === 3 || p.categoryId === 4);
      if (state.variant === "litie") items = items.filter((p) => p.categoryId === 1);
      if (state.variant === "shtamp") items = items.filter((p) => p.categoryId === 2);
      if (state.variant === "universal") items = items.filter((p) => p.categoryId === 3 || p.categoryId === 4);
      if (state.material === "pb") items = items.filter((p) => p.categoryId === 3);
      if (state.material === "fe") items = items.filter((p) => p.categoryId === 4);
      if (state.packWeight) {
        const opt = weightOptions.find((w) => w.value === state.packWeight);
        if (opt) {
          items = items.filter((p) => {
            const pack = /(?:\(|\s)([0-9]+)\s*шт/i.exec(p.name)?.[1] || (p.id === 13 ? "200" : null);
            return pack && Number(pack) === opt.pack;
          });
        }
      }
      if (state.q) items = items.filter((p) => matchesSearch(p, state.q));
      items = items.filter((p) => p.price <= state.maxPrice);
      return items;
    };

    const renderPagination = (totalPages) => {
      const nav = $(".pagination");
      if (!nav) return;
      if (totalPages <= 1) { nav.innerHTML = ""; return; }
      const pages = Array.from({ length: totalPages }, (_, i) => i + 1)
        .map((n) => `<button class="pagination__btn${n === state.page ? " pagination__btn--active" : ""}" data-page="${n}">${n}</button>`)
        .join("");
      nav.innerHTML = `
        <button class="pagination__btn pagination__btn--nav" data-page="prev">← Предыдущая</button>
        ${pages}
        <button class="pagination__btn pagination__btn--nav" data-page="next">Следующая →</button>
      `;
    };

    const renderWeightFilters = () => {
      const container = $("#weightFilters");
      if (!container) return;
      container.innerHTML = weightOptions.map((w) =>
        `<button class="filter-tag${state.packWeight === w.value ? ' filter-tag--active' : ''}" type="button" data-weight="${w.value}">${w.label}</button>`
      ).join("");
    };

    const render = () => {
      const items = applyFilters();
      const totalPages = Math.max(1, Math.ceil(items.length / state.perPage));
      if (state.page > totalPages) state.page = totalPages;
      const start = (state.page - 1) * state.perPage;
      const visible = items.slice(start, start + state.perPage);
      count.textContent = state.q ? `Найдено: ${items.length}` : `${items.length} товаров`;
      grid.innerHTML = visible.length ? visible.map((p) => {
        const bits = parseProductBits(p);
        const title = cleanName(p.name);
        return `
          <article class="catalog-card" data-id="${p.id}" data-price="${p.price}">
            <a href="product.html?id=${p.id}"><img class="catalog-card__img" src="${imgById(p.id)}" alt="${title}"></a>
            <div class="catalog-card__body">
              <p class="catalog-card__title"><a href="product.html?id=${p.id}">${title}</a></p>
              <p class="catalog-card__subtitle">${bits.type} · ${bits.variant}${bits.material === "Pb" || bits.material === "Fe" ? " · " + bits.material : ""} · ${bits.grams !== '—' ? bits.grams + ' г' : '—'}</p>
              <p class="catalog-card__price">Цена: <span class="catalog-card__price-value">${G.fmtRub(p.price)}</span></p>
              <div class="catalog-card__actions">
                <div class="qty-control">
                  <button class="qty-control__btn" type="button" data-act="minus">−</button>
                  <input class="qty-control__input" type="number" min="1" max="999" value="1">
                  <button class="qty-control__btn" type="button" data-act="plus">+</button>
                </div>
                <button class="btn-cart" type="button" data-act="add">В корзину</button>
              </div>
              <a class="catalog-card__more" href="product.html?id=${p.id}">Подробнее</a>
            </div>
          </article>
        `;
      }).join("") : `
        <div class="catalog-empty">
          <div class="catalog-empty__title">Ничего не найдено</div>
          <div class="catalog-empty__text">Попробуй изменить запрос, категорию или диапазон цены.</div>
        </div>
      `;
      renderPagination(totalPages);
      renderWeightFilters();
      syncFilterLinks();

      const titleEl = $(".catalog-header__title");
      if (titleEl) {
        if (state.q) titleEl.textContent = `Поиск: ${state.q}`;
        else if (state.type === "nabivnye" && state.variant === "litie") titleEl.textContent = "Набивные · литые";
        else if (state.type === "nabivnye" && state.variant === "shtamp") titleEl.textContent = "Набивные · штампованные";
        else if (state.material === "pb") titleEl.textContent = "Самоклейка · Pb";
        else if (state.material === "fe") titleEl.textContent = "Самоклейка · Fe";
        else if (state.type === "nabivnye") titleEl.textContent = "Набивные";
        else if (state.type === "samokley") titleEl.textContent = "Самоклейка";
        else titleEl.textContent = "Каталог";
      }
    };

    if (priceVal && priceRange) {
      const syncPrice = () => {
        state.maxPrice = Number(priceRange.value);
        priceVal.textContent = `${state.maxPrice.toLocaleString("ru-RU")} Р`;
      };
      priceVal.textContent = `${Number(priceRange.value).toLocaleString("ru-RU")} Р`;
      priceRange.addEventListener("input", syncPrice);
      const applyPriceFilter = () => { syncPrice(); state.page = 1; render(); };
      priceRange.addEventListener("change", applyPriceFilter);
      priceRange.addEventListener("mouseup", applyPriceFilter);
      priceRange.addEventListener("touchend", applyPriceFilter);
    }

    grid.addEventListener("click", (e) => {
      const btn = e.target.closest("[data-act]");
      if (!btn) return;
      const card = btn.closest(".catalog-card");
      if (!card) return;
      const input = $(".qty-control__input", card);
      const qty = clamp(input?.value, 1, 999);
      const priceNode = $(".catalog-card__price-value", card);
      const basePrice = Number(card.dataset.price || 0);
      const refreshCardPrice = () => {
        if (priceNode) priceNode.textContent = G.fmtRub(basePrice * clamp(input?.value, 1, 999));
      };
      if (btn.dataset.act === "minus") { e.preventDefault(); e.stopPropagation(); input.value = String(Math.max(1, qty - 1)); refreshCardPrice(); return; }
      if (btn.dataset.act === "plus") { e.preventDefault(); e.stopPropagation(); input.value = String(Math.min(999, qty + 1)); refreshCardPrice(); return; }
      if (btn.dataset.act === "add") {
        e.preventDefault(); e.stopPropagation();
        if (btn.dataset.busy === "1") return;
        btn.dataset.busy = "1";
        G.cart.add(card.dataset.id, qty).then(() => {
          G.updateBadge();
          btn.textContent = "✓ Добавлено";
          btn.classList.add("pulse");
        }).catch(() => {
          alert("Не удалось добавить товар. Проверьте, что backend запущен.");
        }).finally(() => {
          setTimeout(() => { btn.textContent = "В корзину"; btn.classList.remove("pulse"); btn.dataset.busy = "0"; }, 1200);
        });
      }
    });

    document.addEventListener("click", (e) => {
      const pageBtn = e.target.closest(".pagination__btn[data-page]");
      if (pageBtn) {
        const value = pageBtn.dataset.page;
        const totalPages = Math.max(1, Math.ceil(applyFilters().length / state.perPage));
        if (value === "prev") state.page = Math.max(1, state.page - 1);
        else if (value === "next") state.page = Math.min(totalPages, state.page + 1);
        else state.page = Number(value) || 1;
        render();
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
      }

      const typeBtn = e.target.closest('button[data-type]');
      if (typeBtn) {
        e.preventDefault();
        state.page = 1;
        state.type = typeBtn.dataset.type || "";
        state.variant = "";
        state.material = "";
        syncSearchUrl();
        render();
        return;
      }

      const variantBtn = e.target.closest('button[data-variant]');
      if (variantBtn) {
        e.preventDefault();
        state.page = 1;
        state.variant = state.variant === variantBtn.dataset.variant ? "" : (variantBtn.dataset.variant || "");
        if (state.variant === "litie" || state.variant === "shtamp") state.type = "nabivnye";
        state.material = "";
        syncSearchUrl();
        render();
        return;
      }

      const materialBtn = e.target.closest('button[data-material]');
      if (materialBtn) {
        e.preventDefault();
        state.page = 1;
        state.material = state.material === materialBtn.dataset.material ? "" : (materialBtn.dataset.material || "");
        if (state.material) { state.type = "samokley"; state.variant = "universal"; }
        syncSearchUrl();
        render();
        return;
      }

      const weightBtn = e.target.closest('button[data-weight]');
      if (weightBtn) {
        e.preventDefault();
        state.page = 1;
        state.packWeight = state.packWeight === weightBtn.dataset.weight ? "" : (weightBtn.dataset.weight || "");
        syncSearchUrl();
        render();
        return;
      }
    });

    $("#resetFilters")?.addEventListener("click", () => {
      state.page = 1;
      state.type = "";
      state.variant = "";
      state.material = "";
      state.packWeight = "";
      state.q = "";
      if (priceRange) priceRange.value = 10000;
      state.maxPrice = 10000;
      if (priceVal) priceVal.textContent = "10 000 Р";
      syncSearchUrl();
      const headerSearchField = document.querySelector(".search-form__input");
      if (headerSearchField) headerSearchField.value = "";
      render();
    });

    const sidebarToggle = $("#sidebarToggle");
    const sidebarBody = $("#sidebarBody");
    const syncSidebar = () => {
      if (!sidebarBody) return;
      if (window.innerWidth > 768) { sidebarBody.classList.add("open"); sidebarBody.style.maxHeight = "none"; }
      else if (!sidebarBody.classList.contains("open")) { sidebarBody.style.maxHeight = "0"; }
    };
    sidebarToggle?.addEventListener("click", () => {
      if (window.innerWidth > 768 || !sidebarBody) return;
      sidebarBody.classList.toggle("open");
      sidebarToggle.classList.toggle("open");
      sidebarBody.style.maxHeight = sidebarBody.classList.contains("open") ? "1000px" : "0";
    });
    window.addEventListener("resize", syncSidebar);
    syncSidebar();

    // ── Показываем заглушку, потом грузим с бэкенда и перерисовываем
    grid.innerHTML = `<div class="catalog-empty"><div class="catalog-empty__title">Загрузка...</div></div>`;

    loadDataFromBackend()
      .then((products) => {
        allProducts = products;
        render();
      })
      .catch((err) => {
        console.warn("Бэкенд недоступен, используем data.js:", err);
        // allProducts уже содержит данные из data.js — просто рендерим
        render();
      });
  }

  function initProduct() {
    G.renderShell("catalog");
    const id = Number(params.get("id") || 13);

    const doInit = () => {
      const product = G.byId(id);
      if (!product) return;

      const category = (G.data?.categories || []).find((item) => item.id === product.categoryId);
      const bits = parseProductBits(product);
      const badges = getBadges(product);
      const galleryImages = [imgById(product.id), imgById(product.id), imgById(product.id)];

      const top = document.querySelector(".buybox__top");
      if (top) {
        top.innerHTML = `
          ${badges.inStock ? '<span class="badge badge--ok">В наличии</span>' : '<span class="badge">Под заказ</span>'}
          ${badges.hot ? '<span class="badge badge--hot">Хит продаж</span>' : ""}
        `;
      }

      $("#pTitle").textContent = cleanName(product.name);
      $("#propType").textContent = bits.type;
      $("#propMaterial").textContent = bits.material;
      $("#propWeight").textContent = bits.grams === "—" ? "—" : `${bits.grams} г`;
      $("#propPack").textContent = bits.pack === "—" ? "—" : `${bits.pack} шт.`;
      $("#crumbCat").textContent = category?.name || "Категория";
      $("#crumbCat").href = category?.id === 1 ? "catalog.html?type=nabivnye&variant=litie" : category?.id === 2 ? "catalog.html?type=nabivnye&variant=shtamp" : category?.id === 3 ? "catalog.html?type=samokley&variant=universal&material=pb" : "catalog.html?type=samokley&variant=universal&material=fe";

      const mainImg = $("#mainImg");
      const thumbs = $("#thumbs");
      mainImg.src = galleryImages[0];
      mainImg.alt = product.name;
      thumbs.innerHTML = galleryImages.map((src, i) => `
        <button class="thumb${i === 0 ? " is-active" : ""}" type="button" data-src="${src}">
          <img src="${src}" alt="${product.name}">
        </button>
      `).join("");
      thumbs.addEventListener("click", (e) => {
        const thumb = e.target.closest(".thumb");
        if (!thumb) return;
        mainImg.src = thumb.dataset.src;
        $$(".thumb", thumbs).forEach((item) => item.classList.toggle("is-active", item === thumb));
      });

      const input = $("#qtyInput");
      const boxsetWrap = $("#boxsetWrap");
      const boxsetSelect = $("#boxsetSelect");
      let packMode = "box";

      const updateProductPrice = () => {
        const qty = clamp(input.value, 1, 999);
        const boxMultiplier = packMode === "boxset" ? clamp(boxsetSelect?.value || 10, 8, 12) : 1;
        const total = Number(product.price) * qty * boxMultiplier;
        $("#pPrice").textContent = G.fmtRub(total);
        $("#pOld").textContent = `${Math.round(total * 1.17).toLocaleString("ru-RU")} ₽`;
        $("#pPer").textContent = bits.pack === "—" ? "" : `${(Number(product.price) / Number(bits.pack)).toFixed(2)} ₽ за шт. · ${packMode === "boxset" ? boxMultiplier + " коробок в боксе" : "1 коробка"} · ${qty} шт.`;
        $("#packNote").textContent = packMode === "boxset" ? `Бокс: ${boxMultiplier} коробок` : "Одна коробка";
      };

      const sync = () => { input.value = String(clamp(input.value, 1, 999)); updateProductPrice(); };
      sync();

      $("#qtyMinus").addEventListener("click", () => { input.value = String(Math.max(1, clamp(input.value, 1, 999) - 1)); sync(); });
      $("#qtyPlus").addEventListener("click", () => { input.value = String(Math.min(999, clamp(input.value, 1, 999) + 1)); sync(); });
      input.addEventListener("input", sync);
      input.addEventListener("change", sync);
      input.addEventListener("keyup", sync);
      input.addEventListener("mouseup", sync);
      boxsetSelect?.addEventListener("change", sync);
      $$("[data-pack-mode]").forEach((btn) => btn.addEventListener("click", () => {
        packMode = btn.dataset.packMode || "box";
        $$("[data-pack-mode]").forEach((item) => item.classList.toggle("is-active", item === btn));
        if (boxsetWrap) boxsetWrap.hidden = packMode !== "boxset";
        sync();
      }));

      const buyBtn = $("#buyBtn");
      buyBtn.textContent = "В корзину";
      buyBtn.addEventListener("click", () => {
        if (buyBtn.dataset.busy === "1") return;
        buyBtn.dataset.busy = "1";
        const boxMultiplier = packMode === "boxset" ? clamp(boxsetSelect?.value || 10, 8, 12) : 1;
        G.cart.add(product.id, clamp(input.value, 1, 999) * boxMultiplier).then(() => {
          G.updateBadge();
          buyBtn.textContent = "✓ Добавлено";
          buyBtn.classList.add("is-added");
        }).catch(() => {
          alert("Не удалось добавить товар. Проверьте, что backend запущен.");
        }).finally(() => {
          setTimeout(() => { buyBtn.textContent = "В корзину"; buyBtn.classList.remove("is-added"); buyBtn.dataset.busy = "0"; }, 1200);
        });
      });

      $$('[data-tab]').forEach((button) => {
        button.addEventListener("click", () => {
          const name = button.dataset.tab;
          $$('[data-tab]').forEach((item) => item.classList.toggle('is-active', item.dataset.tab === name));
          $$('[data-pane]').forEach((item) => item.classList.toggle('is-active', item.dataset.pane === name));
        });
      });
    };

    // Грузим актуальные данные с бэкенда, потом инициализируем страницу товара
    loadDataFromBackend()
      .catch((err) => console.warn("Бэкенд недоступен, используем data.js:", err))
      .finally(doInit);
  }

  function initCart() {
    G.renderShell("cart");

    const list      = $("#cartList");
    const empty     = $("#cartEmpty");
    const note      = $("#cartNote");
    const modal     = $("#sbpModal");
    const createBtn = $("#createOrderBtn");
    const emailInput    = $("#customerEmail");
    const deliverySelect = $("#deliverySelect");
    const qrImage   = $("#sbpQrImage");
    const qrLink    = $("#sbpQrLink");
    const qrWrap    = $("#sbpQrWrap");
    const statusEl  = $("#sbpStatus");
    const timerEl   = $("#sbpTimer");
    const timerVal  = $("#sbpTimerVal");
    const receiptEl = $("#sbpReceipt");
    const trackEl   = $("#sbpTrack");
    const metaEl    = $("#sbpMeta");

    const trash = `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/>
    </svg>`;

    const state = {
      pendingOrderId: null,
      pollId: null,
      timerId: null,
      secondsLeft: 600,
    };

    const openModal = () => {
      if (!modal) return;
      modal.removeAttribute("aria-hidden");
      modal.style.display = "flex";
      setPaymentStatus("", "");
      if (qrWrap) qrWrap.style.display = "none";
      if (metaEl) metaEl.style.display = "none";
      if (timerEl) timerEl.style.display = "none";
      if (createBtn) { createBtn.disabled = false; createBtn.textContent = "Показать QR для оплаты"; }
    };

    const closeModal = () => {
      if (!modal) return;
      modal.setAttribute("aria-hidden", "true");
      modal.style.display = "none";
      stopPoll();
      stopTimer();
      state.pendingOrderId = null;
    };

    const setPaymentStatus = (type, msg) => {
      if (!statusEl) return;
      statusEl.textContent = msg;
      statusEl.style.display = msg ? "block" : "none";
      statusEl.className = "sbp-status" + (type ? ` sbp-status--${type}` : "");
    };

    const stopTimer = () => { clearInterval(state.timerId); state.timerId = null; };
    const startTimer = () => {
      state.secondsLeft = 600;
      if (timerEl) timerEl.style.display = "block";
      const tick = () => {
        state.secondsLeft--;
        const m = String(Math.floor(state.secondsLeft / 60)).padStart(2, "0");
        const s = String(state.secondsLeft % 60).padStart(2, "0");
        if (timerVal) timerVal.textContent = `${m}:${s}`;
        if (state.secondsLeft <= 0) {
          stopTimer(); stopPoll();
          setPaymentStatus("failed", "Время оплаты истекло");
          if (createBtn) { createBtn.disabled = false; createBtn.textContent = "Показать QR для оплаты"; }
        }
      };
      tick();
      state.timerId = setInterval(tick, 1000);
    };

    const stopPoll = () => { clearInterval(state.pollId); state.pollId = null; };

    const startAutoConfirm = () => {
      stopPoll();
      state.pollId = setInterval(async () => {
        if (!state.pendingOrderId) return;
        try {
          const res = await fetch(`${G.cart.API}/api/orders/${state.pendingOrderId}/payment-status`, { method: "GET", credentials: "include" });
          if (!res.ok) return;
          const payment = await res.json();
          if (receiptEl) receiptEl.textContent = payment.receiptNumber || "—";
          if (trackEl) trackEl.textContent = payment.trackNumber || "—";
          if (payment.paymentStatus === "PAID") {
            stopPoll(); stopTimer(); setPaymentStatus("paid", "Оплата подтверждена"); closeModal();
            await G.cart.fetch().catch(() => G.cart.clearLocal()); render();
            if (note) note.style.display = "block";
            return;
          }
          if (payment.paymentStatus === "FAILED") {
            stopPoll(); stopTimer(); setPaymentStatus("failed", "Оплата отклонена");
            if (createBtn) { createBtn.disabled = false; createBtn.textContent = "Показать QR для оплаты"; }
            return;
          }
          if (payment.paymentStatus === "CANCELED") {
            stopPoll(); stopTimer(); setPaymentStatus("canceled", "Оплата отменена");
            if (createBtn) { createBtn.disabled = false; createBtn.textContent = "Показать QR для оплаты"; }
          }
        } catch (_) {}
      }, 2500);
    };

    const render = () => {
      const cart = G.cart.read();
      const rows = cart.items.map((it) => ({ ...it, ...G.byId(it.id) })).filter((it) => it.name);
      const totalItems = rows.reduce((s, it) => s + it.qty, 0);
      const subtotal = rows.reduce((s, it) => s + Number(it.price) * it.qty, 0);
      const delivery = rows.length ? 1500 : 0;
      const total = subtotal + delivery;
      $("#sumItems").textContent = `${totalItems} шт.`;
      $("#sumSubtotal").textContent = G.fmtRub(subtotal);
      $("#sumDelivery").textContent = G.fmtRub(delivery);
      $("#sumTotal").textContent = G.fmtRub(total);
      if (empty) empty.style.display = rows.length ? "none" : "block";
      if (list) list.innerHTML = rows.map((item) => `
        <article class="card cart-row" data-id="${item.id}" data-item-id="${item.itemId}">
          <img class="cart-row__img" src="${imgById(item.id)}" alt="${item.name}">
          <div class="cart-row__info">
            <div class="cart-row__name">${cleanName(item.name)}</div>
            <div class="cart-row__meta">Арт: ${String(item.id).padStart(4, "0")}</div>
            <div class="cart-row__price">Цена ${G.fmtRub(item.price)}</div>
            <div class="cart-row__qty">
              <button type="button" class="cart-qty__btn" data-act="minus">−</button>
              <input class="cart-qty__input" type="number" min="1" max="999" value="${item.qty}" data-act="qty">
              <button type="button" class="cart-qty__btn" data-act="plus">+</button>
            </div>
          </div>
          <button type="button" class="cart-row__trash" data-act="remove" aria-label="Удалить">${trash}</button>
        </article>
      `).join("");
      G.updateBadge();
    };

    list?.addEventListener("click", async (e) => {
      const btn = e.target.closest("[data-act]");
      if (!btn) return;
      const row = btn.closest(".cart-row");
      const itemId = Number(row?.dataset.itemId);
      if (!itemId) return;
      const input = $("[data-act='qty']", row);
      const qty = clamp(input?.value, 1, 999);
      try {
        if (btn.dataset.act === "minus") await G.cart.set(itemId, Math.max(1, qty - 1));
        if (btn.dataset.act === "plus") await G.cart.set(itemId, Math.min(999, qty + 1));
        if (btn.dataset.act === "remove") await G.cart.remove(itemId);
        render();
      } catch (_) { setPaymentStatus("failed", "Не удалось обновить корзину"); }
    });

    list?.addEventListener("change", async (e) => {
      const input = e.target.closest("[data-act='qty']");
      if (!input) return;
      const row = input.closest(".cart-row");
      const itemId = Number(row.dataset.itemId);
      try { await G.cart.set(itemId, clamp(input.value, 1, 999)); render(); }
      catch (_) { setPaymentStatus("failed", "Не удалось обновить корзину"); }
    });

    $("#clearCartTop")?.addEventListener("click", async () => {
      try { await G.cart.clearRemote(); } catch (_) { G.cart.clearLocal(); }
      render();
    });

    $("#payBtn")?.addEventListener("click", () => { if (!G.cart.count()) return; openModal(); });

    document.addEventListener("click", (e) => {
      if (e.target.closest(".sbpClose") || e.target.id === "sbpClose") closeModal();
    });

    createBtn?.addEventListener("click", async () => {
      const customerEmail = (emailInput?.value || "").trim();
      const deliveryType = deliverySelect?.value || "";
      const customerPhone = (document.getElementById("customerPhone")?.value || "").trim();
      const deliveryAddress = (document.getElementById("deliveryAddress")?.value || "").trim();
      if (!customerEmail || !deliveryType) { setPaymentStatus("failed", "Укажите e-mail и способ доставки"); return; }
      createBtn.disabled = true;
      createBtn.textContent = "Ожидаем оплату...";
      try {
        const order = await G.cart.placeOrder({
          customerEmail,
          customerPhone,
          deliveryAddress,
          deliveryType,
          paymentMethod: "SBP",
          paymentConfirmed: false
        });
        state.pendingOrderId = order.id;
        if (receiptEl) receiptEl.textContent = order.receiptNumber || "—";
        if (trackEl) trackEl.textContent = order.trackNumber || "—";
        if (metaEl) metaEl.style.display = "block";
        if (qrImage) qrImage.src = `${G.cart.API}/api/orders/${order.id}/sbp-qr?ts=${Date.now()}`;
        if (qrLink) qrLink.href = `${G.cart.API}/api/orders/${order.id}/pay`;
        if (qrWrap) qrWrap.style.display = "block";
        setPaymentStatus("pending", "Отсканируйте QR-код или нажмите на него для оплаты");
        createBtn.textContent = "QR-код показан";
        startTimer();
        startAutoConfirm();
      } catch (err) {
        setPaymentStatus("failed", "Не удалось создать заказ. Проверьте backend.");
        createBtn.disabled = false;
        createBtn.textContent = "Показать QR для оплаты";
      }
    });

    G.cart.fetch().then(render).catch(render);
  }

  document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    if (page === "catalog") return initCatalog();
    if (page === "product") return initProduct();
    if (page === "cart") return initCart();
    G.renderShell("home");
  });
})(window.GERMES);