-- content_db 一年级人教版基础计算内容种子数据
-- 教材范围：人教版新教材，一年级上、下册，根据 2022 年版义务教育课程标准修订。
-- 目录参考：https://basic.smartedu.cn/syncClassroom
--
-- 使用说明：
--   psql -d content_db -f content-service/src/main/resources/db/seed-grade1-pep.sql
--
-- 约定：
-- 1. 本脚本仅导入基础计算范围，暂不导入图形、数学游戏、综合实践和复习单元。
-- 2. 9 开头的低位正整数为系统种子数据保留编号，避免与线上雪花 ID 冲突。
-- 3. created_by/reviewed_by = 0 表示系统初始化；测试题视为系统审核并直接启用。
-- 4. 所有写入均使用稳定编码判重，可安全重复执行。

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. 内容模块：9 个
--    模块编码保留教材原单元号，因此跳过的非计算单元会在编码中留下空号。
-- -----------------------------------------------------------------------------
WITH seed (
    id, code, name, grade_level, semester, learning_theme,
    publisher, edition_description, description, sort_order
) AS (
    VALUES
        (91010101::BIGINT, 'PEP-G01-S01-U01', '5以内数的认识和加、减法', 1::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '认识1～5，理解数的分与合以及加减法意义，掌握5以内加减法。', 10),
        (91010102::BIGINT, 'PEP-G01-S01-U02', '6～10的认识和加、减法', 1::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '认识6～10，掌握10以内加减法、连加、连减和加减混合。', 20),
        (91010104::BIGINT, 'PEP-G01-S01-U04', '11～20的认识', 1::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '认识11～20的组成和数位，掌握20以内不进位加法与不退位减法。', 40),
        (91010105::BIGINT, 'PEP-G01-S01-U05', '20以内的进位加法', 1::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '掌握20以内进位加法、凑十法及相关实际问题。', 50),
        (91010202::BIGINT, 'PEP-G01-S02-U02', '20以内的退位减法', 1::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '掌握20以内退位减法、破十法、想加算减法及相关实际问题。', 20),
        (91010203::BIGINT, 'PEP-G01-S02-U03', '100以内数的认识', 1::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '认识100以内数的组成、读写、顺序和大小，掌握整十数的简单加减法。', 30),
        (91010204::BIGINT, 'PEP-G01-S02-U04', '100以内的口算加、减法', 1::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '掌握两位数与一位数、整十数之间的口算加减法。', 40),
        (91010205::BIGINT, 'PEP-G01-S02-U05', '100以内的笔算加、减法', 1::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '掌握100以内加减法竖式格式以及进位、退位计算。', 50),
        (91010206::BIGINT, 'PEP-G01-S02-U06', '数量间的加减关系', 1::SMALLINT, 2::SMALLINT,
         '数量关系', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '理解相差、比一个数多或少几等数量关系，并解决连续两问问题。', 60)
)
INSERT INTO content_module (
    id, code, name, grade_level, semester, learning_theme,
    publisher, edition_description, description, sort_order, status, created_by
)
SELECT
    id, code, name, grade_level, semester, learning_theme,
    publisher, edition_description, description, sort_order, 1, 0
