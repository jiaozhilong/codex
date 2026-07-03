create database solutionpilot;

\connect solutionpilot

-- V1 本地默认使用 postgres / postgis 连接。
grant all on schema public to postgres;
alter schema public owner to postgres;
