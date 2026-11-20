// Arena Hub - Navigation active-link highlighting
document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href) {
            if (href === currentPath || (href !== '/' && currentPath.startsWith(href))) {
                link.classList.add('active');
            }
        }
    });
});

// Tournament Bracket 10-second polling
document.addEventListener('DOMContentLoaded', () => {
    const bracketContainer = document.getElementById('tournament-bracket');
    if (!bracketContainer) return;

    const tournamentId = bracketContainer.getAttribute('data-tournament-id');
    if (!tournamentId) return;

    function pollBracket() {
        fetch('/tournaments/' + tournamentId + '/bracket/data')
            .then(res => {
                if (!res.ok) throw new Error('Network error');
                return res.json();
            })
            .then(data => {
                if (!data || !data.rounds) return;

                const tourneyStatusBadge = document.getElementById('tourney-status-badge');
                if (tourneyStatusBadge && data.status) {
                    tourneyStatusBadge.textContent = data.status;
                    if (data.status === 'COMPLETED') {
                        tourneyStatusBadge.className = 'badge bg-success';
                    } else if (data.status === 'IN_PROGRESS') {
                        tourneyStatusBadge.className = 'badge bg-warning text-dark';
                    } else {
                        tourneyStatusBadge.className = 'badge bg-secondary';
                    }
                }

                data.rounds.forEach(round => {
                    if (!round.matches) return;
                    round.matches.forEach(m => {
                        const matchCard = document.querySelector(`[data-match-id="${m.matchId}"]`);
                        if (!matchCard) return;

                        const statusBadge = matchCard.querySelector('.match-status-badge');
                        if (statusBadge) {
                            statusBadge.textContent = m.status;
                            statusBadge.className = 'badge match-status-badge ' + (
                                m.status === 'COMPLETED' ? 'bg-success' :
                                m.status === 'DISPUTED' ? 'bg-warning text-dark' :
                                m.status === 'IN_PROGRESS' ? 'bg-info text-dark' : 'bg-secondary'
                            );
                        }

                        const p1Name = matchCard.querySelector('.match-p1-name');
                        const p1Score = matchCard.querySelector('.match-p1-score');
                        const p1Row = matchCard.querySelector('.match-p1-row');
                        if (m.player1) {
                            if (p1Name) p1Name.textContent = m.player1.name || 'TBD';
                            if (p1Score) {
                                p1Score.textContent = (m.player1.score !== null && m.player1.score !== undefined) ? m.player1.score : '-';
                            }
                            if (p1Row) {
                                if (m.player1.isWinner) {
                                    p1Row.classList.add('fw-bold', 'text-success', 'bg-success-subtle');
                                } else {
                                    p1Row.classList.remove('fw-bold', 'text-success', 'bg-success-subtle');
                                }
                            }
                        }

                        const p2Name = matchCard.querySelector('.match-p2-name');
                        const p2Score = matchCard.querySelector('.match-p2-score');
                        const p2Row = matchCard.querySelector('.match-p2-row');
                        if (m.player2) {
                            if (p2Name) p2Name.textContent = m.player2.name || 'TBD';
                            if (p2Score) {
                                p2Score.textContent = (m.player2.score !== null && m.player2.score !== undefined) ? m.player2.score : '-';
                            }
                            if (p2Row) {
                                if (m.player2.isWinner) {
                                    p2Row.classList.add('fw-bold', 'text-success', 'bg-success-subtle');
                                } else {
                                    p2Row.classList.remove('fw-bold', 'text-success', 'bg-success-subtle');
                                }
                            }
                        }
                    });
                });
            })
            .catch(() => {
                // Silently ignore poll errors
            });
    }

    setInterval(pollBracket, 10000);
});

