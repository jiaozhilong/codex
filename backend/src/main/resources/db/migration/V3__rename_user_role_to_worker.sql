update roles set code = 'WORKER', name = '牛马专用' where code = 'USER';
update roles set name = '管理员' where code = 'ADMIN';
update users set name = '牛马专用' where email = 'user@example.com';
