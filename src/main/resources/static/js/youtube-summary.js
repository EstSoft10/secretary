document.addEventListener("DOMContentLoaded", function () {
    const result = JSON.parse(sessionStorage.getItem("youtubeSummaryResult"));
    if (!result) return;
    const {summary, subtitle, videoUrl} = result;
    const parsedSummary = JSON.parse(summary);
    const videoId = new URL(videoUrl).searchParams.get("v");
    const iframe = document.getElementById("videoFrame");
    iframe.src = `https://www.youtube.com/embed/${videoId}?enablejsapi=1&vq=hd1080`;

    const totalSummaryEl = document.getElementById("totalSummary");
    parsedSummary.summary.total_summary.forEach(line => {
        const li = document.createElement("li");
        const html = line.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");
        li.innerHTML = html;
        totalSummaryEl.appendChild(li);
    });

    const summaryTab = document.getElementById("summaryTab");
    parsedSummary.summary.chapters.forEach((chapter, index) => {
        const block = document.createElement("div");
        block.className = "summary-block";

        const title = document.createElement("div");
        title.className = "timestamp";
        title.textContent = `${chapter.timestamp} - ${chapter.title}`;
        block.appendChild(title);

        const ul = document.createElement("ul");
        chapter.summary.forEach(line => {
            const li = document.createElement("li");
            const html = line.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");
            li.innerHTML = html;
            ul.appendChild(li);
        });

        block.appendChild(ul);
        summaryTab.appendChild(block);
    });

    const scriptTab = document.getElementById("scriptTab");
    subtitle.forEach((chapter, chapterIdx) => {
        chapter.text.forEach(line => {
            const p = document.createElement("p");
            const btn = document.createElement("button");
            btn.textContent = line.timestamp;
            btn.style.marginRight = "0.5rem";
            btn.onclick = () => seekToTimestamp(line.timestamp);
            p.appendChild(btn);

            const span = document.createElement("span");
            span.textContent = line.content;
            p.appendChild(span);

            scriptTab.appendChild(p);
        });
    });
});
document.addEventListener("DOMContentLoaded", function () {
    toggleTab("summary");
});

function toggleTab(tab) {
    document.getElementById("summaryTab").style.display = (tab === "summary") ? "block" : "none";
    document.getElementById("scriptTab").style.display = (tab === "script") ? "block" : "none";
    const buttons = document.querySelectorAll(".tab-buttons button");
    buttons.forEach(btn => btn.classList.remove("active"));
    document.querySelector(`.tab-buttons button[onclick="toggleTab('${tab}')"]`).classList.add("active");
}

function seekToTimestamp(timeStr) {
    const [min, sec] = timeStr.split(":").map(Number);
    const seconds = min * 60 + sec;
    const iframe = document.getElementById("videoFrame");
    const command = (func, args = []) => {
        iframe.contentWindow.postMessage(JSON.stringify({
            event: "command",
            func,
            args
        }), "*");
    };
    command("seekTo", [seconds, true]);
    command("playVideo");
}

