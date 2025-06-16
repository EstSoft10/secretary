// DOM 요소 참조
const calendarEl = document.getElementById('calendar');
const modal = document.getElementById('schedule-modal');
const scheduleForm = document.getElementById('schedule-form');
const saveBtn = document.getElementById('save-btn');
const editButtons = document.getElementById('edit-buttons');
const updateBtn = document.getElementById('update-btn');
const deleteBtn = document.getElementById('delete-btn');
const csrfToken = document.querySelector('input[name="_csrf"]')?.value;
const aiChatBtn = document.getElementById('ai-chatbot-btn');

// FullCalendar 초기화
const calendar = new FullCalendar.Calendar(calendarEl, {
    initialView: 'dayGridMonth',
    locale: 'ko',
    headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'addScheduleButton'
    },
    customButtons: {
        addScheduleButton: {
            text: '일정 추가',
            click: function () {
                scheduleForm.reset();
                document.getElementById('scheduleIdHidden').value = '';
                saveBtn.classList.remove('hidden');
                editButtons.classList.add('hidden');
                modal.classList.remove('hidden');
            }
        }
    },
    events: function (fetchInfo, successCallback) {
        const start = fetchInfo.startStr.split('+')[0];
        const end = fetchInfo.endStr.split('+')[0];

        const startDate = new Date(fetchInfo.start);
        const endDate = new Date(fetchInfo.end);
        const startYear = startDate.getFullYear();
        const endYear = endDate.getFullYear();
        const startMonth = startDate.getMonth() + 1;
        const endMonth = endDate.getMonth() + 1;

        const allEvents = [];

        // 일정, 일정 카운트 불러오기
        const scheduleFetch = Promise.all([
            fetch(`/api/schedules?start=${start}&end=${end}`)
                .then(res => res.ok ? res.json() : []),
            fetch(`/api/schedules/counts?start=${start}&end=${end}`)
                .then(res => res.ok ? res.json() : [])
        ]).then(([events, counts]) => {
            events.forEach(e => allEvents.push(e));
            counts.forEach(c => {
                allEvents.push({
                    title: `${c.count}건`,
                    start: c.date,
                    allDay: true,
                    display: 'background',
                    backgroundColor: '#e3f2fd',
                    textColor: '#0d47a1'
                });
            });
        });

        // 공휴일 불러오기
        const holidayFetches = [];
        for (let y = startYear; y <= endYear; y++) {
            const startM = y === startYear ? startMonth : 1;
            const endM = y === endYear ? endMonth : 12;
            for (let m = startM; m <= endM; m++) {
                holidayFetches.push(
                    fetch(`/api/holidays?year=${y}&month=${m.toString().padStart(2, '0')}`)
                        .then(res => res.ok ? res.json() : [])
                        .then(holidays => {
                            holidays.forEach(h => {
                                allEvents.push({
                                    title: h.title,
                                    start: h.start,
                                    allDay: true,
                                    borderColor: '#ffcccc',
                                    backgroundColor: '#ffcccc',
                                    textColor: '#c00909'
                                });

                                const dateCell = document.querySelector(`.fc-daygrid-day[data-date='${h.start}'] .fc-daygrid-day-number`);
                                if (dateCell) {
                                    dateCell.style.color = '#e53935';
                                }
                            });
                        })
                );
            }
        }

        Promise.all([scheduleFetch, ...holidayFetches])
            .then(() => successCallback(allEvents))
            .catch(err => {
                console.error("이벤트 로드 실패", err);
                successCallback([]);
            });
    },
    eventClick: function (info) {
        const event = info.event;
        if (!event.extendedProps || !event.extendedProps.scheduleId) return;

        const id = event.extendedProps.scheduleId;
        fetch(`/api/schedules/${id}`)
            .then(res => res.ok ? res.json() : Promise.reject("조회 실패"))
            .then(schedule => {
                if (!schedule) return;
                scheduleForm.reset();
                scheduleForm.title.value = schedule.title;
                scheduleForm.content.value = schedule.content;
                scheduleForm.start.value = schedule.start.slice(0, 16);
                scheduleForm.end.value = schedule.end ? schedule.end.slice(0, 16) : '';
                scheduleForm.location.value = schedule.location;
                scheduleForm.scheduleIdHidden.value = id;
                saveBtn.classList.add('hidden');
                editButtons.classList.remove('hidden');
                modal.classList.remove('hidden');
            })
            .catch(console.error);
    },
    dayCellDidMount: function (arg) {
        const day = arg.date.getDay();
        const numberEl = arg.el.querySelector('.fc-daygrid-day-number');
        if (day === 0) {
            if (numberEl) numberEl.style.color = '#e53935';
        } else if (day === 6) {
            if (numberEl) numberEl.style.color = '#1e88e5';
        }
    },
    dayHeaderDidMount: function (arg) {
        if (arg.el.innerText === '일') {
            arg.el.style.color = '#e53935';
        } else if (arg.el.innerText === '토') {
            arg.el.style.color = '#1e88e5';
        }
    }
});

calendar.render();

