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
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

    document.getElementById("youtubeForm").addEventListener("submit", async function (e) {
        e.preventDefault();

        const youtubeUrl = document.getElementById("youtubeInput").value;
        document.getElementById("loadingOverlay").style.display = "block";

        try {
            const response = await fetch("/youtube/extract-and-summary", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({url: youtubeUrl})
            });

            const data = await response.json();
            sessionStorage.setItem("youtubeSummaryResult", JSON.stringify({
                summary: data.summary,
                subtitle: data.subtitle,
                videoUrl: youtubeUrl
            }));

            window.location.href = "/youtube-summary";
        } catch (err) {
            alert("요약 중 오류 발생");
            console.error(err);
            document.getElementById("loadingOverlay").style.display = "none";
        }
    });
});


document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('.card.small-card').forEach(card => {
        card.addEventListener('click', () => {
            const query = card.getAttribute('data-query');
            location.href = `/api/prompt-click?query=${encodeURIComponent(query)}`;
        });
    });
    let recognition;
    let isRecognizing = false;
    let transcriptBuffer = "";
    let idleTimer = null;

    window.startSpeech = function () {
        if (isRecognizing) return;
        isRecognizing = true;

        recognition = createRecognition();
        recognition.start();

        const micButton = document.querySelector('button[title="음성으로 검색"]');
        micButton.classList.add("mic-blinking");
    };

    function createRecognition() {
        const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
        recognition.lang = 'ko-KR';
        recognition.interimResults = false;
        recognition.maxAlternatives = 1;

        recognition.onresult = (event) => {
            const partial = event.results[0][0].transcript;
            transcriptBuffer += (transcriptBuffer ? " " : "") + partial;

            if (idleTimer) clearTimeout(idleTimer);
            idleTimer = setTimeout(() => {
                sendTranscript();
            }, 2000);
        };

        recognition.onerror = (event) => {
            alert("음성 인식 오류: " + event.error);
            cleanup();
        };

        recognition.onend = () => {
            if (isRecognizing) {
                recognition.start();
            }
        };

        return recognition;
    }

    function sendTranscript() {
        if (transcriptBuffer.trim()) {
            const input = document.querySelector('input[name="query"]');
            input.value = transcriptBuffer;
            location.href = `/searchResult?query=${encodeURIComponent(transcriptBuffer)}`;
        }
        cleanup();
    }

    function cleanup() {
        isRecognizing = false;
        transcriptBuffer = "";
        const micButton = document.querySelector('button[title="음성으로 검색"]');
        micButton.classList.remove("mic-blinking");
        if (idleTimer) clearTimeout(idleTimer);
        recognition.stop();
    }
});

let recognition = null;
let isRecording = false;

function toggleVoiceRecording() {
    const btn = document.getElementById("voiceActionBtn");
    const status = document.getElementById("voiceStatus");
    const response = document.getElementById("voiceResponse");

    if (!recognition) {
        recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
        recognition.lang = "ko-KR";
        recognition.interimResults = false;
        recognition.maxAlternatives = 1;

        recognition.onresult = async (event) => {
            const text = event.results[0][0].transcript;
            console.log("🎧 입력:", text);

            try {
                const csrfHeader = document.querySelector("meta[name='_csrf_header']").content;
                const csrfToken = document.querySelector("meta[name='_csrf']").content;

                const res = await fetch("/api/voice/analyze", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify({query: text})
                });

                const data = await res.json();
                document.getElementById("voiceResponse").textContent = data.message;

            } catch (err) {
                response.textContent = "❌ 오류 발생: " + err.message;
            }

            btn.textContent = "말하기 다시 시작";
            btn.disabled = false;
            isRecording = false;
        };

        recognition.onerror = (e) => {
            response.textContent = "❌ 오류: " + e.error;
            btn.textContent = "다시 시도하기";
            btn.disabled = false;
            isRecording = false;
        };

        recognition.onend = () => {
            isRecording = false;
            btn.disabled = false;
            btn.textContent = "말하기 다시 시작";
        };
    }

    if (isRecording) {
        recognition.stop();
        isRecording = false;
        btn.textContent = "말하기 다시 시작";
        status.textContent = "🎤 멈췄습니다.";
        return;
    }

    // 시작
    isRecording = true;
    btn.disabled = true;
    btn.innerHTML = `<span class="recording-dot"></span> 듣는 중...`;
    status.textContent = "🎤 음성 인식 중입니다...";
    recognition.start();
}


function closeVoiceModal() {
    if (recognition && isRecording) {
        recognition.stop();
    }
    isRecording = false;
    document.getElementById("voiceModal").classList.add("hidden");
    document.getElementById("voiceResponse").textContent = "";
    document.getElementById("voiceStatus").textContent = "🎤 음성으로 일정을 관리해보세요!";
}


document.addEventListener("DOMContentLoaded", () => {
    const voiceBtn = document.getElementById("voice-assistant-btn");
    const modal = document.getElementById("voiceModal");

    voiceBtn.addEventListener("click", () => {
        modal.classList.remove("hidden");
    });
});


