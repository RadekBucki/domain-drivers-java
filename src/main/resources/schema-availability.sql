create table if not exists resource_availability (
    id uuid not null,
    resource_id uuid not null,
    from_date timestamp not null,
    to_date timestamp not null,
    owner_id uuid,
    resource_status varchar(10) not null,
    primary key (resource_id, from_date, to_date)
);