// 일정 추가 버튼 동작
const openBtn = document.getElementById('open-modal');
if (openBtn) {
    openBtn.addEventListener('click', () => {
        scheduleForm.reset();
        scheduleForm.scheduleIdHidden.value = '';
        saveBtn.classList.remove('hidden');
        editButtons.classList.add('hidden');
        modal.classList.remove('hidden');
    });
}

// 닫기 버튼
const closeBtn = document.querySelector('.close');
if (closeBtn) {
    closeBtn.addEventListener('click', () => {
        modal.classList.add('hidden');
    });
}

// 일정 저장 (신규)
scheduleForm.addEventListener('submit', function (e) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(scheduleForm));
    const url = data.scheduleIdHidden ? `/api/schedules/${data.scheduleIdHidden}` : '/api/schedules';
    const method = data.scheduleIdHidden ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
        },
        body: JSON.stringify(data)
    }).then(res => {
        if (res.ok) {
            modal.classList.add('hidden');
            scheduleForm.reset();
            calendar.refetchEvents();
            suggestAIQuestions(data);
        }
    });
});

// 일정 수정
updateBtn?.addEventListener('click', () => {
    const data = Object.fromEntries(new FormData(scheduleForm));
    const id = data.scheduleIdHidden;
    if (!id) return;

    fetch(`/api/schedules/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
        },
        body: JSON.stringify(data)
    }).then(res => {
        if (res.ok) {
            modal.classList.add('hidden');
            scheduleForm.reset();
            calendar.refetchEvents();
        }
    });
});

// 일정 삭제
deleteBtn?.addEventListener('click', () => {
    const id = scheduleForm.scheduleIdHidden.value;
    if (!id) return;
    if (!confirm('일정을 삭제하시겠습니까?')) return;

    fetch(`/api/schedules/${id}`, {
        method: 'DELETE',
        headers: {
            'X-CSRF-TOKEN': csrfToken
        }
    }).then(res => {
        if (res.ok) {
            modal.classList.add('hidden');
            scheduleForm.reset();
            calendar.refetchEvents();
        }
    });
});

function openExportModal() {
    document.getElementById('export-modal').classList.remove('hidden');
}

function closeExportModal() {
    document.getElementById('export-modal').classList.add('hidden');
}

document.getElementById('export-form').addEventListener('submit', function (e) {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const csrfToken = document.querySelector('input[name="_csrf"]').value;

    // UI 처리
    document.querySelector('button[type="submit"]').disabled = true;
    document.getElementById('loading-message').classList.remove('hidden');

    fetch('/schedule/export', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': csrfToken
        },
        body: new URLSearchParams({email})
    })
        .then(res => res.text())
        .then(result => {
            if (result === 'ok') {
                alert('메일 전송 요청 완료! 메일함을 확인하세요.');
                closeExportModal();
            } else if (result === 'empty') {
                alert('서비스에 등록된 일정이 없습니다.');
                closeExportModal();
            } else {
                alert('메일 전송 중 문제가 발생했습니다.');
            }
        })
        .catch(() => alert('네트워크 오류가 발생했습니다.'))
        .finally(() => {
            document.querySelector('button[type="submit"]').disabled = false;
            document.getElementById('loading-message').classList.add('hidden');
        });
});

document.getElementById('icsFile').addEventListener('change', function () {
    if (this.files.length > 0) {
        const confirmed = confirm('ICS 파일의 일정을 등록하시겠습니까?');
        if (confirmed) {
            document.getElementById('ics-upload-form').submit();
        } else {
            this.value = '';
        }
    }
});


let currentConversationId = null;
let categoryConfigs = [];

document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('chat-input');
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendCustomQuestion();
        }
    });
    loadCategoryConfigs();
});

function loadCategoryConfigs() {
    return fetch('/json/categories.json')
        .then(response => {
            if (!response.ok) throw new Error('Failed to load category configs');
            return response.json();
        })
        .then(json => {
            categoryConfigs = json;
        })
        .catch(error => {
            console.error('categoryConfigs 불러오기 실패:', error);
            categoryConfigs = [];
        });
}

function suggestAIQuestions(scheduleData) {
    const title = scheduleData.title || '';
    const content = scheduleData.content || '';
    const location = scheduleData.location || '';
    const date = new Date(scheduleData.start);
    const time = date.toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'});
    const dateStr = date.toLocaleDateString('ko-KR', {month: 'long', day: 'numeric', weekday: 'short'});

    const container = document.getElementById('chat-suggestion-container');
    container.innerHTML = '';
    container.classList.remove('hidden');

    const suggestionPairs = getSuggestionPairs(title, content, location, date, time, dateStr);

    suggestionPairs.slice(0, 3).forEach(({display, query}, index) => {
        const bubble = document.createElement('div');
        bubble.className = 'chat-suggestion-bubble-calendar';
        bubble.style.animationDelay = `${index * 0.3}s`;
        bubble.innerText = display;
        bubble.dataset.query = query;
        bubble.onclick = () => {
            container.classList.add('hidden');
            openChatWithQuery(query, display);
            clearTimeout(removeTimeout);
        };
        container.appendChild(bubble);
    });

    const removeTimeout = setTimeout(() => {
        const bubbles = container.querySelectorAll('.chat-suggestion-bubble');
        bubbles.forEach((bubble, i) => {
            setTimeout(() => {
                bubble.classList.add('fade-out-up');
                setTimeout(() => {
                    bubble.remove();
                    if (i === bubbles.length - 1) {
                        container.classList.add('hidden');
                    }
                }, 500);
            }, i * 300);
        });
    }, 10000);

}

function getSuggestionPairs(title, content, location, date, timeStr, dateStr) {
    const baseText = `${dateStr} ${timeStr}, ${location || '일정 장소'}에서 "${title}" 일정이 있으시네요.`;
    const suggestions = [];

    if (!categoryConfigs || categoryConfigs.length === 0) return suggestions;

    const text = `${title} ${content}`.toLowerCase();
    let matched = false;

    for (const category of categoryConfigs) {
        for (const keyword of category.keywords) {
            if (text.includes(keyword.toLowerCase())) {
                category.suggestions.forEach(({display, query}) => {
                    suggestions.push({
                        display: `${baseText} ` + applyTemplate(display, keyword, location),
                        query: applyTemplate(query, keyword, location)
                    });
                });
                matched = true;
                break;
            }
        }
        if (matched) break;
    }

    if (!matched) {
        suggestions.push({
            display: `${baseText} 일정 관련해서 도움이 필요하신가요?`,
            query: `${title} 일정 관련 도움 추천해줘`
        });
    }

    return suggestions;
}

function applyTemplate(template, keyword, location = '') {
    const result = template
        .replace(/{{\s*keyword\s*}}/g, keyword)
        .replace(/{{\s*location\s*}}/g, location.trim());

    return result.replace(/^(\s+)/, '');
}


function openChatWithQuery(query, displayText) {
    const popup = document.getElementById('chat-popup');
    popup.classList.remove('hidden');
    triggerAIChat(query, displayText);
}

function triggerAIChat(query, displayText) {
    const chatBody = document.getElementById('chat-body');

    const userMsg = document.createElement('div');
    userMsg.className = 'user-message';
    userMsg.innerText = query;
    chatBody.appendChild(userMsg);

    const spinnerBubble = document.createElement("div");
    spinnerBubble.className = "spinner-bubble";
    const spinnerText = document.createElement("div");
    spinnerText.textContent = "답변을 생성하고 있어요...";
    const spinner = document.createElement("div");
    spinner.className = "spinner-circle";
    spinnerBubble.appendChild(spinnerText);
    spinnerBubble.appendChild(spinner);
    chatBody.appendChild(spinnerBubble);
    spinnerBubble.scrollIntoView({behavior: "smooth"});

    let apiUrl = `/api/async-search?query=${encodeURIComponent(query)}`;
    if (currentConversationId) apiUrl += `&conversationId=${currentConversationId}`;

    fetch(apiUrl)
        .then(res => res.json())
        .then(data => {
            if (data.conversationId) currentConversationId = data.conversationId;

            let rawContent = data.content || "응답 없음";
            if (typeof rawContent === "string" && rawContent.startsWith("{") && rawContent.includes("content")) {
                try {
                    const parsed = JSON.parse(rawContent);
                    rawContent = parsed.content || rawContent;
                } catch (e) {
                }
            }

            const contentHtml = formatContent(rawContent);
            chatBody.removeChild(spinnerBubble);

            const aiMsg = document.createElement('div');
            aiMsg.className = 'ai-message';
            aiMsg.innerHTML = contentHtml;
            chatBody.appendChild(aiMsg);
            aiMsg.scrollIntoView({behavior: 'smooth'});
        });
}

function sendCustomQuestion() {
    const input = document.getElementById('chat-input');
    const query = input.value.trim();
    if (!query) return;
    openChatWithQuery(query);
    input.value = '';
}

function formatContent(text) {
    const codeBlocks = [];
    let content = text.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
        const escaped = code
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/(^|\n)(\s*)#(.*)/g, '$1$2<span class="comment">#$3</span>')
            .replace(/(^|\n)(\s*)\/(\/.*)/g, '$1$2<span class="comment">//$3</span>');

        const placeholder = `__CODEBLOCK_${codeBlocks.length}__`;
        codeBlocks.push(`<pre><code class="language-${lang}">${escaped}</code></pre>`);
        return placeholder;
    });
    content = content.replace(/^###\s(.+)$/gm, '<h3>$1</h3>')
        .replace(/`([^`]+?)`/g, '<code class="inline">$1</code>')
        .replace(/\[(.+?)\]\((.*?)\)/g, '<a href="$2" target="_blank" class="source-btn">$1</a>')
        .replace(/:\n-\s*/g, ':<br>• ')
        .replace(/(^|\n)-\s*/g, '$1• ');

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

    content = content.replace(/^(\d+)\.\s*<br>\s*<span class="bold">/gm, '$1. <span class="bold">')
        .replace(/([^\n])\n(?!(<\/?h3>|<\/?pre>))/g, '$1<br>');

    codeBlocks.forEach((block, i) => {
        content = content.replace(`__CODEBLOCK_${i}__`, block);
    });

    return content;
}