FROM seed
ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 2. 原子知识点：65 个（上册 32 个，下册 33 个）
-- -----------------------------------------------------------------------------
WITH seed (
    id, module_code, code, name, description, difficulty_level, sort_order
) AS (
    VALUES
        -- 上册 U01：5以内数的认识和加、减法（9）
        (9201010101::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-01', '认识1～5',
         '能够用数表示数量为1～5的事物，并正确认读和书写1～5。', 1::SMALLINT, 10),
        (9201010102::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-02', '5以内数的大小比较',
         '能够使用大于、小于和等于关系比较5以内两个数的大小。', 1::SMALLINT, 20),
        (9201010103::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-03', '第几和几个',
         '能够区分数量几个与顺序第几。', 1::SMALLINT, 30),
        (9201010104::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-04', '5以内数的分与合',
         '能够正确完成5以内数的分解与组成。', 2::SMALLINT, 40),
        (9201010105::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-05', '加法的意义',
         '理解把两部分合起来用加法计算。', 1::SMALLINT, 50),
        (9201010106::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-06', '减法的意义',
         '理解从整体中去掉一部分或求剩余用减法计算。', 1::SMALLINT, 60),
        (9201010107::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-07', '5以内加法',
         '能够正确口算和不超过5的加法。', 1::SMALLINT, 70),
        (9201010108::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-08', '5以内减法',
         '能够正确口算被减数不超过5的减法。', 1::SMALLINT, 80),
        (9201010109::BIGINT, 'PEP-G01-S01-U01', 'KP-PEP-G01-S01-U01-09', '0的认识和有关加减法',
         '理解0表示没有，并能计算含0的5以内加减法。', 2::SMALLINT, 90),

        -- 上册 U02：6～10的认识和加、减法（10）
        (9201010201::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-01', '认识6～10',
         '能够正确认读、书写和表示6～10。', 1::SMALLINT, 10),
        (9201010202::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-02', '6～10的分与合',
         '能够正确完成6～10各数的分解与组成。', 2::SMALLINT, 20),
        (9201010203::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-03', '10以内加法',
         '能够正确口算和不超过10的加法。', 2::SMALLINT, 30),
        (9201010204::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-04', '10以内减法',
         '能够正确口算被减数不超过10的减法。', 2::SMALLINT, 40),
        (9201010205::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-05', '10以内加减法的关系',
         '能够根据一组数的组成写出有联系的加法和减法算式。', 2::SMALLINT, 50),
        (9201010206::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-06', '用加法解决简单实际问题',
         '能够根据合并情境选择加法并正确列式计算。', 2::SMALLINT, 60),
        (9201010207::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-07', '用减法解决简单实际问题',
         '能够根据去掉或求剩余情境选择减法并正确列式计算。', 2::SMALLINT, 70),
        (9201010208::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-08', '10以内连加',
         '能够按从左到右的顺序正确计算10以内连加。', 3::SMALLINT, 80),
        (9201010209::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-09', '10以内连减',
         '能够按从左到右的顺序正确计算10以内连减。', 3::SMALLINT, 90),
        (9201010210::BIGINT, 'PEP-G01-S01-U02', 'KP-PEP-G01-S01-U02-10', '10以内加减混合',
         '能够按从左到右的顺序正确计算10以内加减混合算式。', 3::SMALLINT, 100),

        -- 上册 U04：11～20的认识（7）
        (9201010401::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-01', '认识计数单位十',
         '理解10个一组成1个十。', 1::SMALLINT, 10),
        (9201010402::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-02', '11～20的认识和读写',
         '能够正确认读和书写11～20各数。', 1::SMALLINT, 20),
        (9201010403::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-03', '11～20的数位组成',
         '能够用几个十和几个一说明11～20各数的组成。', 2::SMALLINT, 30),
        (9201010404::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-04', '11～20的顺序和大小比较',
         '能够按顺序排列并比较11～20各数的大小。', 2::SMALLINT, 40),
        (9201010405::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-05', '10加几及相应减法',
         '能够计算10加几以及十几减10、十几减几。', 2::SMALLINT, 50),
        (9201010406::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-06', '20以内不进位加法',
         '能够正确口算20以内不进位加法。', 2::SMALLINT, 60),
        (9201010407::BIGINT, 'PEP-G01-S01-U04', 'KP-PEP-G01-S01-U04-07', '20以内不退位减法',
         '能够正确口算20以内不退位减法。', 2::SMALLINT, 70),

        -- 上册 U05：20以内的进位加法（6）
        (9201010501::BIGINT, 'PEP-G01-S01-U05', 'KP-PEP-G01-S01-U05-01', '9加几',
         '能够正确口算9加几的20以内进位加法。', 3::SMALLINT, 10),
        (9201010502::BIGINT, 'PEP-G01-S01-U05', 'KP-PEP-G01-S01-U05-02', '8、7、6加几',
         '能够正确口算8、7、6加几的20以内进位加法。', 3::SMALLINT, 20),
        (9201010503::BIGINT, 'PEP-G01-S01-U05', 'KP-PEP-G01-S01-U05-03', '5、4、3、2加几',
         '能够正确口算5、4、3、2加几的20以内进位加法。', 3::SMALLINT, 30),
        (9201010504::BIGINT, 'PEP-G01-S01-U05', 'KP-PEP-G01-S01-U05-04', '用凑十法计算进位加法',
         '能够把一个加数分解并凑成10，正确完成20以内进位加法。', 3::SMALLINT, 40),
        (9201010505::BIGINT, 'PEP-G01-S01-U05', 'KP-PEP-G01-S01-U05-05', '用进位加法解决实际问题',
         '能够从实际情境中提取数量关系并使用进位加法解决问题。', 3::SMALLINT, 50),
        (9201010506::BIGINT, 'PEP-G01-S01-U05', 'KP-PEP-G01-S01-U05-06', '逆向求未知加数',
         '能够根据总数和其中一部分逆向求出未知加数。', 4::SMALLINT, 60),

        -- 下册 U02：20以内的退位减法（6）
        (9201020201::BIGINT, 'PEP-G01-S02-U02', 'KP-PEP-G01-S02-U02-01', '十几减9',
         '能够正确口算十几减9。', 3::SMALLINT, 10),
        (9201020202::BIGINT, 'PEP-G01-S02-U02', 'KP-PEP-G01-S02-U02-02', '十几减8、7、6',
         '能够正确口算十几减8、7、6。', 3::SMALLINT, 20),
        (9201020203::BIGINT, 'PEP-G01-S02-U02', 'KP-PEP-G01-S02-U02-03', '十几减5、4、3、2',
         '能够正确口算十几减5、4、3、2。', 3::SMALLINT, 30),
        (9201020204::BIGINT, 'PEP-G01-S02-U02', 'KP-PEP-G01-S02-U02-04', '用破十法计算退位减法',
         '能够把十几分成10和几，并用破十法完成退位减法。', 3::SMALLINT, 40),
        (9201020205::BIGINT, 'PEP-G01-S02-U02', 'KP-PEP-G01-S02-U02-05', '用想加算减法计算退位减法',
         '能够利用加减法关系，通过想加法计算20以内退位减法。', 3::SMALLINT, 50),
        (9201020206::BIGINT, 'PEP-G01-S02-U02', 'KP-PEP-G01-S02-U02-06', '用退位减法解决实际问题',
         '能够从实际情境中提取数量关系并使用退位减法解决问题。', 3::SMALLINT, 60),

        -- 下册 U03：100以内数的认识（10）
        (9201020301::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-01', '100以内数数',
         '能够按一、十等单位正确数出100以内物体的数量。', 1::SMALLINT, 10),
        (9201020302::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-02', '100以内数的组成',
         '能够用几个十和几个一说明100以内数的组成。', 2::SMALLINT, 20),
        (9201020303::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-03', '100以内数的读写',
         '能够根据数位顺序正确读写100以内各数。', 2::SMALLINT, 30),
        (9201020304::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-04', '认识100',
         '理解10个十是100，并能正确认读和书写100。', 1::SMALLINT, 40),
        (9201020305::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-05', '100以内数的顺序',
         '能够根据百数表等确定100以内数的前后和排列顺序。', 2::SMALLINT, 50),
        (9201020306::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-06', '100以内数的大小比较',
         '能够根据十位和个位比较100以内两个数的大小。', 2::SMALLINT, 60),
        (9201020307::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-07', '用大得多、大一些等描述大小关系',
         '能够用大得多、大一些、小得多、小一些描述数的相对大小。', 2::SMALLINT, 70),
        (9201020308::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-08', '整十数加减整十数',
         '能够正确口算100以内整十数加减整十数。', 2::SMALLINT, 80),
        (9201020309::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-09', '整十数加一位数',
         '能够正确口算整十数加一位数。', 2::SMALLINT, 90),
        (9201020310::BIGINT, 'PEP-G01-S02-U03', 'KP-PEP-G01-S02-U03-10', '整十数加一位数的相应减法',
         '能够计算几十几减整十数或几十几减一位数的基础算式。', 2::SMALLINT, 100),

        -- 下册 U04：100以内的口算加、减法（7）
        (9201020401::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-01', '两位数加一位数（不进位）',
         '能够正确口算两位数加一位数且个位不进位的算式。', 2::SMALLINT, 10),
        (9201020402::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-02', '两位数加整十数',
         '能够正确口算两位数加整十数。', 2::SMALLINT, 20),
        (9201020403::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-03', '两位数加一位数（进位）',
         '能够正确口算两位数加一位数且个位需要进位的算式。', 3::SMALLINT, 30),
        (9201020404::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-04', '两位数减一位数（不退位）',
         '能够正确口算两位数减一位数且个位不退位的算式。', 2::SMALLINT, 40),
        (9201020405::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-05', '两位数减整十数',
         '能够正确口算两位数减整十数。', 2::SMALLINT, 50),
        (9201020406::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-06', '两位数减一位数（退位）',
         '能够正确口算两位数减一位数且个位需要退位的算式。', 3::SMALLINT, 60),
        (9201020407::BIGINT, 'PEP-G01-S02-U04', 'KP-PEP-G01-S02-U04-07', '用连减解决实际问题',
         '能够识别需要连续减去相同或不同数量的情境并正确列式。', 4::SMALLINT, 70),

        -- 下册 U05：100以内的笔算加、减法（6）
        (9201020501::BIGINT, 'PEP-G01-S02-U05', 'KP-PEP-G01-S02-U05-01', '加法竖式的格式和数位对齐',
         '能够按相同数位对齐的规则正确书写加法竖式。', 2::SMALLINT, 10),
        (9201020502::BIGINT, 'PEP-G01-S02-U05', 'KP-PEP-G01-S02-U05-02', '两位数加两位数（不进位）',
         '能够用竖式正确计算两位数加两位数的不进位加法。', 2::SMALLINT, 20),
        (9201020503::BIGINT, 'PEP-G01-S02-U05', 'KP-PEP-G01-S02-U05-03', '两位数加两位数（进位）',
         '能够用竖式正确计算两位数加两位数的进位加法。', 3::SMALLINT, 30),
        (9201020504::BIGINT, 'PEP-G01-S02-U05', 'KP-PEP-G01-S02-U05-04', '减法竖式的格式和数位对齐',
         '能够按相同数位对齐的规则正确书写减法竖式。', 2::SMALLINT, 40),
        (9201020505::BIGINT, 'PEP-G01-S02-U05', 'KP-PEP-G01-S02-U05-05', '两位数减两位数（不退位）',
         '能够用竖式正确计算两位数减两位数的不退位减法。', 2::SMALLINT, 50),
        (9201020506::BIGINT, 'PEP-G01-S02-U05', 'KP-PEP-G01-S02-U05-06', '两位数减两位数（退位）',
         '能够用竖式正确计算两位数减两位数的退位减法。', 3::SMALLINT, 60),

        -- 下册 U06：数量间的加减关系（4）
        (9201020601::BIGINT, 'PEP-G01-S02-U06', 'KP-PEP-G01-S02-U06-01', '求两个数相差几',
         '能够用较大数减较小数求两个数量相差多少。', 3::SMALLINT, 10),
        (9201020602::BIGINT, 'PEP-G01-S02-U06', 'KP-PEP-G01-S02-U06-02', '求比一个数多几的数',
         '能够用加法求比已知数多几的数。', 3::SMALLINT, 20),
        (9201020603::BIGINT, 'PEP-G01-S02-U06', 'KP-PEP-G01-S02-U06-03', '求比一个数少几的数',
         '能够用减法求比已知数少几的数。', 3::SMALLINT, 30),
        (9201020604::BIGINT, 'PEP-G01-S02-U06', 'KP-PEP-G01-S02-U06-04', '含相差关系的连续两问问题',
         '能够分析含相差关系的连续两问问题并分步列式解答。', 4::SMALLINT, 40)
)
INSERT INTO knowledge_point (
    id, module_id, parent_id, code, name, description,
    difficulty_level, sort_order, status, created_by
)
SELECT
    seed.id,
    module.id,
    NULL,
    seed.code,
    seed.name,
    seed.description,
    seed.difficulty_level,
    seed.sort_order,
    1,
    0
