const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const outputDir = 'C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52';

function panel(title, height, aria) {
  return { title, width: 720, height, aria, nodes: [], edges: [] };
}

function entity(p, id, label, x, y, attributes = [], options = {}) {
  p.nodes.push({ id, type: 'entity', label, x, y, reuse: Boolean(options.reuse), note: options.note || '' });
  attributes.forEach(([attributeId, attributeLabel, attributeX, attributeY, key = false]) => {
    attribute(p, attributeId, attributeLabel, attributeX, attributeY, key);
    connect(p, attributeId, id);
  });
}

function relation(p, id, label, x, y, attributes = []) {
  p.nodes.push({ id, type: 'relation', label, x, y });
  attributes.forEach(([attributeId, attributeLabel, attributeX, attributeY, key = false]) => {
    attribute(p, attributeId, attributeLabel, attributeX, attributeY, key);
    connect(p, attributeId, id);
  });
}

function attribute(p, id, label, x, y, key = false) {
  p.nodes.push({ id, type: 'attribute', label, x, y, key });
}

function connect(p, from, to, fromCardinality = '', toCardinality = '', options = {}) {
  p.edges.push({ from, to, fromCardinality, toCardinality, ...options });
}

function makeBatch2() {
  const p1 = panel('教材编排与适用范围', 500, '教材版本包含课程单元，课程单元归属年级并安排在学期中。');
  entity(p1, 'textbook', '教材版本', 115, 180, [
    ['textbookId', '教材编号', 62, 70, true],
    ['textbookName', '教材名称', 166, 62],
    ['publisher', '出版社', 55, 280],
    ['editionYear', '版本年份', 175, 290],
    ['textbookStatus', '教材状态', 115, 345],
  ]);
  relation(p1, 'contains', '包含', 255, 180);
  entity(p1, 'unit', '课程单元', 395, 180, [
    ['unitId', '单元编号', 330, 65, true],
    ['unitName', '单元名称', 445, 58],
    ['unitOrder', '单元顺序', 365, 285],
    ['unitStatus', '单元状态', 455, 330],
  ]);
  relation(p1, 'belongsGrade', '归属', 525, 115);
  entity(p1, 'grade', '年级', 640, 80, [
    ['gradeName', '年级名称', 640, 30],
  ], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'scheduledTerm', '安排', 525, 255);
  entity(p1, 'term', '学期', 640, 300, [
    ['termId', '学期编号', 625, 390, true],
    ['termName', '学期名称', 625, 440],
    ['termOrder', '顺序号', 530, 410],
  ]);
  connect(p1, 'textbook', 'contains', '1', '');
  connect(p1, 'contains', 'unit', '', 'N');
  connect(p1, 'unit', 'belongsGrade', 'N', '');
  connect(p1, 'belongsGrade', 'grade', '', '1');
  connect(p1, 'unit', 'scheduledTerm', 'N', '');
  connect(p1, 'scheduledTerm', 'term', '', '1');

  const p2 = panel('知识点体系', 470, '课程单元覆盖多个知识点，知识点之间可以形成层级关系。');
  entity(p2, 'unitRef', '课程单元', 95, 220, [], { reuse: true, note: '沿用上图实体' });
  relation(p2, 'covers', '覆盖', 245, 220, [
    ['coverOrder', '顺序号', 235, 110],
    ['importance', '重要程度', 335, 125],
  ]);
  entity(p2, 'knowledge', '知识点', 405, 220, [
    ['knowledgeId', '知识点编号', 365, 60, true],
    ['knowledgeName', '知识点名称', 495, 65],
    ['knowledgeLevel', '知识层级', 330, 340],
    ['baseDifficulty', '难度基准', 440, 380],
    ['knowledgeStatus', '知识点状态', 555, 340],
  ]);
  relation(p2, 'parentRelation', '从属', 555, 220);
  entity(p2, 'parentKnowledge', '知识点', 660, 220, [], { reuse: true, note: '上级知识点' });
  connect(p2, 'unitRef', 'covers', 'M', '');
  connect(p2, 'covers', 'knowledge', '', 'N');
  connect(p2, 'knowledge', 'parentRelation', 'N', '');
  connect(p2, 'parentRelation', 'parentKnowledge', '', '1');

  return {
    number: 2,
    slug: 'course-knowledge-chen',
    title: '2 课程与知识体系概念实体联系图',
    description: '教材版本、课程单元、年级、学期和知识点之间的概念关系。',
    panels: [p1, p2],
  };
}

function makeBatch3() {
  const p1 = panel('题目基本结构', 540, '题目由题型分类、难度标定，考查知识点并设置标准答案。');
  entity(p1, 'questionType', '题型', 95, 105, [
    ['typeId', '题型编号', 45, 38, true],
    ['typeName', '题型名称', 150, 38],
    ['typeStatus', '题型状态', 95, 220],
  ]);
  relation(p1, 'classified', '分类', 230, 160);
  entity(p1, 'question', '题目', 365, 235, [
    ['questionId', '题目编号', 315, 70, true],
    ['questionStem', '题干内容', 420, 58],
    ['questionSource', '题目来源', 310, 345],
    ['questionStatus', '题目状态', 420, 355],
  ]);
  entity(p1, 'difficulty', '难度等级', 625, 105, [
    ['difficultyId', '难度编号', 570, 38, true],
    ['difficultyName', '难度名称', 670, 45],
    ['difficultyValue', '难度值', 625, 220],
  ]);
  relation(p1, 'rated', '标定', 500, 160);
  entity(p1, 'knowledgeRef', '知识点', 95, 405, [], { reuse: true, note: '沿用第2图实体' });
  relation(p1, 'examines', '考查', 230, 335, [
    ['knowledgeWeight', '知识点权重', 175, 455],
    ['primaryKnowledge', '是否主要', 285, 465],
  ]);
  relation(p1, 'hasAnswer', '设有', 450, 430);
  entity(p1, 'answer', '题目答案', 620, 430, [
    ['answerId', '答案编号', 565, 505, true],
    ['answerContent', '答案内容', 670, 505],
    ['answerRule', '判定规则', 620, 365],
  ]);
  connect(p1, 'questionType', 'classified', '1', '');
  connect(p1, 'classified', 'question', '', 'N');
  connect(p1, 'difficulty', 'rated', '1', '');
  connect(p1, 'rated', 'question', '', 'N');
  connect(p1, 'knowledgeRef', 'examines', 'N', '');
  connect(p1, 'examines', 'question', '', 'M');
  connect(p1, 'question', 'hasAnswer', '1', '');
  connect(p1, 'hasAnswer', 'answer', '', 'N');

  const p2 = panel('题目适用范围与出题规则', 560, '题目适用于教材版本，出题规则约束知识点、题型和难度等级。');
  entity(p2, 'questionRef', '题目', 85, 120, [], { reuse: true, note: '沿用上图实体' });
  relation(p2, 'applicable', '适用', 260, 120, [
    ['applicableGrade', '适用年级', 225, 45],
    ['applicableTerm', '适用学期', 335, 45],
  ]);
  entity(p2, 'textbookRef', '教材版本', 540, 120, [], { reuse: true, note: '沿用第2图实体' });
  entity(p2, 'rule', '出题规则', 360, 345, [
    ['ruleId', '规则编号', 360, 245, true],
    ['ruleName', '规则名称', 260, 275],
    ['questionCount', '题目数量', 460, 275],
    ['ruleStatus', '规则状态', 360, 455],
  ]);
  entity(p2, 'knowledgeRule', '知识点', 80, 470, [], { reuse: true });
  relation(p2, 'constrainsKnowledge', '约束', 220, 405, [
    ['ruleWeight', '选题权重', 190, 510],
  ]);
  entity(p2, 'typeRule', '题型', 630, 455, [], { reuse: true });
  relation(p2, 'usesType', '采用', 505, 405);
  entity(p2, 'difficultyRule', '难度等级', 630, 310, [], { reuse: true });
  relation(p2, 'limitsDifficulty', '限定', 505, 330);
  connect(p2, 'questionRef', 'applicable', 'M', '');
  connect(p2, 'applicable', 'textbookRef', '', 'N');
  connect(p2, 'rule', 'constrainsKnowledge', 'M', '');
  connect(p2, 'constrainsKnowledge', 'knowledgeRule', '', 'N');
  connect(p2, 'rule', 'usesType', 'M', '');
  connect(p2, 'usesType', 'typeRule', '', 'N');
  connect(p2, 'rule', 'limitsDifficulty', 'M', '');
  connect(p2, 'limitsDifficulty', 'difficultyRule', '', 'N');

  return {
    number: 3,
    slug: 'question-bank-rules-chen',
    title: '3 题库与出题规则概念实体联系图',
    description: '题目、题型、知识点、答案、难度、教材适用范围和出题规则之间的概念关系。',
    panels: [p1, p2],
  };
}

