<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import BrandLogo from '@/components/BrandLogo.vue'

const capabilities = [
  {
    icon: 'target',
    index: '01',
    title: '个性化练习',
    description: '结合近期错题、知识点掌握度与复习间隔，为每位学生组织更合适的下一组练习。',
    tone: 'blue',
  },
  {
    icon: 'camera',
    index: '02',
    title: '拍照辅助批改',
    description: '从作业图片到识别、可信度分流与人工复核，让纸笔练习也能进入数字学习档案。',
    tone: 'violet',
  },
  {
    icon: 'users',
    index: '03',
    title: '双人实时 PK',
    description: '支持好友房与随机匹配，把同题限时挑战的结果可靠汇入错题与掌握度分析。',
    tone: 'orange',
  },
  {
    icon: 'chart',
    index: '04',
    title: '学情洞察',
    description: '从个人到班级呈现正确率、薄弱知识点与学习趋势，帮助教师快速发现真正的问题。',
    tone: 'green',
  },
]

const audiences = [
  {
    label: '学生',
    icon: 'book',
    title: '知道哪里需要再练一次',
    points: ['自主练习与教师作业', '错题、掌握度与巩固推荐', '拍照批改与双人 PK'],
  },
  {
    label: '教师',
    icon: 'edit',
    title: '从数据中看见真实学情',
    points: ['班级与分层作业管理', '薄弱知识点和异常提醒', '教学 Agent 决策辅助'],
  },
  {
    label: '管理员',
    icon: 'shield',
    title: '让内容与平台运行更可信',
    points: ['题库、知识点与错因维护', 'OCR 与内容质量复核', '运营指标和全局审计'],
  },
]

const technologies = [
  { label: 'Web', value: 'Vue 3 · TypeScript' },
  { label: 'Core', value: 'Java 21 · Spring Boot 4' },
  { label: 'AI', value: 'FastAPI · RAG · Agent' },
  { label: 'Data', value: 'PostgreSQL · Redis · RabbitMQ' },
]
</script>

