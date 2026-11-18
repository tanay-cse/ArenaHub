INSERT INTO users (role_id, email, password_hash, full_name, phone, is_active)
VALUES 
((SELECT id FROM roles WHERE name = 'ADMIN'), 'admin@arenahub.com', '$2a$10$VUZYgoGU61jP.GOSCX/C3.AjR5RQpneChZ3fXCQL3wFlPL8onkBMu', 'System Admin', '1000000001', TRUE),
((SELECT id FROM roles WHERE name = 'STAFF'), 'staff@arenahub.com', '$2a$10$3myHildNOxn3LC1IrlMJgevBf8ZcLF2FUmIgm5azhlXoofFng8i6G', 'Arena Staff', '1000000002', TRUE),
((SELECT id FROM roles WHERE name = 'STUDENT'), 'student@arenahub.com', '$2a$10$EBFgC/doKV7x/wEX6tDEHuGLMAkFWXSMyKLCfdi7FsroiFjeFLFz.', 'Demo Student', '1000000003', TRUE);