function makeBatch4() {
  const p1 = panel('练习与在线作答', 570, '学生开始练习单，练习单包含练习题，学生对练习题提交作答记录。');
  entity(p1, 'studentRef', '学生', 70, 130, [], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'startsPractice', '开始', 185, 130);
  entity(p1, 'practice', '练习单', 300, 130, [
    ['practiceId', '练习编号', 255, 45, true],
    ['practiceType', '练习类型', 355, 45],
    ['practiceTime', '开始时间', 300, 245],
    ['practiceStatus', '练习状态', 405, 230],
  ]);
  relation(p1, 'containsItem', '包含', 425, 130);
  entity(p1, 'practiceItem', '练习题', 545, 130, [
    ['itemId', '练习题编号', 535, 45, true],
    ['itemOrder', '题目顺序', 640, 65],
    ['itemScore', '题目分值', 640, 215],
  ]);
  relation(p1, 'referencesQuestion', '引用', 625, 310);
  entity(p1, 'questionRef', '题目', 625, 420, [], { reuse: true, note: '沿用第3图实体' });
  entity(p1, 'answerRecord', '作答记录', 300, 430, [
    ['recordId', '作答编号', 225, 520, true],
    ['answerText', '学生答案', 330, 520],
    ['answerStart', '答题开始', 415, 485],
    ['answerDuration', '答题用时', 180, 450],
    ['answerSource', '作答来源', 300, 335],
  ]);
  relation(p1, 'studentAnswers', '作答', 170, 335);
  relation(p1, 'producesRecord', '产生', 430, 330);
  connect(p1, 'studentRef', 'startsPractice', '1', '');
  connect(p1, 'startsPractice', 'practice', '', 'N');
  connect(p1, 'practice', 'containsItem', '1', '');
  connect(p1, 'containsItem', 'practiceItem', '', 'N');
  connect(p1, 'practiceItem', 'referencesQuestion', 'N', '');
  connect(p1, 'referencesQuestion', 'questionRef', '', '1');
  connect(p1, 'studentRef', 'studentAnswers', '1', '');
  connect(p1, 'studentAnswers', 'answerRecord', '', 'N');
  connect(p1, 'practiceItem', 'producesRecord', '1', '');
  connect(p1, 'producesRecord', 'answerRecord', '', 'N');

  const p2 = panel('拍照识别与可信批改', 650, '拍照任务切分为作答记录，程序生成批改结果，不确定结果由教师复核。');
  entity(p2, 'studentPhoto', '学生', 70, 120, [], { reuse: true });
  relation(p2, 'submitsPhoto', '提交', 175, 120);
  entity(p2, 'photoTask', '拍照任务', 285, 120, [
    ['photoId', '任务编号', 250, 40, true],
    ['imageAddress', '图片地址', 355, 40],
    ['uploadTime', '上传时间', 235, 225],
    ['photoStatus', '识别状态', 340, 225],
  ]);
  relation(p2, 'splits', '切分', 410, 120);
  entity(p2, 'answerRef', '作答记录', 540, 120, [], { reuse: true, note: '沿用上图实体' });
  relation(p2, 'getsResult', '得到', 540, 265);
  entity(p2, 'grading', '批改结果', 540, 390, [
    ['gradingId', '批改编号', 540, 585, true],
    ['correctness', '是否正确', 650, 555],
    ['standardAnswer', '标准答案', 650, 475],
    ['confidence', '可信程度', 650, 365],
    ['confirmStatus', '确认状态', 455, 330],
    ['gradingMethod', '批改方式', 650, 280],
  ]);
  entity(p2, 'teacherRef', '教师', 70, 500, [], { reuse: true, note: '沿用第1图实体' });
  relation(p2, 'performsReview', '执行', 190, 500);
  entity(p2, 'review', '复核记录', 290, 500, [
    ['reviewId', '复核编号', 210, 600, true],
    ['reviewConclusion', '复核结论', 330, 615],
    ['reviewTime', '复核时间', 275, 410],
    ['reviewNote', '复核说明', 155, 405],
  ]);
  relation(p2, 'acceptsReview', '接受', 410, 500);
  connect(p2, 'studentPhoto', 'submitsPhoto', '1', '');
  connect(p2, 'submitsPhoto', 'photoTask', '', 'N');
  connect(p2, 'photoTask', 'splits', '1', '');
  connect(p2, 'splits', 'answerRef', '', 'N');
  connect(p2, 'answerRef', 'getsResult', '1', '');
  connect(p2, 'getsResult', 'grading', '', '1');
  connect(p2, 'teacherRef', 'performsReview', '1', '');
  connect(p2, 'performsReview', 'review', '', 'N');
  connect(p2, 'review', 'acceptsReview', 'N', '');
  connect(p2, 'acceptsReview', 'grading', '', '1');

  return {
    number: 4,
    slug: 'practice-grading-chen',
    title: '4 练习、作答与可信批改概念实体联系图',
    description: '学生练习、在线作答、拍照识别、程序批改与教师复核之间的概念关系。',
    panels: [p1, p2],
  };
}