// Arena Status Board 15-second polling and live client-side countdown timer
document.addEventListener('DOMContentLoaded', () => {
    const board = document.getElementById('arena-status-board');
    if (!board) return;

    function formatTimeRemaining(endTimeStr) {
        if (!endTimeStr) return '';
        const end = new Date(endTimeStr).getTime();
        const now = new Date().getTime();
        const diff = end - now;
        if (diff <= 0) {
            return 'Ending soon...';
        }
        const totalSeconds = Math.floor(diff / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        if (hours > 0) {
            return `${hours}h ${String(minutes).padStart(2, '0')}m ${String(seconds).padStart(2, '0')}s remaining`;
        }
        return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')} remaining`;
    }

    function updateCountdowns() {
        const timers = document.querySelectorAll('[data-countdown-end]');
        timers.forEach(timer => {
            const endStr = timer.getAttribute('data-countdown-end');
            timer.textContent = formatTimeRemaining(endStr);
        });
    }

    function pollStatusBoard() {
        fetch('/status-board/data')
            .then(res => {
                if (!res.ok) throw new Error('Status board network error');
                return res.json();
            })
            .then(stations => {
                if (!Array.isArray(stations)) return;
                stations.forEach(s => {
                    const card = document.getElementById('station-card-' + s.stationId);
                    if (!card) return;

                    const badge = card.querySelector('.station-status-badge');
                    if (badge) {
                        badge.textContent = s.status;
                        badge.className = 'badge station-status-badge ' + (
                            s.status === 'AVAILABLE' ? 'bg-success' :
                            s.status === 'ACTIVE' ? 'bg-danger' : 'bg-warning text-dark'
                        );
                    }

                    const countdownContainer = card.querySelector('.countdown-container');
                    if (countdownContainer) {
                        if (s.status === 'ACTIVE' && s.endTime) {
                            countdownContainer.innerHTML = `
                                <div>
                                    <i class="bi bi-stopwatch text-danger me-1"></i>
                                    <span class="countdown-timer fw-bold font-monospace text-danger" data-countdown-end="${s.endTime}">
                                        ${formatTimeRemaining(s.endTime)}
                                    </span>
                                </div>
                            `;
                        } else if (s.status === 'AVAILABLE') {
                            countdownContainer.innerHTML = `
                                <div>
                                    <span class="text-success small fw-medium"><i class="bi bi-check-circle me-1"></i>Ready for play</span>
                                </div>
                            `;
                        } else if (s.status === 'MAINTENANCE') {
                            countdownContainer.innerHTML = `
                                <div>
                                    <span class="text-warning small fw-medium"><i class="bi bi-tools me-1"></i>Under maintenance</span>
                                </div>
                            `;
                        }
                    }

                    const actionContainer = card.querySelector('.action-container');
                    if (actionContainer) {
                        if (s.status === 'AVAILABLE') {
                            actionContainer.innerHTML = `
                                <a href="/bookings?stationId=${s.stationId}" class="btn btn-navy btn-sm w-100">
                                    <i class="bi bi-calendar-plus me-1"></i>Book Station
                                </a>
                            `;
                        } else if (s.status === 'ACTIVE') {
                            actionContainer.innerHTML = `
                                <a href="/waitlist" class="btn btn-outline-secondary btn-sm w-100">
                                    <i class="bi bi-hourglass-split me-1"></i>Join Waitlist
                                </a>
                            `;
                        } else if (s.status === 'MAINTENANCE') {
                            actionContainer.innerHTML = `
                                <button class="btn btn-secondary btn-sm w-100" disabled>
                                    <i class="bi bi-slash-circle me-1"></i>Unavailable
                                </button>
                            `;
                        }
                    }
                });
            })
            .catch(() => {
                // Silently ignore poll errors
            });
    }

    updateCountdowns();
    setInterval(updateCountdowns, 1000);
    setInterval(pollStatusBoard, 15000);
});

// ==========================================
// Booking UI & Dynamic Pricing (Prompt 18 Formula)
// ==========================================
function calculateArenaPrice(baseRate, hour) {
    let peakOffPeakMultiplier = 1.0;
    let label = 'Standard (1.0x)';
    if (hour >= 10 && hour < 14) {
        peakOffPeakMultiplier = 0.80;
        label = 'Morning Off-Peak (0.8x)';
    } else if (hour >= 18 && hour < 22) {
        peakOffPeakMultiplier = 1.50;
        label = 'Evening Peak (1.5x)';
    }

    const memberMultiplier = 0.90;
    const total = Math.round(baseRate * peakOffPeakMultiplier * memberMultiplier * 100) / 100;
    return {
        baseRate: baseRate,
        multiplier: peakOffPeakMultiplier,
        ruleLabel: label,
        total: total.toFixed(2)
    };
}

// ------------------------------------------
// /bookings/new Interactive Wizard
// ------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    const wizardContainer = document.getElementById('booking-wizard-container');
    if (!wizardContainer) return;

    const stationSelect = document.getElementById('station-select');
    const startTimeInput = document.getElementById('startTime-input');
    const dateStrip = document.getElementById('booking-date-strip');
    const slotChipsGrid = document.getElementById('slot-chips-grid');
    const confirmBtn = document.getElementById('btn-confirm-booking');

    const summaryStationName = document.getElementById('summary-station-name');
    const summaryStationType = document.getElementById('summary-station-type');
    const summaryDate = document.getElementById('summary-date');
    const summaryTime = document.getElementById('summary-time');
    const summaryBaseRate = document.getElementById('summary-base-rate');
    const summaryRuleLabel = document.getElementById('summary-rule-label');
    const summaryMultiplier = document.getElementById('summary-multiplier');
    const summaryTotalPrice = document.getElementById('summary-total-price');

    let selectedStation = null;
    let selectedDate = null; // 'YYYY-MM-DD'
    let selectedHour = null; // integer 10..21

    // 1. Station-type tab filtering
    const tabs = document.querySelectorAll('#station-type-tabs [data-station-filter]');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            const filter = tab.getAttribute('data-station-filter');
            const items = document.querySelectorAll('#stations-grid .station-item');
            items.forEach(item => {
                const type = item.getAttribute('data-station-type');
                if (filter === 'ALL' || type === filter) {
                    item.style.display = '';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    });

    // 2. Station selection cards
    const stationCards = document.querySelectorAll('#stations-grid .station-choice-card');
    function selectStationCard(card) {
        stationCards.forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        const stationId = card.getAttribute('data-station-id');
        const parentCol = card.closest('.station-item');

        const number = parentCol.getAttribute('data-station-number');
        const type = parentCol.getAttribute('data-station-type');
        const rate = parseFloat(parentCol.getAttribute('data-hourly-rate'));

        selectedStation = {
            id: stationId,
            number: number,
            type: type,
            rate: rate
        };

        if (stationSelect) {
            stationSelect.value = stationId;
        }

        if (summaryStationName) summaryStationName.textContent = number;
        if (summaryStationType) summaryStationType.textContent = type;
        if (summaryBaseRate) summaryBaseRate.textContent = '₹' + rate.toFixed(2) + '/hr';

        updateSummary();
    }

    stationCards.forEach(card => {
        card.addEventListener('click', () => selectStationCard(card));
    });

    // 3. 7-Day Date Strip
    function formatIsoDate(d) {
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Parse prefilled startTime if present
    let prefilledDateStr = null;
    let prefilledHourVal = null;
    if (startTimeInput && startTimeInput.value) {
        const parts = startTimeInput.value.split('T');
        if (parts.length >= 2) {
            prefilledDateStr = parts[0];
            prefilledHourVal = parseInt(parts[1].split(':')[0], 10);
        }
    }

    if (dateStrip) {
        dateStrip.innerHTML = '';
        for (let i = 0; i < 7; i++) {
            const dateObj = new Date(today);
            dateObj.setDate(today.getDate() + i);
            const iso = formatIsoDate(dateObj);
            const dayLabel = i === 0 ? 'Today' : dayNames[dateObj.getDay()];
            const dateLabel = `${monthNames[dateObj.getMonth()]} ${dateObj.getDate()}`;

            const pill = document.createElement('div');
            pill.className = 'date-pill';
            pill.setAttribute('data-date', iso);
            pill.innerHTML = `
                <span class="fw-bold fs-6">${dayLabel}</span>
                <small class="text-secondary" style="font-size: 0.75rem;">${dateLabel}</small>
            `;

            pill.addEventListener('click', () => {
                document.querySelectorAll('#booking-date-strip .date-pill').forEach(p => p.classList.remove('active'));
                pill.classList.add('active');
                selectedDate = iso;
                if (summaryDate) {
                    summaryDate.textContent = `${dayNames[dateObj.getDay()]}, ${monthNames[dateObj.getMonth()]} ${dateObj.getDate()}, ${dateObj.getFullYear()}`;
                }
                renderSlotChips();
                updateSummary();
            });

            dateStrip.appendChild(pill);
        }
    }

    // 4. Render Hourly Slot Chips (10:00 to 21:00)
    function renderSlotChips() {
        if (!slotChipsGrid) return;
        slotChipsGrid.innerHTML = '';

        const now = new Date();
        const currentIso = formatIsoDate(now);
        const currentHour = now.getHours();

        for (let h = 10; h <= 21; h++) {
            const timeLabel = `${String(h).padStart(2, '0')}:00 – ${String(h + 1).padStart(2, '0')}:00`;
            const isToday = (selectedDate === currentIso);
            const isPast = isToday && (h <= currentHour);

            const col = document.createElement('div');
            col.className = 'col';

            const chip = document.createElement('div');
            chip.setAttribute('data-hour', h);
            chip.setAttribute('data-time-label', timeLabel);

            if (isPast) {
                chip.className = 'slot-chip slot-past h-100';
                chip.innerHTML = `
                    <span class="fs-6 fw-bold text-muted mb-1">${timeLabel}</span>
                    <span class="badge bg-secondary">PAST</span>
                    <small class="text-muted mt-2">Past</small>
                `;
            } else {
                const isSelected = (selectedHour === h);
                chip.className = `slot-chip h-100 ${isSelected ? 'slot-selected' : 'slot-available'}`;
                chip.innerHTML = `
                    <span class="fs-6 fw-bold mb-1">${timeLabel}</span>
                    <span class="badge ${isSelected ? 'bg-light text-dark' : 'bg-success'}">${isSelected ? 'SELECTED' : 'AVAILABLE'}</span>
                    <small class="mt-2 fw-semibold ${isSelected ? 'text-white' : 'text-success'}">${isSelected ? 'Chosen' : 'Select'}</small>
                `;

                chip.addEventListener('click', () => {
                    selectedHour = h;
                    renderSlotChips();
                    updateSummary();
                });
            }

            col.appendChild(chip);
            slotChipsGrid.appendChild(col);
        }
    }

    // 5. Update Booking Summary Card & Form Inputs
    function updateSummary() {
        if (selectedDate && selectedHour !== null) {
            const timeLabel = `${String(selectedHour).padStart(2, '0')}:00 – ${String(selectedHour + 1).padStart(2, '0')}:00`;
            if (summaryTime) summaryTime.textContent = timeLabel;
            if (startTimeInput) {
                startTimeInput.value = `${selectedDate}T${String(selectedHour).padStart(2, '0')}:00:00`;
            }
        } else {
            if (summaryTime) summaryTime.textContent = 'Select time slot';
        }

        if (selectedStation && selectedHour !== null) {
            const pricing = calculateArenaPrice(selectedStation.rate, selectedHour);
            if (summaryRuleLabel) summaryRuleLabel.textContent = pricing.ruleLabel;
            if (summaryMultiplier) summaryMultiplier.textContent = `${pricing.multiplier.toFixed(1)}x`;
            if (summaryTotalPrice) summaryTotalPrice.textContent = '₹' + pricing.total;
        } else if (selectedStation) {
            if (summaryTotalPrice) summaryTotalPrice.textContent = '₹' + selectedStation.rate.toFixed(2);
        }

        const isReady = (selectedStation !== null && selectedDate !== null && selectedHour !== null);
        if (confirmBtn) {
            confirmBtn.disabled = !isReady;
        }
    }

    // Initialize with preselected or first items
    const initialSelectedCard = document.querySelector('#stations-grid .station-choice-card.selected')
        || document.querySelector('#stations-grid .station-choice-card');
    if (initialSelectedCard) {
        selectStationCard(initialSelectedCard);
    }

    // Set initial date
    const targetDateStr = prefilledDateStr || formatIsoDate(today);
    const targetPill = document.querySelector(`#booking-date-strip [data-date="${targetDateStr}"]`)
        || document.querySelector('#booking-date-strip .date-pill');
    if (targetPill) {
        targetPill.click();
    }

    // Set initial hour if prefilled
    if (prefilledHourVal !== null && prefilledHourVal >= 10 && prefilledHourVal <= 21) {
        selectedHour = prefilledHourVal;
        renderSlotChips();
        updateSummary();
    }
});

// ------------------------------------------
// /stations/{id}/availability Interactive Strip & Selection
// ------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    const availContainer = document.getElementById('availability-view-container');
    if (!availContainer) return;

    const stationId = availContainer.getAttribute('data-station-id');
    const hourlyRate = parseFloat(availContainer.getAttribute('data-hourly-rate')) || 100.0;
    const selectedDate = availContainer.getAttribute('data-selected-date');

    const startTimeInput = document.getElementById('avail-startTime-input');
    const summaryTime = document.getElementById('avail-summary-time');
    const summaryRuleLabel = document.getElementById('avail-rule-label');
    const summaryMultiplier = document.getElementById('avail-multiplier');
    const summaryTotalPrice = document.getElementById('avail-total-price');
    const confirmBtn = document.getElementById('avail-confirm-btn');

    // 1. Render Horizontal 7-Day Date Strip linking to this station's availability
    const dateStrip = document.getElementById('availability-date-strip');
    if (dateStrip) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

        function formatIso(d) {
            const year = d.getFullYear();
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        }

        dateStrip.innerHTML = '';
        for (let i = 0; i < 7; i++) {
            const dateObj = new Date(today);
            dateObj.setDate(today.getDate() + i);
            const iso = formatIso(dateObj);
            const isActive = (iso === selectedDate);
            const dayLabel = i === 0 ? 'Today' : dayNames[dateObj.getDay()];
            const dateLabel = `${monthNames[dateObj.getMonth()]} ${dateObj.getDate()}`;

            const a = document.createElement('a');
            a.className = `date-pill ${isActive ? 'active' : ''}`;
            a.href = `/stations/${stationId}/availability?date=${iso}`;
            a.innerHTML = `
                <span class="fw-bold fs-6">${dayLabel}</span>
                <small class="text-secondary" style="font-size: 0.75rem;">${dateLabel}</small>
            `;
            dateStrip.appendChild(a);
        }
    }

    // 2. Handle slot chip clicks
    const availableChips = document.querySelectorAll('#availability-view-container .slot-chip.slot-available');
    availableChips.forEach(chip => {
        chip.addEventListener('click', () => {
            // Deselect other chips
            availableChips.forEach(c => {
                c.classList.remove('slot-selected');
                const badge = c.querySelector('.badge');
                if (badge) {
                    badge.className = 'badge bg-success';
                    badge.textContent = 'AVAILABLE';
                }
                const actionText = c.querySelector('.slot-action-text');
                if (actionText) {
                    actionText.className = 'text-success mt-2 fw-semibold slot-action-text';
                    actionText.innerHTML = '<i class="bi bi-hand-index me-1"></i>Select';
                }
            });

            // Select this chip
            chip.classList.add('slot-selected');
            const badge = chip.querySelector('.badge');
            if (badge) {
                badge.className = 'badge bg-light text-dark';
                badge.textContent = 'SELECTED';
            }
            const actionText = chip.querySelector('.slot-action-text');
            if (actionText) {
                actionText.className = 'text-white mt-2 fw-semibold slot-action-text';
                actionText.innerHTML = '<i class="bi bi-check-circle me-1"></i>Selected';
            }

            const hour = parseInt(chip.getAttribute('data-hour'), 10);
            const timeLabel = chip.getAttribute('data-time-label');

            if (summaryTime) summaryTime.textContent = timeLabel;
            if (startTimeInput) {
                startTimeInput.value = `${selectedDate}T${String(hour).padStart(2, '0')}:00:00`;
            }

            // Calculate price
            const pricing = calculateArenaPrice(hourlyRate, hour);
            if (summaryRuleLabel) summaryRuleLabel.textContent = pricing.ruleLabel;
            if (summaryMultiplier) summaryMultiplier.textContent = `${pricing.multiplier.toFixed(1)}x`;
            if (summaryTotalPrice) summaryTotalPrice.textContent = '₹' + pricing.total;

            if (confirmBtn) {
                confirmBtn.disabled = false;
            }
        });
    });
});

