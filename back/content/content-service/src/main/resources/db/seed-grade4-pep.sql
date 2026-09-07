-- content_db 四年级人教版基础计算内容种子数据
-- 教材范围：现行人教版四年级上、下册；排除图形、测量、位置、统计、综合实践和复习。
-- 目录参考：https://basic.smartedu.cn/syncClassroom
-- 使用：psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade4-pep.sql
-- 建议先按年级顺序执行低年级种子脚本。

BEGIN;

-- 1. 计算内容模块：6 个。
WITH seed (id, code, name, grade_level, semester, learning_theme, description, sort_order) AS (
    VALUES
        (91040104::BIGINT, 'PEP-G04-S01-U04', '三位数乘两位数', 4::SMALLINT, 1::SMALLINT, '数与运算', '掌握三位数乘两位数笔算、积的变化规律及常见数量关系。', 40),
        (91040106::BIGINT, 'PEP-G04-S01-U06', '除数是两位数的除法', 4::SMALLINT, 1::SMALLINT, '数与运算', '掌握除数是两位数的口算、试商、笔算和商的变化规律。', 60),
        (91040201::BIGINT, 'PEP-G04-S02-U01', '四则运算', 4::SMALLINT, 2::SMALLINT, '数与运算', '掌握四则运算各部分关系、运算顺序和括号。', 10),
        (91040203::BIGINT, 'PEP-G04-S02-U03', '运算律与简便计算', 4::SMALLINT, 2::SMALLINT, '数与运算', '应用加法和乘法运算律进行简便计算。', 30),
        (91040204::BIGINT, 'PEP-G04-S02-U04', '小数的性质与计算准备', 4::SMALLINT, 2::SMALLINT, '数与运算', '掌握小数性质、大小比较、小数点移动、单位换算和近似数。', 40),
        (91040206::BIGINT, 'PEP-G04-S02-U06', '小数的加法和减法', 4::SMALLINT, 2::SMALLINT, '数与运算', '掌握小数加减法、混合运算及简便计算。', 60)
)
INSERT INTO content_module (
    id, code, name, grade_level, semester, learning_theme, publisher,
    edition_description, description, sort_order, status, created_by
)
SELECT id, code, name, grade_level, semester, learning_theme, '人民教育出版社',
       '现行人教版义务教育教科书', description, sort_order, 1, 0
FROM seed
ON CONFLICT (code) DO NOTHING;

