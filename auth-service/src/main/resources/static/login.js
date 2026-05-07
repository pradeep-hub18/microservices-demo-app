const form = document.querySelector("#loginForm");
const message = document.querySelector("#message");
const button = document.querySelector("#loginButton");

async function loadConfig() {
  const response = await fetch("/api/config");
  if (!response.ok) {
    return { catalogUrl: "http://localhost:8082" };
  }
  return response.json();
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  message.textContent = "";
  button.disabled = true;
  button.textContent = "Signing in...";

  const payload = {
    username: form.username.value.trim(),
    password: form.password.value
  };

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error("Invalid username or password");
    }

    const data = await response.json();
    localStorage.setItem("microshop_token", data.token);
    localStorage.setItem("microshop_user", data.username);
    localStorage.setItem("microshop_role", data.role);

    const config = await loadConfig();
    window.location.href = config.catalogUrl || data.catalogUrl || "http://localhost:8082";
  } catch (error) {
    message.textContent = error.message;
  } finally {
    button.disabled = false;
    button.textContent = "Login";
  }
});

