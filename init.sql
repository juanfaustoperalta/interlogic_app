CREATE TABLE IF NOT EXISTS roles
(
    id   serial not null
        constraint roles_pkey
            primary key,
    name varchar(20)
);

CREATE TABLE IF NOT EXISTS transaction_history
(
    id               bigint not null
        constraint transaction_history_pkey
            primary key,
    cause            varchar(255),
    created_at       timestamp,
    exception        varchar(255),
    status           varchar(255),
    transaction_name varchar(255)
);

CREATE TABLE IF NOT EXISTS users
(
    id       bigint not null
        constraint users_pkey
            primary key,
    password varchar(255),
    username varchar(255)
        constraint ukr43af9ap4edm43mmtq01oddj6
            unique
);


CREATE TABLE IF NOT EXISTS user_roles
(
    user_id bigint  not null
        constraint fkhfh9dx7w3ubf1co1vdev94g3f
            references users,
    role_id integer not null
        constraint fkh8ciramu9cc9q3qcqiv4ue8a6
            references roles,
    constraint user_roles_pkey
        primary key (user_id, role_id)
);

INSERT INTO public.roles (id, name)
VALUES (1, 'USER_ROLE');

