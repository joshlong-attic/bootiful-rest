create table account ( ACCOUNT_NAME varchar(255) not null,
                      PASSWORD VARCHAR (255 ) not null,
                      ID serial ,
                      ENABLED bool default true);