function makeBatch5() {
  const p1 = panel('错因诊断与掌握度更新', 590, '批改结果产生错因记录，错因与作答结果共同更新学生的知识点掌握度。');
  entity(p1, 'gradingRef', '批改结果', 80, 125, [], { reuse: true, note: '沿用第4图实体' });
  relation(p1, 'producesError', '产生', 200, 125);
  entity(p1, 'errorRecord', '错因记录', 325, 125, [
    ['errorRecordId', '错因记录编号', 275, 40, true],
    ['errorPosition', '错误位置', 385, 40],
    ['errorExplanation', '错因说明', 280, 240],
    ['errorConfidence', '可信程度', 390, 240],
  ]);
  relation(p1, 'categorized', '归类', 455, 125);
  entity(p1, 'errorCategory', '错因分类', 600, 125, [
    ['errorCategoryId', '错因编号', 555, 40, true],
    ['errorCategoryName', '错因名称', 665, 40],
    ['errorLevel', '错误层级', 555, 235],
    ['errorStatus', '分类状态', 665, 235],
  ]);
  relation(p1, 'updatesMastery', '更新', 330, 330, [
    ['masteryChange', '变化值', 250, 330],
    ['masteryUpdateTime', '更新时间', 410, 330],
  ]);
  entity(p1, 'studentRef', '学生', 80, 470, [], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'ownsMastery', '拥有', 205, 470);
  entity(p1, 'mastery', '掌握度记录', 355, 470, [
    ['masteryId', '掌握度编号', 300, 555, true],
    ['masteryLevel', '掌握程度', 420, 555],
    ['masteryStability', '稳定程度', 470, 415],
    ['answerSpeed', '答题速度', 355, 380],
    ['lastPractice', '最近练习时间', 220, 400],
  ]);
  relation(p1, 'correspondsKnowledge', '对应', 515, 470);
  entity(p1, 'knowledgeRef', '知识点', 650, 470, [], { reuse: true, note: '沿用第2图实体' });
  connect(p1, 'gradingRef', 'producesError', '1', '');
  connect(p1, 'producesError', 'errorRecord', '', 'N');
  connect(p1, 'errorCategory', 'categorized', '1', '');
  connect(p1, 'categorized', 'errorRecord', '', 'N');
  connect(p1, 'errorRecord', 'updatesMastery', 'M', '');
  connect(p1, 'updatesMastery', 'mastery', '', 'N');
  connect(p1, 'studentRef', 'ownsMastery', '1', '');
  connect(p1, 'ownsMastery', 'mastery', '', 'N');
  connect(p1, 'knowledgeRef', 'correspondsKnowledge', '1', '');
  connect(p1, 'correspondsKnowledge', 'mastery', '', 'N');

  const p2 = panel('个性化推荐', 540, '学生获得推荐计划，推荐项目依据掌握度和错因选择适合的题目。');
  entity(p2, 'studentRecommend', '学生', 75, 120, [], { reuse: true });
  relation(p2, 'getsPlan', '获得', 185, 120);
  entity(p2, 'plan', '推荐计划', 305, 120, [
    ['planId', '计划编号', 265, 40, true],
    ['planReason', '推荐原因', 375, 40],
    ['planTime', '生成时间', 260, 230],
    ['planStatus', '计划状态', 370, 230],
  ]);
  relation(p2, 'containsRecommendation', '包含', 430, 120);
  entity(p2, 'recommendationItem', '推荐项目', 560, 120, [
    ['recommendItemId', '项目编号', 525, 40, true],
    ['recommendOrder', '推荐顺序', 635, 40],
    ['recommendType', '推荐类型', 520, 230],
    ['recommendPriority', '优先级', 635, 230],
  ]);
  entity(p2, 'masteryRef', '掌握度记录', 80, 390, [], { reuse: true, note: '沿用上图实体' });
  relation(p2, 'drivesRecommendation', '驱动', 220, 340);
  entity(p2, 'errorRef', '错因记录', 300, 485, [], { reuse: true });
  relation(p2, 'basisRecommendation', '依据', 420, 410);
  relation(p2, 'pointsQuestion', '指向', 590, 300);
  entity(p2, 'questionRef', '题目', 650, 400, [], { reuse: true, note: '沿用第3图实体' });
  connect(p2, 'studentRecommend', 'getsPlan', '1', '');
  connect(p2, 'getsPlan', 'plan', '', 'N');
  connect(p2, 'plan', 'containsRecommendation', '1', '');
  connect(p2, 'containsRecommendation', 'recommendationItem', '', 'N');
  connect(p2, 'masteryRef', 'drivesRecommendation', 'M', '');
  connect(p2, 'drivesRecommendation', 'recommendationItem', '', 'N');
  connect(p2, 'errorRef', 'basisRecommendation', 'M', '');
  connect(p2, 'basisRecommendation', 'recommendationItem', '', 'N');
  connect(p2, 'recommendationItem', 'pointsQuestion', 'N', '');
  connect(p2, 'pointsQuestion', 'questionRef', '', '1');

  const p3 = panel('再次验证', 420, '学生完成推荐项目后形成验证结果，检查相同错误是否再次出现。');
  entity(p3, 'studentVerify', '学生', 80, 190, [], { reuse: true });
  relation(p3, 'validates', '再次验证', 310, 190, [
    ['verifyResult', '验证结果', 260, 80],
    ['verifyTime', '验证时间', 370, 80],
    ['verifyStatus', '验证状态', 310, 305],
  ]);
  entity(p3, 'recommendationVerify', '推荐项目', 540, 190, [], { reuse: true, note: '沿用上图实体' });
  relation(p3, 'verifyPointsQuestion', '指向', 585, 305);
  entity(p3, 'verifyQuestion', '题目', 650, 375, [], { reuse: true });
  connect(p3, 'studentVerify', 'validates', '1', '');
  connect(p3, 'validates', 'recommendationVerify', '', 'N');
  connect(p3, 'recommendationVerify', 'verifyPointsQuestion', 'N', '');
  connect(p3, 'verifyPointsQuestion', 'verifyQuestion', '', '1');

  return {
    number: 5,
    slug: 'diagnosis-mastery-recommendation-chen',
    title: '5 错因、掌握度与个性化推荐概念实体联系图',
    description: '错因记录、掌握度更新、推荐计划、推荐项目和再次验证之间的概念关系。',
    panels: [p1, p2, p3],
  };
}

