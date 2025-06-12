const urlParams = new URLSearchParams(window.location.search);
const query = urlParams.get("query");
const resultDiv = document.getElementById("result");
const form = document.getElementById("search-form");
const input = form.query;
let isFirst = true;
let currentConversationId = urlParams.get("conversationId");
console.log(currentConversationId);

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
document.addEventListener("DOMContentLoaded", () => {
    const urlParams = new URLSearchParams(window.location.search);
    const conversationId = urlParams.get("conversationId");
    const query = urlParams.get("query");

    const resultElement = document.getElementById("result");
    const cachedResult = resultElement.dataset.result;

    if (cachedResult && cachedResult.trim()) {
        renderAIResponse(cachedResult);
    } else if (query) {
        fetchResult(query);
    } else if (conversationId) {
        fetchConversation(conversationId);
    }
});

function renderAIResponse(content) {
    let parsedContent = content;

    try {
        const parsed = JSON.parse(content);
        if (parsed.content) {
            parsedContent = parsed.content;
        }
    } catch (e) {
    }

    const formatted = formatContent(parsedContent);
    const aiContent = document.createElement("div");
    aiContent.className = "chat-bubble ai";
    aiContent.innerHTML = formatted;
    resultDiv.appendChild(aiContent);
    aiContent.scrollIntoView({behavior: "smooth"});
}

function fetchConversation(conversationId) {
    fetch(`/api/conversation/detail/${conversationId}`)
        .then(res => res.json())
        .then(messages => {
            messages.forEach(msg => {
                const bubble = document.createElement("div");
                bubble.className = `chat-bubble ${msg.sender === "USER" ? "user" : "ai"}`;

                let textContent = msg.message;

                if (msg.sender === "AI") {
                    try {
                        const parsed = JSON.parse(msg.message);
                        textContent = parsed.content || msg.message;
                    } catch (e) {
                        console.warn("AI 응답 JSON 파싱 실패:", e);
                    }
                    bubble.innerHTML = formatContent(textContent);
                } else {
                    bubble.textContent = textContent;
                }

                resultDiv.appendChild(bubble);
            });
            resultDiv.scrollTop = resultDiv.scrollHeight;
        })
        .catch(err => alert("❌ 대화 불러오기 오류: " + err));
}


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

    const submitButton = form.querySelector("button");
    submitButton.disabled = true;

    let apiUrl = "/api/async-search?query=" + encodeURIComponent(query);
    if (currentConversationId) {
        apiUrl += "&conversationId=" + currentConversationId;
    }

    fetch(apiUrl)
        .then(res => res.json())
        .then(data => {
            if (data.conversationId) {
                currentConversationId = data.conversationId;
            }

            let rawContent = data.content || "응답 없음";

            if (typeof rawContent === "string" && rawContent.startsWith("{") && rawContent.includes("content")) {
                try {
                    const parsed = JSON.parse(rawContent);
                    rawContent = parsed.content || rawContent;
                } catch (e) {
                    console.warn("content JSON 파싱 실패:", e);
                }
            }

            const contentHtml = formatContent(rawContent);

            resultDiv.removeChild(spinnerBubble);
            renderAIResponse(rawContent);

            const aiContent = document.createElement("div");
            aiContent.className = "chat-bubble ai";
            aiContent.innerHTML = contentHtml;
            resultDiv.appendChild(aiContent);
            aiContent.scrollIntoView({behavior: "smooth"});
        })
        .catch(err => alert("❌ 오류 발생: " + err))
        .finally(() => {
            submitButton.disabled = false;
        });
}

function formatContent(text) {
    const codeBlocks = [];
    let content = text.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
        const escaped = code
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/(^|\n)(\s*)#(.*)/g, '$1$2<span class="comment">#$3</span>')
            .replace(/(^|\n)(\s*)\/\/(.*)/g, '$1$2<span class="comment">//$3</span>');

        const placeholder = `__CODEBLOCK_${codeBlocks.length}__`;
        codeBlocks.push(`<pre><code class="language-${lang}">${escaped}</code></pre>`);
        return placeholder;
    });
    content = content.replace(/^###\s(.+)$/gm, '<h3>$1</h3>');
    content = content.replace(/`([^`]+?)`/g, '<code class="inline">$1</code>');
    content = content.replace(/\[(.+?)\]\((.*?)\)/g, '<a href="$2" target="_blank" class="source-btn">$1</a>');
    content = content.replace(/:\n-\s*/g, ':<br>• ');
    content = content.replace(/(^|\n)-\s*/g, '$1• ');
    let isFirstBold = true;
    content = content.replace(/\*\*(.+?)\*\*/g, (match, text, offset, fullText) => {
        const before = fullText.slice(Math.max(0, offset - 40), offset);
        const isInH3 = /<h3>[^<]*$/.test(before);
        const isStartOfLine = /(^|\n)[\d\-•]*\s*$/.test(before);

        if (isFirstBold || isStartOfLine || isInH3) {
            isFirstBold = false;
            return `<span class="bold">${text}</span>`;
        }
        return `<br><span class="bold">${text}</span>`;
    });
    content = content.replace(/^(\d+)\.\s*<br>\s*<span class="bold">/gm, '$1. <span class="bold">');
    content = content.replace(/([^\n])\n(?!(<\/?h3>|<\/?pre>))/g, '$1<br>');
    codeBlocks.forEach((block, i) => {
        content = content.replace(`__CODEBLOCK_${i}__`, block);
    });

    return content;
}
