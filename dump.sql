drop databaseFabData fab_data;
create databaseFabData fab_data;
use fab_data;

create table event
(
  id       int auto_increment
    primary key,
  equip    varchar(30)          not null,
  recipe   varchar(30)          not null,
  step     varchar(30)          null,
  holdtype varchar(30)          not null,
  holdflag tinyint(1) default 0 not null,
  datetime date                 not null
);