FROM seed
JOIN content_module module ON module.code = seed.module_code
ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3. 基础错因字典：14 个
-- -----------------------------------------------------------------------------
WITH seed (
    id, code, name, category, applicable_question_type,
    description, explanation_template, remediation_hint
) AS (
    VALUES
        (94000001::BIGINT, 'CALC_RESULT_ERROR', '计算结果错误', 'CALCULATION', NULL,
         '运算方法基本正确，但最终计算结果不正确。',
         '这道题的计算结果不正确，请重新检查每一步。',
         '从较小数值开始复算，并检查最后一次加减。'),
        (94000002::BIGINT, 'OPERATOR_CONFUSION', '加减符号混淆', 'CONCEPT', NULL,
         '没有正确识别加号或减号，使用了相反的运算。',
         '你把运算符号看错了，先确认题目要求做加法还是减法。',
         '练习先圈出运算符号，再开始计算。'),
        (94000003::BIGINT, 'COUNT_MISSING_REPEAT', '数数遗漏或重复', 'CALCULATION', NULL,
         '顺序数数时出现数字遗漏、跳数或重复计数。',
         '数数时有遗漏或重复，请按顺序一个一个地数。',
         '使用点数、画记号或数轴辅助数数。'),
        (94000004::BIGINT, 'COMPOSITION_ERROR', '数的分与合错误', 'CONCEPT', 'FILL_BLANK',
         '不能正确分解或组成给定的数。',
         '两部分合起来应该等于原来的数，请重新检查分与合。',
         '使用小棒或圆点练习数的分解与组成。'),
        (94000005::BIGINT, 'PLACE_VALUE_CONFUSION', '数位概念混淆', 'PLACE_VALUE', NULL,
         '混淆十位和个位，或不能正确理解几个十和几个一。',
         '请分清十位和个位，十位表示几个十，个位表示几个一。',
         '使用数位表拆分两位数，再进行读写和计算。'),
        (94000006::BIGINT, 'DIGIT_NOT_ALIGNED', '竖式数位未对齐', 'FORMAT', 'VERTICAL',
         '竖式中个位与个位、十位与十位没有对齐。',
         '竖式计算要把相同数位对齐，再从个位算起。',
         '在方格纸或数位表中练习竖式书写。'),
        (94000007::BIGINT, 'CARRY_OMITTED', '进位遗漏', 'CARRY', NULL,
         '个位相加满十后，没有向十位进1。',
         '个位相加满十，需要向十位进1。',
         '在个位满十的位置标出进位1，再计算十位。'),
        (94000008::BIGINT, 'CARRY_DUPLICATED', '进位重复计算', 'CARRY', NULL,
         '进位数被重复加入或加入了错误的数位。',
         '进位1只能在相邻高一位计算一次。',
         '写出进位标记，并在使用后划掉。'),
        (94000009::BIGINT, 'BORROW_OMITTED', '退位遗漏', 'BORROW', NULL,
         '个位不够减时，没有从十位退1。',
         '个位不够减，需要从十位退1，把个位增加10后再减。',
         '在被减数十位上标记退位，再完成个位计算。'),
        (94000010::BIGINT, 'BORROW_NOT_REDUCED', '退位后十位未减1', 'BORROW', NULL,
         '已经向个位退1，但十位仍按原数计算。',
         '从十位退1以后，十位上的数要相应减1。',
         '退位时同时修改十位标记，再计算十位。'),
        (94000011::BIGINT, 'OPERATION_SELECTION_ERROR', '实际问题运算选择错误', 'CONCEPT', NULL,
         '没有根据题意正确选择加法或减法。',
         '先判断题目是在求合起来、剩余、相差，还是多几少几。',
         '圈出已知条件和问题，画数量关系图后再列式。'),
        (94000012::BIGINT, 'MULTI_STEP_OMITTED', '连续计算步骤遗漏', 'CALCULATION', NULL,
         '连加、连减、混合运算或连续两问中遗漏了计算步骤。',
         '这道题需要连续计算，请检查是否完成了每一步。',
         '按顺序写出每一步中间结果，再继续计算。'),
        (94000013::BIGINT, 'INVERSE_RELATION_ERROR', '逆向数量关系错误', 'CONCEPT', NULL,
         '求未知加数或逆向数量时仍按正向关系直接相加。',
         '已知总数和其中一部分时，要用减法求另一部分。',
         '画出整体和部分关系图，先确定未知量的位置。'),
        (94000014::BIGINT, 'ANSWER_FORMAT_ERROR', '答案格式不规范', 'FORMAT', NULL,
         '计算结果可能正确，但漏写答案、单位或格式不符合要求。',
         '请按题目要求写完整答案，并检查单位和格式。',
         '完成计算后单独检查答案、单位和竖式格式。')
)
INSERT INTO error_type (
    id, code, name, category, applicable_question_type,
    description, explanation_template, remediation_hint, status
)
SELECT
    id, code, name, category, applicable_question_type,
    description, explanation_template, remediation_hint, 1
