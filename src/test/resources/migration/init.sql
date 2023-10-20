create table corrections
(
    id          integer primary key auto_increment,
    what_did_do varchar(255)
);

create table phone
(
    id          integer primary key auto_increment,
    external_id bigint,
    phone       varchar(255),
    status      varchar(25)
);

create table phone_corrections
(
    phone_id       bigint  not null
        constraint phone_id_pk references phone,
    corrections_id integer not null
        constraint corrections_id_pk unique
        constraint fkk0qij7hr0758yn4mqfqih6vvp references corrections
);
