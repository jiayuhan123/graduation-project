const zlib = require('zlib');
const fs = require('fs');
const https = require('https');

const text = `erDiagram
    USER_INFO ||--o{ USER_AUTH : has
    USER_INFO ||--o{ USER_ADDRESS : has
    SYS_USER ||--o{ SYS_USER_ROLE : assigns
    SYS_ROLE ||--o{ SYS_USER_ROLE : includes
    SYS_ROLE ||--o{ SYS_ROLE_MENU : has
    SYS_MENU ||--o{ SYS_ROLE_MENU : grants
    SYS_USER ||--o| USER_INFO : binds
    USER_INFO { bigint id PK varchar nickname varchar avatar tinyint gender tinyint status datetime created_at }
    USER_AUTH { bigint id PK bigint user_id FK varchar identity_type varchar identifier varchar credential }
    USER_ADDRESS { bigint id PK bigint user_id FK varchar recipient_name varchar phone varchar region varchar detail_address tinyint is_default }
    SYS_USER { bigint id PK varchar username varchar password varchar real_name varchar phone bigint bind_user_id FK tinyint status }
    SYS_ROLE { bigint id PK varchar role_name varchar role_code tinyint status }
    SYS_MENU { bigint id PK bigint parent_id varchar menu_name varchar path varchar perms char menu_type }
    SYS_USER_ROLE { bigint user_id PK bigint role_id PK }
    SYS_ROLE_MENU { bigint role_id PK bigint menu_id PK }`;

const b64 = zlib.deflateSync(text).toString('base64').replace(/\+/g, '-').replace(/\//g, '_');
const url = 'https://kroki.io/mermaid/png/' + b64;

const file1 = fs.createWriteStream('e:/graduation-project/项目文档/4-数据库设计/er_diagram.png');
const file2 = fs.createWriteStream('C:/Users/33114/.gemini/antigravity/brain/16a08acd-ab1d-44e4-92b6-41f29e4510db/er_diagram.png');

https.get(url, function(response) {
  response.pipe(file1);
  response.pipe(file2);
  file1.on('finish', () => console.log('Downloaded file1'));
  file2.on('finish', () => console.log('Downloaded file2'));
});