function makeBatch6() {
  const p1 = panel('作业编制与发布', 620, '教师创建作业，作业包含题目并发布为面向班级或学生的作业任务。');
  entity(p1, 'teacherRef', '教师', 70, 120, [], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'createsAssignment', '创建', 180, 120);
  entity(p1, 'assignment', '作业', 300, 120, [
    ['assignmentId', '作业编号', 255, 40, true],
    ['assignmentTitle', '作业标题', 365, 40],
    ['assignmentType', '作业类型', 250, 230],
    ['assignmentStatus', '作业状态', 365, 230],
  ]);
  relation(p1, 'containsAssignmentItem', '包含', 420, 120);
  entity(p1, 'assignmentItem', '作业题', 545, 120, [
    ['assignmentItemId', '作业题编号', 520, 40, true],
    ['assignmentOrder', '题目顺序', 640, 45],
    ['assignmentScore', '题目分值', 640, 205],
  ]);
  relation(p1, 'assignmentReferences', '引用', 625, 305);
  entity(p1, 'questionRef', '题目', 625, 410, [], { reuse: true, note: '沿用第3图实体' });
  relation(p1, 'publishesTask', '发布为', 300, 320);
  entity(p1, 'assignmentTask', '作业任务', 300, 460, [
    ['taskId', '任务编号', 250, 555, true],
    ['targetType', '布置类型', 365, 555],
    ['taskStart', '开始时间', 190, 500],
    ['taskDeadline', '截止时间', 410, 500],
    ['taskStatus', '任务状态', 300, 385],
  ]);
  entity(p1, 'classRef', '班级', 70, 500, [], { reuse: true });
  relation(p1, 'assignedClass', '布置', 180, 460);
  entity(p1, 'studentRef', '学生', 575, 540, [], { reuse: true });
  relation(p1, 'assignedStudent', '指定', 450, 460);
  connect(p1, 'teacherRef', 'createsAssignment', '1', '');
  connect(p1, 'createsAssignment', 'assignment', '', 'N');
  connect(p1, 'assignment', 'containsAssignmentItem', '1', '');
  connect(p1, 'containsAssignmentItem', 'assignmentItem', '', 'N');
  connect(p1, 'assignmentItem', 'assignmentReferences', 'N', '');
  connect(p1, 'assignmentReferences', 'questionRef', '', '1');
  connect(p1, 'assignment', 'publishesTask', '1', '');
  connect(p1, 'publishesTask', 'assignmentTask', '', 'N');
  connect(p1, 'classRef', 'assignedClass', 'M', '');
  connect(p1, 'assignedClass', 'assignmentTask', '', 'N');
  connect(p1, 'studentRef', 'assignedStudent', 'M', '');
  connect(p1, 'assignedStudent', 'assignmentTask', '', 'N');

  const p2 = panel('作业提交', 420, '学生针对作业任务提交作业，系统记录提交状态、得分和正确率。');
  entity(p2, 'studentSubmit', '学生', 70, 180, [], { reuse: true });
  relation(p2, 'submitsAssignment', '提交', 200, 180);
  entity(p2, 'submission', '作业提交', 360, 180, [
    ['submissionId', '提交编号', 310, 65, true],
    ['submitTime', '提交时间', 420, 65],
    ['submissionStatus', '提交状态', 285, 310],
    ['submissionScore', '作业得分', 405, 320],
    ['correctRate', '正确率', 505, 260],
  ]);
  relation(p2, 'taskReceives', '接收', 525, 180);
  entity(p2, 'taskRef', '作业任务', 660, 180, [], { reuse: true, note: '沿用上图实体' });
  connect(p2, 'studentSubmit', 'submitsAssignment', '1', '');
  connect(p2, 'submitsAssignment', 'submission', '', 'N');
  connect(p2, 'taskRef', 'taskReceives', '1', '');
  connect(p2, 'taskReceives', 'submission', '', 'N');

  const p3 = panel('训练计划', 430, '教师制定训练计划，学生按照计划完成针对性训练。');
  entity(p3, 'teacherPlan', '教师', 70, 190, [], { reuse: true });
  relation(p3, 'makesPlan', '制定', 210, 190);
  entity(p3, 'trainingPlan', '训练计划', 370, 190, [
    ['trainingPlanId', '计划编号', 315, 65, true],
    ['trainingScope', '训练范围', 430, 65],
    ['trainingPeriod', '计划周期', 315, 320],
    ['trainingStatus', '计划状态', 435, 320],
  ]);
  relation(p3, 'studentExecutes', '执行', 525, 190);
  entity(p3, 'studentPlan', '学生', 660, 190, [], { reuse: true });
  connect(p3, 'teacherPlan', 'makesPlan', '1', '');
  connect(p3, 'makesPlan', 'trainingPlan', '', 'N');
  connect(p3, 'trainingPlan', 'studentExecutes', 'M', '');
  connect(p3, 'studentExecutes', 'studentPlan', '', 'N');

  const p4 = panel('学习报告', 440, '学生形成阶段性学习报告，教师在权限范围内查看和导出报告。');
  entity(p4, 'studentReport', '学生', 70, 200, [], { reuse: true });
  relation(p4, 'formsReport', '形成', 210, 200);
  entity(p4, 'report', '学习报告', 370, 200, [
    ['reportId', '报告编号', 315, 70, true],
    ['reportType', '报告类型', 430, 70],
    ['reportPeriod', '统计周期', 315, 330],
    ['reportTime', '生成时间', 435, 330],
  ]);
  relation(p4, 'viewsReport', '查看', 525, 200);
  entity(p4, 'teacherReport', '教师', 660, 200, [], { reuse: true });
  connect(p4, 'studentReport', 'formsReport', '1', '');
  connect(p4, 'formsReport', 'report', '', 'N');
  connect(p4, 'teacherReport', 'viewsReport', 'M', '');
  connect(p4, 'viewsReport', 'report', '', 'N');

  return {
    number: 6,
    slug: 'assignment-report-chen',
    title: '6 教师作业与学习报告概念实体联系图',
    description: '作业编制、发布、提交、训练计划和学习报告之间的概念关系。',
    panels: [p1, p2, p3, p4],
  };
}

