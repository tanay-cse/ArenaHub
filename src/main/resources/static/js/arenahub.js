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
