-- content_db 五年级人教版基础计算内容种子数据
-- 教材范围：现行人教版五年级上、下册；排除位置、概率、图形、统计、综合实践和复习。
-- 目录参考：https://basic.smartedu.cn/syncClassroom
-- 使用：psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade5-pep.sql
-- 建议先按年级顺序执行低年级种子脚本。

BEGIN;

-- 1. 计算内容模块：6 个。
WITH seed (id, code, name, grade_level, semester, learning_theme, description, sort_order) AS (
    VALUES
        (91050101::BIGINT, 'PEP-G05-S01-U01', '小数乘法', 5::SMALLINT, 1::SMALLINT, '数与运算', '掌握小数乘整数、小数乘小数、近似数、简算和应用。', 10),
        (91050103::BIGINT, 'PEP-G05-S01-U03', '小数除法', 5::SMALLINT, 1::SMALLINT, '数与运算', '掌握小数除法、商的近似数、循环小数和实际应用。', 30),
        (91050105::BIGINT, 'PEP-G05-S01-U05', '简易方程', 5::SMALLINT, 1::SMALLINT, '式与方程', '运用等式性质解简易方程并解决实际问题。', 50),
        (91050202::BIGINT, 'PEP-G05-S02-U02', '因数与倍数的计算基础', 5::SMALLINT, 2::SMALLINT, '数与运算', '判断因数、倍数、整除特征、奇偶性和质合性。', 20),
        (91050204::BIGINT, 'PEP-G05-S02-U04', '分数的性质与计算准备', 5::SMALLINT, 2::SMALLINT, '数与运算', '掌握分数与除法、约分、通分、最大公因数、最小公倍数及互化。', 40),
        (91050206::BIGINT, 'PEP-G05-S02-U06', '分数的加法和减法', 5::SMALLINT, 2::SMALLINT, '数与运算', '掌握同分母、异分母分数加减和混合运算。', 60)
)
INSERT INTO content_module (
    id, code, name, grade_level, semester, learning_theme, publisher,
    edition_description, description, sort_order, status, created_by
)
SELECT id, code, name, grade_level, semester, learning_theme, '人民教育出版社',
       '现行人教版义务教育教科书', description, sort_order, 1, 0
FROM seed ON CONFLICT (code) DO NOTHING;

