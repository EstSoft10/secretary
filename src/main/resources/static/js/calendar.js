// calendar.js 전체 스크립트

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