document.addEventListener('DOMContentLoaded', () => {
    const chatbotBtn = document.getElementById('ai-chatbot-btn');
    const chatPopup = document.getElementById('chat-popup');
    const closeChatBtn = document.getElementById('close-chat-btn');
    const chatBody = document.getElementById('chat-body');
    const chatInput = document.getElementById('chat-input');
    const sendChatBtn = document.getElementById('send-chat-btn');
    const suggestionContainer = document.getElementById('chat-suggestion-container');

    const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
    const currentUserId = document.querySelector("meta[name='user_id']")?.getAttribute("content");
    let conversationHistory = [];

    chatbotBtn.addEventListener('click', () => toggleChat(true));
    closeChatBtn.addEventListener('click', () => {
        toggleChat(false);
        conversationHistory = [];
    });
    sendChatBtn.addEventListener('click', () => handleUserInput());
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleUserInput();
    });

    fetchAndDisplaySuggestion();

    async function fetchAndDisplaySuggestion() {
        if (!currentUserId) {
            console.error("사용자 ID를 찾을 수 없어 추천 기능을 실행할 수 없습니다.");
            return;
        }
        const today = new Date().toISOString().slice(0, 10);
        const cacheKey = `aiSuggestions_${currentUserId}_${today}`;
        const FIVE_MINUTES_IN_MS = 5 * 60 * 1000;
        try {
            const cachedItem = localStorage.getItem(cacheKey);
            if (cachedItem) {
                const cachedWrapper = JSON.parse(cachedItem);
                if (cachedWrapper.expiresAt < Date.now()) {
                    localStorage.removeItem(cacheKey);
                } else {
                    const suggestions = cachedWrapper.data;
                    if (suggestions && suggestions.length > 0) {
                        suggestions.forEach(suggestionObj => createSuggestionBubble(suggestionObj));
                    }
                    return;
                }
            }
        } catch (e) {
            localStorage.removeItem(cacheKey);
        }
        try {
            const response = await fetch('/api/ai/suggestion');
            if (!response.ok) return;
            const data = await response.json();
            const suggestions = data.suggestions;
            if (suggestions && Array.isArray(suggestions) && suggestions.length > 0) {
                const expiresAt = Date.now() + FIVE_MINUTES_IN_MS;
                const cacheWrapper = {data: suggestions, expiresAt: expiresAt};
                localStorage.setItem(cacheKey, JSON.stringify(cacheWrapper));
                suggestions.forEach(suggestionObj => createSuggestionBubble(suggestionObj));
            }
        } catch (error) {
            console.error('AI 추천 기능 실행 중 오류:', error);
        }
    }

    function createSuggestionBubble(suggestion) {
        const bubble = document.createElement('div');
        bubble.className = 'chat-suggestion-bubble';
        bubble.textContent = suggestion.displayText;
        suggestionContainer.appendChild(bubble);
        suggestionContainer.classList.remove('hidden');
        bubble.addEventListener('click', () => {
            conversationHistory = [];
            handleGenericAction(suggestion.displayText, suggestion.actionQuery);
        });
    }

    function handleUserInput() {
        const userQuery = chatInput.value;
        if (!userQuery.trim()) return;
        handleGenericAction(null, userQuery);
    }

    async function handleGenericAction(displayTextForAI, queryToServer) {
        toggleChat(true);

        if (displayTextForAI) {
            addMessageToChat('ai', displayTextForAI);
        } else {
            addMessageToChat('user', queryToServer);
        }

        chatInput.value = '';
        const spinnerId = 'spinner-' + Date.now();
        addMessageToChat('ai-loading', '', spinnerId);

        try {
            const headers = {'Content-Type': 'application/json'};
            if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

            const response = await fetch('/api/ai/action', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    actionQuery: queryToServer,
                    history: conversationHistory
                })
            });

            if (!response.ok) throw new Error(`서버 응답 오류: ${response.status}`);

            const result = await response.json();
            const aiResponse = result.message;
            updateMessage(spinnerId, 'ai', aiResponse);

            conversationHistory.push({role: 'user', content: queryToServer});
            conversationHistory.push({role: 'model', content: aiResponse});

        } catch (error) {
            console.error('AI 액션 처리 오류:', error);
            updateMessage(spinnerId, 'ai', '죄송합니다. 요청을 처리하는 중 오류가 발생했어요.');
        }
    }

    function toggleChat(show) {
        if (show) {
            chatPopup.classList.remove('hidden');
            if (suggestionContainer) {
                suggestionContainer.style.display = 'none';
            }
        } else {
            chatPopup.classList.add('hidden');
        }
    }

    function addMessageToChat(sender, message, id) {
        const messageDiv = document.createElement('div');
        if (id) messageDiv.id = id;

        if (sender === 'ai-loading') {
            messageDiv.className = 'ai-message spinner-bubble';
            messageDiv.innerHTML = '<div class="spinner-circle"></div><span>잠시만 기다려주세요...</span>';
        } else {
            messageDiv.className = sender === 'user' ? 'user-message' : 'ai-message';
            messageDiv.textContent = message;
        }

        const chatBody = document.getElementById('chat-body');
        chatBody.appendChild(messageDiv);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    function updateMessage(id, sender, message) {
        const messageDiv = document.getElementById(id);
        if (messageDiv) {
            messageDiv.className = sender === 'user' ? 'user-message' : 'ai-message';
            messageDiv.innerHTML = '';
            messageDiv.textContent = message;
        }
    }
});
