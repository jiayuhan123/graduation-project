-- content_db 二年级人教版基础计算内容种子数据
-- 教材范围：人教版新教材，二年级上、下册，根据 2022 年版义务教育课程标准修订。
-- 目录参考：https://basic.smartedu.cn/syncClassroom
--
-- 使用说明：
--   psql -v ON_ERROR_STOP=1 -d content_db \
--     -f content-service/src/main/resources/db/seed-grade2-pep.sql
--
-- 约定：
-- 1. 本脚本仅导入基础计算范围，暂不导入分类、长度、时间实践、综合实践和复习单元。
-- 2. 9 开头的低位正整数为系统种子数据保留编号，避免与线上雪花 ID 冲突。
-- 3. created_by/reviewed_by = 0 表示系统初始化；测试题视为系统审核并直接启用。
-- 4. 所有写入均使用稳定编码判重，可安全重复执行。
-- 5. 建议先执行 seed-grade1-pep.sql，以同时获得通用基础错因；本脚本会补充二年级专项错因。

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. 内容模块：7 个
--    模块编码保留教材原单元号，因此跳过的非计算单元会在编码中留下空号。
-- -----------------------------------------------------------------------------
WITH seed (
    id, code, name, grade_level, semester, learning_theme,
    publisher, edition_description, description, sort_order
) AS (
    VALUES
        (91020102::BIGINT, 'PEP-G02-S01-U02', '1～6的表内乘法', 2::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '理解乘法意义，掌握1～6的乘法口诀、乘加乘减及简单乘法实际问题。', 20),
        (91020103::BIGINT, 'PEP-G02-S01-U03', '1～6的表内除法', 2::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '理解平均分和除法意义，掌握用1～6的乘法口诀求商。', 30),
        (91020105::BIGINT, 'PEP-G02-S01-U05', '7～9的表内乘、除法', 2::SMALLINT, 1::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '掌握7～9的乘法口诀及相应除法，形成完整的表内乘除法能力。', 50),
        (91020201::BIGINT, 'PEP-G02-S02-U01', '有余数的除法', 2::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '理解余数含义和余数小于除数的关系，掌握试商、竖式及相关实际问题。', 10),
        (91020202::BIGINT, 'PEP-G02-S02-U02', '数量间的乘除关系', 2::SMALLINT, 2::SMALLINT,
         '数量关系', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '理解倍的含义以及乘除数量关系，解决求倍数、求几倍数和逆向问题。', 20),
        (91020203::BIGINT, 'PEP-G02-S02-U03', '万以内数的认识', 2::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '认识万以内数的组成、数位、读写、比较、近似数及简单加减法。', 30),
        (91020204::BIGINT, 'PEP-G02-S02-U04', '万以内的加法和减法', 2::SMALLINT, 2::SMALLINT,
         '数与运算', '人民教育出版社', '人教版新教材，根据2022年版课程标准修订',
         '掌握万以内加减法竖式、连续进位、隔位退位、各部分关系和估算。', 40)
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
-- 2. 原子知识点：61 个（上册 27 个，下册 34 个）
-- -----------------------------------------------------------------------------
WITH seed (
    id, module_code, code, name, description, difficulty_level, sort_order
) AS (
    VALUES
        -- 上册 U02：1～6的表内乘法（11）
        (9202010201::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-01', '乘法的意义',
         '理解求几个相同加数的和可以用乘法表示。', 1::SMALLINT, 10),
        (9202010202::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-02', '乘法算式的读写和各部分名称',
         '能够正确读写乘法算式，并认识乘数、乘号和积。', 1::SMALLINT, 20),
        (9202010203::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-03', '2的乘法口诀',
         '能够熟练运用2的乘法口诀计算相应乘法。', 1::SMALLINT, 30),
        (9202010204::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-04', '3的乘法口诀',
         '能够熟练运用3的乘法口诀计算相应乘法。', 1::SMALLINT, 40),
        (9202010205::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-05', '4的乘法口诀',
         '能够熟练运用4的乘法口诀计算相应乘法。', 1::SMALLINT, 50),
        (9202010206::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-06', '5的乘法口诀',
         '能够熟练运用5的乘法口诀计算相应乘法。', 1::SMALLINT, 60),
        (9202010207::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-07', '6的乘法口诀',
         '能够熟练运用6的乘法口诀计算相应乘法。', 2::SMALLINT, 70),
        (9202010208::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-08', '乘加计算',
         '能够先算乘法再算加法，正确完成乘加算式。', 2::SMALLINT, 80),
        (9202010209::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-09', '乘减计算',
         '能够先算乘法再算减法，正确完成乘减算式。', 2::SMALLINT, 90),
        (9202010210::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-10', '选择加法或乘法解决问题',
         '能够根据是否存在相同加数，选择加法或乘法解决问题。', 3::SMALLINT, 100),
        (9202010211::BIGINT, 'PEP-G02-S01-U02', 'KP-PEP-G02-S01-U02-11', '用乘法解决简单实际问题',
         '能够从购物等实际情境中提取每份数和份数并列乘法算式。', 3::SMALLINT, 110),

        -- 上册 U03：1～6的表内除法（8）
        (9202010301::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-01', '平均分中的包含分',
         '理解按每几个一份进行分组的包含分，并求份数。', 2::SMALLINT, 10),
        (9202010302::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-02', '平均分中的等分',
         '理解把总数平均分成若干份的等分，并求每份数。', 2::SMALLINT, 20),
        (9202010303::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-03', '除法的意义',
         '理解平均分可以用除法表示。', 2::SMALLINT, 30),
        (9202010304::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-04', '除法算式的读写和各部分名称',
         '能够正确读写除法算式，并认识被除数、除数、除号和商。', 2::SMALLINT, 40),
        (9202010305::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-05', '乘法和除法的关系',
         '能够根据一个乘法算式写出相应除法算式。', 2::SMALLINT, 50),
        (9202010306::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-06', '用1～6的乘法口诀求商',
         '能够熟练运用1～6的乘法口诀计算表内除法。', 2::SMALLINT, 60),
        (9202010307::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-07', '用除法解决包含分问题',
         '能够用除法解决总数中包含几个每份数的问题。', 3::SMALLINT, 70),
        (9202010308::BIGINT, 'PEP-G02-S01-U03', 'KP-PEP-G02-S01-U03-08', '用除法解决等分问题',
         '能够用除法解决把总数平均分成若干份并求每份数的问题。', 3::SMALLINT, 80),

        -- 上册 U05：7～9的表内乘、除法（8）
        (9202010501::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-01', '7的乘法口诀',
         '能够熟练运用7的乘法口诀计算相应乘法。', 2::SMALLINT, 10),
        (9202010502::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-02', '8的乘法口诀',
         '能够熟练运用8的乘法口诀计算相应乘法。', 2::SMALLINT, 20),
        (9202010503::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-03', '9的乘法口诀',
         '能够熟练运用9的乘法口诀计算相应乘法。', 2::SMALLINT, 30),
        (9202010504::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-04', '用7的乘法口诀求商',
         '能够运用7的乘法口诀计算相应除法。', 2::SMALLINT, 40),
        (9202010505::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-05', '用8的乘法口诀求商',
         '能够运用8的乘法口诀计算相应除法。', 2::SMALLINT, 50),
        (9202010506::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-06', '用9的乘法口诀求商',
         '能够运用9的乘法口诀计算相应除法。', 2::SMALLINT, 60),
        (9202010507::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-07', '选择乘法或除法解决问题',
         '能够根据每份数、份数和总数之间的关系选择乘法或除法。', 3::SMALLINT, 70),
        (9202010508::BIGINT, 'PEP-G02-S01-U05', 'KP-PEP-G02-S01-U05-08', '乘除法连续两问问题',
         '能够分析包含乘除数量关系的连续两问问题并分步计算。', 4::SMALLINT, 80),

        -- 下册 U01：有余数的除法（8）
        (9202020101::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-01', '有余数除法和余数的意义',
         '理解平均分后有剩余时可以用有余数的除法表示。', 2::SMALLINT, 10),
        (9202020102::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-02', '余数小于除数',
         '理解并能判断有余数除法中余数必须小于除数。', 2::SMALLINT, 20),
        (9202020103::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-03', '有余数除法竖式和各部分名称',
         '能够正确书写有余数除法竖式并认识商和余数。', 3::SMALLINT, 30),
        (9202020104::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-04', '有余数除法的试商',
         '能够根据乘法口诀选择合适的商，使积不超过被除数且余数小于除数。', 3::SMALLINT, 40),
        (9202020105::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-05', '有余数除法的验算',
         '能够使用除数乘商加余数等于被除数进行验算。', 3::SMALLINT, 50),
        (9202020106::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-06', '用进一法解决有余数除法问题',
         '能够在剩余对象仍需安排时根据实际意义把商加1。', 4::SMALLINT, 60),
        (9202020107::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-07', '用去尾法解决有余数除法问题',
         '能够在只计算完整份数时根据实际意义舍去余数。', 4::SMALLINT, 70),
        (9202020108::BIGINT, 'PEP-G02-S02-U01', 'KP-PEP-G02-S02-U01-08', '用有余数除法解决周期问题',
         '能够根据余数确定周期排列中指定位置的对象。', 4::SMALLINT, 80),

        -- 下册 U02：数量间的乘除关系（7）
        (9202020201::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-01', '倍的认识和乘除数量关系',
         '理解一个数量中包含几个另一个数量就是它的几倍。', 2::SMALLINT, 10),
        (9202020202::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-02', '求一个数是另一个数的几倍',
         '能够用除法求一个数是另一个数的几倍。', 3::SMALLINT, 20),
        (9202020203::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-03', '求一个数的几倍是多少',
         '能够用乘法求一个数的几倍是多少。', 3::SMALLINT, 30),
        (9202020204::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-04', '已知一个数的几倍求这个数',
         '能够用除法根据倍数和比较量逆向求标准量。', 4::SMALLINT, 40),
        (9202020205::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-05', '乘除数量关系连续两问问题',
         '能够分析包含倍数关系的连续两问问题并分步计算。', 4::SMALLINT, 50),
        (9202020206::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-06', '根据条件提出乘除法问题',
         '能够根据给定条件提出可以用乘法或除法解决的问题。', 3::SMALLINT, 60),
        (9202020207::BIGINT, 'PEP-G02-S02-U02', 'KP-PEP-G02-S02-U02-07', '根据问题补充乘除法条件',
         '能够根据问题和数量关系补充必要的已知条件。', 3::SMALLINT, 70),

        -- 下册 U03：万以内数的认识（10）
        (9202020301::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-01', '认识1000以内数的组成',
         '能够用几个百、几个十和几个一说明1000以内数的组成。', 1::SMALLINT, 10),
        (9202020302::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-02', '认识1000',
         '理解10个一百是一千，并认识计数单位千。', 1::SMALLINT, 20),
        (9202020303::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-03', '认识10000以内数的组成',
         '能够用几个千、几个百、几个十和几个一说明万以内数的组成。', 2::SMALLINT, 30),
        (9202020304::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-04', '认识算盘',
         '认识算盘结构，理解上珠和下珠表示的数并能表示简单数。', 2::SMALLINT, 40),
        (9202020305::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-05', '认识计数单位万和数位顺序表',
         '理解10个一千是一万，掌握万以内数位顺序和相邻进率。', 2::SMALLINT, 50),
        (9202020306::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-06', '万以内数的读法',
         '能够正确读出万以内各数，处理中间和末尾含0的情况。', 2::SMALLINT, 60),
        (9202020307::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-07', '万以内数的写法',
         '能够根据数的读法和数位顺序正确写出万以内各数。', 2::SMALLINT, 70),
        (9202020308::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-08', '万以内数的大小比较',
         '能够先比较数位多少，再从最高位起比较万以内数的大小。', 2::SMALLINT, 80),
        (9202020309::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-09', '万以内数的近似数',
         '能够根据实际情境用接近的整百、整千数表示近似数。', 3::SMALLINT, 90),
        (9202020310::BIGINT, 'PEP-G02-S02-U03', 'KP-PEP-G02-S02-U03-10', '万以内整百整千数的简单加减法',
         '能够借助数的组成口算整百、整千数的简单加减法。', 2::SMALLINT, 100),

        -- 下册 U04：万以内的加法和减法（9）
        (9202020401::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-01', '万以内加法竖式和不进位加法',
         '能够相同数位对齐，从个位起计算万以内不进位加法。', 2::SMALLINT, 10),
        (9202020402::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-02', '万以内一次进位加法',
         '能够用竖式正确计算万以内只有一次进位的加法。', 3::SMALLINT, 20),
        (9202020403::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-03', '万以内连续进位加法',
         '能够用竖式正确计算万以内连续进位加法。', 4::SMALLINT, 30),
        (9202020404::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-04', '万以内减法竖式和不退位减法',
         '能够相同数位对齐，从个位起计算万以内不退位减法。', 2::SMALLINT, 40),
        (9202020405::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-05', '万以内退位减法',
         '能够用竖式正确计算万以内退位减法。', 3::SMALLINT, 50),
        (9202020406::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-06', '万以内隔位退位减法',
         '能够处理被减数中间有0的隔位退位减法。', 4::SMALLINT, 60),
        (9202020407::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-07', '加法各部分间的关系',
         '理解加数加加数等于和，并能根据和与一个加数求另一个加数。', 3::SMALLINT, 70),
        (9202020408::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-08', '减法各部分间的关系',
         '理解被减数、减数和差的关系，并能根据其中两个量求第三个量。', 3::SMALLINT, 80),
        (9202020409::BIGINT, 'PEP-G02-S02-U04', 'KP-PEP-G02-S02-U04-09', '用估算解决实际问题',
         '能够选择合适的近似数估算万以内加减法并解决实际问题。', 4::SMALLINT, 90)
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
-- 3. 二年级专项错因字典：10 个
--    一年级脚本中的 14 个通用错因继续复用，本节只补充乘除法和万以内计算专项错因。
-- -----------------------------------------------------------------------------
WITH seed (
    id, code, name, category, applicable_question_type,
    description, explanation_template, remediation_hint
) AS (
    VALUES
        (94000015::BIGINT, 'MULTIPLICATION_MEANING_ERROR', '乘法意义理解错误', 'CONCEPT', NULL,
         '没有识别相同加数，或混淆每份数和份数。',
         '乘法表示几个相同加数的和，请先找出每份数和份数。',
         '用圈一圈、摆一摆的方法表示几个几，再列乘法算式。'),
        (94000016::BIGINT, 'MULTIPLICATION_FACT_ERROR', '乘法口诀使用错误', 'CALCULATION', 'ARITHMETIC',
         '乘法意义和算式识别正确，但口诀记忆或调用错误。',
         '这道题使用的乘法口诀不正确，请根据两个乘数重新想口诀。',
         '按口诀组进行专项练习，并用交换乘数进行自检。'),
        (94000017::BIGINT, 'DIVISION_MEANING_ERROR', '除法意义理解错误', 'CONCEPT', NULL,
         '混淆包含分与等分，或不能确定所求是份数还是每份数。',
         '先判断题目是在求可以分成几份，还是求每份有几个。',
         '画出总数、份数和每份数的关系图后再列式。'),
        (94000018::BIGINT, 'QUOTIENT_FACT_ERROR', '用口诀求商错误', 'CALCULATION', 'ARITHMETIC',
         '除法算式识别正确，但没有找到对应乘法口诀或商计算错误。',
         '想一想除数乘几等于被除数，再确定商。',
         '把除法算式改写成缺少一个乘数的乘法算式练习。'),
        (94000019::BIGINT, 'MULTIPLICATION_DIVISION_RELATION_ERROR', '乘除关系混淆', 'CONCEPT', NULL,
         '不能根据乘法算式写出相应除法算式，或混淆三个数量的位置。',
         '积除以一个乘数等于另一个乘数，请检查三个数的关系。',
         '使用乘除法算式组进行对照练习。'),
        (94000020::BIGINT, 'REMAINDER_RANGE_ERROR', '余数不小于除数', 'CONCEPT', NULL,
         '计算所得余数大于或等于除数，不符合有余数除法规则。',
         '余数必须比除数小，否则商还可以再增加。',
         '计算后固定检查余数是否小于除数。'),
        (94000021::BIGINT, 'TRIAL_QUOTIENT_ERROR', '试商错误', 'CALCULATION', NULL,
         '选择的商使除数与商的积超过被除数，或留下了过大的余数。',
         '试商时要找最接近且不超过被除数的乘法口诀结果。',
         '按除数整理口诀表，再练习最大不超过被除数的积。'),
        (94000022::BIGINT, 'QUOTIENT_REMAINDER_REVERSED', '商和余数位置混淆', 'FORMAT', NULL,
         '答案中商和余数写反，或没有按规范表示余数。',
         '除法结果先写商，剩下不能再分的数写作余数。',
         '结合除法竖式指认商和余数的位置。'),
        (94000023::BIGINT, 'CROSS_ZERO_BORROW_ERROR', '隔位退位错误', 'BORROW', 'VERTICAL',
         '被减数中间有0时，跨过0退位的处理不正确。',
         '中间是0时要从更高位退1，再逐位完成退位。',
         '在每个发生变化的数位上写出退位后的数字。'),
        (94000024::BIGINT, 'ESTIMATION_ERROR', '估算方法错误', 'CALCULATION', NULL,
         '选择的近似数不合理，或精确计算与估算混淆。',
         '估算要先根据问题选择接近且便于计算的整百或整千数。',
         '分别练习向相邻整百、整千数估计并比较误差。')
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
-- 4. 用于联调学习闭环的固定题：38 道
--    当前不导入 TEMPLATE 题，待题目生成器和 generation_rule 契约实现后再补充。
-- -----------------------------------------------------------------------------
WITH seed (
    id, code, question_type, stem, answer_json, analysis,
    difficulty, estimated_seconds
) AS (
    VALUES
        -- 上册 U02：表内乘法（6）
        (930201020001::BIGINT, 'Q-PEP-G02-S01-U02-001', 'ARITHMETIC', '3 × 4 = ?', '{"value":12}',
         '使用三四十二，结果是12。', 1::SMALLINT, 10),
        (930201020002::BIGINT, 'Q-PEP-G02-S01-U02-002', 'ARITHMETIC', '5 × 6 = ?', '{"value":30}',
         '使用五六三十，结果是30。', 1::SMALLINT, 10),
        (930201020003::BIGINT, 'Q-PEP-G02-S01-U02-003', 'ARITHMETIC', '6 × 6 = ?', '{"value":36}',
         '使用六六三十六，结果是36。', 2::SMALLINT, 10),
        (930201020004::BIGINT, 'Q-PEP-G02-S01-U02-004', 'ARITHMETIC', '4 × 3 + 2 = ?', '{"value":14}',
         '先算4乘3等于12，再加2等于14。', 2::SMALLINT, 15),
        (930201020005::BIGINT, 'Q-PEP-G02-S01-U02-005', 'ARITHMETIC', '5 × 4 - 3 = ?', '{"value":17}',
         '先算5乘4等于20，再减3等于17。', 2::SMALLINT, 15),
        (930201020006::BIGINT, 'Q-PEP-G02-S01-U02-006', 'FILL_BLANK',
         '有6盒铅笔，每盒4支，一共有多少支铅笔？', '{"value":24}',
         '每盒4支，有6盒，就是6个4，用乘法计算，6乘4等于24。', 3::SMALLINT, 20),

        -- 上册 U03：表内除法（5）
        (930201030001::BIGINT, 'Q-PEP-G02-S01-U03-001', 'ARITHMETIC', '12 ÷ 3 = ?', '{"value":4}',
         '因为3乘4等于12，所以12除以3等于4。', 2::SMALLINT, 10),
        (930201030002::BIGINT, 'Q-PEP-G02-S01-U03-002', 'ARITHMETIC', '24 ÷ 6 = ?', '{"value":4}',
         '因为6乘4等于24，所以24除以6等于4。', 2::SMALLINT, 10),
        (930201030003::BIGINT, 'Q-PEP-G02-S01-U03-003', 'FILL_BLANK',
         '有18颗糖，每3颗装一袋，可以装几袋？', '{"value":6}',
         '求18里面有几个3，用除法计算，18除以3等于6。', 3::SMALLINT, 20),
        (930201030004::BIGINT, 'Q-PEP-G02-S01-U03-004', 'FILL_BLANK',
         '把20颗糖平均分给5名同学，每名同学分到几颗？', '{"value":4}',
         '把20平均分成5份，用除法计算，20除以5等于4。', 3::SMALLINT, 20),
        (930201030005::BIGINT, 'Q-PEP-G02-S01-U03-005', 'FILL_BLANK',
         '根据4 × 6 = 24，计算24 ÷ 6 = ?。', '{"value":4}',
         '积除以一个乘数等于另一个乘数，所以结果是4。', 2::SMALLINT, 12),

        -- 上册 U05：7～9的表内乘、除法（5）
        (930201050001::BIGINT, 'Q-PEP-G02-S01-U05-001', 'ARITHMETIC', '7 × 8 = ?', '{"value":56}',
         '使用七八五十六，结果是56。', 2::SMALLINT, 10),
        (930201050002::BIGINT, 'Q-PEP-G02-S01-U05-002', 'ARITHMETIC', '9 × 6 = ?', '{"value":54}',
         '使用六九五十四，结果是54。', 2::SMALLINT, 10),
        (930201050003::BIGINT, 'Q-PEP-G02-S01-U05-003', 'ARITHMETIC', '56 ÷ 7 = ?', '{"value":8}',
         '因为7乘8等于56，所以56除以7等于8。', 2::SMALLINT, 10),
        (930201050004::BIGINT, 'Q-PEP-G02-S01-U05-004', 'ARITHMETIC', '72 ÷ 9 = ?', '{"value":8}',
         '因为9乘8等于72，所以72除以9等于8。', 2::SMALLINT, 10),
        (930201050005::BIGINT, 'Q-PEP-G02-S01-U05-005', 'FILL_BLANK',
         '商店有8盒水彩笔，每盒7支，卖出20支后还剩多少支？', '{"value":36}',
         '先算8乘7等于56，再算56减20等于36。', 4::SMALLINT, 30),

        -- 下册 U01：有余数的除法（6）
        (930202010001::BIGINT, 'Q-PEP-G02-S02-U01-001', 'ARITHMETIC', '17 ÷ 5 = ?……?',
         '{"quotient":3,"remainder":2}', '5乘3等于15，17减15等于2，所以商3余2。', 2::SMALLINT, 15),
        (930202010002::BIGINT, 'Q-PEP-G02-S02-U01-002', 'VERTICAL', '列竖式计算：29 ÷ 4 = ?……?',
         '{"quotient":7,"remainder":1}', '4乘7等于28，29减28等于1，所以商7余1。', 3::SMALLINT, 25),
        (930202010003::BIGINT, 'Q-PEP-G02-S02-U01-003', 'FILL_BLANK',
         '在有余数的除法中，除数是6，余数最大是几？', '{"value":5}',
         '余数必须小于除数6，所以最大余数是5。', 2::SMALLINT, 15),
        (930202010004::BIGINT, 'Q-PEP-G02-S02-U01-004', 'FILL_BLANK',
         '22名同学坐船，每条船最多坐4人，至少需要几条船？', '{"value":6}',
         '22除以4等于5余2，剩余2人还需要1条船，所以需要6条船。', 4::SMALLINT, 30),
        (930202010005::BIGINT, 'Q-PEP-G02-S02-U01-005', 'FILL_BLANK',
         '有22朵花，每4朵扎成一束，最多可以扎成几束完整的花束？', '{"value":5}',
         '22除以4等于5余2，只计算完整花束，所以最多扎5束。', 4::SMALLINT, 30),
        (930202010006::BIGINT, 'Q-PEP-G02-S02-U01-006', 'FILL_BLANK',
         '彩旗按红、黄、蓝的顺序循环排列，第20面彩旗是什么颜色？', '{"text":"黄色"}',
         '20除以3等于6余2，余数2对应每组的第2面，所以是黄色。', 4::SMALLINT, 30),

        -- 下册 U02：数量间的乘除关系（4）
        (930202020001::BIGINT, 'Q-PEP-G02-S02-U02-001', 'FILL_BLANK',
         '18是3的几倍？', '{"value":6}',
         '求18是3的几倍，用除法计算，18除以3等于6。', 3::SMALLINT, 20),
        (930202020002::BIGINT, 'Q-PEP-G02-S02-U02-002', 'FILL_BLANK',
         '7的5倍是多少？', '{"value":35}',
         '求7的5倍，用乘法计算，7乘5等于35。', 3::SMALLINT, 20),
        (930202020003::BIGINT, 'Q-PEP-G02-S02-U02-003', 'FILL_BLANK',
         '一个数的4倍是32，这个数是多少？', '{"value":8}',
         '已知一个数的4倍是32，用除法逆向计算，32除以4等于8。', 4::SMALLINT, 25),
        (930202020004::BIGINT, 'Q-PEP-G02-S02-U02-004', 'FILL_BLANK',
         '每袋有6个苹果，4袋共有多少个？再放入8个后共有多少个？', '{"value":32}',
         '先算6乘4等于24，再算24加8等于32。', 4::SMALLINT, 30),

        -- 下册 U03：万以内数的认识（5）
        (930202030001::BIGINT, 'Q-PEP-G02-S02-U03-001', 'FILL_BLANK',
         '由3个千、5个十和6个一组成的数是多少？', '{"value":3056}',
         '千位写3，百位没有计数单位写0，十位写5，个位写6，得到3056。', 2::SMALLINT, 20),
        (930202030002::BIGINT, 'Q-PEP-G02-S02-U03-002', 'JUDGEMENT',
         '判断：3980 > 4020。', '{"value":false}',
         '3980是三千多，4020是四千多，所以3980小于4020。', 2::SMALLINT, 15),
        (930202030003::BIGINT, 'Q-PEP-G02-S02-U03-003', 'ARITHMETIC', '3000 + 500 = ?', '{"value":3500}',
         '3个千加5个百组成3500。', 2::SMALLINT, 12),
        (930202030004::BIGINT, 'Q-PEP-G02-S02-U03-004', 'ARITHMETIC', '7600 - 600 = ?', '{"value":7000}',
         '76个百减6个百等于70个百，也就是7000。', 2::SMALLINT, 12),
        (930202030005::BIGINT, 'Q-PEP-G02-S02-U03-005', 'FILL_BLANK',
         '398大约是几百？', '{"value":400}',
         '398最接近400，所以可以用400作为近似数。', 3::SMALLINT, 15),

        -- 下册 U04：万以内的加法和减法（7）
        (930202040001::BIGINT, 'Q-PEP-G02-S02-U04-001', 'VERTICAL', '列竖式计算：346 + 278 = ?', '{"value":624}',
         '个位6加8满十进1，十位4加7再加1满十进1，结果是624。', 3::SMALLINT, 35),
        (930202040002::BIGINT, 'Q-PEP-G02-S02-U04-002', 'VERTICAL', '列竖式计算：586 + 475 = ?', '{"value":1061}',
         '个位和十位连续进位，依次计算得到1061。', 4::SMALLINT, 40),
        (930202040003::BIGINT, 'Q-PEP-G02-S02-U04-003', 'VERTICAL', '列竖式计算：764 - 328 = ?', '{"value":436}',
         '个位4不够减8向十位退1，再依次计算，结果是436。', 3::SMALLINT, 35),
        (930202040004::BIGINT, 'Q-PEP-G02-S02-U04-004', 'VERTICAL', '列竖式计算：1000 - 286 = ?', '{"value":714}',
         '从千位跨过百位和十位连续退位，结果是714。', 4::SMALLINT, 45),
        (930202040005::BIGINT, 'Q-PEP-G02-S02-U04-005', 'FILL_BLANK', '428 + ? = 900', '{"value":472}',
         '未知加数等于和减去已知加数，900减428等于472。', 3::SMALLINT, 25),
        (930202040006::BIGINT, 'Q-PEP-G02-S02-U04-006', 'FILL_BLANK', '? - 356 = 420', '{"value":776}',
         '被减数等于差加减数，420加356等于776。', 3::SMALLINT, 25),
        (930202040007::BIGINT, 'Q-PEP-G02-S02-U04-007', 'FILL_BLANK',
         '估算298 + 403大约等于多少？', '{"value":700}',
         '298接近300，403接近400，300加400等于700。', 4::SMALLINT, 25)
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
        ('Q-PEP-G02-S01-U02-001', 'KP-PEP-G02-S01-U02-04'),
        ('Q-PEP-G02-S01-U02-002', 'KP-PEP-G02-S01-U02-06'),
        ('Q-PEP-G02-S01-U02-003', 'KP-PEP-G02-S01-U02-07'),
        ('Q-PEP-G02-S01-U02-004', 'KP-PEP-G02-S01-U02-08'),
        ('Q-PEP-G02-S01-U02-005', 'KP-PEP-G02-S01-U02-09'),
        ('Q-PEP-G02-S01-U02-006', 'KP-PEP-G02-S01-U02-11'),
        ('Q-PEP-G02-S01-U03-001', 'KP-PEP-G02-S01-U03-06'),
        ('Q-PEP-G02-S01-U03-002', 'KP-PEP-G02-S01-U03-06'),
        ('Q-PEP-G02-S01-U03-003', 'KP-PEP-G02-S01-U03-07'),
        ('Q-PEP-G02-S01-U03-004', 'KP-PEP-G02-S01-U03-08'),
        ('Q-PEP-G02-S01-U03-005', 'KP-PEP-G02-S01-U03-05'),
        ('Q-PEP-G02-S01-U05-001', 'KP-PEP-G02-S01-U05-01'),
        ('Q-PEP-G02-S01-U05-002', 'KP-PEP-G02-S01-U05-03'),
        ('Q-PEP-G02-S01-U05-003', 'KP-PEP-G02-S01-U05-04'),
        ('Q-PEP-G02-S01-U05-004', 'KP-PEP-G02-S01-U05-06'),
        ('Q-PEP-G02-S01-U05-005', 'KP-PEP-G02-S01-U05-08'),
        ('Q-PEP-G02-S02-U01-001', 'KP-PEP-G02-S02-U01-01'),
        ('Q-PEP-G02-S02-U01-002', 'KP-PEP-G02-S02-U01-04'),
        ('Q-PEP-G02-S02-U01-003', 'KP-PEP-G02-S02-U01-02'),
        ('Q-PEP-G02-S02-U01-004', 'KP-PEP-G02-S02-U01-06'),
        ('Q-PEP-G02-S02-U01-005', 'KP-PEP-G02-S02-U01-07'),
        ('Q-PEP-G02-S02-U01-006', 'KP-PEP-G02-S02-U01-08'),
        ('Q-PEP-G02-S02-U02-001', 'KP-PEP-G02-S02-U02-02'),
        ('Q-PEP-G02-S02-U02-002', 'KP-PEP-G02-S02-U02-03'),
        ('Q-PEP-G02-S02-U02-003', 'KP-PEP-G02-S02-U02-04'),
        ('Q-PEP-G02-S02-U02-004', 'KP-PEP-G02-S02-U02-05'),
        ('Q-PEP-G02-S02-U03-001', 'KP-PEP-G02-S02-U03-07'),
        ('Q-PEP-G02-S02-U03-002', 'KP-PEP-G02-S02-U03-08'),
        ('Q-PEP-G02-S02-U03-003', 'KP-PEP-G02-S02-U03-10'),
        ('Q-PEP-G02-S02-U03-004', 'KP-PEP-G02-S02-U03-10'),
        ('Q-PEP-G02-S02-U03-005', 'KP-PEP-G02-S02-U03-09'),
        ('Q-PEP-G02-S02-U04-001', 'KP-PEP-G02-S02-U04-02'),
        ('Q-PEP-G02-S02-U04-002', 'KP-PEP-G02-S02-U04-03'),
        ('Q-PEP-G02-S02-U04-003', 'KP-PEP-G02-S02-U04-05'),
        ('Q-PEP-G02-S02-U04-004', 'KP-PEP-G02-S02-U04-06'),
        ('Q-PEP-G02-S02-U04-005', 'KP-PEP-G02-S02-U04-07'),
        ('Q-PEP-G02-S02-U04-006', 'KP-PEP-G02-S02-U04-08'),
        ('Q-PEP-G02-S02-U04-007', 'KP-PEP-G02-S02-U04-09')
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
SELECT COUNT(*) AS grade2_pep_module_count
FROM content_module
WHERE code IN (
    'PEP-G02-S01-U02', 'PEP-G02-S01-U03', 'PEP-G02-S01-U05',
    'PEP-G02-S02-U01', 'PEP-G02-S02-U02', 'PEP-G02-S02-U03', 'PEP-G02-S02-U04'
);

SELECT COUNT(*) AS grade2_pep_knowledge_point_count
FROM knowledge_point
WHERE code LIKE 'KP-PEP-G02-%';

SELECT COUNT(*) AS grade2_pep_test_question_count
FROM question
WHERE code LIKE 'Q-PEP-G02-%';

SELECT COUNT(*) AS grade2_specific_error_type_count
FROM error_type
WHERE id BETWEEN 94000015 AND 94000024;