// ------------------------------------------
// Tournaments Tab Filtering
// ------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    const tourneyTabs = document.querySelectorAll('#tournament-filter-tabs [data-tournament-filter]');
    if (!tourneyTabs || tourneyTabs.length === 0) return;

    const tourneyItems = document.querySelectorAll('.tournament-item');
    const emptyFilterAlert = document.getElementById('tournaments-filter-empty');

    tourneyTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tourneyTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            const filter = tab.getAttribute('data-tournament-filter');
            let visibleCount = 0;

            tourneyItems.forEach(item => {
                const status = item.getAttribute('data-tournament-status');
                let match = false;
                if (filter === 'ALL') {
                    match = true;
                } else if (filter === 'OPEN') {
                    match = (status === 'REGISTRATION_OPEN' || status === 'ANNOUNCED');
                } else if (filter === 'IN_PROGRESS') {
                    match = (status === 'IN_PROGRESS');
                } else if (filter === 'COMPLETED') {
                    match = (status === 'COMPLETED');
                }

                if (match) {
                    item.style.display = '';
                    visibleCount++;
                } else {
                    item.style.display = 'none';
                }
            });

            if (emptyFilterAlert) {
                emptyFilterAlert.style.display = (visibleCount === 0) ? 'block' : 'none';
            }
        });
    });
});

