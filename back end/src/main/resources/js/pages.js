(function (G) {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const clamp = (n, min, max) => Math.min(max, Math.max(min, Number(n) || min));
  const params = new URLSearchParams(location.search);

  const imgById = (id) => `img/products/${Number(id)}.jpg`;
  const parseProductBits = (product) => {
    const grams = /([0-9]+)\s*гр/i.exec(product.name)?.[1] || "—";
    const pack = /\(([0-9]+)\s*шт/i.exec(product.name)?.[1] || (product.id === 13 ? "200" : "—");
    return {
      grams,
      pack,
      type: product.categoryId >= 3 ? "Самоклеящиеся" : "Набивные",
      material: product.categoryId === 3 ? "Свинец" : product.categoryId === 4 ? "Цинк" : "Сталь",
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
    const normalizedQuery = normalizeText(query);
    if (!normalizedQuery) return true;
    const haystack = buildSearchText(product);
    return normalizedQuery.split(/\s+/).filter(Boolean).every((token) => haystack.includes(token));
  };


  function initCatalog() {
    G.renderShell("catalog");

    const grid = $("#catalogGrid");
    const count = $("#catalogCount");
    const priceRange = $("#priceRange");
    const priceVal = $("#priceVal");
    const chips = $("#categoryChips");
    if (!grid || !count) return;

    const state = {
      page: 1,
      perPage: 6,
      category: Number((location.hash.match(/cat=(\d+)/) || [])[1]) || null,
      q: (params.get("q") || "").trim().toLowerCase(),
      maxPrice: Number(priceRange?.value || 10000),
    };

    const categories = G.data?.categories || [];
    const allProducts = G.data?.products || [];
    const headerSearch = document.querySelector(".search-form__input");

    const syncSearchUrl = () => {
      const url = new URL(window.location.href);
      if (state.q) url.searchParams.set("q", state.q);
      else url.searchParams.delete("q");
      history.replaceState(null, "", `${url.pathname}${url.search}${url.hash}`);
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
      $$('[data-cat]').forEach((el) => {
        const cat = Number(el.dataset.cat) || null;
        el.classList.toggle('filter-tag--active', state.category === cat);
        el.classList.toggle('category-chip--active', state.category === cat);
      });
    };

    const applyFilters = () => {
      let items = [...allProducts];
      if (state.category) items = items.filter((p) => p.categoryId === state.category);
      if (state.q) items = items.filter((p) => matchesSearch(p, state.q));
      items = items.filter((p) => p.price <= state.maxPrice);
      return items;
    };

    const renderPagination = (totalPages) => {
      const nav = $(".pagination");
      if (!nav) return;
      if (totalPages <= 1) {
        nav.innerHTML = "";
        return;
      }
      const pages = Array.from({ length: totalPages }, (_, i) => i + 1)
        .map((n) => `<button class="pagination__btn${n === state.page ? " pagination__btn--active" : ""}" data-page="${n}">${n}</button>`)
        .join("");
      nav.innerHTML = `
        <button class="pagination__btn pagination__btn--nav" data-page="prev">← Предыдущая</button>
        ${pages}
        <button class="pagination__btn pagination__btn--nav" data-page="next">Следующая →</button>
      `;
    };

    const renderChips = () => {
      if (!chips) return;
      chips.innerHTML = `
        <button class="category-chip" type="button" data-cat="">Все товары</button>
        ${categories.map((c) => `<button class="category-chip" type="button" data-cat="${c.id}">${c.name}</button>`).join("")}
      `;
      syncFilterLinks();
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
        return `
          <article class="catalog-card" data-id="${p.id}" data-price="${p.price}">
            <a href="product.html?id=${p.id}"><img class="catalog-card__img" src="${imgById(p.id)}" alt="${p.name}"></a>
            <div class="catalog-card__body">
              <p class="catalog-card__title"><a href="product.html?id=${p.id}">${p.name}</a></p>
              <p class="catalog-card__subtitle">${bits.material} · ${bits.pack} шт. · ${bits.grams !== '—' ? bits.grams + ' г' : '—'}</p>
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
      const titleEl = $(".catalog-header__title");
      if (titleEl) {
        if (state.q) titleEl.textContent = `Поиск: ${state.q}`;
        else titleEl.textContent = state.category ? (categories.find((c) => c.id === state.category)?.name || "Каталог") : "Каталог";
      }
      syncFilterLinks();
    };

    if (priceVal && priceRange) {
      const syncPrice = () => {
        state.maxPrice = Number(priceRange.value);
        priceVal.textContent = `${state.maxPrice.toLocaleString("ru-RU")} Р`;
      };

      priceVal.textContent = `${Number(priceRange.value).toLocaleString("ru-RU")} Р`;
      priceRange.addEventListener("input", syncPrice);

      const applyPriceFilter = () => {
        syncPrice();
        state.page = 1;
        render();
      };

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

      if (btn.dataset.act === "minus") {
        e.preventDefault();
        e.stopPropagation();
        input.value = String(Math.max(1, qty - 1));
        refreshCardPrice();
        return;
      }

      if (btn.dataset.act === "plus") {
        e.preventDefault();
        e.stopPropagation();
        input.value = String(Math.min(999, qty + 1));
        refreshCardPrice();
        return;
      }

      if (btn.dataset.act === "add") {
        e.preventDefault();
        e.stopPropagation();
        if (btn.dataset.busy === "1") return;
        btn.dataset.busy = "1";
        G.cart.add(card.dataset.id, qty).then(() => {
          G.updateBadge();
          btn.textContent = "✓ Добавлено";
          btn.classList.add("pulse");
        }).catch(() => {
          alert("Не удалось добавить товар. Проверьте, что backend запущен.");
        }).finally(() => {
          setTimeout(() => {
            btn.textContent = "В корзину";
            btn.classList.remove("pulse");
            btn.dataset.busy = "0";
          }, 1200);
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

      const catBtn = e.target.closest('[data-cat]');
      if (catBtn && (catBtn.classList.contains('filter-tag') || catBtn.classList.contains('category-chip'))) {
        e.preventDefault();
        state.page = 1;
        state.category = Number(catBtn.dataset.cat) || null;
        location.hash = state.category ? `cat=${state.category}` : '';
        render();
      }
    });

    window.addEventListener('hashchange', () => {
      state.category = Number((location.hash.match(/cat=(\d+)/) || [])[1]) || null;
      state.page = 1;
      render();
    });

    $("#resetFilters")?.addEventListener("click", () => {
      state.page = 1;
      state.category = null;
      state.q = "";
      if (priceRange) priceRange.value = 10000;
      state.maxPrice = 10000;
      if (priceVal) priceVal.textContent = "10 000 Р";
      history.replaceState(null, '', location.pathname);
      const headerSearchField = document.querySelector(".search-form__input");
      if (headerSearchField) headerSearchField.value = "";
      render();
    });

    $(".btn-filter--apply")?.addEventListener("click", () => {
      state.page = 1;
      render();
    });

    const sidebarToggle = $("#sidebarToggle");
    const sidebarBody = $("#sidebarBody");
    const syncSidebar = () => {
      if (!sidebarBody) return;
      if (window.innerWidth > 768) {
        sidebarBody.classList.add("open");
        sidebarBody.style.maxHeight = "none";
      } else if (!sidebarBody.classList.contains("open")) {
        sidebarBody.style.maxHeight = "0";
      }
    };
    sidebarToggle?.addEventListener("click", () => {
      if (window.innerWidth > 768 || !sidebarBody) return;
      sidebarBody.classList.toggle("open");
      sidebarToggle.classList.toggle("open");
      sidebarBody.style.maxHeight = sidebarBody.classList.contains("open") ? "1000px" : "0";
    });
    window.addEventListener("resize", syncSidebar);
    syncSidebar();

    renderChips();
    render();
  }

  function initProduct() {
    G.renderShell("catalog");
    const id = Number(params.get("id") || 13);
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

    $("#pTitle").textContent = product.name;
    $("#propType").textContent = bits.type;
    $("#propMaterial").textContent = bits.material;
    $("#propWeight").textContent = bits.grams === "—" ? "—" : `${bits.grams} г`;
    $("#propPack").textContent = bits.pack === "—" ? "—" : `${bits.pack} шт.`;
    $("#crumbCat").textContent = category?.name || "Категория";
    $("#crumbCat").href = `catalog.html#cat=${product.categoryId}`;

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

    const updateProductPrice = () => {
      const qty = clamp(input.value, 1, 999);
      const total = Number(product.price) * qty;
      $("#pPrice").textContent = G.fmtRub(total);
      $("#pOld").textContent = `${Math.round(total * 1.17).toLocaleString("ru-RU")} ₽`;
      $("#pPer").textContent = bits.pack === "—" ? "" : `${(Number(product.price) / Number(bits.pack)).toFixed(2)} ₽ за шт. · ${qty} шт.`;
    };

    const sync = () => {
      input.value = String(clamp(input.value, 1, 999));
      updateProductPrice();
    };

    sync();

    $("#qtyMinus").addEventListener("click", () => {
      input.value = String(Math.max(1, clamp(input.value, 1, 999) - 1));
      sync();
    });

    $("#qtyPlus").addEventListener("click", () => {
      input.value = String(Math.min(999, clamp(input.value, 1, 999) + 1));
      sync();
    });

    input.addEventListener("input", sync);
    input.addEventListener("change", sync);
    input.addEventListener("keyup", sync);
    input.addEventListener("mouseup", sync);

    const buyBtn = $("#buyBtn");
    buyBtn.textContent = "В корзину";
    buyBtn.addEventListener("click", () => {
      if (buyBtn.dataset.busy === "1") return;
      buyBtn.dataset.busy = "1";
      G.cart.add(product.id, clamp(input.value, 1, 999)).then(() => {
        G.updateBadge();
        buyBtn.textContent = "✓ Добавлено";
        buyBtn.classList.add("is-added");
      }).catch(() => {
        alert("Не удалось добавить товар. Проверьте, что backend запущен.");
      }).finally(() => {
        setTimeout(() => {
          buyBtn.textContent = "В корзину";
          buyBtn.classList.remove("is-added");
          buyBtn.dataset.busy = "0";
        }, 1200);
      });
    });

    $$('[data-tab]').forEach((button) => {
      button.addEventListener("click", () => {
        const name = button.dataset.tab;
        $$('[data-tab]').forEach((item) => item.classList.toggle('is-active', item.dataset.tab === name));
        $$('[data-pane]').forEach((item) => item.classList.toggle('is-active', item.dataset.pane === name));
      });
    });
  }

  function initCart() {
    G.renderShell("catalog");

    const list = $("#cartList");
    const empty = $("#cartEmpty");
    const trash = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14H6L5 6"></path><path d="M10 11v6"></path><path d="M14 11v6"></path><path d="M9 6V4h6v2"></path></svg>`;
    if (!list || !empty) return;

    // Рендер корзины из серверных данных
    const renderCart = (serverCart) => {
      const items = (serverCart?.items || []).map((item) => {
        // Маппим серверный item: id=itemId, productId отдельно нет — ищем по productName
        const product = (G.data?.products || []).find(
          (p) => p.name === item.productName
        ) || {};
        return {
          itemId: item.id,           // id записи в cart_items (для удаления)
          id: product.id || 0,       // id продукта (для картинки)
          name: item.productName,
          price: Number(item.price),
          qty: item.quantity,
        };
      }).filter((it) => it.name);

      const totalItems = items.reduce((s, it) => s + it.qty, 0);
      const subtotal = items.reduce((s, it) => s + it.price * it.qty, 0);
      const delivery = items.length ? 1500 : 0;
      const total = subtotal + delivery;

      $("#sumItems").textContent = `${totalItems} шт.`;
      $("#sumSubtotal").textContent = G.fmtRub(subtotal);
      $("#sumDelivery").textContent = G.fmtRub(delivery);
      $("#sumTotal").textContent = G.fmtRub(total);
      empty.style.display = items.length ? "none" : "block";

      list.innerHTML = items.map((item) => `
        <article class="card cart-row" data-item-id="${item.itemId}" data-id="${item.id}">
          <img class="cart-row__img" src="${imgById(item.id)}" alt="${item.name}">
          <div class="cart-row__info">
            <div class="cart-row__name">${item.name}</div>
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

    // Загружаем корзину с сервера при открытии страницы
    let currentCart = { items: [] };
    G.cart.fetch().then((cart) => {
      currentCart = cart;
      renderCart(cart);
    }).catch(() => {
      empty.style.display = "block";
    });

    const sbpModal = $("#sbpModal");
    const sbpSuccess = $("#sbpSuccess");

    list.addEventListener("click", async (e) => {
      const btn = e.target.closest("[data-act]");
      if (!btn) return;
      const row = btn.closest(".cart-row");
      const itemId = Number(row?.dataset.itemId);
      const productId = Number(row?.dataset.id);
      if (!itemId) return;

      const input = $("[data-act='qty']", row);
      const qty = clamp(input?.value, 1, 999);

      try {
        if (btn.dataset.act === "minus") {
          currentCart = await G.cart.add(productId, -1);
          renderCart(currentCart);
        }
        if (btn.dataset.act === "plus") {
          currentCart = await G.cart.add(productId, 1);
          renderCart(currentCart);
        }
        if (btn.dataset.act === "remove") {
          currentCart = await G.cart.remove(itemId);
          renderCart(currentCart);
        }
      } catch (_) {
        alert("Ошибка обновления корзины. Проверьте, что backend запущен.");
      }
    });

    list.addEventListener("change", async (e) => {
      const input = e.target.closest("[data-act='qty']");
      if (!input) return;
      const row = input.closest(".cart-row");
      const itemId = Number(row?.dataset.itemId);
      const productId = Number(row?.dataset.id);
      const newQty = clamp(input.value, 1, 999);
      // Считаем разницу и добавляем/убавляем
      const currentItem = (currentCart.items || []).find((it) => it.id === itemId);
      const diff = newQty - (currentItem?.quantity || 1);
      if (diff === 0) return;
      try {
        currentCart = await G.cart.add(productId, diff);
        renderCart(currentCart);
      } catch (_) {
        alert("Ошибка обновления корзины.");
      }
    });

    $("#clearCartTop")?.addEventListener("click", async () => {
      // Удаляем все товары по одному через сервер
      const items = [...(currentCart.items || [])];
      for (const item of items) {
        try { await G.cart.remove(item.id); } catch (_) {}
      }
      currentCart = { items: [] };
      G.cart.clear();
      renderCart(currentCart);
    });

    $("#payBtn")?.addEventListener("click", () => {
      if (!(currentCart.items || []).length) return;
      if (sbpSuccess) sbpSuccess.style.display = "none";
      if (sbpModal) sbpModal.classList.add("is-open");
    });

    $("#sbpClose")?.addEventListener("click", () => {
      sbpModal?.classList.remove("is-open");
    });

    $("#sbpPaid")?.addEventListener("click", async () => {
      const paidBtn = $("#sbpPaid");
      if (paidBtn.dataset.busy === "1") return;
      paidBtn.dataset.busy = "1";
      try {
        if (sbpSuccess) sbpSuccess.style.display = "block";
        await G.cart.placeOrder();
        sbpModal?.classList.remove("is-open");
        currentCart = { items: [] };
        renderCart(currentCart);
        const note = $("#cartNote");
        if (note) note.style.display = "block";
      } catch (_) {
        alert("Не удалось создать заказ в базе. Проверьте, что backend запущен.");
      } finally {
        paidBtn.dataset.busy = "0";
      }
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    if (page === "catalog") return initCatalog();
    if (page === "product") return initProduct();
    if (page === "cart") return initCart();
    G.renderShell("home");
  });
})(window.GERMES);