/* Errata 官网交互：导航、滚动揭示、皮肤切换、常见问题、读取最新 Release。 */
(function () {
  "use strict";

  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var $$ = function (sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); };

  /* ---------- Header ---------- */
  var header = $(".site-header");
  function onScroll() {
    if (header) header.classList.toggle("is-scrolled", window.scrollY > 8);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ---------- Mobile nav ---------- */
  var menuBtn = $("#menuBtn");
  var nav = $("#siteNav");
  function closeMenu() {
    if (!nav || !menuBtn) return;
    nav.classList.remove("is-open");
    menuBtn.setAttribute("aria-expanded", "false");
    menuBtn.setAttribute("aria-label", "打开菜单");
    document.body.classList.remove("menu-open");
  }
  function toggleMenu() {
    if (!nav || !menuBtn) return;
    var open = !nav.classList.contains("is-open");
    nav.classList.toggle("is-open", open);
    menuBtn.setAttribute("aria-expanded", String(open));
    menuBtn.setAttribute("aria-label", open ? "关闭菜单" : "打开菜单");
    document.body.classList.toggle("menu-open", open);
  }
  if (menuBtn) menuBtn.addEventListener("click", toggleMenu);
  if (nav) {
    $$("a", nav).forEach(function (a) { a.addEventListener("click", closeMenu); });
  }
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") closeMenu();
  });
  window.addEventListener("resize", function () {
    if (window.innerWidth > 980) closeMenu();
  });

  /* ---------- Scroll reveal ---------- */
  var revealItems = $$(".reveal");
  if (revealItems.length) {
    if ("IntersectionObserver" in window) {
      document.body.classList.add("has-reveal");
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          io.unobserve(entry.target);
        });
      }, { rootMargin: "0px 0px -7% 0px", threshold: 0.08 });
      revealItems.forEach(function (el) { io.observe(el); });
    } else {
      revealItems.forEach(function (el) { el.classList.add("is-visible"); });
    }
  }

  /* ---------- Skin tabs ---------- */
  var tabs = $$(".skin-tab");
  var panels = $$(".skin-panel");
  function activateSkin(key, focusTab) {
    var activeTab = null;
    tabs.forEach(function (tab) {
      var on = tab.dataset.skin === key;
      tab.classList.toggle("is-active", on);
      tab.setAttribute("aria-selected", String(on));
      tab.tabIndex = on ? 0 : -1;
      if (on) activeTab = tab;
    });
    panels.forEach(function (panel) {
      var on = panel.dataset.theme === key;
      panel.classList.toggle("is-active", on);
      if (on) panel.hidden = false;
      else panel.hidden = true;
    });
    if (focusTab && activeTab) activeTab.focus();
  }
  tabs.forEach(function (tab, index) {
    tab.addEventListener("click", function () { activateSkin(tab.dataset.skin, false); });
    tab.addEventListener("keydown", function (e) {
      var next = null;
      if (e.key === "ArrowRight") next = (index + 1) % tabs.length;
      else if (e.key === "ArrowLeft") next = (index - 1 + tabs.length) % tabs.length;
      else if (e.key === "Home") next = 0;
      else if (e.key === "End") next = tabs.length - 1;
      if (next === null) return;
      e.preventDefault();
      activateSkin(tabs[next].dataset.skin, true);
    });
  });

  /* ---------- FAQ: one open at a time ---------- */
  var faqItems = $$(".faq-list details");
  faqItems.forEach(function (item) {
    item.addEventListener("toggle", function () {
      if (!item.open) return;
      faqItems.forEach(function (other) {
        if (other !== item) other.open = false;
      });
    });
  });

  /* ---------- Latest GitHub release ----------
     Fallback values are already in the HTML. If the API is rate-limited or
     offline, the buttons keep pointing at the last known release. */
  (function loadLatestRelease() {
    if (!window.fetch) return;
    var controller = ("AbortController" in window) ? new AbortController() : null;
    var timer = controller ? window.setTimeout(function () { controller.abort(); }, 9000) : null;

    fetch("https://api.github.com/repos/chenzhizixin2010/wrongbook/releases?per_page=5", {
      headers: { Accept: "application/vnd.github+json" },
      signal: controller ? controller.signal : undefined
    }).then(function (res) {
      if (!res.ok) throw new Error("release request failed");
      return res.json();
    }).then(function (releases) {
      if (!Array.isArray(releases)) return;
      var release = releases.filter(function (r) { return r && !r.draft; })[0];
      if (!release || !release.tag_name) return;

      var version = String(release.tag_name);
      if (version.charAt(0) !== "v") version = "v" + version;
      var assets = Array.isArray(release.assets) ? release.assets : [];
      var apk = assets.filter(function (a) {
        return a && a.name && /\.apk$/i.test(a.name) && a.browser_download_url;
      })[0];

      $$("[data-version]").forEach(function (el) { el.textContent = version; });

      if (apk && apk.size) {
        var mb = Math.max(1, Math.round(apk.size / 1024 / 1024));
        $$("[data-size]").forEach(function (el) { el.textContent = mb + " MB"; });
      }

      if (release.published_at) {
        var date = String(release.published_at).slice(0, 10);
        $$("[data-date]").forEach(function (el) { el.textContent = date; });
      }

      if (apk) {
        $$("[data-download]").forEach(function (el) {
          el.href = apk.browser_download_url;
        });
      }
    }).catch(function () {
      /* 保留 HTML 内的回退版本，不打断页面。 */
    }).then(function () {
      if (timer) window.clearTimeout(timer);
    });
  })();
})();
