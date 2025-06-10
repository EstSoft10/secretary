document.addEventListener('DOMContentLoaded', function () {
    const calendarEl = document.getElementById('calendar');
    const selectedDate = document.getElementById('selected-date');
    const scheduleList = document.getElementById('schedule-list');
    const modal = document.getElementById('schedule-modal');
    const openModalBtn = document.getElementById('open-modal');
    const closeModalBtn = document.querySelector('.close');
    const scheduleForm = document.getElementById('schedule-form');
    let currentSelectedDate = null;
    let previouslySelectedCell = null;

    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'ko',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: ''
        },
        events: function (fetchInfo, successCallback) {
            const startParam = fetchInfo.startStr.split('+')[0];
            const endParam = fetchInfo.endStr.split('+')[0];

            const startDate = new Date(fetchInfo.start);
            const endDate = new Date(fetchInfo.end);

            const startYear = startDate.getFullYear();
            const startMonth = startDate.getMonth() + 1;
            const endYear = endDate.getFullYear();
            const endMonth = endDate.getMonth() + 1;
            const allEvents = [];

            const scheduleFetch = Promise.all([
                fetch(`/api/schedules?start=${startParam}&end=${endParam}`)
                    .then(res => res.ok ? res.json() : Promise.reject("일정 조회 실패")),
                fetch(`/api/schedules/counts?start=${startParam}&end=${endParam}`)
                    .then(res => res.ok ? res.json() : Promise.reject("일정 count 실패"))
            ]).then(([events, counts]) => {
                (events || []).forEach(e => allEvents.push(e));
                (counts || []).forEach(c => {
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

            const holidayFetches = [];
            for (let y = startYear; y <= endYear; y++) {
                const startM = y === startYear ? startMonth : 1;
                const endM = y === endYear ? endMonth : 12;
                for (let m = startM; m <= endM; m++) {
                    holidayFetches.push(
                        fetch(`/api/holidays?year=${y}&month=${m.toString().padStart(2, '0')}`)
                            .then(res => res.ok ? res.json() : [])
                            .then(holidays => {
                                (holidays || []).forEach(h => {
                                    allEvents.push({
                                        title: h.title,
                                        start: h.start,
                                        allDay: true,
                                        borderColor: '#ffccb6',
                                        backgroundColor: '#ffccb6',
                                        textColor: '#c00909'
                                    });
                                });
                            })
                    );
                }
            }

            Promise.all([scheduleFetch, ...holidayFetches])
                .then(() => successCallback(allEvents))
                .catch(err => {
                    console.error("캘린더 Events 로드 실패", err);
                    successCallback([]);
                });
        },
        dateClick: function (info) {
            currentSelectedDate = info.dateStr;
            selectedDate.innerText = info.dateStr;
            loadSchedules(info.dateStr);

            if (previouslySelectedCell) {
                previouslySelectedCell.classList.remove('selected-date');
            }
            const cell = document.querySelector(`.fc-daygrid-day[data-date='${info.dateStr}']`);
            if (cell) {
                cell.classList.add('selected-date');
                previouslySelectedCell = cell;
            }
        }
    });

    calendar.render();

    const today = new Date().toISOString().slice(0, 10);
    currentSelectedDate = today;
    selectedDate.innerText = today;
    const todayCell = document.querySelector(`.fc-daygrid-day[data-date='${today}']`);
    if (todayCell) {
        todayCell.classList.add('selected-date');
        previouslySelectedCell = todayCell;
    }
    loadSchedules(today);

    function loadSchedules(dateStr) {
        fetch(`/api/schedules/day?date=${dateStr}`)
            .then(res => res.json())
            .then(data => {
                scheduleList.innerHTML = '';
                data.forEach(s => {
                    const start = new Date(s.start)
                        .toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
                    const end = s.end
                        ? new Date(s.end)
                            .toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
                        : '';
                    const li = document.createElement('li');

                    const hasLocation = !!s.location;
                    const aiButton = hasLocation
                        ? `<button class="ai-btn" data-location="${s.location}" data-title="${s.title}">이곳은 어때요?</button>`
                        : '';

                    li.innerHTML = `
                    <div>
                        <strong>${start}${end ? ' ~ ' + end : ''} ${hasLocation ? '(' + s.location + ')' : ''} ${s.title}</strong><br>
                        <button class="edit-btn"   data-id="${s.scheduleId}">수정</button>
                        <button class="delete-btn" data-id="${s.scheduleId}">삭제</button>
                        ${aiButton}
                    </div>
                `;
                    scheduleList.appendChild(li);
                });
            });
    }

    scheduleList.addEventListener('click', (e) => {
        const target = e.target;
        if (target.classList.contains('delete-btn')) {
            const id = target.dataset.id;
            if (!id) return;
            if (confirm("일정을 삭제하시겠습니까?")) {
                fetch(`/api/schedules/${id}`, {method: 'DELETE'})
                    .then(res => {
                        if (res.ok) {
                            calendar.refetchEvents();
                            loadSchedules(currentSelectedDate);
                        }
                    });
            }
        } else if (target.classList.contains('edit-btn')) {
            const id = target.dataset.id;
            if (!id) return;
            fetch(`/api/schedules/day?date=${currentSelectedDate}`)
                .then(res => res.json())
                .then(data => {
                    const schedule = data.find(s => s.scheduleId == id);
                    if (!schedule) return;
                    scheduleForm.title.value = schedule.title;
                    scheduleForm.content.value = schedule.content;
                    scheduleForm.start.value = schedule.start.slice(0, 16);
                    scheduleForm.end.value = schedule.end ? schedule.end.slice(0, 16) : '';
                    scheduleForm.location.value = schedule.location;
                    scheduleForm.querySelector('input[name="scheduleIdHidden"]').value = id;
                    modal.classList.remove('hidden');
                });
        } else if (target.classList.contains('ai-btn')) {
            const location = target.dataset.location;
            const title = target.dataset.title;
            if (location && title) {
                const query = `${location} 근처에서 ${title} 하기 좋은 장소 추천해줘`;
                const encodedQuery = encodeURIComponent(query);

                fetch(`/ai/async-search?query=${encodedQuery}`)
                    .then(res => res.json())
                    .then(data => {
                        window.location.href = `/searchResult?query=${encodedQuery}&conversationId=${data.conversationId}`;
                    });
            }
        }
    });

    scheduleForm.addEventListener('submit', function handleFormSubmit(e) {
        e.preventDefault();
        if (!currentSelectedDate) return;

        const formData = new FormData(scheduleForm);
        const requestData = {};
        formData.forEach((v, k) => requestData[k] = v);

        if (requestData.end && new Date(requestData.end) <= new Date(requestData.start)) {
            alert("종료 시간은 시작 시간 이후여야 합니다.");
            return;
        }

        const scheduleId = requestData.scheduleIdHidden;
        let url, method;
        if (scheduleId) {
            url = `/api/schedules/${scheduleId}`;
            method = 'PUT';
        } else {
            url = '/api/schedules';
            method = 'POST';
        }

        fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(requestData)
        }).then(res => {
            if (res.ok) {
                modal.classList.add('hidden');
                scheduleForm.reset();
                calendar.refetchEvents();
                loadSchedules(currentSelectedDate);
            }
        });
    });

    openModalBtn.addEventListener('click', () => {
        scheduleForm.reset();
        scheduleForm.querySelector('input[name="scheduleIdHidden"]').value = '';
        modal.classList.remove('hidden');
    });

    closeModalBtn.addEventListener('click', () => {
        modal.classList.add('hidden');
    });

});