-- 2. 原子知识点：44 个。
WITH seed (id, module_code, code, name, description, difficulty_level, sort_order) AS (
    VALUES
        -- 上册 U04：三位数乘两位数（9）
        (9204010401::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-01', '三位数乘两位数一般笔算', '用两位数每一位分别乘三位数并把部分积相加。', 3::SMALLINT, 10),
        (9204010402::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-02', '因数中间有0的乘法', '正确保留数位并处理因数中间的0。', 3::SMALLINT, 20),
        (9204010403::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-03', '因数末尾有0的乘法', '先乘非0部分，再在积末尾添相应个数的0。', 3::SMALLINT, 30),
        (9204010404::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-04', '积的变化规律', '一个因数不变，另一个因数扩大或缩小，积相应变化。', 3::SMALLINT, 40),
        (9204010405::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-05', '乘法估算', '将因数看作接近的整十、整百数估算乘积。', 3::SMALLINT, 50),
        (9204010406::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-06', '已知单价和数量求总价', '运用单价乘数量等于总价。', 2::SMALLINT, 60),
        (9204010407::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-07', '已知总价求单价或数量', '运用总价除以数量求单价，或总价除以单价求数量。', 3::SMALLINT, 70),
        (9204010408::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-08', '已知速度和时间求路程', '运用速度乘时间等于路程。', 2::SMALLINT, 80),
        (9204010409::BIGINT, 'PEP-G04-S01-U04', 'KP-PEP-G04-S01-U04-09', '已知路程求速度或时间', '运用路程除以时间求速度，或路程除以速度求时间。', 3::SMALLINT, 90),

        -- 上册 U06：除数是两位数的除法（7）
        (9204010601::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-01', '整十数除法口算和估算', '利用商不变规律口算并用接近数估算。', 2::SMALLINT, 10),
        (9204010602::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-02', '商是一位数的笔算除法', '判断前两位或前三位够除并确定一位商。', 3::SMALLINT, 20),
        (9204010603::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-03', '用四舍五入法试商', '把除数看作接近的整十数试商并调商。', 4::SMALLINT, 30),
        (9204010604::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-04', '除数不接近整十数的试商', '利用乘法关系或灵活方法确定合适的商。', 4::SMALLINT, 40),
        (9204010605::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-05', '商是两位数的笔算除法', '逐位试商并处理商中间或末尾的0。', 4::SMALLINT, 50),
        (9204010606::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-06', '除数是两位数的除法验算', '用除数乘商加余数等于被除数验算。', 3::SMALLINT, 60),
        (9204010607::BIGINT, 'PEP-G04-S01-U06', 'KP-PEP-G04-S01-U06-07', '商的变化规律', '理解被除数、除数变化对商的影响并进行简算。', 4::SMALLINT, 70),

        -- 下册 U01：四则运算（7）
        (9204020101::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-01', '加减法各部分关系', '根据和、差及已知数求未知加数、被减数或减数。', 3::SMALLINT, 10),
        (9204020102::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-02', '乘除法各部分关系', '根据积、商及已知数求未知因数、被除数或除数。', 3::SMALLINT, 20),
        (9204020103::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-03', '有关0的运算', '掌握0在四则运算中的规则以及0不能作除数。', 2::SMALLINT, 30),
        (9204020104::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-04', '无括号四则混合运算', '同级从左到右，两级先乘除后加减。', 3::SMALLINT, 40),
        (9204020105::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-05', '含小括号的四则运算', '先算小括号内，再按顺序计算括号外。', 3::SMALLINT, 50),
        (9204020106::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-06', '含中括号的四则运算', '先小括号、再中括号、最后括号外。', 4::SMALLINT, 60),
        (9204020107::BIGINT, 'PEP-G04-S02-U01', 'KP-PEP-G04-S02-U01-07', '四则混合运算实际问题', '正确列综合算式解决多步实际问题。', 4::SMALLINT, 70),

        -- 下册 U03：运算律（8）
        (9204020301::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-01', '加法交换律', '运用a+b=b+a调整加数顺序。', 2::SMALLINT, 10),
        (9204020302::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-02', '加法结合律', '运用(a+b)+c=a+(b+c)凑整。', 2::SMALLINT, 20),
        (9204020303::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-03', '连减简便计算', '运用a-b-c=a-(b+c)等方法简算。', 3::SMALLINT, 30),
        (9204020304::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-04', '乘法交换律', '运用a×b=b×a调整因数顺序。', 2::SMALLINT, 40),
        (9204020305::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-05', '乘法结合律', '运用(a×b)×c=a×(b×c)凑整。', 2::SMALLINT, 50),
        (9204020306::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-06', '乘法分配律', '运用(a+b)×c=a×c+b×c进行简算。', 3::SMALLINT, 60),
        (9204020307::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-07', '乘法分配律逆用', '从两个积中提取相同因数进行简算。', 4::SMALLINT, 70),
        (9204020308::BIGINT, 'PEP-G04-S02-U03', 'KP-PEP-G04-S02-U03-08', '连除简便计算', '运用a÷b÷c=a÷(b×c)等方法简算。', 3::SMALLINT, 80),

        -- 下册 U04：小数性质与计算准备（8）
        (9204020401::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-01', '小数的性质', '在小数末尾添上或去掉0，小数大小不变。', 2::SMALLINT, 10),
        (9204020402::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-02', '小数大小比较', '从高位起逐位比较小数大小。', 2::SMALLINT, 20),
        (9204020403::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-03', '小数乘10、100、1000', '小数点分别向右移动一、二、三位。', 2::SMALLINT, 30),
        (9204020404::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-04', '小数除以10、100、1000', '小数点分别向左移动一、二、三位。', 2::SMALLINT, 40),
        (9204020405::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-05', '小数点移动规律应用', '根据倍数关系确定原数或变化后的数。', 3::SMALLINT, 50),
        (9204020406::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-06', '低级单位改写成高级单位', '除以单位间进率，把低级单位改写成高级单位。', 3::SMALLINT, 60),
        (9204020407::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-07', '高级单位改写成低级单位', '乘单位间进率，把高级单位改写成低级单位。', 3::SMALLINT, 70),
        (9204020408::BIGINT, 'PEP-G04-S02-U04', 'KP-PEP-G04-S02-U04-08', '求小数近似数', '用四舍五入法把小数保留指定数位。', 3::SMALLINT, 80),

        -- 下册 U06：小数加减（5）
        (9204020601::BIGINT, 'PEP-G04-S02-U06', 'KP-PEP-G04-S02-U06-01', '小数加法', '小数点对齐，从末位起相加并处理进位。', 2::SMALLINT, 10),
        (9204020602::BIGINT, 'PEP-G04-S02-U06', 'KP-PEP-G04-S02-U06-02', '小数减法', '小数点对齐，必要时末尾补0并处理退位。', 2::SMALLINT, 20),
        (9204020603::BIGINT, 'PEP-G04-S02-U06', 'KP-PEP-G04-S02-U06-03', '小数加减混合运算', '按照从左到右或括号优先顺序计算。', 3::SMALLINT, 30),
        (9204020604::BIGINT, 'PEP-G04-S02-U06', 'KP-PEP-G04-S02-U06-04', '小数加减简便计算', '把整数加法运算律推广到小数。', 3::SMALLINT, 40),
        (9204020605::BIGINT, 'PEP-G04-S02-U06', 'KP-PEP-G04-S02-U06-05', '小数加减实际问题', '根据数量关系选择小数加法或减法。', 3::SMALLINT, 50)
)
INSERT INTO knowledge_point (
    id, module_id, parent_id, code, name, description, difficulty_level, sort_order, status, created_by
)
SELECT seed.id, module.id, NULL, seed.code, seed.name, seed.description,
       seed.difficulty_level, seed.sort_order, 1, 0
FROM seed JOIN content_module module ON module.code = seed.module_code
ON CONFLICT (code) DO NOTHING;

-- 3. 四年级专项错因：9 个。
WITH seed (id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint) AS (
    VALUES
        (94000033::BIGINT, 'PARTIAL_PRODUCT_ALIGNMENT_ERROR', '部分积数位对齐错误', 'PLACE_VALUE', 'VERTICAL', '三位数乘两位数时第二个部分积没有错位。', '乘十位上的数，部分积要从十位写起。', '在竖式中标出每个部分积对应的数位。'),
        (94000034::BIGINT, 'PRODUCT_CHANGE_RULE_ERROR', '积的变化规律使用错误', 'CONCEPT', 'ARITHMETIC', '因数变化倍数与积的变化不一致。', '一个因数不变，另一个因数怎样变化，积也怎样变化。', '用简单算式组观察并验证变化倍数。'),
        (94000035::BIGINT, 'TWO_DIGIT_DIVISOR_TRIAL_ERROR', '两位数除法试商错误', 'CALCULATION', 'VERTICAL', '试商过大或过小且未正确调商。', '先把除数看作接近的整十数，再根据余数调商。', '集中练习需要调大或调小的试商题。'),
        (94000036::BIGINT, 'QUOTIENT_CHANGE_RULE_ERROR', '商的变化规律使用错误', 'CONCEPT', 'ARITHMETIC', '被除数和除数变化时错误判断商。', '同时乘或除以相同非0数，商不变。', '用算式组验证商不变规律。'),
        (94000037::BIGINT, 'BRACKET_ORDER_ERROR', '中括号运算顺序错误', 'CALCULATION', 'ARITHMETIC', '含两层括号时未先算小括号。', '先小括号，再中括号，最后括号外。', '给每一步运算标上序号。'),
        (94000038::BIGINT, 'OPERATION_LAW_MISUSE', '运算律误用', 'CALCULATION', 'ARITHMETIC', '不满足运算律条件时强行凑整或分配。', '先确认算式结构，再选择匹配的运算律。', '把原式与变形后的算式代入小数验证。'),
        (94000039::BIGINT, 'DECIMAL_POINT_SHIFT_ERROR', '小数点移动方向错误', 'PLACE_VALUE', 'ARITHMETIC', '乘除10、100、1000时移动方向或位数错误。', '乘整十幂向右移，除整十幂向左移。', '在数位表中练习移动小数点。'),
        (94000040::BIGINT, 'DECIMAL_UNIT_CONVERSION_ERROR', '小数单位换算错误', 'CALCULATION', 'FILL_BLANK', '单位进率和小数点移动方向不匹配。', '先写出单位进率，再判断乘还是除。', '使用单位换算方向箭头练习。'),
        (94000041::BIGINT, 'DECIMAL_ROUNDING_ERROR', '小数四舍五入错误', 'CALCULATION', 'FILL_BLANK', '看错保留位的下一位或保留位数错误。', '确定保留位，再看它右边第一位决定舍入。', '圈出保留位和判断位后计算。')
)
INSERT INTO error_type (
    id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, status
)
SELECT id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, 1
FROM seed ON CONFLICT (code) DO NOTHING;

-- 4. 固定联调题：24 道。
WITH seed (id, code, question_type, stem, answer_json, analysis, difficulty, estimated_seconds) AS (
    VALUES
        (930401040001::BIGINT, 'Q-PEP-G04-S01-U04-001', 'VERTICAL', '列竖式计算：248 × 36 = ?', '{"value":8928}', '分别乘个位6和十位3，再把部分积相加。', 3::SMALLINT, 45),
        (930401040002::BIGINT, 'Q-PEP-G04-S01-U04-002', 'VERTICAL', '列竖式计算：305 × 24 = ?', '{"value":7320}', '保持中间0的数位，结果是7320。', 3::SMALLINT, 45),
        (930401040003::BIGINT, 'Q-PEP-G04-S01-U04-003', 'ARITHMETIC', '若16 × 25 = 400，则16 × 250 = ?', '{"value":4000}', '一个因数扩大10倍，积也扩大10倍。', 3::SMALLINT, 18),
        (930401040004::BIGINT, 'Q-PEP-G04-S01-U04-004', 'FILL_BLANK', '一辆车每小时行75千米，4小时行多少千米？', '{"value":300}', '速度乘时间等于路程。', 2::SMALLINT, 20),

        (930401060001::BIGINT, 'Q-PEP-G04-S01-U06-001', 'ARITHMETIC', '560 ÷ 70 = ?', '{"value":8}', '把被除数和除数同时除以10，计算56除以7。', 2::SMALLINT, 12),
        (930401060002::BIGINT, 'Q-PEP-G04-S01-U06-002', 'VERTICAL', '列竖式计算：612 ÷ 34 = ?', '{"value":18}', '逐位试商，商是18。', 4::SMALLINT, 45),
        (930401060003::BIGINT, 'Q-PEP-G04-S01-U06-003', 'VERTICAL', '列竖式计算：940 ÷ 31 = ?……?', '{"quotient":30,"remainder":10}', '31乘30等于930，余数10小于31。', 4::SMALLINT, 45),
        (930401060004::BIGINT, 'Q-PEP-G04-S01-U06-004', 'ARITHMETIC', '若480 ÷ 12 = 40，则4800 ÷ 120 = ?', '{"value":40}', '被除数和除数同时扩大10倍，商不变。', 3::SMALLINT, 18),

        (930402010001::BIGINT, 'Q-PEP-G04-S02-U01-001', 'FILL_BLANK', '? - 368 = 425', '{"value":793}', '被减数等于差加减数。', 3::SMALLINT, 20),
        (930402010002::BIGINT, 'Q-PEP-G04-S02-U01-002', 'FILL_BLANK', '35 × ? = 840', '{"value":24}', '未知因数等于积除以已知因数。', 3::SMALLINT, 20),
        (930402010003::BIGINT, 'Q-PEP-G04-S02-U01-003', 'ARITHMETIC', '240 - 40 × 3 = ?', '{"value":120}', '先乘法后减法，结果是120。', 3::SMALLINT, 18),
        (930402010004::BIGINT, 'Q-PEP-G04-S02-U01-004', 'ARITHMETIC', '360 ÷ [（12 + 6）× 2] = ?', '{"value":10}', '先小括号，再中括号，最后除法。', 4::SMALLINT, 28),

        (930402030001::BIGINT, 'Q-PEP-G04-S02-U03-001', 'ARITHMETIC', '158 + 67 + 42 = ?', '{"value":267}', '用加法交换律和结合律先算158加42。', 2::SMALLINT, 18),
        (930402030002::BIGINT, 'Q-PEP-G04-S02-U03-002', 'ARITHMETIC', '25 × 17 × 4 = ?', '{"value":1700}', '先算25乘4等于100，再乘17。', 2::SMALLINT, 18),
        (930402030003::BIGINT, 'Q-PEP-G04-S02-U03-003', 'ARITHMETIC', '125 × 32 = ?', '{"value":4000}', '把32分成8乘4，先算125乘8等于1000。', 3::SMALLINT, 22),
        (930402030004::BIGINT, 'Q-PEP-G04-S02-U03-004', 'ARITHMETIC', '99 × 36 = ?', '{"value":3564}', '把99看作100减1，使用乘法分配律。', 3::SMALLINT, 22),

        (930402040001::BIGINT, 'Q-PEP-G04-S02-U04-001', 'FILL_BLANK', '不改变大小，把3.5改写成两位小数。', '{"value":3.50}', '在小数末尾添0，大小不变。', 2::SMALLINT, 12),
        (930402040002::BIGINT, 'Q-PEP-G04-S02-U04-002', 'ARITHMETIC', '0.764 × 100 = ?', '{"value":76.4}', '小数点向右移动两位。', 2::SMALLINT, 12),
        (930402040003::BIGINT, 'Q-PEP-G04-S02-U04-003', 'FILL_BLANK', '3.6米等于多少厘米？', '{"value":360}', '米到厘米乘100。', 3::SMALLINT, 18),
        (930402040004::BIGINT, 'Q-PEP-G04-S02-U04-004', 'FILL_BLANK', '把6.384保留一位小数。', '{"value":6.4}', '十分位后是8，向十分位进1。', 3::SMALLINT, 18),

        (930402060001::BIGINT, 'Q-PEP-G04-S02-U06-001', 'VERTICAL', '列竖式计算：12.56 + 7.8 = ?', '{"value":20.36}', '小数点对齐，7.8写作7.80后计算。', 3::SMALLINT, 28),
        (930402060002::BIGINT, 'Q-PEP-G04-S02-U06-002', 'VERTICAL', '列竖式计算：20 - 8.75 = ?', '{"value":11.25}', '把20写作20.00后对齐相减。', 3::SMALLINT, 30),
        (930402060003::BIGINT, 'Q-PEP-G04-S02-U06-003', 'ARITHMETIC', '9.6 + 5.4 - 3.8 = ?', '{"value":11.2}', '从左到右计算，结果是11.2。', 3::SMALLINT, 22),
        (930402060004::BIGINT, 'Q-PEP-G04-S02-U06-004', 'ARITHMETIC', '3.75 + 6.28 + 6.25 = ?', '{"value":16.28}', '先算3.75加6.25等于10，再加6.28。', 3::SMALLINT, 22)
)
INSERT INTO question (
    id, code, question_mode, question_type, stem, options, standard_answer, analysis,
    generation_rule, difficulty, estimated_seconds, status, created_by, reviewed_by, reviewed_at
)
SELECT id, code, 'FIXED', question_type, stem, NULL, answer_json::JSONB, analysis,
       NULL, difficulty, estimated_seconds, 'ENABLED', 0, 0, CURRENT_TIMESTAMP
FROM seed ON CONFLICT (code) DO NOTHING;

WITH seed (question_code, knowledge_point_code) AS (
    VALUES
        ('Q-PEP-G04-S01-U04-001', 'KP-PEP-G04-S01-U04-01'), ('Q-PEP-G04-S01-U04-002', 'KP-PEP-G04-S01-U04-02'),
        ('Q-PEP-G04-S01-U04-003', 'KP-PEP-G04-S01-U04-04'), ('Q-PEP-G04-S01-U04-004', 'KP-PEP-G04-S01-U04-08'),
        ('Q-PEP-G04-S01-U06-001', 'KP-PEP-G04-S01-U06-01'), ('Q-PEP-G04-S01-U06-002', 'KP-PEP-G04-S01-U06-05'),
        ('Q-PEP-G04-S01-U06-003', 'KP-PEP-G04-S01-U06-05'), ('Q-PEP-G04-S01-U06-004', 'KP-PEP-G04-S01-U06-07'),
        ('Q-PEP-G04-S02-U01-001', 'KP-PEP-G04-S02-U01-01'), ('Q-PEP-G04-S02-U01-002', 'KP-PEP-G04-S02-U01-02'),
        ('Q-PEP-G04-S02-U01-003', 'KP-PEP-G04-S02-U01-04'), ('Q-PEP-G04-S02-U01-004', 'KP-PEP-G04-S02-U01-06'),
        ('Q-PEP-G04-S02-U03-001', 'KP-PEP-G04-S02-U03-02'), ('Q-PEP-G04-S02-U03-002', 'KP-PEP-G04-S02-U03-05'),
        ('Q-PEP-G04-S02-U03-003', 'KP-PEP-G04-S02-U03-05'), ('Q-PEP-G04-S02-U03-004', 'KP-PEP-G04-S02-U03-07'),
        ('Q-PEP-G04-S02-U04-001', 'KP-PEP-G04-S02-U04-01'), ('Q-PEP-G04-S02-U04-002', 'KP-PEP-G04-S02-U04-03'),
        ('Q-PEP-G04-S02-U04-003', 'KP-PEP-G04-S02-U04-07'), ('Q-PEP-G04-S02-U04-004', 'KP-PEP-G04-S02-U04-08'),
        ('Q-PEP-G04-S02-U06-001', 'KP-PEP-G04-S02-U06-01'), ('Q-PEP-G04-S02-U06-002', 'KP-PEP-G04-S02-U06-02'),
        ('Q-PEP-G04-S02-U06-003', 'KP-PEP-G04-S02-U06-03'), ('Q-PEP-G04-S02-U06-004', 'KP-PEP-G04-S02-U06-04')
)
INSERT INTO question_knowledge_point (question_id, knowledge_point_id, is_primary, weight)
SELECT question.id, knowledge_point.id, TRUE, 1.0000
FROM seed JOIN question ON question.code = seed.question_code
JOIN knowledge_point ON knowledge_point.code = seed.knowledge_point_code
ON CONFLICT (question_id, knowledge_point_id) DO NOTHING;

COMMIT;

SELECT COUNT(*) AS grade4_pep_module_count FROM content_module WHERE code LIKE 'PEP-G04-%';
SELECT COUNT(*) AS grade4_pep_knowledge_point_count FROM knowledge_point WHERE code LIKE 'KP-PEP-G04-%';
SELECT COUNT(*) AS grade4_pep_test_question_count FROM question WHERE code LIKE 'Q-PEP-G04-%';
SELECT COUNT(*) AS grade4_specific_error_type_count FROM error_type WHERE id BETWEEN 94000033 AND 94000041;