FROM seed
ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4. 用于联调学习闭环的固定题：27 道
--    当前不导入 TEMPLATE 题，待题目生成器和 generation_rule 契约实现后再补充。
-- -----------------------------------------------------------------------------
WITH seed (
    id, code, question_type, stem, answer_json, analysis,
    difficulty, estimated_seconds
) AS (
    VALUES
        (930101010001::BIGINT, 'Q-PEP-G01-S01-U01-001', 'ARITHMETIC', '2 + 3 = ?', '{"value":5}',
         '把2和3合起来，一共是5。', 1::SMALLINT, 10),
        (930101010002::BIGINT, 'Q-PEP-G01-S01-U01-002', 'ARITHMETIC', '5 - 2 = ?', '{"value":3}',
         '从5里面去掉2，还剩3。', 1::SMALLINT, 10),
        (930101020001::BIGINT, 'Q-PEP-G01-S01-U02-001', 'ARITHMETIC', '4 + 5 = ?', '{"value":9}',
         '4和5合起来是9。', 2::SMALLINT, 10),
        (930101020002::BIGINT, 'Q-PEP-G01-S01-U02-002', 'ARITHMETIC', '9 - 6 = ?', '{"value":3}',
         '9可以分成6和3，所以9减6等于3。', 2::SMALLINT, 10),
        (930101020003::BIGINT, 'Q-PEP-G01-S01-U02-003', 'ARITHMETIC', '2 + 3 + 4 = ?', '{"value":9}',
         '按从左到右计算，2加3等于5，5加4等于9。', 3::SMALLINT, 15),
        (930101020004::BIGINT, 'Q-PEP-G01-S01-U02-004', 'ARITHMETIC', '8 - 3 + 2 = ?', '{"value":7}',
         '按从左到右计算，8减3等于5，5加2等于7。', 3::SMALLINT, 15),
        (930101040001::BIGINT, 'Q-PEP-G01-S01-U04-001', 'ARITHMETIC', '10 + 7 = ?', '{"value":17}',
         '1个十和7个一组成17。', 2::SMALLINT, 10),
        (930101040002::BIGINT, 'Q-PEP-G01-S01-U04-002', 'ARITHMETIC', '12 + 5 = ?', '{"value":17}',
         '个位2加5等于7，十位不变，结果是17。', 2::SMALLINT, 12),
        (930101040003::BIGINT, 'Q-PEP-G01-S01-U04-003', 'ARITHMETIC', '18 - 6 = ?', '{"value":12}',
         '个位8减6等于2，十位不变，结果是12。', 2::SMALLINT, 12),
        (930101050001::BIGINT, 'Q-PEP-G01-S01-U05-001', 'ARITHMETIC', '9 + 6 = ?', '{"value":15}',
         '把6分成1和5，9加1凑成10，再加5等于15。', 3::SMALLINT, 15),
        (930101050002::BIGINT, 'Q-PEP-G01-S01-U05-002', 'ARITHMETIC', '8 + 7 = ?', '{"value":15}',
         '把7分成2和5，8加2凑成10，再加5等于15。', 3::SMALLINT, 15),
        (930102020001::BIGINT, 'Q-PEP-G01-S02-U02-001', 'ARITHMETIC', '15 - 9 = ?', '{"value":6}',
         '把15分成10和5，10减9等于1，再加5等于6。', 3::SMALLINT, 15),
        (930102020002::BIGINT, 'Q-PEP-G01-S02-U02-002', 'ARITHMETIC', '13 - 7 = ?', '{"value":6}',
         '因为7加6等于13，所以13减7等于6。', 3::SMALLINT, 15),
        (930102030001::BIGINT, 'Q-PEP-G01-S02-U03-001', 'ARITHMETIC', '40 + 30 = ?', '{"value":70}',
         '4个十加3个十等于7个十，也就是70。', 2::SMALLINT, 12),
        (930102030002::BIGINT, 'Q-PEP-G01-S02-U03-002', 'ARITHMETIC', '50 + 6 = ?', '{"value":56}',
         '5个十和6个一组成56。', 2::SMALLINT, 10),
        (930102040001::BIGINT, 'Q-PEP-G01-S02-U04-001', 'ARITHMETIC', '35 + 4 = ?', '{"value":39}',
         '个位5加4等于9，十位不变，结果是39。', 2::SMALLINT, 12),
        (930102040002::BIGINT, 'Q-PEP-G01-S02-U04-002', 'ARITHMETIC', '27 + 8 = ?', '{"value":35}',
         '个位7加8等于15，写5并向十位进1，结果是35。', 3::SMALLINT, 15),
        (930102040003::BIGINT, 'Q-PEP-G01-S02-U04-003', 'ARITHMETIC', '46 - 5 = ?', '{"value":41}',
         '个位6减5等于1，十位不变，结果是41。', 2::SMALLINT, 12),
        (930102040004::BIGINT, 'Q-PEP-G01-S02-U04-004', 'ARITHMETIC', '52 - 30 = ?', '{"value":22}',
         '十位5减3等于2，个位不变，结果是22。', 2::SMALLINT, 12),
        (930102040005::BIGINT, 'Q-PEP-G01-S02-U04-005', 'ARITHMETIC', '42 - 7 = ?', '{"value":35}',
         '个位2不够减7，从十位退1，12减7等于5，结果是35。', 3::SMALLINT, 15),
        (930102050001::BIGINT, 'Q-PEP-G01-S02-U05-001', 'VERTICAL', '列竖式计算：24 + 35 = ?', '{"value":59}',
         '相同数位对齐，从个位加起，4加5等于9，2个十加3个十等于5个十。', 2::SMALLINT, 25),
        (930102050002::BIGINT, 'Q-PEP-G01-S02-U05-002', 'VERTICAL', '列竖式计算：28 + 37 = ?', '{"value":65}',
         '个位8加7等于15，写5向十位进1；十位2加3再加1等于6。', 3::SMALLINT, 30),
        (930102050003::BIGINT, 'Q-PEP-G01-S02-U05-003', 'VERTICAL', '列竖式计算：76 - 42 = ?', '{"value":34}',
         '相同数位对齐，个位6减2等于4，十位7减4等于3。', 2::SMALLINT, 25),
        (930102050004::BIGINT, 'Q-PEP-G01-S02-U05-004', 'VERTICAL', '列竖式计算：52 - 28 = ?', '{"value":24}',
         '个位2不够减8，从十位退1，12减8等于4；十位4减2等于2。', 3::SMALLINT, 30),
        (930102060001::BIGINT, 'Q-PEP-G01-S02-U06-001', 'FILL_BLANK',
         '小明有15个苹果，小红有9个苹果，小明比小红多几个？', '{"value":6}',
         '求两个数量相差多少，用较大数减较小数，15减9等于6。', 3::SMALLINT, 25),
        (930102060002::BIGINT, 'Q-PEP-G01-S02-U06-002', 'FILL_BLANK',
         '小红有12朵花，小明比小红多5朵，小明有多少朵花？', '{"value":17}',
         '求比12多5的数，用加法计算，12加5等于17。', 3::SMALLINT, 25),
        (930102060003::BIGINT, 'Q-PEP-G01-S02-U06-003', 'FILL_BLANK',
         '书架上有18本故事书，科技书比故事书少6本，科技书有多少本？', '{"value":12}',
         '求比18少6的数，用减法计算，18减6等于12。', 3::SMALLINT, 25)
)
INSERT INTO question (
    id, code, question_mode, question_type, stem, options,
    standard_answer, analysis, generation_rule, difficulty,
    estimated_seconds, status, created_by, reviewed_by, reviewed_at
)
SELECT
    id, code, 'FIXED', question_type, stem, NULL,
    answer_json::JSONB, analysis, NULL, difficulty,
    estimated_seconds, 'ENABLED', 0, 0, CURRENT_TIMESTAMP