function makeBatch7() {
  const p1 = panel('积分、经验与等级', 550, '学生拥有成长账户，积分规则生成流水，账户累计经验并达到等级。');
  entity(p1, 'studentGrowth', '学生', 75, 180, [], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'ownsGrowth', '拥有', 190, 180);
  entity(p1, 'growthAccount', '成长账户', 320, 180, [
    ['growthId', '账户编号', 265, 65, true],
    ['pointsBalance', '积分余额', 380, 65],
    ['experienceValue', '经验值', 270, 300],
    ['continuousDays', '连续学习天数', 400, 310],
  ]);
  relation(p1, 'producesTransaction', '产生', 455, 120);
  entity(p1, 'pointsTransaction', '积分流水', 610, 100, [
    ['transactionId', '流水编号', 555, 35, true],
    ['pointsChange', '积分变动', 670, 35],
    ['transactionType', '变动类型', 560, 205],
    ['transactionTime', '变动时间', 670, 205],
  ]);
  relation(p1, 'reachesLevel', '达到', 465, 300);
  entity(p1, 'level', '等级', 620, 330, [
    ['levelId', '等级编号', 565, 260, true],
    ['levelName', '等级名称', 675, 260],
    ['minimumExperience', '最低经验', 560, 440],
    ['maximumExperience', '最高经验', 675, 440],
  ]);
  entity(p1, 'pointsRule', '积分规则', 255, 465, [
    ['pointsRuleId', '规则编号', 160, 515, true],
    ['rewardScene', '奖励场景', 260, 530],
    ['rewardValue', '奖励数值', 365, 515],
    ['pointsRuleStatus', '规则状态', 255, 385],
  ]);
  relation(p1, 'ruleCreatesTransaction', '生成', 455, 465);
  connect(p1, 'studentGrowth', 'ownsGrowth', '1', '');
  connect(p1, 'ownsGrowth', 'growthAccount', '', '1');
  connect(p1, 'growthAccount', 'producesTransaction', '1', '');
  connect(p1, 'producesTransaction', 'pointsTransaction', '', 'N');
  connect(p1, 'growthAccount', 'reachesLevel', 'N', '');
  connect(p1, 'reachesLevel', 'level', '', '1');
  connect(p1, 'pointsRule', 'ruleCreatesTransaction', '1', '');
  connect(p1, 'ruleCreatesTransaction', 'pointsTransaction', '', 'N');

  const p2 = panel('徽章与成就', 520, '学生可以获得徽章并解锁成就，联系记录获得或解锁时间。');
  entity(p2, 'studentReward', '学生', 80, 260, [], { reuse: true });
  relation(p2, 'getsBadge', '获得', 260, 130, [
    ['badgeTime', '获得时间', 205, 50],
    ['badgeStatus', '佩戴状态', 315, 50],
  ]);
  entity(p2, 'badge', '徽章', 500, 120, [
    ['badgeId', '徽章编号', 455, 40, true],
    ['badgeName', '徽章名称', 565, 40],
    ['badgeCondition', '获得条件', 500, 230],
  ]);
  relation(p2, 'unlocksAchievement', '解锁', 260, 365, [
    ['achievementTime', '解锁时间', 260, 455],
  ]);
  entity(p2, 'achievement', '成就', 500, 365, [
    ['achievementId', '成就编号', 455, 285, true],
    ['achievementName', '成就名称', 565, 285],
    ['achievementCondition', '达成条件', 500, 475],
  ]);
  connect(p2, 'studentReward', 'getsBadge', 'M', '');
  connect(p2, 'getsBadge', 'badge', '', 'N');
  connect(p2, 'studentReward', 'unlocksAchievement', 'M', '');
  connect(p2, 'unlocksAchievement', 'achievement', '', 'N');

  const p3 = panel('每日任务与排行榜', 590, '学生完成每日任务，并按照统计范围和周期形成排行榜记录。');
  entity(p3, 'studentTaskRank', '学生', 80, 285, [], { reuse: true });
  relation(p3, 'completesTask', '完成', 230, 135, [
    ['taskProgress', '完成进度', 175, 55],
    ['rewardClaimed', '领取状态', 285, 55],
  ]);
  entity(p3, 'dailyTask', '每日任务', 430, 120, [
    ['dailyTaskId', '任务编号', 380, 40, true],
    ['dailyTaskName', '任务名称', 490, 40],
    ['dailyTaskReward', '任务奖励', 430, 230],
  ]);
  relation(p3, 'listed', '上榜', 350, 380, [
    ['rankValue', '名次', 300, 485],
    ['rankScore', '榜单得分', 415, 485],
    ['rankUpdate', '更新时间', 350, 550],
  ]);
  entity(p3, 'leaderboard', '排行榜', 610, 380, [
    ['leaderboardId', '榜单编号', 555, 275, true],
    ['leaderboardType', '榜单类型', 670, 275],
    ['leaderboardScope', '榜单范围', 555, 500],
    ['leaderboardPeriod', '统计周期', 670, 500],
  ]);
  connect(p3, 'studentTaskRank', 'completesTask', 'M', '');
  connect(p3, 'completesTask', 'dailyTask', '', 'N');
  connect(p3, 'studentTaskRank', 'listed', 'M', '');
  connect(p3, 'listed', 'leaderboard', '', 'N');

  return {
    number: 7,
    slug: 'gamification-chen',
    title: '7 游戏化激励概念实体联系图',
    description: '积分、经验、等级、徽章、成就、每日任务和排行榜之间的概念关系。',
    panels: [p1, p2, p3],
  };
}

