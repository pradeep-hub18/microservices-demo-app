const tokenKey = "microshop_token";
const userKey = "microshop_user";
const grid = document.querySelector("#catalogGrid");
const message = document.querySelector("#message");
const username = document.querySelector("#username");
const logoutButton = document.querySelector("#logoutButton");

async function loadConfig() {
  const response = await fetch("/api/catalog/config");
  if (!response.ok) {
    return { authLoginUrl: "http://localhost:8081" };
  }
  return response.json();
}

async function redirectToLogin() {
  const config = await loadConfig();
  window.location.href = config.authLoginUrl || "http://localhost:8081";
}

function renderItems(items) {
  grid.innerHTML = items.map((item) => `
    <article class="catalog-card">
      <img src="${item.imageUrl}" alt="${item.name}">
      <div class="catalog-card-body">
        <p class="category">${item.category}</p>
        <h2>${item.name}</h2>
        <p>${item.description}</p>
        <strong>$${Number(item.price).toFixed(2)}</strong>
      </div>
    </article>
  `).join("");
}

async function loadCatalog() {
  const token = localStorage.getItem(tokenKey);
  if (!token) {
    await redirectToLogin();
    return;
  }

  username.textContent = localStorage.getItem(userKey) || "Signed in";

  const response = await fetch("/api/catalog/items", {
    headers: { Authorization: `Bearer ${token}` }
  });

  if (response.status === 401) {
    localStorage.removeItem(tokenKey);
    localStorage.removeItem(userKey);
    await redirectToLogin();
    return;
  }

  if (!response.ok) {
    message.textContent = "Unable to load catalog.";
    return;
  }

  renderItems(await response.json());
}

logoutButton.addEventListener("click", async () => {
  localStorage.removeItem(tokenKey);
  localStorage.removeItem(userKey);
  localStorage.removeItem("microshop_role");
  await redirectToLogin();
});

loadCatalog();
