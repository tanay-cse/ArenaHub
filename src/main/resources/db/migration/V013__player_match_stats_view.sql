CREATE VIEW player_match_stats AS
SELECT 
    mr.user_id, 
    m.game_id, 
    COUNT(*) AS matches_played, 
    SUM(CASE WHEN mr.outcome = 'WIN' THEN 1 ELSE 0 END) AS wins, 
    SUM(CASE WHEN mr.outcome = 'LOSS' THEN 1 ELSE 0 END) AS losses, 
    SUM(CASE WHEN mr.outcome = 'DRAW' THEN 1 ELSE 0 END) AS draws 
FROM match_results mr 
JOIN matches m ON m.id = mr.match_id 
GROUP BY mr.user_id, m.game_id;