function makeBatch8() {
  const p1 = panel('匹配与对战房间', 550, '学生提交匹配请求，匹配记录或对战房间创建一对一对局。');
  entity(p1, 'studentDuel', '学生', 75, 160, [], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'submitsMatch', '提交', 195, 115);
  entity(p1, 'matching', '匹配记录', 335, 90, [
    ['matchingId', '匹配编号', 280, 35, true],
    ['matchingMethod', '匹配方式', 395, 35],
    ['matchingTime', '请求时间', 280, 190],
    ['matchingStatus', '匹配状态', 395, 190],
  ]);
  relation(p1, 'createsDuel', '创建', 455, 160);
  entity(p1, 'duel', '一对一对局', 585, 210, [
    ['duelId', '对局编号', 545, 85, true],
    ['duelMode', '对局模式', 655, 85],
    ['duelStatus', '对局状态', 520, 320],
    ['duelStart', '开始时间', 625, 330],
    ['participantLimit', '参赛人数', 675, 250],
  ]);
  entity(p1, 'room', '对战房间', 335, 390, [
    ['roomId', '房间编号', 275, 500, true],
    ['inviteCode', '邀请码', 390, 500],
    ['roomStatus', '房间状态', 335, 290],
  ]);
  relation(p1, 'roomHoldsDuel', '承载', 465, 390);
  connect(p1, 'studentDuel', 'submitsMatch', '1', '');
  connect(p1, 'submitsMatch', 'matching', '', 'N');
  connect(p1, 'matching', 'createsDuel', 'N', '');
  connect(p1, 'createsDuel', 'duel', '', '1');
  connect(p1, 'room', 'roomHoldsDuel', '1', '');
  connect(p1, 'roomHoldsDuel', 'duel', '', 'N');
  
  const p1b = panel('一对一参赛', 410, '学生参加一对一对局，参赛联系记录参赛方、得分与对局结果。');
  entity(p1b, 'studentParticipant', '学生', 80, 200, [], { reuse: true });
  relation(p1b, 'participatesDuel', '参赛', 350, 200, [
    ['playerSide', '参赛方', 255, 85],
    ['duelScore', '对局得分', 350, 65],
    ['duelResult', '对局结果', 450, 85],
  ]);
  entity(p1b, 'duelParticipant', '一对一对局', 630, 200, [], { reuse: true, note: '沿用上图实体' });
  connect(p1b, 'studentParticipant', 'participatesDuel', 'M', '');
  connect(p1b, 'participatesDuel', 'duelParticipant', '', 'N');

  const p2 = panel('对局答题与结算', 600, '对局包含对局题目，学生提交对局作答并形成结算结果。');
  entity(p2, 'duelRef', '对局', 75, 120, [], { reuse: true });
  relation(p2, 'containsBattleQuestion', '包含', 190, 120);
  entity(p2, 'battleQuestion', '对局题目', 320, 120, [
    ['battleQuestionId', '对局题编号', 275, 40, true],
    ['battleQuestionOrder', '题目顺序', 390, 40],
    ['battleQuestionScore', '题目分值', 320, 225],
  ]);
  relation(p2, 'battleReferences', '引用', 455, 120);
  entity(p2, 'questionRef', '题目', 600, 120, [], { reuse: true, note: '沿用第3图实体' });
  entity(p2, 'studentAnswer', '学生', 75, 400, [], { reuse: true });
  relation(p2, 'submitsBattleAnswer', '提交', 205, 400);
  entity(p2, 'battleAnswer', '对局作答', 345, 400, [
    ['battleAnswerId', '作答编号', 285, 520, true],
    ['battleAnswerText', '作答内容', 405, 520],
    ['battleCorrect', '是否正确', 285, 300],
    ['battleDuration', '答题用时', 400, 300],
    ['battleScore', '本题得分', 470, 455],
  ]);
  relation(p2, 'questionReceivesAnswer', '接收', 340, 255);
  entity(p2, 'settlement', '对局结算', 610, 400, [
    ['settlementId', '结算编号', 550, 520, true],
    ['winner', '获胜方', 660, 520],
    ['totalScore', '总得分', 545, 310],
    ['settlementTime', '结算时间', 650, 310],
    ['abnormalStatus', '异常状态', 660, 240],
  ]);
  relation(p2, 'formsSettlement', '形成', 540, 255);
  connect(p2, 'duelRef', 'containsBattleQuestion', '1', '');
  connect(p2, 'containsBattleQuestion', 'battleQuestion', '', 'N');
  connect(p2, 'battleQuestion', 'battleReferences', 'N', '');
  connect(p2, 'battleReferences', 'questionRef', '', '1');
  connect(p2, 'studentAnswer', 'submitsBattleAnswer', '1', '');
  connect(p2, 'submitsBattleAnswer', 'battleAnswer', '', 'N');
  connect(p2, 'battleQuestion', 'questionReceivesAnswer', '1', '');
  connect(p2, 'questionReceivesAnswer', 'battleAnswer', '', 'N');
  connect(p2, 'duelRef', 'formsSettlement', '1', '');
  connect(p2, 'formsSettlement', 'settlement', '', '1');

  const p3 = panel('队伍与组队PK', 620, '学生加入队伍，队伍参加组队对局，系统记录个人贡献。');
  entity(p3, 'studentTeam', '学生', 75, 120, [], { reuse: true });
  relation(p3, 'joinsTeam', '加入', 195, 120, [
    ['memberRole', '成员角色', 180, 40],
    ['joinTeamTime', '加入时间', 285, 45],
    ['memberStatus', '成员状态', 210, 215],
  ]);
  entity(p3, 'team', '队伍', 335, 120, [
    ['teamId', '队伍编号', 330, 35, true],
    ['teamName', '队伍名称', 430, 45],
    ['teamStatus', '队伍状态', 335, 225],
  ]);
  relation(p3, 'teamParticipates', '参赛', 455, 250, [
    ['teamScore', '队伍得分', 535, 210],
    ['teamRank', '队伍名次', 535, 290],
  ]);
  entity(p3, 'teamBattle', '组队对局', 610, 250, [
    ['teamBattleId', '对局编号', 575, 120, true],
    ['teamBattleRule', '竞赛规则', 675, 120],
    ['teamBattleStatus', '对局状态', 610, 365],
  ]);
  entity(p3, 'contribution', '个人贡献', 335, 470, [
    ['contributionId', '贡献编号', 275, 580, true],
    ['contributionScore', '贡献得分', 395, 580],
    ['correctCount', '正确题数', 245, 410],
    ['contributionRank', '贡献排名', 430, 410],
  ]);
  relation(p3, 'studentGetsContribution', '获得', 190, 470);
  relation(p3, 'battleRecordsContribution', '记录', 490, 470);
  connect(p3, 'studentTeam', 'joinsTeam', 'M', '');
  connect(p3, 'joinsTeam', 'team', '', 'N');
  connect(p3, 'team', 'teamParticipates', 'M', '');
  connect(p3, 'teamParticipates', 'teamBattle', '', 'N');
  connect(p3, 'studentTeam', 'studentGetsContribution', '1', '');
  connect(p3, 'studentGetsContribution', 'contribution', '', 'N');
  connect(p3, 'teamBattle', 'battleRecordsContribution', '1', '');
  connect(p3, 'battleRecordsContribution', 'contribution', '', 'N');

  return {
    number: 8,
    slug: 'pk-battle-chen',
    title: '8 一对一与组队PK概念实体联系图',
    description: '匹配、对局、对局题目、实时作答、结算、队伍和个人贡献之间的概念关系。',
    panels: [p1, p1b, p2, p3],
  };
}