FROM seed
ON CONFLICT (code) DO NOTHING;

-- 每道测试题绑定且只绑定一个主要知识点。
WITH seed (question_code, knowledge_point_code) AS (
    VALUES
        ('Q-PEP-G01-S01-U01-001', 'KP-PEP-G01-S01-U01-07'),
        ('Q-PEP-G01-S01-U01-002', 'KP-PEP-G01-S01-U01-08'),
        ('Q-PEP-G01-S01-U02-001', 'KP-PEP-G01-S01-U02-03'),
        ('Q-PEP-G01-S01-U02-002', 'KP-PEP-G01-S01-U02-04'),
        ('Q-PEP-G01-S01-U02-003', 'KP-PEP-G01-S01-U02-08'),
        ('Q-PEP-G01-S01-U02-004', 'KP-PEP-G01-S01-U02-10'),
        ('Q-PEP-G01-S01-U04-001', 'KP-PEP-G01-S01-U04-05'),
        ('Q-PEP-G01-S01-U04-002', 'KP-PEP-G01-S01-U04-06'),
        ('Q-PEP-G01-S01-U04-003', 'KP-PEP-G01-S01-U04-07'),
        ('Q-PEP-G01-S01-U05-001', 'KP-PEP-G01-S01-U05-01'),
        ('Q-PEP-G01-S01-U05-002', 'KP-PEP-G01-S01-U05-02'),
        ('Q-PEP-G01-S02-U02-001', 'KP-PEP-G01-S02-U02-01'),
        ('Q-PEP-G01-S02-U02-002', 'KP-PEP-G01-S02-U02-02'),
        ('Q-PEP-G01-S02-U03-001', 'KP-PEP-G01-S02-U03-08'),
        ('Q-PEP-G01-S02-U03-002', 'KP-PEP-G01-S02-U03-09'),
        ('Q-PEP-G01-S02-U04-001', 'KP-PEP-G01-S02-U04-01'),
        ('Q-PEP-G01-S02-U04-002', 'KP-PEP-G01-S02-U04-03'),
        ('Q-PEP-G01-S02-U04-003', 'KP-PEP-G01-S02-U04-04'),
        ('Q-PEP-G01-S02-U04-004', 'KP-PEP-G01-S02-U04-05'),
        ('Q-PEP-G01-S02-U04-005', 'KP-PEP-G01-S02-U04-06'),
        ('Q-PEP-G01-S02-U05-001', 'KP-PEP-G01-S02-U05-02'),
        ('Q-PEP-G01-S02-U05-002', 'KP-PEP-G01-S02-U05-03'),
        ('Q-PEP-G01-S02-U05-003', 'KP-PEP-G01-S02-U05-05'),
        ('Q-PEP-G01-S02-U05-004', 'KP-PEP-G01-S02-U05-06'),
        ('Q-PEP-G01-S02-U06-001', 'KP-PEP-G01-S02-U06-01'),
        ('Q-PEP-G01-S02-U06-002', 'KP-PEP-G01-S02-U06-02'),
        ('Q-PEP-G01-S02-U06-003', 'KP-PEP-G01-S02-U06-03')
)
INSERT INTO question_knowledge_point (
    question_id, knowledge_point_id, is_primary, weight
)
SELECT question.id, knowledge_point.id, TRUE, 1.0000
FROM seed
JOIN question ON question.code = seed.question_code
JOIN knowledge_point ON knowledge_point.code = seed.knowledge_point_code
ON CONFLICT (question_id, knowledge_point_id) DO NOTHING;

COMMIT;

-- 导入结果核对；重复执行时总数不会增加。
SELECT COUNT(*) AS grade1_pep_module_count
FROM content_module
WHERE code IN (
    'PEP-G01-S01-U01', 'PEP-G01-S01-U02', 'PEP-G01-S01-U04', 'PEP-G01-S01-U05',
    'PEP-G01-S02-U02', 'PEP-G01-S02-U03', 'PEP-G01-S02-U04', 'PEP-G01-S02-U05',
    'PEP-G01-S02-U06'
);

SELECT COUNT(*) AS grade1_pep_knowledge_point_count
FROM knowledge_point
WHERE code LIKE 'KP-PEP-G01-%';

SELECT COUNT(*) AS grade1_pep_test_question_count
FROM question
WHERE code LIKE 'Q-PEP-G01-%';

SELECT COUNT(*) AS basic_error_type_count
FROM error_type
WHERE id BETWEEN 94000001 AND 94000014;
