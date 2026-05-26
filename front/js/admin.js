(function () {
  const API_PRODUCTS = "https://germes-gotfa.amvera.run/api/products";
  const API_ORDERS   = "https://germes-gotfa.amvera.run/api/orders";
  const AUTH_KEY = "germes_admin_auth";

  const escapeHtml = (v) => String(v ?? "")
    .replaceAll("&","&amp;").replaceAll("<","&lt;")
    .replaceAll(">","&gt;").replaceAll('"',"&quot;");

  const ORDER_STATUS_LABELS = {
    NEW:       "Новый",
    PAID:      "Оплачен",
    SHIPPED:   "Отправлен",
    DELIVERED: "Доставлен"
  };

  // Маппинг способов доставки → чип-класс + метка
  const DELIVERY_LABELS = {
    CDEK:    { label: "СДЭК",        cls: "delivery-chip--cdek" },
    POST:    { label: "Почта РФ",    cls: "delivery-chip--post" },
    PICKUP:  { label: "Самовывоз",   cls: "delivery-chip--pickup" },
    COURIER: { label: "Курьер",      cls: "" },
  };

  const state = {
    auth: sessionStorage.getItem(AUTH_KEY) || "",
    categories: [],
    products: [],
    orders: [],
    editingProductId: null,
  };

  const el = (id) => document.getElementById(id);

  const setAuth = (login, password) => {
    state.auth = "Basic " + btoa(login + ":" + password);
    sessionStorage.setItem(AUTH_KEY, state.auth);
  };
  const clearAuth = () => {
    state.auth = "";
    sessionStorage.removeItem(AUTH_KEY);
  };

  const setMessage = (text, isError) => {
    const m = el("loginMessage");
    if (!m) return;
    m.textContent = text;
    m.className = "admin-message" + (isError ? " is-error" : text ? " is-success" : "");
  };

  /* ── FETCH ── */
  async function apiFetch(url, options) {
    options = options || {};
    const headers = {};
    if (options.body) headers["Content-Type"] = "application/json";
    headers["Authorization"] = state.auth;
    Object.assign(headers, options.headers || {});

    const res = await fetch(url, Object.assign({}, options, { headers }));

    if (res.status === 401 || res.status === 403) throw new Error("auth");
    if (!res.ok) {
      var msg = "Ошибка запроса (" + res.status + ")";
      try { msg = (await res.json()).message || msg; } catch(_) {}
      throw new Error(msg);
    }
    if (res.status === 204) return null;
    return res.json();
  }

  const apiProducts = (path, opts) => apiFetch(API_PRODUCTS + path, opts);
  const apiOrders   = (path, opts) => apiFetch(API_ORDERS + path, opts);

  /* ── TABS ── */
  function switchTab(tab) {
    ["categories","products","orders"].forEach(function(t) {
      var tabEl = el("tab-" + t);
      if (tabEl) tabEl.style.display = (t === tab) ? "block" : "none";
    });
    document.querySelectorAll(".sidebar-nav__item[data-tab]").forEach(function(btn) {
      btn.classList.toggle("active", btn.dataset.tab === tab);
    });
  }

  /* ── RENDER CATEGORIES ── */
  function renderCategories() {
    var tbody = el("categoriesTableBody");
    var badge = el("catCount");
    if (badge) badge.textContent = state.categories.length;
    if (!tbody) return;

    if (!state.categories.length) {
      tbody.innerHTML = '<tr><td colspan="3" class="empty-row">Категорий пока нет</td></tr>';
    } else {
      tbody.innerHTML = state.categories.map(function(cat) {
        return '<tr>' +
          '<td style="color:#8b8fa8;">' + cat.id + '</td>' +
          '<td><input class="table-input" type="text" value="' + escapeHtml(cat.name) + '" data-role="category-name" data-id="' + cat.id + '"></td>' +
          '<td class="actions-cell">' +
            '<button class="tbl-btn" type="button" data-act="category-save" data-id="' + cat.id + '">Сохранить</button>' +
            '<button class="tbl-btn tbl-btn--danger" type="button" data-act="category-delete" data-id="' + cat.id + '">Удалить</button>' +
          '</td></tr>';
      }).join("");
    }

    var sel = el("productCategory");
    if (sel) {
      sel.innerHTML = '<option value="">Выберите категорию</option>' +
        state.categories.map(function(c) {
          return '<option value="' + c.id + '">' + escapeHtml(c.name) + '</option>';
        }).join("");
    }
  }

  /* ── RENDER PRODUCTS ── */
  function renderProducts() {
    var tbody = el("productsTableBody");
    var badge = el("prodCount");
    if (badge) badge.textContent = state.products.length;
    if (!tbody) return;

    if (!state.products.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="empty-row">Товаров пока нет</td></tr>';
      return;
    }
    tbody.innerHTML = state.products.map(function(p) {
      return '<tr>' +
        '<td style="color:#8b8fa8;">' + p.id + '</td>' +
        '<td>' + escapeHtml(p.name) + '</td>' +
        '<td style="font-weight:600;">' + Number(p.price).toLocaleString("ru-RU",{maximumFractionDigits:2}) + ' \u20BD</td>' +
        '<td><span class="cat-chip">' + escapeHtml(p.categoryName || "\u2014") + '</span></td>' +
        '<td class="actions-cell">' +
          '<button class="tbl-btn" type="button" data-act="product-edit" data-id="' + p.id + '">\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c</button>' +
          '<button class="tbl-btn tbl-btn--danger" type="button" data-act="product-delete" data-id="' + p.id + '">\u0423\u0434\u0430\u043b\u0438\u0442\u044c</button>' +
        '</td></tr>';
    }).join("");
  }

  /* ── RENDER ORDERS ── */
  function renderOrders() {
    var tbody  = el("ordersTableBody");
    var badge  = el("orderCount");
    var filter = el("orderFilter");
    if (!tbody) return;

    var newCnt  = state.orders.filter(function(o){ return o.status === "NEW"; }).length;
    var paidCnt = state.orders.filter(function(o){ return o.status === "PAID"; }).length;
    var shipCnt = state.orders.filter(function(o){ return o.status === "SHIPPED"; }).length;
    var delCnt  = state.orders.filter(function(o){ return o.status === "DELIVERED"; }).length;

    if (badge) badge.textContent = state.orders.length;
    var sv = el("statTotal");      if (sv) sv.textContent = state.orders.length;
    var sn = el("statNew");        if (sn) sn.textContent = newCnt;
    var sp = el("statProcessing"); if (sp) sp.textContent = paidCnt;
    var sd = el("statDone");       if (sd) sd.textContent = shipCnt + delCnt;

    var filterVal = filter ? filter.value : "";
    var list = filterVal
      ? state.orders.filter(function(o){ return o.status === filterVal; })
      : state.orders;

    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-row">Заказов нет</td></tr>';
      return;
    }

    tbody.innerHTML = list.map(function(o) {
      var status = o.status || "NEW";
      var statusLabel = ORDER_STATUS_LABELS[status] || status;

      var date = o.createdAt
        ? new Date(o.createdAt).toLocaleDateString("ru-RU",{day:"2-digit",month:"2-digit",year:"numeric"})
        : "\u2014";

      /* ── ТОВАРЫ ── */
      var itemsHtml = "\u2014";
      if (Array.isArray(o.items) && o.items.length) {
        itemsHtml = o.items.map(function(i) {
          var name  = escapeHtml(i.productName || i.name || "Товар");
          var qty   = i.quantity || 1;
          var price = (i.price != null)
            ? ' <span style="color:#8b8fa8;font-size:11px;">' + Number(i.price).toLocaleString("ru-RU",{maximumFractionDigits:2}) + ' \u20BD</span>'
            : "";
          return '<div class="order-item-row">' + name + ' <span class="order-item-qty">\xd7 ' + qty + '</span>' + price + '</div>';
        }).join("");
      }

      /* ── СУММА ── */
      var rawTotal = o.totalPrice != null ? o.totalPrice : o.total != null ? o.total : null;
      var totalHtml = rawTotal != null
        ? '<strong>' + Number(rawTotal).toLocaleString("ru-RU",{maximumFractionDigits:2}) + ' \u20BD</strong>'
        : "\u2014";

      /* ── ПОКУПАТЕЛЬ: email, телефон, доставка, адрес ── */
      var email    = escapeHtml(o.customerEmail || o.email || "");
      // Телефон — пробуем разные поля бэкенда
      var phone    = escapeHtml(o.customerPhone || o.phone || o.contactPhone || "");
      // Способ доставки
      var delivRaw = (o.deliveryMethod || o.delivery || "").toUpperCase();
      var delivInfo = DELIVERY_LABELS[delivRaw] || null;
      var delivChip = delivInfo
        ? '<span class="delivery-chip ' + delivInfo.cls + '">' + delivInfo.label + '</span>'
        : (delivRaw ? '<span class="delivery-chip">' + escapeHtml(o.deliveryMethod || o.delivery || "") + '</span>' : "");
      // Адрес
      var address  = escapeHtml(o.deliveryAddress || o.address || o.shippingAddress || "");

      var customerHtml = '<div class="order-customer">';
      if (email)    customerHtml += '<div class="order-customer__email">' + email + '</div>';
      if (phone)    customerHtml += '<div class="order-customer__phone">' + phone + '</div>';
      if (delivChip) customerHtml += '<div style="margin-top:3px;">' + delivChip + '</div>';
      if (address)  customerHtml += '<div class="order-customer__address">' + address + '</div>';
      // Трек и квитанция
      if (o.trackNumber)   customerHtml += '<div class="order-customer__meta">Трек: ' + escapeHtml(o.trackNumber) + '</div>';
      if (o.receiptNumber) customerHtml += '<div class="order-customer__meta">Квит: ' + escapeHtml(o.receiptNumber) + '</div>';
      if (!email && !phone && !address) customerHtml += '<span style="color:#8b8fa8;">\u2014</span>';
      customerHtml += '</div>';

      /* ── Цвет статуса ── */
      var statusColor = {
        NEW:       'style="background:#e3f2fd;color:#1565c0;"',
        PAID:      'style="background:#fff8e1;color:#e65100;"',
        SHIPPED:   'style="background:#f3e5f5;color:#6a1b9a;"',
        DELIVERED: 'style="background:#e8f5e9;color:#1b5e20;"'
      }[status] || "";

      return '<tr>' +
        '<td style="color:#8b8fa8;font-weight:600;">#' + o.id + '</td>' +
        '<td style="color:#8b8fa8;font-size:12px;white-space:nowrap;">' + date + '</td>' +
        '<td>' + customerHtml + '</td>' +
        '<td class="order-items-cell">' + itemsHtml + '</td>' +
        '<td style="white-space:nowrap;">' + totalHtml + '</td>' +
        '<td>' +
          '<select class="status-select" data-act="order-status" data-id="' + o.id + '" ' + statusColor + '>' +
            Object.keys(ORDER_STATUS_LABELS).map(function(key) {
              return '<option value="' + key + '"' + (status === key ? " selected" : "") + '>' + ORDER_STATUS_LABELS[key] + '</option>';
            }).join("") +
          '</select>' +
        '</td>' +
        '<td>' +
          '<button class="tbl-btn tbl-btn--danger" type="button" data-act="order-delete" data-id="' + o.id + '">Удалить</button>' +
        '</td>' +
      '</tr>';
    }).join("");
  }

  /* ── PRODUCT FORM ── */
  function resetProductForm() {
    state.editingProductId = null;
    var pn = el("productName");      if (pn) pn.value = "";
    var pp = el("productPrice");     if (pp) pp.value = "";
    var pc = el("productCategory");  if (pc) pc.value = "";
    var ml = el("productModeLabel"); if (ml) ml.textContent = "Добавление нового товара";
    var sb = el("saveProductBtn");   if (sb) sb.textContent = "Сохранить";
  }

  function fillProductForm(product) {
    state.editingProductId = product.id;
    var pn = el("productName");     if (pn) pn.value = product.name || "";
    var pp = el("productPrice");    if (pp) pp.value = product.price || "";
    var pc = el("productCategory");
    if (pc) {
      var cat = state.categories.find(function(c){ return c.name === product.categoryName; });
      pc.value = cat ? String(cat.id) : "";
    }
    var ml = el("productModeLabel"); if (ml) ml.textContent = "Редактирование #" + product.id;
    var sb = el("saveProductBtn");   if (sb) sb.textContent = "Обновить";
    switchTab("products");
  }

  /* ── LOAD DATA ── */
  async function loadAll() {
    var results = await Promise.all([
      apiProducts("/categories"),
      apiProducts("")
    ]);
    state.categories = results[0] || [];
    state.products   = results[1] || [];
    renderCategories();
    renderProducts();
  }

  async function loadOrders() {
    try {
      state.orders = (await apiOrders("")) || [];
    } catch(e) {
      console.warn("Orders API:", e.message);
      state.orders = [];
    }
    renderOrders();
  }

  /* ── VISIBILITY ── */
  function updateVisibility() {
    var ok = !!state.auth;
    var lc = el("loginCard");
    var ac = el("adminContent");
    if (lc) lc.style.display = ok ? "none" : "flex";
    if (ac) ac.style.display = ok ? "flex" : "none";
  }

  /* ── LOGIN ── */
  async function login() {
    var loginVal = (el("adminLogin") && el("adminLogin").value || "").trim();
    var pass     = (el("adminPassword") && el("adminPassword").value || "");
    if (!loginVal || !pass) {
      setMessage("Введите логин и пароль.", true);
      return;
    }
    setAuth(loginVal, pass);
    try {
      await loadAll();
      await loadOrders();
      updateVisibility();
      setMessage("Вход выполнен.");
      var pw = el("adminPassword"); if (pw) pw.value = "";
      resetProductForm();
    } catch(err) {
      clearAuth();
      updateVisibility();
      setMessage(err.message === "auth" ? "Неверный логин или пароль." : err.message, true);
    }
  }

  /* ── CRUD CATEGORIES ── */
  async function createCategory() {
    var input = el("newCategoryName");
    var name  = (input && input.value || "").trim();
    if (!name) return;
    await apiProducts("/category", { method:"POST", body: JSON.stringify({ name: name }) });
    if (input) input.value = "";
    await loadAll();
  }

  async function updateCategory(id) {
    var input = document.querySelector('[data-role="category-name"][data-id="' + id + '"]');
    var name  = (input && input.value || "").trim();
    if (!name) return;
    await apiProducts("/category/" + id, { method:"PUT", body: JSON.stringify({ name: name }) });
    await loadAll();
  }

  async function deleteCategory(id) {
    if (!confirm("Удалить категорию? Товары этой категории также будут удалены.")) return;
    await apiProducts("/category/" + id, { method:"DELETE" });
    await loadAll();
  }

  /* ── CRUD PRODUCTS ── */
  async function saveProduct() {
    var name       = (el("productName") && el("productName").value || "").trim();
    var price      = Number(el("productPrice") && el("productPrice").value);
    var categoryId = Number(el("productCategory") && el("productCategory").value);
    if (!name || !price || !categoryId) {
      alert("Заполните название, цену и категорию.");
      return;
    }
    var payload = { name: name, price: price, categoryId: categoryId };
    if (state.editingProductId) {
      await apiProducts("/" + state.editingProductId, { method:"PUT", body: JSON.stringify(payload) });
    } else {
      await apiProducts("", { method:"POST", body: JSON.stringify(payload) });
    }
    resetProductForm();
    await loadAll();
  }

  async function deleteProduct(id) {
    if (!confirm("Удалить товар?")) return;
    await apiProducts("/" + id, { method:"DELETE" });
    if (state.editingProductId === id) resetProductForm();
    await loadAll();
  }

  /* ── CRUD ORDERS ── */
  async function updateOrderStatus(id, status) {
    try {
      await apiOrders("/" + id + "/status", { method:"PATCH", body: JSON.stringify({ status: status }) });
    } catch(e) {
      console.warn("Ошибка смены статуса:", e.message);
    }
    var o = state.orders.find(function(x){ return x.id === id; });
    if (o) o.status = status;
    renderOrders();
  }

  async function deleteOrder(id) {
    if (!confirm("Удалить заказ #" + id + "? Действие необратимо.")) return;
    await apiOrders("/" + id, { method:"DELETE" });
    state.orders = state.orders.filter(function(o){ return o.id !== id; });
    renderOrders();
  }

  /* ── BIND EVENTS ── */
  function bindEvents() {
    el("loginBtn") && el("loginBtn").addEventListener("click", login);
    el("fillLoginBtn") && el("fillLoginBtn").addEventListener("click", function() {
      var li = el("adminLogin");    if (li) li.value = "admin";
      var pw = el("adminPassword"); if (pw) pw.value = "1234";
      setMessage("Данные подставлены.");
    });
    el("adminPassword") && el("adminPassword").addEventListener("keydown", function(e){
      if (e.key === "Enter") login();
    });
    el("logoutBtn") && el("logoutBtn").addEventListener("click", function(){
      clearAuth(); updateVisibility(); setMessage("");
    });

    document.querySelectorAll(".sidebar-nav__item[data-tab]").forEach(function(btn) {
      btn.addEventListener("click", function() {
        var tab = btn.dataset.tab;
        switchTab(tab);
        if (tab === "orders") loadOrders();
      });
    });

    el("createCategoryBtn") && el("createCategoryBtn").addEventListener("click", function(){
      createCategory().catch(function(e){ alert(e.message); });
    });
    el("newCategoryName") && el("newCategoryName").addEventListener("keydown", function(e){
      if (e.key === "Enter") createCategory().catch(function(er){ alert(er.message); });
    });
    el("refreshCategoriesBtn") && el("refreshCategoriesBtn").addEventListener("click", function(){
      loadAll().catch(function(e){ alert(e.message); });
    });

    el("refreshProductsBtn") && el("refreshProductsBtn").addEventListener("click", function(){
      loadAll().catch(function(e){ alert(e.message); });
    });
    el("saveProductBtn") && el("saveProductBtn").addEventListener("click", function(){
      saveProduct().catch(function(e){ alert(e.message); });
    });
    el("resetProductBtn") && el("resetProductBtn").addEventListener("click", resetProductForm);

    el("refreshOrdersBtn") && el("refreshOrdersBtn").addEventListener("click", function(){
      loadOrders();
    });
    el("orderFilter") && el("orderFilter").addEventListener("change", renderOrders);

    document.addEventListener("click", function(e) {
      var btn = e.target.closest("[data-act]");
      if (!btn || btn.tagName === "SELECT") return;
      var id  = Number(btn.dataset.id);
      var act = btn.dataset.act;
      if (act === "category-save")   updateCategory(id).catch(function(er){ alert(er.message); });
      if (act === "category-delete") deleteCategory(id).catch(function(er){ alert(er.message); });
      if (act === "product-edit") {
        var p = state.products.find(function(x){ return x.id === id; });
        if (p) fillProductForm(p);
      }
      if (act === "product-delete") deleteProduct(id).catch(function(er){ alert(er.message); });
      if (act === "order-delete")   deleteOrder(id).catch(function(er){ alert(er.message); });
    });

    document.addEventListener("change", function(e) {
      var sel = e.target.closest("select[data-act='order-status']");
      if (!sel) return;
      updateOrderStatus(Number(sel.dataset.id), sel.value);
    });
  }

  /* ── INIT ── */
  document.addEventListener("DOMContentLoaded", async function() {
    window.GERMES && window.GERMES.renderShell && window.GERMES.renderShell("about");
    bindEvents();
    updateVisibility();
    if (state.auth) {
      try {
        await loadAll();
        await loadOrders();
        updateVisibility();
      } catch(_) {
        clearAuth();
        updateVisibility();
      }
    }
  });
})();