function makeBatch9() {
  const p1 = panel('第三方应用与接口授权', 610, '管理员审核第三方应用，应用持有密钥、获得接口授权并产生调用日志。');
  entity(p1, 'adminApp', '管理员', 70, 120, [], { reuse: true, note: '沿用第1图实体' });
  relation(p1, 'reviewsApp', '审核', 180, 120);
  entity(p1, 'application', '第三方应用', 315, 120, [
    ['applicationId', '应用编号', 260, 40, true],
    ['applicationName', '应用名称', 375, 40],
    ['applicationOwner', '应用主体', 250, 230],
    ['applicationStatus', '应用状态', 375, 230],
  ]);
  relation(p1, 'holdsKey', '持有', 445, 90);
  entity(p1, 'applicationKey', '应用密钥', 600, 80, [
    ['keyId', '密钥编号', 550, 30, true],
    ['keyDigest', '密钥摘要', 660, 30],
    ['keyExpiry', '到期时间', 550, 180],
    ['keyStatus', '密钥状态', 660, 180],
  ]);
  relation(p1, 'authorized', '授权', 455, 300, [
    ['authorizedQuota', '调用额度', 365, 300],
    ['rateLimit', '限流参数', 455, 215],
    ['grantTime', '授权时间', 545, 300],
    ['authorizationStatus', '授权状态', 455, 390],
  ]);
  entity(p1, 'interface', '开放接口', 620, 300, [
    ['interfaceId', '接口编号', 570, 230, true],
    ['interfaceName', '接口名称', 675, 230],
    ['interfacePath', '接口路径', 570, 405],
    ['interfaceStatus', '接口状态', 675, 405],
  ]);
  entity(p1, 'callLog', '调用日志', 315, 500, [
    ['callLogId', '日志编号', 250, 585, true],
    ['callTime', '调用时间', 365, 585],
    ['callResult', '调用结果', 250, 400],
    ['callDuration', '调用耗时', 370, 400],
    ['errorCode', '错误代码', 475, 585],
  ]);
  relation(p1, 'producesCallLog', '产生', 185, 450);
  relation(p1, 'recordsCall', '记录', 500, 500);
  connect(p1, 'adminApp', 'reviewsApp', '1', '');
  connect(p1, 'reviewsApp', 'application', '', 'N');
  connect(p1, 'application', 'holdsKey', '1', '');
  connect(p1, 'holdsKey', 'applicationKey', '', 'N');
  connect(p1, 'application', 'authorized', 'M', '');
  connect(p1, 'authorized', 'interface', '', 'N');
  connect(p1, 'application', 'producesCallLog', '1', '');
  connect(p1, 'producesCallLog', 'callLog', '', 'N');
  connect(p1, 'interface', 'recordsCall', '1', '');
  connect(p1, 'recordsCall', 'callLog', '', 'N');

  const p2 = panel('智能服务、审计与异常告警', 650, '管理员配置智能服务，管理操作形成审计日志，智能服务和调用日志触发异常告警。');
  entity(p2, 'adminService', '管理员', 70, 120, [], { reuse: true });
  relation(p2, 'configuresService', '配置', 185, 120);
  entity(p2, 'smartService', '智能服务', 315, 120, [
    ['serviceId', '服务编号', 260, 40, true],
    ['serviceType', '服务类型', 375, 40],
    ['providerName', '服务提供方', 250, 230],
    ['serviceStatus', '服务状态', 375, 230],
  ]);
  relation(p2, 'hasConfiguration', '具有', 445, 120);
  entity(p2, 'serviceConfiguration', '服务配置', 610, 120, [
    ['configurationId', '配置编号', 555, 40, true],
    ['modelName', '模型名称', 670, 40],
    ['serviceQuota', '服务额度', 555, 230],
    ['timeoutValue', '超时时间', 670, 230],
  ]);
  entity(p2, 'auditLog', '审计日志', 315, 440, [
    ['auditLogId', '审计编号', 250, 585, true],
    ['operationType', '操作类型', 365, 585],
    ['operationTarget', '操作对象', 230, 350],
    ['operationTime', '操作时间', 335, 330],
    ['operationResult', '操作结果', 445, 350],
  ]);
  relation(p2, 'producesAudit', '产生', 180, 440);
  entity(p2, 'callLogRef', '调用日志', 70, 560, [], { reuse: true, note: '沿用上图实体' });
  entity(p2, 'alarm', '异常告警', 610, 440, [
    ['alarmId', '告警编号', 555, 550, true],
    ['alarmType', '告警类型', 670, 550],
    ['alarmLevel', '告警级别', 540, 350],
    ['alarmTime', '告警时间', 650, 350],
    ['alarmStatus', '处理状态', 610, 615],
  ]);
  relation(p2, 'serviceTriggersAlarm', '触发', 485, 300);
  relation(p2, 'logTriggersAlarm', '触发', 360, 540);
  connect(p2, 'adminService', 'configuresService', '1', '');
  connect(p2, 'configuresService', 'smartService', '', 'N');
  connect(p2, 'smartService', 'hasConfiguration', '1', '');
  connect(p2, 'hasConfiguration', 'serviceConfiguration', '', 'N');
  connect(p2, 'adminService', 'producesAudit', '1', '');
  connect(p2, 'producesAudit', 'auditLog', '', 'N');
  connect(p2, 'smartService', 'serviceTriggersAlarm', '1', '');
  connect(p2, 'serviceTriggersAlarm', 'alarm', '', 'N');
  connect(p2, 'callLogRef', 'logTriggersAlarm', '1', '');
  connect(p2, 'logTriggersAlarm', 'alarm', '', 'N');

  return {
    number: 9,
    slug: 'open-platform-audit-chen',
    title: '9 开放平台与运行审计概念实体联系图',
    description: '第三方应用、密钥、接口授权、调用日志、智能服务、审计与异常告警之间的概念关系。',
    panels: [p1, p2],
  };
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function nodeById(panelSpec, id) {
  const result = panelSpec.nodes.find((item) => item.id === id);
  if (!result) throw new Error(`Missing node ${id} in panel ${panelSpec.title}`);
  return result;
}

function attributeRadius(node) {
  return Math.max(43, Math.min(78, node.label.length * 8.5 + 15));
}

function nodeExtent(node, unitX, unitY) {
  if (node.type === 'entity') {
    return 1 / Math.max(Math.abs(unitX) / 58, Math.abs(unitY) / 22);
  }
  if (node.type === 'relation') {
    return 1 / (Math.abs(unitX) / 52 + Math.abs(unitY) / 23);
  }
  const radiusX = attributeRadius(node);
  return 1 / Math.sqrt((unitX * unitX) / (radiusX * radiusX) + (unitY * unitY) / (19 * 19));
}

function cardinalityPosition(from, to, nearFrom, distance = 12) {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const length = Math.hypot(dx, dy) || 1;
  const unitX = dx / length;
  const unitY = dy / length;
  const fromDistance = nodeExtent(from, unitX, unitY) + 16;
  const toDistance = length - nodeExtent(to, -unitX, -unitY) - 16;
  const along = nearFrom ? fromDistance : toDistance;
  return {
    x: from.x + unitX * along + unitY * distance,
    y: from.y + unitY * along - unitX * distance,
  };
}

function renderEdge(panelSpec, edge) {
  const from = nodeById(panelSpec, edge.from);
  const to = nodeById(panelSpec, edge.to);
  let markup = `<line class="link" x1="${from.x}" y1="${from.y}" x2="${to.x}" y2="${to.y}" />`;
  if (edge.fromCardinality) {
    const point = cardinalityPosition(from, to, true, edge.cardinalityOffset || 12);
    markup += `<text class="cardinality" x="${point.x.toFixed(1)}" y="${point.y.toFixed(1)}">${escapeHtml(edge.fromCardinality)}</text>`;
  }
  if (edge.toCardinality) {
    const point = cardinalityPosition(from, to, false, edge.cardinalityOffset || 12);
    markup += `<text class="cardinality" x="${point.x.toFixed(1)}" y="${point.y.toFixed(1)}">${escapeHtml(edge.toCardinality)}</text>`;
  }
  return markup;
}

function renderNode(node) {
  const label = escapeHtml(node.label);
  if (node.type === 'entity') {
    const note = node.note ? `<text class="panel-note" x="${node.x}" y="${node.y + 48}">${escapeHtml(node.note)}</text>` : '';
    return `<rect class="entity${node.reuse ? ' reuse' : ''}" x="${node.x - 58}" y="${node.y - 22}" width="116" height="44" />
      <text class="node-label" x="${node.x}" y="${node.y}">${label}</text>${note}`;
  }
  if (node.type === 'relation') {
    return `<polygon class="relation" points="${node.x},${node.y - 23} ${node.x + 52},${node.y} ${node.x},${node.y + 23} ${node.x - 52},${node.y}" />
      <text class="node-label" x="${node.x}" y="${node.y}">${label}</text>`;
  }
  const radiusX = attributeRadius(node);
  return `<ellipse class="attribute" cx="${node.x}" cy="${node.y}" rx="${radiusX}" ry="19" />
      <text class="${node.key ? 'key-label' : ''}" x="${node.x}" y="${node.y}">${label}</text>`;
}

function renderPanel(panelSpec) {
  const edges = panelSpec.edges.map((edge) => renderEdge(panelSpec, edge)).join('\n      ');
  const nodes = panelSpec.nodes.map(renderNode).join('\n      ');
  return `<h3>${escapeHtml(panelSpec.title)}</h3>
    <svg class="diagram" viewBox="0 0 ${panelSpec.width} ${panelSpec.height}" role="img" aria-label="${escapeHtml(panelSpec.aria)}">
      <title>${escapeHtml(panelSpec.title)}</title>
      <desc>${escapeHtml(panelSpec.aria)}</desc>
      <g>${edges}</g>
      <g>${nodes}</g>
    </svg>`;
}

function buildFragment(batch) {
  const rootId = `er-${batch.slug}`;
  const panels = batch.panels.map(renderPanel).join('\n  ');
  return `<div id="${rootId}" class="chen-er-root">
  <style>
    #${rootId} {
      color: var(--foreground);
      font-family: system-ui, "Microsoft YaHei", sans-serif;
      width: 100%;
    }
    #${rootId} h2,
    #${rootId} h3 {
      text-align: center;
      font-weight: 500;
    }
    #${rootId} h2 { margin: 0 0 10px; }
    #${rootId} h3 { margin: 22px 0 4px; }
    #${rootId} .legend {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      align-items: center;
      gap: 16px;
      margin-bottom: 8px;
    }
    #${rootId} .legend-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    #${rootId} .legend svg { width: 42px; height: 28px; }
    #${rootId} .diagram {
      display: block;
      width: 100%;
      max-width: 720px;
      height: auto;
      margin: 0 auto;
      overflow: visible;
    }
    #${rootId} .entity {
      fill: color-mix(in srgb, var(--viz-series-1) 14%, var(--background));
      stroke: var(--viz-series-1);
      stroke-width: 2;
      vector-effect: non-scaling-stroke;
    }
    #${rootId} .relation {
      fill: color-mix(in srgb, var(--viz-series-2) 16%, var(--background));
      stroke: var(--viz-series-2);
      stroke-width: 2;
      vector-effect: non-scaling-stroke;
    }
    #${rootId} .attribute {
      fill: color-mix(in srgb, var(--muted) 72%, var(--background));
      stroke: var(--border);
      stroke-width: 1.5;
      vector-effect: non-scaling-stroke;
    }
    #${rootId} .link {
      fill: none;
      stroke: var(--foreground);
      stroke-width: 1.35;
      opacity: 0.75;
      vector-effect: non-scaling-stroke;
    }
    #${rootId} text {
      fill: var(--foreground);
      font-size: 16px;
      text-anchor: middle;
      dominant-baseline: middle;
    }
    #${rootId} .node-label { font-weight: 500; }
    #${rootId} .cardinality {
      font-size: 14px;
      font-weight: 500;
      paint-order: stroke;
      stroke: var(--background);
      stroke-width: 5px;
      stroke-linejoin: round;
    }
    #${rootId} .key-label { text-decoration: underline; }
    #${rootId} .reuse { stroke-dasharray: 5 4; }
    #${rootId} .panel-note {
      fill: var(--muted-foreground);
      font-size: 14px;
    }
  </style>

  <h2>${escapeHtml(batch.title)}</h2>
  <div class="legend" aria-label="图例">
    <span class="legend-item"><svg viewBox="0 0 42 28" aria-hidden="true"><rect class="entity" x="4" y="5" width="34" height="18" /></svg><span>实体</span></span>
    <span class="legend-item"><svg viewBox="0 0 42 28" aria-hidden="true"><polygon class="relation" points="21,3 39,14 21,25 3,14" /></svg><span>联系</span></span>
    <span class="legend-item"><svg viewBox="0 0 42 28" aria-hidden="true"><ellipse class="attribute" cx="21" cy="14" rx="18" ry="10" /></svg><span>属性</span></span>
    <span class="legend-item"><span class="key-label">带下划线的属性</span><span>为主键</span></span>
  </div>
  ${panels}
</div>
`;
}

async function renderBatch(browser, batch) {
  const htmlPath = path.join(outputDir, `${batch.slug}.html`);
  const pngPath = path.join(outputDir, `${batch.slug}.png`);
  fs.writeFileSync(htmlPath, buildFragment(batch), 'utf8');
  const sourceUrl = `file:///${htmlPath.replaceAll('\\', '/')}`;
  const themes = {
    light: {
      background: '#ffffff', foreground: '#1f2937', mutedForeground: '#64748b', muted: '#e5e7eb', border: '#94a3b8', series1: '#2563eb', series2: '#ea580c',
    },
    dark: {
      background: '#171717', foreground: '#f1f5f9', mutedForeground: '#a8b0bc', muted: '#2b2b2b', border: '#3f3f46', series1: '#60a5fa', series2: '#fb923c',
    },
  };

  for (const [themeName, theme] of Object.entries(themes)) {
    for (const width of [736, 360]) {
      const page = await browser.newPage({ viewport: { width, height: 900 }, deviceScaleFactor: 1 });
      await page.emulateMedia({ colorScheme: themeName, reducedMotion: 'reduce' });
      await page.goto(sourceUrl, { waitUntil: 'load' });
      await page.addStyleTag({ content: `:root {
        --background: ${theme.background};
        --foreground: ${theme.foreground};
        --muted-foreground: ${theme.mutedForeground};
        --muted: ${theme.muted};
        --border: ${theme.border};
        --viz-series-1: ${theme.series1};
        --viz-series-2: ${theme.series2};
      }
      html, body { background: ${theme.background}; }
      body { margin: 8px; }
      ` });
      await page.screenshot({
        path: path.join('E:\\graduation-project\\.codex-tmp', `${batch.slug}-${themeName}-${width}.png`),
        fullPage: true,
      });
      await page.close();
    }
  }

  const exportPage = await browser.newPage({ viewport: { width: 736, height: 900 }, deviceScaleFactor: 2 });
  await exportPage.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' });
  await exportPage.goto(sourceUrl, { waitUntil: 'load' });
  await exportPage.addStyleTag({ content: `:root {
    --background: #ffffff;
    --foreground: #1f2937;
    --muted-foreground: #64748b;
    --muted: #e5e7eb;
    --border: #94a3b8;
    --viz-series-1: #2563eb;
    --viz-series-2: #ea580c;
  }
  html, body { background: #ffffff; }
  body { margin: 8px; }
  ` });
  await exportPage.screenshot({ path: pngPath, fullPage: true });
  await exportPage.close();
  return { htmlPath, pngPath };
}

(async () => {
  const batches = [makeBatch2(), makeBatch3(), makeBatch4(), makeBatch5(), makeBatch6(), makeBatch7(), makeBatch8(), makeBatch9()];
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  });
  const outputs = [];
  for (const batch of batches) outputs.push(await renderBatch(browser, batch));
  await browser.close();
  process.stdout.write(JSON.stringify(outputs, null, 2));
})();
