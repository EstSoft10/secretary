function updateTime() {
    const now = new Date();
    const formatted = now.toLocaleString("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "numeric",
        minute: "numeric"
    });
    document.getElementById("weather-time").textContent = formatted;
}

function renderWeather(data) {
    document.getElementById("weather-location").textContent = data.location;
    document.getElementById("weather-icon").src = `https://openweathermap.org/img/wn/${data.icon}@2x.png`;
    document.getElementById("weather-text").textContent = `${data.description} (${data.temp}℃)`;
}


function fetchAndStoreWeather(lat, lon) {
    fetch(`/weather?lat=${lat}&lon=${lon}`)
        .then(res => res.json())
        .then(data => {
            const cached = {
                timestamp: new Date().getTime(),
                ...data
            };
            localStorage.setItem("weatherData", JSON.stringify(cached));
            updateTime();
            renderWeather(cached);
        })
        .catch(err => {
            console.error("날씨 요청 실패:", err);
        });
}

function getWeather(lat, lon) {
    const cached = JSON.parse(localStorage.getItem("weatherData"));
    const oneHour = 1000 * 60 * 60;
    const now = new Date().getTime();

    if (cached && now - cached.timestamp < oneHour) {
        updateTime();
        renderWeather(cached);
    } else {
        fetchAndStoreWeather(lat, lon);
    }
}

navigator.geolocation.getCurrentPosition(
    pos => {
        const {latitude, longitude} = pos.coords;
        getWeather(latitude, longitude);
    },
    err => console.error("위치 허용 실패:", err)
);


document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("youtubeForm").addEventListener("submit", async function (e) {
        e.preventDefault();
        const youtubeUrl = document.getElementById("youtubeInput").value;

        try {
            const response = await fetch("/youtube/extract-and-summary", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({url: youtubeUrl})
            });

            const data = await response.json();

            const {summary, subtitle} = data;
            sessionStorage.setItem("youtubeSummaryResult", JSON.stringify({
                summary,
                subtitle,
                videoUrl: youtubeUrl
            }));
            window.location.href = "/youtube-summary";


        } catch (err) {
            console.error("요약 중 오류 발생", err);
            alert("요약 중 오류 발생");
        }
    });
});