<template>
  <div class="official-page">
    <header class="site-header">
      <div class="site-container site-header__inner">
        <RouterLink class="site-brand" to="/" aria-label="数学成长空间首页">
          <BrandLogo />
        </RouterLink>
        <nav class="site-nav" aria-label="官网导航">
          <a href="#capabilities">核心能力</a>
          <a href="#loop">学习闭环</a>
          <a href="#roles">角色体验</a>
          <a href="#technology">技术实现</a>
        </nav>
        <div class="site-actions">
          <RouterLink class="text-link" to="/login?redirect=/dashboard">账号登录</RouterLink>
          <RouterLink class="header-cta" to="/login?redirect=/dashboard">
            进入演示 <AppIcon name="arrow" :size="16" />
          </RouterLink>
        </div>
      </div>
    </header>

    <main>
      <section class="hero-section">
        <div class="hero-glow hero-glow--one" aria-hidden="true" />
        <div class="hero-glow hero-glow--two" aria-hidden="true" />
        <div class="site-container hero-grid">
          <div class="hero-copy">
            <div class="hero-badge"><span /> 小学数学个性化学习平台</div>
            <h1>让每一次练习，<br /><em>都有方向。</em></h1>
            <p class="hero-lead">
              围绕练习、作业、拍照批改与双人
              PK，持续沉淀可信学习数据，帮助学生看见进步，也帮助教师看见问题。
            </p>
            <div class="hero-actions">
              <RouterLink class="primary-cta" to="/login?redirect=/dashboard">
                体验完整系统 <AppIcon name="arrow" :size="18" />
              </RouterLink>
              <a class="secondary-cta" href="#capabilities">了解核心能力</a>
            </div>
            <div class="hero-notes" aria-label="产品特点">
              <span><i>✓</i> 公开演示账号</span>
              <span><i>✓</i> 三种角色视角</span>
              <span><i>✓</i> 完整学习闭环</span>
            </div>
          </div>

          <div class="hero-product" aria-label="产品界面预览">
            <div class="product-window">
              <div class="product-window__bar">
                <div class="window-dots" aria-hidden="true"><i /><i /><i /></div>
                <span>学生学习概览</span>
                <div class="window-avatar">林</div>
              </div>
              <div class="product-body">
                <aside class="product-sidebar" aria-hidden="true">
                  <span class="mini-mark">π</span>
                  <i class="active"><AppIcon name="home" :size="17" /></i>
                  <i><AppIcon name="target" :size="17" /></i>
                  <i><AppIcon name="book" :size="17" /></i>
                  <i><AppIcon name="chart" :size="17" /></i>
                </aside>
                <div class="product-content">
                  <div class="product-greeting">
                    <div><small>下午好，林小满</small><strong>今天也一起进步吧</strong></div>
                    <span>连续学习 <b>7</b> 天</span>
                  </div>
                  <div class="metric-grid">
                    <article>
                      <small>本周练习</small><strong>48 <i>题</i></strong
                      ><span class="trend">↑ 12%</span>
                    </article>
                    <article>
                      <small>平均正确率</small><strong>86<i>%</i></strong
                      ><span class="trend">↑ 5%</span>
                    </article>
                    <article>
                      <small>待巩固知识点</small><strong>3 <i>个</i></strong
                      ><span>正在变少</span>
                    </article>
                  </div>
                  <div class="product-lower">
                    <article class="mastery-card">
                      <div class="mock-card-title">
                        <strong>知识点掌握度</strong><span>本周</span>
                      </div>
                      <div class="radar-wrap">
                        <svg viewBox="0 0 220 148" role="img" aria-label="知识点掌握度雷达图">
                          <g fill="none" stroke="#dfe8fa" stroke-width="1">
                            <path d="M110 12 194 60 162 132H58L26 60Z" />
                            <path d="m110 32 62 36-24 52H72L48 68Z" />
                            <path d="m110 52 39 23-15 33H86L71 75Z" />
                            <path d="M110 12v120M26 60l136 72M194 60 58 132" />
                          </g>
                          <path
                            d="m110 27 65 41-29 49-78-4-24-48Z"
                            fill="rgb(75 112 226 / 22%)"
                            stroke="#4b70e2"
                            stroke-width="2"
                          />
                          <g fill="#4b70e2">
                            <circle cx="110" cy="27" r="3" />
                            <circle cx="175" cy="68" r="3" />
                            <circle cx="146" cy="117" r="3" />
                            <circle cx="68" cy="113" r="3" />
                            <circle cx="44" cy="65" r="3" />
                          </g>
                        </svg>
                      </div>
                    </article>
                    <article class="recommend-card">
                      <div class="mock-card-title">
                        <strong>今日巩固</strong><span>为你推荐</span>
                      </div>
                      <div class="recommend-topic">
                        <span><AppIcon name="target" :size="20" /></span>
                        <div><strong>两位数加减法</strong><small>根据近期错题生成</small></div>
                      </div>
                      <div class="progress-track"><i /></div>
                      <div class="recommend-meta"><span>8 道练习</span><b>开始练习 →</b></div>
                    </article>
                  </div>
                </div>
              </div>
            </div>
            <div class="floating-card floating-card--accuracy">
              <span><AppIcon name="check" :size="20" /></span>
              <div><small>本次练习</small><strong>正确率 92%</strong></div>
            </div>
            <div class="floating-card floating-card--growth">
              <div class="growth-icon"><AppIcon name="chart" :size="20" /></div>
              <div><small>掌握度提升</small><strong>+ 8.6%</strong></div>
            </div>
          </div>
        </div>
      </section>

      <section class="trust-strip" aria-label="系统关键词">
        <div class="site-container trust-strip__inner">
          <p>用同一份学习档案，连接每一种学习场景</p>
          <div>
            <span>自主练习</span><i>+</i><span>教师作业</span><i>+</i><span>拍照批改</span><i>+</i
            ><span>双人 PK</span>
          </div>
        </div>
      </section>

      <section id="capabilities" class="section-block capabilities-section">
        <div class="site-container">
          <div class="section-heading section-heading--split">
            <div>
              <p class="section-kicker">CORE CAPABILITIES</p>
              <h2>不止是做题，<br />更是持续理解学习。</h2>
            </div>
            <p>从每一道题的结果出发，形成可追踪、可解释、可再次验证的个性化学习路径。</p>
          </div>
          <div class="capability-grid">
            <article v-for="item in capabilities" :key="item.title" class="capability-card">
              <div class="capability-card__top">
                <span class="feature-icon" :class="`feature-icon--${item.tone}`"
                  ><AppIcon :name="item.icon" :size="23"
                /></span>
                <span class="feature-index">{{ item.index }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <span class="card-link">了解能力 <AppIcon name="arrow" :size="16" /></span>
            </article>
          </div>
        </div>
      </section>

      <section id="loop" class="section-block loop-section">
        <div class="site-container loop-grid">
          <div class="loop-copy">
            <p class="section-kicker">LEARNING LOOP</p>
            <h2>一次作答，<br />成为下一次进步的依据。</h2>
            <p>
              数据不是终点。系统把分散的学习行为汇入同一条闭环，让发现问题、针对巩固和再次验证自然发生。
            </p>
            <RouterLink class="inline-cta" to="/login?redirect=/dashboard"
              >查看演示数据 <AppIcon name="arrow" :size="17"
            /></RouterLink>
          </div>
          <div class="loop-visual">
            <div class="loop-line" aria-hidden="true" />
            <article>
              <span>01</span>
              <div>
                <strong>多场景练习</strong>
                <p>自主练习、作业、拍照批改与双人 PK</p>
              </div>
              <AppIcon name="edit" />
            </article>
            <article>
              <span>02</span>
              <div>
                <strong>可信判题</strong>
                <p>固定题库、可信度控制与人工复核</p>
              </div>
              <AppIcon name="check" />
            </article>
            <article>
              <span>03</span>
              <div>
                <strong>诊断与推荐</strong>
                <p>更新错题、掌握度并生成针对性巩固</p>
              </div>
              <AppIcon name="target" />
            </article>
            <article>
              <span>04</span>
              <div>
                <strong>再次验证</strong>
                <p>用新的练习结果持续校准学习判断</p>
              </div>
              <AppIcon name="chart" />
            </article>
          </div>
        </div>
      </section>

      <section id="roles" class="section-block roles-section">
        <div class="site-container">
          <div class="section-heading section-heading--center">
            <p class="section-kicker">ONE PLATFORM, THREE VIEWS</p>
            <h2>为每一种角色，提供恰到好处的视角</h2>
            <p>学生专注成长，教师专注教学，管理员保障平台可信运行。</p>
          </div>
          <div class="role-grid">
            <article
              v-for="(audience, index) in audiences"
              :key="audience.label"
              class="role-card"
              :class="{ 'role-card--featured': index === 1 }"
            >
              <div class="role-card__header">
                <span><AppIcon :name="audience.icon" :size="22" /></span
                ><small>{{ audience.label }}</small>
              </div>
              <h3>{{ audience.title }}</h3>
              <ul>
                <li v-for="point in audience.points" :key="point"><i>✓</i>{{ point }}</li>
              </ul>
              <RouterLink to="/login?redirect=/dashboard"
                >体验{{ audience.label }}端 <AppIcon name="arrow" :size="16"
              /></RouterLink>
            </article>
          </div>
        </div>
      </section>

      <section id="technology" class="technology-section">
        <div class="site-container technology-grid">
          <div class="technology-copy">
            <p class="section-kicker">ENGINEERING</p>
            <h2>从产品体验到工程实现，<br />完整构建，真实可运行。</h2>
            <p>
              面向真实业务边界设计，覆盖权限控制、可靠事件、AI 工具调用、数据审计与响应式多端体验。
            </p>
          </div>
          <div class="technology-list">
            <div v-for="technology in technologies" :key="technology.label">
              <span>{{ technology.label }}</span
              ><strong>{{ technology.value }}</strong>
            </div>
          </div>
        </div>
      </section>

      <section class="final-cta-section">
        <div class="site-container final-cta-card">
          <div class="final-orbit final-orbit--one" aria-hidden="true" />
          <div class="final-orbit final-orbit--two" aria-hidden="true" />
          <div class="final-cta-copy">
            <span class="mini-mark mini-mark--light">π</span>
            <h2>现在，进入数学成长空间。</h2>
            <p>无需注册，使用公开演示账号即可查看完整示例数据。</p>
            <RouterLink class="light-cta" to="/login?redirect=/dashboard"
              >进入在线演示 <AppIcon name="arrow" :size="18"
            /></RouterLink>
          </div>
        </div>
      </section>
    </main>

    <footer class="site-footer">
      <div class="site-container site-footer__inner">
        <BrandLogo />
        <p>小学数学个性化练习与双人 PK 平台 · 毕业设计作品</p>
        <RouterLink to="/login?redirect=/dashboard">登录系统</RouterLink>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.official-page {
  --site-primary: #315fe6;
  --site-ink: #12213f;
  --site-body: #52617a;
  --site-line: #e7ebf3;

  overflow: hidden;
  background: #fff;
  color: var(--site-ink);
}

.site-container {
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
}

.site-header {
  position: relative;
  z-index: 20;
  height: 76px;
  background: rgb(255 255 255 / 88%);
  border-bottom: 1px solid rgb(226 231 240 / 76%);
  backdrop-filter: blur(18px);
}

.site-header__inner {
  display: flex;
  height: 100%;
  align-items: center;
}

.site-brand {
  text-decoration: none;
}

.site-nav {
  display: flex;
  margin-left: auto;
  gap: 34px;
}

.site-nav a,
.text-link {
  color: #536078;
  text-decoration: none;
  font-size: 13px;
  transition: color 160ms;
}

.site-nav a:hover,
.text-link:hover {
  color: var(--site-primary);
}

.site-actions {
  display: flex;
  margin-left: 48px;
  align-items: center;
  gap: 24px;
}

.header-cta,
.primary-cta,
.secondary-cta,
.inline-cta,
.light-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.header-cta {
  min-height: 40px;
  padding: 0 17px;
  background: var(--site-ink);
  border-radius: 9px;
  color: #fff;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.hero-section {
  position: relative;
  padding: 90px 0 112px;
  background: linear-gradient(180deg, #f8faff 0%, #fff 92%);
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(1px);
  pointer-events: none;
}

.hero-glow--one {
  width: 520px;
  height: 520px;
  top: -180px;
  left: -240px;
  background: radial-gradient(circle, rgb(92 128 237 / 12%), transparent 68%);
}

.hero-glow--two {
  width: 640px;
  height: 640px;
  right: -270px;
  bottom: -280px;
  background: radial-gradient(circle, rgb(110 92 225 / 10%), transparent 68%);
}

.hero-grid {
  position: relative;
  display: grid;
  align-items: center;
  grid-template-columns: 0.88fr 1.12fr;
  gap: 68px;
}

.hero-copy {
  position: relative;
  z-index: 2;
}

.hero-badge {
  display: inline-flex;
  padding: 7px 11px;
  align-items: center;
  background: #edf2ff;
  border: 1px solid #dae3ff;
  border-radius: 999px;
  color: #3858ac;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.hero-badge span {
  width: 6px;
  height: 6px;
  background: #5278e9;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgb(82 120 233 / 12%);
}

.hero-copy h1 {
  margin-top: 26px;
  color: var(--site-ink);
  font-size: clamp(46px, 5.2vw, 72px);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.055em;
}

.hero-copy h1 em {
  color: var(--site-primary);
  font-style: normal;
}

.hero-lead {
  max-width: 530px;
  margin-top: 26px;
  color: var(--site-body);
  font-size: 17px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  margin-top: 38px;
  gap: 14px;
}

.primary-cta,
.secondary-cta {
  min-height: 52px;
  padding: 0 24px;
  border-radius: 11px;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
}

.primary-cta {
  background: var(--site-primary);
  box-shadow: 0 12px 28px rgb(49 95 230 / 22%);
  color: #fff;
  transition:
    transform 160ms,
    box-shadow 160ms;
}

.primary-cta:hover {
  box-shadow: 0 16px 34px rgb(49 95 230 / 28%);
  transform: translateY(-2px);
}

.secondary-cta {
  background: #fff;
  border: 1px solid #dfe5ef;
  color: #33415d;
}

.hero-notes {
  display: flex;
  margin-top: 28px;
  flex-wrap: wrap;
  color: #6d7890;
  gap: 19px;
  font-size: 12px;
}

.hero-notes span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hero-notes i {
  display: grid;
  width: 17px;
  height: 17px;
  place-items: center;
  background: #e8f7f0;
  border-radius: 50%;
  color: #24946a;
  font-size: 10px;
  font-style: normal;
  font-weight: 700;
}

.hero-product {
  position: relative;
  padding: 26px 0 32px;
}

.product-window {
  position: relative;
  z-index: 2;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dfe5f0;
  border-radius: 18px;
  box-shadow:
    0 32px 70px rgb(38 57 100 / 16%),
    0 3px 12px rgb(38 57 100 / 6%);
  transform: perspective(1200px) rotateY(-2deg) rotateX(1deg);
}

.product-window__bar {
  display: flex;
  height: 49px;
  padding: 0 16px;
  align-items: center;
  background: #fbfcff;
  border-bottom: 1px solid #edf0f5;
  color: #65728a;
  font-size: 10px;
}

.window-dots {
  display: flex;
  gap: 5px;
}

.window-dots i {
  width: 7px;
  height: 7px;
  background: #dce2ec;
  border-radius: 50%;
}

.product-window__bar > span {
  margin: auto;
}

.window-avatar {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  background: #e6edff;
  border-radius: 50%;
  color: #4266ce;
  font-size: 9px;
  font-weight: 700;
}

.product-body {
  display: flex;
  min-height: 365px;
}

.product-sidebar {
  display: flex;
  width: 58px;
  padding: 15px 9px;
  align-items: center;
  background: #f7f9fd;
  border-right: 1px solid #edf0f5;
  flex-direction: column;
  gap: 12px;
}

.mini-mark {
  display: grid;
  width: 31px;
  height: 31px;
  margin-bottom: 7px;
  place-items: center;
  background: var(--site-primary);
  border-radius: 9px;
  color: #fff;
  font-family: Georgia, serif;
  font-size: 21px;
  font-weight: 700;
}

.product-sidebar i {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 9px;
  color: #9aa7bb;
  font-style: normal;
}

.product-sidebar i.active {
  background: #e9efff;
  color: var(--site-primary);
}

.product-content {
  min-width: 0;
  padding: 27px 23px;
  flex: 1;
  background: #fbfcff;
}

.product-greeting {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-greeting div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.product-greeting small {
  color: #8a96a9;
  font-size: 9px;
}

.product-greeting strong {
  color: #263654;
  font-size: 15px;
}

.product-greeting > span {
  padding: 7px 10px;
  background: #fff6e6;
  border: 1px solid #f8e7c7;
  border-radius: 7px;
  color: #976f2a;
  font-size: 9px;
}

.product-greeting b {
  color: #e78b24;
  font-size: 12px;
}

.metric-grid {
  display: grid;
  margin-top: 20px;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.metric-grid article {
  position: relative;
  display: flex;
  min-height: 86px;
  padding: 13px;
  background: #fff;
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  flex-direction: column;
}

.metric-grid small {
  color: #8b96a8;
  font-size: 8px;
}

.metric-grid strong {
  margin-top: 8px;
  color: #263654;
  font-size: 21px;
}

.metric-grid strong i {
  color: #748199;
  font-size: 8px;
  font-style: normal;
  font-weight: 500;
}

.metric-grid article > span {
  margin-top: auto;
  color: #9aa4b4;
  font-size: 7px;
}

.metric-grid article > .trend {
  color: #24a16c;
}

.product-lower {
  display: grid;
  margin-top: 11px;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 10px;
}

.product-lower > article {
  min-width: 0;
  height: 174px;
  padding: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e8ecf3;
  border-radius: 10px;
}

.mock-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mock-card-title strong {
  color: #35435e;
  font-size: 9px;
}

.mock-card-title span {
  color: #96a0b1;
  font-size: 7px;
}

.radar-wrap {
  display: grid;
  height: 138px;
  place-items: center;
}

.radar-wrap svg {
  width: 178px;
  height: 120px;
}

.recommend-topic {
  display: flex;
  margin-top: 25px;
  align-items: center;
  gap: 10px;
}

.recommend-topic > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  background: #edf2ff;
  border-radius: 10px;
  color: var(--site-primary);
}

.recommend-topic div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.recommend-topic strong {
  overflow: hidden;
  color: #34425d;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 9px;
}

.recommend-topic small {
  color: #9ba5b4;
  font-size: 7px;
}

.progress-track {
  height: 5px;
  margin-top: 22px;
  overflow: hidden;
  background: #edf0f5;
  border-radius: 99px;
}

.progress-track i {
  display: block;
  width: 68%;
  height: 100%;
  background: linear-gradient(90deg, #5279e9, #7391eb);
  border-radius: inherit;
}

.recommend-meta {
  display: flex;
  margin-top: 13px;
  align-items: center;
  justify-content: space-between;
  color: #9aa4b4;
  font-size: 7px;
}

.recommend-meta b {
  color: var(--site-primary);
  font-weight: 600;
}

.floating-card {
  position: absolute;
  z-index: 3;
  display: flex;
  padding: 12px 15px;
  align-items: center;
  background: rgb(255 255 255 / 94%);
  border: 1px solid #e1e6ef;
  border-radius: 11px;
  box-shadow: 0 15px 32px rgb(39 56 91 / 15%);
  gap: 10px;
  backdrop-filter: blur(10px);
}

.floating-card > span,
.growth-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  background: #e9f8f1;
  border-radius: 9px;
  color: #26946a;
}

.floating-card div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.floating-card small {
  color: #8d98aa;
  font-size: 8px;
}

.floating-card strong {
  color: #30405c;
  font-size: 10px;
}

.floating-card--accuracy {
  right: -34px;
  bottom: 0;
}

.floating-card--growth {
  top: 2px;
  left: -34px;
}

.growth-icon {
  background: #eef0ff;
  color: #6c5cdd;
}

.floating-card--growth strong {
  color: #5f51c8;
}

.trust-strip {
  border-top: 1px solid #edf0f5;
  border-bottom: 1px solid #edf0f5;
}

.trust-strip__inner {
  display: flex;
  min-height: 88px;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
}

.trust-strip p {
  color: #7a879c;
  font-size: 12px;
}

.trust-strip__inner > div {
  display: flex;
  align-items: center;
  color: #33415d;
  gap: 17px;
  font-size: 13px;
  font-weight: 600;
}

.trust-strip i {
  color: #b1bac9;
  font-style: normal;
  font-weight: 400;
}

.section-block {
  padding: 118px 0;
}

.section-kicker {
  color: var(--site-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.17em;
}

.section-heading--split {
  display: grid;
  align-items: end;
  grid-template-columns: 1fr 390px;
  gap: 70px;
}

.section-heading h2,
.loop-copy h2,
.technology-copy h2 {
  margin-top: 16px;
  color: var(--site-ink);
  font-size: clamp(34px, 4vw, 50px);
  font-weight: 650;
  line-height: 1.28;
  letter-spacing: -0.04em;
}

.section-heading--split > p,
.loop-copy > p:not(.section-kicker),
.technology-copy > p:not(.section-kicker) {
  color: var(--site-body);
  font-size: 15px;
  line-height: 1.9;
}

.capability-grid {
  display: grid;
  margin-top: 56px;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.capability-card {
  display: flex;
  min-height: 310px;
  padding: 28px 25px;
  background: #fff;
  border: 1px solid var(--site-line);
  border-radius: 16px;
  flex-direction: column;
  transition:
    transform 180ms,
    box-shadow 180ms,
    border-color 180ms;
}

.capability-card:hover {
  border-color: #d3dbeb;
  box-shadow: 0 18px 42px rgb(35 53 90 / 8%);
  transform: translateY(-5px);
}

.capability-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.feature-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  background: #edf2ff;
  border-radius: 13px;
  color: #4168dc;
}

.feature-icon--violet {
  background: #f2efff;
  color: #6b58ce;
}

.feature-icon--orange {
  background: #fff3e5;
  color: #d67a23;
}

.feature-icon--green {
  background: #eaf8f2;
  color: #278f68;
}

.feature-index {
  color: #bec5d1;
  font-family: Georgia, serif;
  font-size: 12px;
}

.capability-card h3 {
  margin-top: 31px;
  color: #21314e;
  font-size: 19px;
}

.capability-card > p {
  margin-top: 15px;
  color: #718097;
  font-size: 13px;
  line-height: 1.85;
}

.card-link {
  display: flex;
  margin-top: auto;
  align-items: center;
  color: #60708a;
  gap: 7px;
  font-size: 11px;
  font-weight: 600;
}

.loop-section {
  background: #f6f8fc;
}

.loop-grid {
  display: grid;
  align-items: center;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 110px;
}

.loop-copy > p:not(.section-kicker) {
  margin-top: 24px;
}

.inline-cta {
  margin-top: 28px;
  color: var(--site-primary);
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.loop-visual {
  position: relative;
  display: grid;
  gap: 12px;
}

.loop-line {
  position: absolute;
  top: 44px;
  bottom: 44px;
  left: 35px;
  width: 1px;
  background: linear-gradient(#bbc9ef, #ccd6ee);
}

.loop-visual article {
  position: relative;
  display: grid;
  min-height: 86px;
  padding: 18px 20px;
  align-items: center;
  background: #fff;
  border: 1px solid #e5eaf3;
  border-radius: 13px;
  box-shadow: 0 5px 18px rgb(31 49 87 / 3%);
  grid-template-columns: 34px 1fr auto;
  gap: 18px;
}

.loop-visual article > span {
  position: relative;
  z-index: 1;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  background: #eef3ff;
  border: 4px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 1px #d9e2f8;
  color: var(--site-primary);
  font-family: Georgia, serif;
  font-size: 9px;
}

.loop-visual article > div {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.loop-visual strong {
  color: #2b3a55;
  font-size: 14px;
}

.loop-visual p {
  color: #8290a4;
  font-size: 11px;
}

.loop-visual article > .app-icon {
  color: #98a8c6;
}

.section-heading--center {
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
}

.section-heading--center > p:last-child {
  margin-top: 18px;
  color: #77859b;
  font-size: 14px;
}

.role-grid {
  display: grid;
  margin-top: 56px;
  align-items: stretch;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.role-card {
  padding: 34px;
  background: #fff;
  border: 1px solid var(--site-line);
  border-radius: 17px;
}

.role-card--featured {
  position: relative;
  border-color: #bfcdf5;
  box-shadow: 0 20px 45px rgb(49 95 230 / 10%);
}

.role-card--featured::before {
  position: absolute;
  inset: -1px -1px auto;
  height: 4px;
  background: var(--site-primary);
  border-radius: 17px 17px 0 0;
  content: '';
}

.role-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-card__header > span {
  display: grid;
  width: 45px;
  height: 45px;
  place-items: center;
  background: #eff3ff;
  border-radius: 12px;
  color: var(--site-primary);
}

.role-card__header small {
  padding: 5px 9px;
  background: #f4f6fa;
  border-radius: 99px;
  color: #718096;
  font-size: 10px;
}

.role-card h3 {
  max-width: 240px;
  margin-top: 26px;
  color: #263653;
  font-size: 20px;
  line-height: 1.55;
}

.role-card ul {
  display: grid;
  margin-top: 25px;
  list-style: none;
  gap: 14px;
}

.role-card li {
  display: flex;
  align-items: center;
  color: #6b7a91;
  gap: 9px;
  font-size: 12px;
}

.role-card li i {
  display: grid;
  width: 17px;
  height: 17px;
  place-items: center;
  background: #eaf7f1;
  border-radius: 50%;
  color: #258c66;
  font-size: 9px;
  font-style: normal;
}

.role-card > a {
  display: flex;
  margin-top: 30px;
  align-items: center;
  color: var(--site-primary);
  gap: 8px;
  text-decoration: none;
  font-size: 12px;
  font-weight: 600;
}

.technology-section {
  padding: 105px 0;
  background: #121f3b;
}

.technology-grid {
  display: grid;
  align-items: center;
  grid-template-columns: 1fr 1fr;
  gap: 100px;
}

.technology-copy h2 {
  color: #fff;
}

.technology-copy > p:not(.section-kicker) {
  margin-top: 23px;
  color: #aebad0;
}

.technology-list {
  border-top: 1px solid rgb(255 255 255 / 14%);
}

.technology-list > div {
  display: grid;
  min-height: 64px;
  align-items: center;
  border-bottom: 1px solid rgb(255 255 255 / 14%);
  grid-template-columns: 80px 1fr;
  gap: 20px;
}

.technology-list span {
  color: #71809e;
  font-family: Georgia, serif;
  font-size: 11px;
  letter-spacing: 0.1em;
}

.technology-list strong {
  color: #ecf1fb;
  font-size: 13px;
  font-weight: 500;
}

.final-cta-section {
  padding: 96px 0;
}

.final-cta-card {
  position: relative;
  display: grid;
  min-height: 330px;
  overflow: hidden;
  background: linear-gradient(125deg, #315fe6, #5f54d7);
  border-radius: 24px;
  place-items: center;
}

.final-cta-copy {
  position: relative;
  z-index: 2;
  text-align: center;
}

.mini-mark--light {
  margin: 0 auto 18px;
  background: rgb(255 255 255 / 17%);
  border: 1px solid rgb(255 255 255 / 25%);
}

.final-cta-copy h2 {
  color: #fff;
  font-size: clamp(30px, 4vw, 44px);
  letter-spacing: -0.04em;
}

.final-cta-copy p {
  margin-top: 15px;
  color: #dce4ff;
  font-size: 14px;
}

.light-cta {
  min-height: 48px;
  margin-top: 29px;
  padding: 0 22px;
  background: #fff;
  border-radius: 10px;
  color: #385ccb;
  gap: 9px;
  font-size: 13px;
  font-weight: 650;
}

.final-orbit {
  position: absolute;
  border: 1px solid rgb(255 255 255 / 13%);
  border-radius: 50%;
}

.final-orbit--one {
  width: 440px;
  height: 440px;
  top: -260px;
  left: -90px;
}

.final-orbit--two {
  width: 520px;
  height: 520px;
  right: -160px;
  bottom: -360px;
}

.site-footer {
  border-top: 1px solid var(--site-line);
}

.site-footer__inner {
  display: flex;
  min-height: 105px;
  align-items: center;
}

.site-footer__inner p {
  margin-left: auto;
  color: #8a96a9;
  font-size: 11px;
}

.site-footer__inner > a {
  margin-left: 34px;
  color: #5f6d84;
  text-decoration: none;
  font-size: 11px;
}

@media (width <= 1040px) {
  .site-nav {
    display: none;
  }

  .site-actions {
    margin-left: auto;
  }

  .hero-grid {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    max-width: 720px;
    text-align: center;
    margin: 0 auto;
  }

  .hero-lead {
    margin-right: auto;
    margin-left: auto;
  }

  .hero-actions,
  .hero-notes {
    justify-content: center;
  }

  .hero-product {
    width: min(720px, 92%);
    margin: 16px auto 0;
  }

  .section-heading--split {
    grid-template-columns: 1fr 320px;
    gap: 40px;
  }

  .capability-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .loop-grid {
    gap: 60px;
  }

  .technology-grid {
    gap: 60px;
  }
}

@media (width <= 760px) {
  .site-container {
    width: min(100% - 32px, 620px);
  }

  .site-header {
    height: 68px;
  }

  .site-header :deep(.brand__copy small) {
    display: none;
  }

  .text-link {
    display: none;
  }

  .site-actions {
    gap: 0;
  }

  .hero-section {
    padding: 68px 0 84px;
  }

  .hero-copy h1 {
    font-size: clamp(42px, 13vw, 58px);
  }

  .hero-lead {
    font-size: 15px;
  }

  .hero-product {
    width: 100%;
    transform: scale(0.96);
  }

  .floating-card--growth {
    left: -12px;
  }

  .floating-card--accuracy {
    right: -12px;
  }

  .trust-strip__inner {
    padding: 24px 0;
    flex-direction: column;
    justify-content: center;
    text-align: center;
  }

  .trust-strip__inner > div {
    flex-wrap: wrap;
    justify-content: center;
  }

  .section-block {
    padding: 84px 0;
  }

  .section-heading--split {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .capability-grid {
    margin-top: 38px;
    grid-template-columns: 1fr;
  }

  .capability-card {
    min-height: 260px;
  }

  .loop-grid,
  .technology-grid {
    grid-template-columns: 1fr;
    gap: 45px;
  }

  .role-grid {
    grid-template-columns: 1fr;
  }

  .technology-section {
    padding: 82px 0;
  }

  .final-cta-section {
    padding: 64px 0;
  }

  .final-cta-card {
    min-height: 310px;
    padding: 40px 22px;
  }

  .site-footer__inner {
    padding: 28px 0;
    flex-direction: column;
    gap: 18px;
    text-align: center;
  }

  .site-footer__inner p,
  .site-footer__inner > a {
    margin-left: 0;
  }
}

@media (width <= 520px) {
  .site-header :deep(.brand__copy) {
    display: none;
  }

  .header-cta {
    min-height: 38px;
  }

  .hero-section {
    padding-top: 52px;
  }

  .hero-badge {
    font-size: 10px;
  }

  .hero-copy h1 {
    margin-top: 22px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .primary-cta,
  .secondary-cta {
    width: 100%;
  }

  .hero-notes {
    display: grid;
    grid-template-columns: 1fr 1fr;
    text-align: left;
  }

  .hero-product {
    margin-top: -5px;
    transform: scale(0.9);
    transform-origin: top center;
    margin-bottom: -55px;
  }

  .product-content {
    padding: 20px 14px;
  }

  .metric-grid article:last-child {
    display: none;
  }

  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .product-lower {
    grid-template-columns: 1fr;
  }

  .recommend-card {
    display: none;
  }

  .floating-card--growth {
    top: 3px;
    left: -18px;
  }

  .floating-card--accuracy {
    right: -18px;
  }

  .section-heading h2,
  .loop-copy h2,
  .technology-copy h2 {
    font-size: 34px;
  }

  .loop-visual article {
    padding: 16px 14px;
    grid-template-columns: 34px 1fr;
    gap: 13px;
  }

  .loop-visual article > .app-icon {
    display: none;
  }

  .loop-visual p {
    line-height: 1.5;
  }

  .role-card {
    padding: 28px;
  }

  .technology-list > div {
    grid-template-columns: 58px 1fr;
  }

  .final-cta-copy h2 {
    font-size: 31px;
    line-height: 1.35;
  }
}
</style>
