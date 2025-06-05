const urlParams = new URLSearchParams(window.location.search);
const query = urlParams.get("query");
const resultDiv = document.getElementById("result");
const form = document.getElementById("search-form");
const input = form.query;
let isFirst = true;

if (query) fetchResult(query);

document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("search-form");
    const input = form.query;

    form.addEventListener("submit", async function (e) {
        e.preventDefault();
        const query = input.value.trim();

        if (query.includes("youtube") || query.includes("유튜브")) {
            await handleYoutubeSummary(query);
        } else {
            fetchResult(query);
        }

        input.value = "";
    });
});

async function handleYoutubeSummary(query) {
    input.disabled = true;
    document.getElementById("loadingOverlay").style.display = "block";

    const youtubeUrl = extractYoutubeUrl(query);
    if (!youtubeUrl) {
        alert("유효한 유튜브 링크를 찾을 수 없어요.");
        document.getElementById("loadingOverlay").style.display = "none";
        input.disabled = false;
        return;
    }

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
        alert("❌ 유튜브 요약 중 오류 발생");
        console.error(err);
        document.getElementById("loadingOverlay").style.display = "none";
    } finally {
        input.disabled = false;
    }
}

function extractYoutubeUrl(text) {
    const regex = /(https?:\/\/)?(www\.)?(youtube\.com\/watch\?v=[\w\-]+|youtu\.be\/[\w\-]+)/;
    const match = text.match(regex);
    return match ? (match[0].startsWith("http") ? match[0] : "https://" + match[0]) : null;
}


function fetchResult(query) {
    input.disabled = true;
    const userBubble = document.createElement("div");
    userBubble.className = "chat-bubble user";
    userBubble.textContent = query;
    resultDiv.appendChild(userBubble);
    const spinnerBubble = document.createElement("div");
    spinnerBubble.className = "spinner-bubble";
    spinnerBubble.style.alignSelf = "flex-start";
    const spinnerText = document.createElement("div");
    spinnerText.textContent = "검색 결과를 바탕으로 답변을 생성하고 있어요.";
    const spinner = document.createElement("div");
    spinner.className = "spinner-circle";
    spinnerBubble.appendChild(spinnerText);
    spinnerBubble.appendChild(spinner);
    resultDiv.appendChild(spinnerBubble);
    requestAnimationFrame(() => spinnerBubble.scrollIntoView({behavior: "smooth"}));

    fetch("/api/async-search?query=" + encodeURIComponent(query))
        .then(res => res.text())
        .then(text => {
            const data = JSON.parse(text);
            const contentHtml = formatContent(data.content || "응답 없음");
            resultDiv.removeChild(spinnerBubble);
            const aiContent = document.createElement("div");
            aiContent.className = "chat-bubble ai";
            aiContent.innerHTML = contentHtml;
            resultDiv.appendChild(aiContent);
            aiContent.scrollIntoView({behavior: "smooth"});
            isFirst = false;
        })
        .catch(err => alert("❌ 오류 발생: " + err))
        .finally(() => input.disabled = false);
}

function formatContent(text) {
    let codeBlocks = [];
    let content = text.replace(/```(\w*)\n([\s\S]*?)```/g, function (_, lang, code) {
        const highlighted = code
            .replace(/(^|\n)(\s*)#(.*)/g, '$1$2<span class="comment">#$3</span>')
            .replace(/(^|\n)(\s*)\/\/(.*)/g, '$1$2<span class="comment">//$3</span>');
        const placeholder = `__CODEBLOCK_${codeBlocks.length}__`;
        codeBlocks.push(`<pre><code class="language-${lang}">${highlighted}</code></pre>`);
        return placeholder;
    });
    content = content.replace(/^###\s(.+)$/gm, '<h3>$1</h3>');
    content = content.replace(/`([^`]+?)`/g, '<code class="inline">$1</code>');
    content = content.replace(/\[(.+?)\]\((.*?)\)/g, '<a href="$2" target="_blank" class="source-btn">$1</a>');
    let isFirstBold = true;
    content = content.replace(/\*\*(.+?)\*\*/g, (match, text, offset, fullText) => {
        const before = fullText.slice(Math.max(0, offset - 20), offset).trim();
        const isNumberLine = /^\d+\.\s*$/.test(before);
        const isDashLine = before.endsWith("-") || before.endsWith("-<br>");
        if (isFirstBold) {
            isFirstBold = false;
            return `<span class="bold">${text}</span>`;
        }
        if (before.endsWith('<br>') || isNumberLine || isDashLine) {
            return `<span class="bold">${text}</span>`;
        }
        return `<br><span class="bold">${text}</span>`;
    });
    content = content.replace(/^(\d+)\.\s*<br>\s*<span class="bold">/gm, '$1. <span class="bold">');
    content = content.replace(/([^\n])\n(?!<\/h3>)/g, '$1<br>');
    content = content.replace(/(\.<br>)(?!<br>)/g, '$1<br>');
    codeBlocks.forEach((block, i) => {
        content = content.replace(`__CODEBLOCK_${i}__`, block);
    });
    return content;
}

