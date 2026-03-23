create table job_execution_locks (
    job_name varchar(150) primary key,
    owner_id varchar(150) not null,
    locked_until timestamp with time zone not null,
    last_heartbeat timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null
);
