document.addEventListener("DOMContentLoaded", () => {
    const loginModal = document.getElementById("loginModal");
    const openBtn = document.getElementById("openLoginModal");
    const closeBtn = document.getElementById("closeLoginModal");

    openBtn.addEventListener("click", () => {
        loginModal.style.display = "block";
    });

    closeBtn.addEventListener("click", () => {
        loginModal.style.display = "none";
    });

    window.addEventListener("click", (e) => {
        if (e.target === loginModal) {
            loginModal.style.display = "none";
        }
    });
});