-- 2. 原子知识点：41 个。
WITH seed (id, module_code, code, name, description, difficulty_level, sort_order) AS (
    VALUES
        -- 上册 U01：小数乘法（7）
        (9205010101::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-01', '小数乘整数', '先按整数乘法计算，再确定积的小数位数。', 2::SMALLINT, 10),
        (9205010102::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-02', '小数乘小数', '积的小数位数等于两个因数小数位数之和。', 3::SMALLINT, 20),
        (9205010103::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-03', '积末尾0的处理', '确定小数点后去掉积末尾不必要的0。', 3::SMALLINT, 30),
        (9205010104::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-04', '积的近似数', '用四舍五入法把积保留指定数位。', 3::SMALLINT, 40),
        (9205010105::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-05', '小数乘法简便计算', '把整数乘法运算律推广到小数。', 3::SMALLINT, 50),
        (9205010106::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-06', '小数乘法估算应用', '根据实际问题选择上估或下估判断结果。', 4::SMALLINT, 60),
        (9205010107::BIGINT, 'PEP-G05-S01-U01', 'KP-PEP-G05-S01-U01-07', '分段计费问题', '按不同计费区间分别计算并合并费用。', 4::SMALLINT, 70),

        -- 上册 U03：小数除法（7）
        (9205010301::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-01', '除数是整数的小数除法', '商的小数点与被除数的小数点对齐。', 3::SMALLINT, 10),
        (9205010302::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-02', '一个数除以小数', '把除数转化为整数，被除数同时扩大相同倍数。', 4::SMALLINT, 20),
        (9205010303::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-03', '商中补0继续除', '被除数末尾添0继续计算到指定精度。', 3::SMALLINT, 30),
        (9205010304::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-04', '商的近似数', '根据要求除到下一位后四舍五入。', 3::SMALLINT, 40),
        (9205010305::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-05', '循环小数与简写', '识别循环节并规范表示循环小数。', 3::SMALLINT, 50),
        (9205010306::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-06', '小数四则混合运算', '按四则运算顺序计算含小数的算式。', 4::SMALLINT, 60),
        (9205010307::BIGINT, 'PEP-G05-S01-U03', 'KP-PEP-G05-S01-U03-07', '小数除法取整应用', '根据实际意义使用进一法或去尾法。', 4::SMALLINT, 70),

        -- 上册 U05：简易方程（7）
        (9205010501::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-01', '代入求含字母式子的值', '把给定数值代入含字母式子并按顺序计算。', 2::SMALLINT, 10),
        (9205010502::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-02', '用等式性质解加减方程', '方程两边同时加或减同一个数。', 3::SMALLINT, 20),
        (9205010503::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-03', '用等式性质解乘除方程', '方程两边同时乘或除以同一个非0数。', 3::SMALLINT, 30),
        (9205010504::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-04', '解ax加减b等于c型方程', '先消去常数项，再求未知数。', 4::SMALLINT, 40),
        (9205010505::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-05', '解a乘x加减b乘x等于c型方程', '先合并含x的项，再求未知数。', 4::SMALLINT, 50),
        (9205010506::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-06', '列方程解决单步问题', '根据等量关系列出并求解简单方程。', 3::SMALLINT, 60),
        (9205010507::BIGINT, 'PEP-G05-S01-U05', 'KP-PEP-G05-S01-U05-07', '列方程解决两步问题', '找到关键等量关系并列两步方程。', 4::SMALLINT, 70),

        -- 下册 U02：因数与倍数（6）
        (9205020201::BIGINT, 'PEP-G05-S02-U02', 'KP-PEP-G05-S02-U02-01', '求一个数的因数', '成对列乘法算式，有序找出全部因数。', 2::SMALLINT, 10),
        (9205020202::BIGINT, 'PEP-G05-S02-U02', 'KP-PEP-G05-S02-U02-02', '求一个数的倍数', '按整数倍有序写出指定范围内的倍数。', 2::SMALLINT, 20),
        (9205020203::BIGINT, 'PEP-G05-S02-U02', 'KP-PEP-G05-S02-U02-03', '2和5的倍数特征', '用个位数字判断一个数是否为2或5的倍数。', 2::SMALLINT, 30),
        (9205020204::BIGINT, 'PEP-G05-S02-U02', 'KP-PEP-G05-S02-U02-04', '3的倍数特征', '用各位数字之和判断是否为3的倍数。', 2::SMALLINT, 40),
        (9205020205::BIGINT, 'PEP-G05-S02-U02', 'KP-PEP-G05-S02-U02-05', '质数和合数判断', '依据因数个数判断质数与合数。', 3::SMALLINT, 50),
        (9205020206::BIGINT, 'PEP-G05-S02-U02', 'KP-PEP-G05-S02-U02-06', '和的奇偶性', '根据奇数偶数相加规律判断和的奇偶性。', 3::SMALLINT, 60),

        -- 下册 U04：分数性质与计算准备（9）
        (9205020401::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-01', '分数与除法的关系', '用分数表示两个整数相除的商。', 2::SMALLINT, 10),
        (9205020402::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-02', '假分数与带分数互化', '用除法把假分数化成整数或带分数，并能逆向转换。', 3::SMALLINT, 20),
        (9205020403::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-03', '分数的基本性质', '分子分母同时乘或除以相同非0数，分数大小不变。', 2::SMALLINT, 30),
        (9205020404::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-04', '求最大公因数', '用列举、分解或短除法求最大公因数。', 3::SMALLINT, 40),
        (9205020405::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-05', '约分', '用分子分母的公因数把分数化成最简分数。', 3::SMALLINT, 50),
        (9205020406::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-06', '求最小公倍数', '用列举、分解或短除法求最小公倍数。', 3::SMALLINT, 60),
        (9205020407::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-07', '通分', '用最小公倍数作公分母，把异分母分数化成同分母分数。', 3::SMALLINT, 70),
        (9205020408::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-08', '分数化小数', '用分子除以分母把分数化成小数。', 3::SMALLINT, 80),
        (9205020409::BIGINT, 'PEP-G05-S02-U04', 'KP-PEP-G05-S02-U04-09', '小数化分数', '按小数意义写成分母为10、100等的分数并约分。', 3::SMALLINT, 90),

        -- 下册 U06：分数加减（5）
        (9205020601::BIGINT, 'PEP-G05-S02-U06', 'KP-PEP-G05-S02-U06-01', '同分母分数加减', '分母不变，分子相加减，结果约成最简分数。', 2::SMALLINT, 10),
        (9205020602::BIGINT, 'PEP-G05-S02-U06', 'KP-PEP-G05-S02-U06-02', '异分母分数加法', '先通分，再按同分母分数加法计算。', 3::SMALLINT, 20),
        (9205020603::BIGINT, 'PEP-G05-S02-U06', 'KP-PEP-G05-S02-U06-03', '异分母分数减法', '先通分，再按同分母分数减法计算。', 3::SMALLINT, 30),
        (9205020604::BIGINT, 'PEP-G05-S02-U06', 'KP-PEP-G05-S02-U06-04', '分数加减混合运算', '按从左到右、括号优先和运算律计算。', 4::SMALLINT, 40),
        (9205020605::BIGINT, 'PEP-G05-S02-U06', 'KP-PEP-G05-S02-U06-05', '分数加减实际问题', '根据数量关系列分数加减算式解决问题。', 4::SMALLINT, 50)
)
INSERT INTO knowledge_point (
    id, module_id, parent_id, code, name, description, difficulty_level, sort_order, status, created_by
)
SELECT seed.id, module.id, NULL, seed.code, seed.name, seed.description,
       seed.difficulty_level, seed.sort_order, 1, 0
FROM seed JOIN content_module module ON module.code = seed.module_code
ON CONFLICT (code) DO NOTHING;

-- 3. 五年级专项错因：10 个。
WITH seed (id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint) AS (
    VALUES
        (94000042::BIGINT, 'DECIMAL_PRODUCT_SCALE_ERROR', '小数积位数错误', 'PLACE_VALUE', 'VERTICAL', '没有按两个因数的小数位数之和确定积的小数点。', '先按整数乘，再数两个因数共有几位小数。', '每题先标记小数位数再点小数点。'),
        (94000043::BIGINT, 'DECIMAL_DIVISOR_TRANSFORM_ERROR', '小数除数转化错误', 'PLACE_VALUE', 'VERTICAL', '只移动除数小数点，未同步移动被除数。', '除数扩大多少倍，被除数也要扩大相同倍数。', '用箭头同步标记两个小数点。'),
        (94000044::BIGINT, 'DECIMAL_QUOTIENT_POINT_ERROR', '小数商的小数点错误', 'PLACE_VALUE', 'VERTICAL', '商的小数点没有与被除数变化后的小数点对齐。', '除数化成整数后，商的小数点对齐被除数的小数点。', '列竖式前先确定商的小数点位置。'),
        (94000045::BIGINT, 'EQUATION_BALANCE_ERROR', '等式两边未同步变化', 'CONCEPT', 'FILL_BLANK', '解方程时只改变一边或两边变化不同。', '等式两边必须同时进行相同运算。', '把每一步对等式两边的操作并排写出。'),
        (94000046::BIGINT, 'EQUATION_INVERSE_OPERATION_ERROR', '方程逆运算错误', 'CALCULATION', 'FILL_BLANK', '移除已知项时选择了错误的逆运算。', '加对应减、乘对应除，再检查未知数。', '求解后把结果代回原方程验算。'),
        (94000047::BIGINT, 'DIVISIBILITY_RULE_ERROR', '倍数特征判断错误', 'CONCEPT', 'JUDGEMENT', '混淆2、5、3的倍数特征。', '2和5看个位，3看各位数字之和。', '将三个判断规则制成对照表。'),
        (94000048::BIGINT, 'PRIME_COMPOSITE_ERROR', '质数合数判断错误', 'CONCEPT', 'JUDGEMENT', '没有完整检查因数个数或把1误分类。', '质数只有1和本身两个因数，1既不是质数也不是合数。', '按从小到大试除并列出因数。'),
        (94000049::BIGINT, 'FRACTION_REDUCTION_ERROR', '约分不彻底或规则错误', 'CALCULATION', 'FILL_BLANK', '分子分母没有同时除以同一数或结果非最简。', '分子分母同时除以最大公因数可一次约成最简。', '约分后检查分子分母是否仍有公因数。'),
        (94000050::BIGINT, 'COMMON_DENOMINATOR_ERROR', '通分错误', 'CALCULATION', 'FILL_BLANK', '改变分母时没有按相同倍数改变分子。', '分母乘几，分子也必须乘几。', '在分子分母旁同时标出扩大的倍数。'),
        (94000051::BIGINT, 'UNLIKE_FRACTION_ADD_ERROR', '异分母分数直接相加减', 'CALCULATION', 'ARITHMETIC', '未通分就直接计算分子或分母。', '异分母分数先通分，再只计算分子。', '每题先写最小公分母再计算。')
)
INSERT INTO error_type (
    id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, status
)
SELECT id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, 1
FROM seed ON CONFLICT (code) DO NOTHING;

-- 4. 固定联调题：24 道。
WITH seed (id, code, question_type, stem, answer_json, analysis, difficulty, estimated_seconds) AS (
    VALUES
        (930501010001::BIGINT, 'Q-PEP-G05-S01-U01-001', 'VERTICAL', '列竖式计算：3.6 × 24 = ?', '{"value":86.4}', '先算36乘24，再把积缩小10倍。', 3::SMALLINT, 35),
        (930501010002::BIGINT, 'Q-PEP-G05-S01-U01-002', 'VERTICAL', '列竖式计算：2.35 × 1.4 = ?', '{"value":3.29}', '按整数乘法计算3290，两个因数共三位小数，得到3.290即3.29。', 4::SMALLINT, 40),
        (930501010003::BIGINT, 'Q-PEP-G05-S01-U01-003', 'FILL_BLANK', '把6.38 × 2.7的积保留一位小数。', '{"value":17.2}', '精确积17.226，保留一位小数得17.2。', 3::SMALLINT, 28),
        (930501010004::BIGINT, 'Q-PEP-G05-S01-U01-004', 'ARITHMETIC', '0.25 × 7.8 × 4 = ?', '{"value":7.8}', '先用结合律计算0.25乘4等于1。', 3::SMALLINT, 20),

        (930501030001::BIGINT, 'Q-PEP-G05-S01-U03-001', 'VERTICAL', '列竖式计算：42.84 ÷ 7 = ?', '{"value":6.12}', '商的小数点与被除数小数点对齐。', 3::SMALLINT, 35),
        (930501030002::BIGINT, 'Q-PEP-G05-S01-U03-002', 'VERTICAL', '列竖式计算：7.98 ÷ 0.42 = ?', '{"value":19}', '除数和被除数同时扩大100倍后计算798除以42。', 4::SMALLINT, 40),
        (930501030003::BIGINT, 'Q-PEP-G05-S01-U03-003', 'FILL_BLANK', '8 ÷ 11的商保留两位小数是多少？', '{"value":0.73}', '算到千分位后四舍五入，得到0.73。', 3::SMALLINT, 28),
        (930501030004::BIGINT, 'Q-PEP-G05-S01-U03-004', 'FILL_BLANK', '45千克油，每桶装6.5千克，至少需要几个桶？', '{"value":7}', '45除以6.5约等于6.92，剩余油仍需一个桶，进一得7。', 4::SMALLINT, 30),

        (930501050001::BIGINT, 'Q-PEP-G05-S01-U05-001', 'FILL_BLANK', '当a=3.5时，2a+4等于多少？', '{"value":11}', '代入后计算2乘3.5加4等于11。', 2::SMALLINT, 18),
        (930501050002::BIGINT, 'Q-PEP-G05-S01-U05-002', 'FILL_BLANK', '解方程：x + 7.8 = 15.2。x = ?', '{"value":7.4}', '方程两边同时减7.8。', 3::SMALLINT, 22),
        (930501050003::BIGINT, 'Q-PEP-G05-S01-U05-003', 'FILL_BLANK', '解方程：4x - 6 = 30。x = ?', '{"value":9}', '先两边加6，再除以4。', 4::SMALLINT, 28),
        (930501050004::BIGINT, 'Q-PEP-G05-S01-U05-004', 'FILL_BLANK', '3x + 2x = 45。x = ?', '{"value":9}', '合并得到5x等于45，所以x等于9。', 4::SMALLINT, 28),

        (930502020001::BIGINT, 'Q-PEP-G05-S02-U02-001', 'FILL_BLANK', '24一共有多少个正因数？', '{"value":8}', '24的因数是1、2、3、4、6、8、12、24，共8个。', 3::SMALLINT, 25),
        (930502020002::BIGINT, 'Q-PEP-G05-S02-U02-002', 'JUDGEMENT', '判断：735既是3的倍数，也是5的倍数。', '{"value":true}', '个位是5且各位数字和为15，所以判断正确。', 2::SMALLINT, 18),
        (930502020003::BIGINT, 'Q-PEP-G05-S02-U02-003', 'JUDGEMENT', '判断：1是质数。', '{"value":false}', '1只有一个正因数，既不是质数也不是合数。', 2::SMALLINT, 15),
        (930502020004::BIGINT, 'Q-PEP-G05-S02-U02-004', 'FILL_BLANK', '奇数与奇数的和是奇数还是偶数？', '{"text":"偶数"}', '两个奇数的和是偶数。', 2::SMALLINT, 15),

        (930502040001::BIGINT, 'Q-PEP-G05-S02-U04-001', 'FILL_BLANK', '11/4化成带分数是多少？', '{"text":"2又3/4"}', '11除以4商2余3，得到2又3/4。', 3::SMALLINT, 20),
        (930502040002::BIGINT, 'Q-PEP-G05-S02-U04-002', 'FILL_BLANK', '18和30的最大公因数是多少？', '{"value":6}', '共同因数中最大的是6。', 3::SMALLINT, 22),
        (930502040003::BIGINT, 'Q-PEP-G05-S02-U04-003', 'FILL_BLANK', '12和18的最小公倍数是多少？', '{"value":36}', '两数最小的共同倍数是36。', 3::SMALLINT, 22),
        (930502040004::BIGINT, 'Q-PEP-G05-S02-U04-004', 'FILL_BLANK', '把18/24约成最简分数。', '{"text":"3/4"}', '分子分母同时除以最大公因数6。', 3::SMALLINT, 20),

        (930502060001::BIGINT, 'Q-PEP-G05-S02-U06-001', 'ARITHMETIC', '7/12 - 5/12 = ?', '{"text":"1/6"}', '先得2/12，再约分为1/6。', 2::SMALLINT, 18),
        (930502060002::BIGINT, 'Q-PEP-G05-S02-U06-002', 'ARITHMETIC', '2/3 + 5/8 = ?', '{"text":"31/24"}', '公分母24，16/24加15/24等于31/24。', 3::SMALLINT, 25),
        (930502060003::BIGINT, 'Q-PEP-G05-S02-U06-003', 'ARITHMETIC', '5/6 - 1/4 = ?', '{"text":"7/12"}', '通分后10/12减3/12等于7/12。', 3::SMALLINT, 25),
        (930502060004::BIGINT, 'Q-PEP-G05-S02-U06-004', 'ARITHMETIC', '1 - (1/3 + 1/4) = ?', '{"text":"5/12"}', '括号内是7/12，1减7/12等于5/12。', 4::SMALLINT, 28)
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
        ('Q-PEP-G05-S01-U01-001', 'KP-PEP-G05-S01-U01-01'), ('Q-PEP-G05-S01-U01-002', 'KP-PEP-G05-S01-U01-02'),
        ('Q-PEP-G05-S01-U01-003', 'KP-PEP-G05-S01-U01-04'), ('Q-PEP-G05-S01-U01-004', 'KP-PEP-G05-S01-U01-05'),
        ('Q-PEP-G05-S01-U03-001', 'KP-PEP-G05-S01-U03-01'), ('Q-PEP-G05-S01-U03-002', 'KP-PEP-G05-S01-U03-02'),
        ('Q-PEP-G05-S01-U03-003', 'KP-PEP-G05-S01-U03-04'), ('Q-PEP-G05-S01-U03-004', 'KP-PEP-G05-S01-U03-07'),
        ('Q-PEP-G05-S01-U05-001', 'KP-PEP-G05-S01-U05-01'), ('Q-PEP-G05-S01-U05-002', 'KP-PEP-G05-S01-U05-02'),
        ('Q-PEP-G05-S01-U05-003', 'KP-PEP-G05-S01-U05-04'), ('Q-PEP-G05-S01-U05-004', 'KP-PEP-G05-S01-U05-05'),
        ('Q-PEP-G05-S02-U02-001', 'KP-PEP-G05-S02-U02-01'), ('Q-PEP-G05-S02-U02-002', 'KP-PEP-G05-S02-U02-04'),
        ('Q-PEP-G05-S02-U02-003', 'KP-PEP-G05-S02-U02-05'), ('Q-PEP-G05-S02-U02-004', 'KP-PEP-G05-S02-U02-06'),
        ('Q-PEP-G05-S02-U04-001', 'KP-PEP-G05-S02-U04-02'), ('Q-PEP-G05-S02-U04-002', 'KP-PEP-G05-S02-U04-04'),
        ('Q-PEP-G05-S02-U04-003', 'KP-PEP-G05-S02-U04-06'), ('Q-PEP-G05-S02-U04-004', 'KP-PEP-G05-S02-U04-05'),
        ('Q-PEP-G05-S02-U06-001', 'KP-PEP-G05-S02-U06-01'), ('Q-PEP-G05-S02-U06-002', 'KP-PEP-G05-S02-U06-02'),
        ('Q-PEP-G05-S02-U06-003', 'KP-PEP-G05-S02-U06-03'), ('Q-PEP-G05-S02-U06-004', 'KP-PEP-G05-S02-U06-04')
)
INSERT INTO question_knowledge_point (question_id, knowledge_point_id, is_primary, weight)
SELECT question.id, knowledge_point.id, TRUE, 1.0000
FROM seed JOIN question ON question.code = seed.question_code
JOIN knowledge_point ON knowledge_point.code = seed.knowledge_point_code
ON CONFLICT (question_id, knowledge_point_id) DO NOTHING;

COMMIT;

SELECT COUNT(*) AS grade5_pep_module_count FROM content_module WHERE code LIKE 'PEP-G05-%';
SELECT COUNT(*) AS grade5_pep_knowledge_point_count FROM knowledge_point WHERE code LIKE 'KP-PEP-G05-%';
SELECT COUNT(*) AS grade5_pep_test_question_count FROM question WHERE code LIKE 'Q-PEP-G05-%';
SELECT COUNT(*) AS grade5_specific_error_type_count FROM error_type WHERE id BETWEEN 94000042 AND 94000051;
