document.addEventListener("DOMContentLoaded", function () {
    window.toggleMenu = function (show) {
        const menu = document.getElementById("sideMenu");
        const body = document.body;

        if (show) {
            menu.classList.add("open");
            body.classList.add("no-scroll");
        } else {
            menu.classList.remove("open");
            body.classList.remove("no-scroll");
        }
    };
});

document.addEventListener("click", function (e) {
    const menu = document.getElementById("sideMenu");
    const isInside = menu.contains(e.target);
    const isHamburger = e.target.closest(".hamburger");
    if (!isInside && !isHamburger) {
        menu.classList.remove("open");
    }
});
