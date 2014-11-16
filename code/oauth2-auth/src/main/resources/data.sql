-- users in system
insert into account(account_name , password) values('dsyer', 'password');
insert into account(account_name , password) values('jlong', 'password');
insert into account(account_name , password) values('rwinch', 'password');

-- oauth client details
insert into client_details(   client_id, client_secret,  resource_ids,   scopes,   grant_types,                                  authorities)
                    values(   'acme' ,  'acmesecret',    null,           'read',   'authorization_code,refresh_token,password',  null );
