import LightRays from './components/LightRays'
import TrueFocus from './components/TrueFocus'
import FallingText from './components/FallingText'
import ShinyText from './components/ShinyText'
import CardNav from './components/CardNav'
import ChromaGrid from './components/ChromaGrid'
import InfiniteMenu from './components/InfiniteMenu'
import Lanyard from './components/Lanyard'
import './App.css'

function App() {
  return (
    <main style={{ overflowX: 'hidden', width: '100%' }}>
      
      {/* 导航栏 */}
      <CardNav
        logo={null}
        logoAlt="以外"
        items={[
          {
            label: '关于',
            bgColor: '#1c1917',
            textColor: '#e7e5e4',
            links: [
              { label: '项目故事', ariaLabel: '了解项目背景', href: '#features' },
              { label: '开源协议', ariaLabel: '查看开源协议', href: '#' }
            ]
          },
          {
            label: '仪式',
            bgColor: '#292524',
            textColor: '#e7e5e4',
            links: [
              { label: '落笔流程', ariaLabel: '了解落笔仪式', href: '#ritual' },
              { label: '散步送别', ariaLabel: '了解散步仪式', href: '#ritual' }
            ]
          },
          {
            label: '参与',
            bgColor: '#292524',
            textColor: '#e7e5e4',
            links: [
              { label: '落笔写信', ariaLabel: '开始落笔', href: '#cta' },
              { label: '读一个故事', ariaLabel: '阅读他人故事', href: '#' },
              { label: '联系我们', ariaLabel: '联系项目团队', href: '#' }
            ]
          }
        ]}
        baseColor="rgba(12, 10, 9, 0.85)"
        menuColor="#e7e5e4"
        buttonBgColor="#D97706"
        buttonTextColor="#0C0A09"
        buttonText="落笔"
        ease="power3.out"
      />

      {/* 英雄区 — LightRays 背景 */}
      <section className="hero">
        <LightRays
          raysOrigin="top-center"
          raysColor="#D97706"
          raysSpeed={0.6}
          lightSpread={2.0}
          rayLength={5}
          followMouse={true}
          mouseInfluence={0.08}
          noiseAmount={0}
          distortion={0}
          className="custom-rays"
          pulsating
          fadeDistance={3.0}
          saturation={1.0}
        />

        <div className="hero-content">
          <h1 className="hero-headline fade-in">
            <TrueFocus
              sentence="以外"
              separator=""
              manualMode={false}
              blurAmount={5}
              borderColor="#D97706"
              glowColor="rgba(217, 119, 6, 0.4)"
              animationDuration={0.8}
              pauseBetweenAnimations={2}
            />
          </h1>
          <p className="hero-subtitle fade-in fade-in-delay-1">
            <ShinyText
              text="每个人都有那个错过的他/她。写下你们之间的故事，你的经历会教会以外Agent理解感情——不止理性判断，而是像一个爱过的人那样思考，不让错过重演。"
              speed={4.6}
              delay={0}
              color="#a8a29e"
              shineColor="#D97706"
              spread={120}
              direction="left"
              yoyo={false}
              pauseOnHover={false}
              className="hero-shiny-text"
            />
          </p>
          <div className="hero-actions fade-in fade-in-delay-2">
            <button className="btn-primary breathing">
              落笔写一封信
            </button>
            <button className="btn-ghost">
              读一个故事
            </button>
          </div>
        </div>
      </section>

      {/* 诗句揭示区 — 悬停触发文字下落 */}
      <section className="reveal-section">
        <FallingText
          text="每个人心里都有一个无法替代的人。你的故事会教会以外Agent感知遗憾与珍惜——让它不止做理性判断，而是作为一个爱过的他/她来思考。让错过不再重演。"
          highlightWords={["以外Agent", "不再重演", "无法替代"]}
          highlightClass="highlighted"
          trigger="hover"
          backgroundColor="transparent"
          wireframes={false}
          gravity={0.56}
          fontSize="clamp(1.2rem, 2.5vw, 1.8rem)"
          mouseConstraintStiffness={0.9}
        />
      </section>

      {/* 特色区 */}
      <section className="features" id="features">
        <div className="features-header">
          <h2 className="features-title">
            你的每一段故事，都在构建以外Agent的情感认知引擎
          </h2>
        </div>
        <div className="chroma-grid-wrapper">
          <ChromaGrid
            items={[
              {
                image: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80',
                title: '全平台语义检索',
                subtitle: '以外Agent会检索全平台的故事与经历，通过语义理解找到与你情感共鸣的篇章。不是关键词匹配，而是真正读懂字里行间的遗憾与温柔。',
                handle: '01',
                borderColor: '#D97706',
                gradient: 'linear-gradient(145deg, rgba(190,40,50,0.35), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=600&q=80',
                title: '情感模式学习',
                subtitle: '每一封信都是一个训练样本。以外Agent从中学习人类情感的表达模式、遗憾的共性、珍惜的时机——越读越像一个爱过的人。',
                handle: '02',
                borderColor: '#b45309',
                gradient: 'linear-gradient(210deg, rgba(220,120,20,0.35), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=600&q=80',
                title: '记忆图谱构建',
                subtitle: '所有故事被编织成一张情感记忆图谱。人物、场景、情绪彼此关联，让以外Agent理解：错过不是孤立事件，而是人类共同的经历。',
                handle: '03',
                borderColor: '#92400e',
                gradient: 'linear-gradient(165deg, rgba(200,180,40,0.35), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&q=80',
                title: '上下文共情推理',
                subtitle: '结合故事发生的时间、场景与情感脉络，以外Agent不只是理解文字表面，而是推断写信时的心境——像亲历者一样感同身受。',
                handle: '04',
                borderColor: '#c2410c',
                gradient: 'linear-gradient(195deg, rgba(40,180,100,0.3), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=600&q=80',
                title: '故事时序分析',
                subtitle: '自动识别故事中的时间线：初遇、相知、分离、回忆。以外Agent理解情感是随时间流动的，而不只是静态的文字片段。',
                handle: '05',
                borderColor: '#ea580c',
                gradient: 'linear-gradient(225deg, rgba(50,140,210,0.3), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=600&q=80',
                title: '情感向量化',
                subtitle: '将每段故事转化为多维情感向量。遗憾、珍惜、释然……以外Agent在向量空间中理解人类情感的微妙差异。',
                handle: '06',
                borderColor: '#d97706',
                gradient: 'linear-gradient(135deg, rgba(100,80,200,0.3), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1563986768609-322da13575f2?w=600&q=80',
                title: '隐私安全守护',
                subtitle: '端到端加密，你的故事只属于你。以外Agent在保护隐私的前提下学习，绝不让任何人的秘密被泄露。',
                handle: '07',
                borderColor: '#78350f',
                gradient: 'linear-gradient(180deg, rgba(210,70,140,0.3), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1519337265831-281ec6cc8514?w=600&q=80',
                title: '智能情感摘要',
                subtitle: '用一段话凝练一封信的灵魂。以外Agent能提炼出故事中最动人的情感内核，帮你看见自己未曾察觉的心意。',
                handle: '08',
                borderColor: '#a16207',
                gradient: 'linear-gradient(150deg, rgba(40,180,180,0.3), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              },
              {
                image: 'https://images.unsplash.com/photo-1475274047050-1d0c55b91aca?w=600&q=80',
                title: '跨故事关联',
                subtitle: '发现不同故事之间的隐秘联系：相似的人、相同的遗憾、不同的结局。以外Agent在关联中洞察人类情感的普遍规律。',
                handle: '09',
                borderColor: '#854d0e',
                gradient: 'linear-gradient(200deg, rgba(150,70,200,0.3), #0C0A09)',
                spotlightColor: 'rgba(255, 255, 255, 0.12)'
              }
            ]}
            radius={350}
            columns={3}
            rows={3}
            damping={0.5}
            fadeOut={0.6}
            ease="power3.out"
          />
        </div>
      </section>

      {/* 记忆星球 — 3D旋转球体 */}
      <section className="memory-sphere">
        <div className="memory-sphere-header">
          <h2 className="memory-sphere-title">每段遗憾，都是一个未完成的宇宙</h2>
          <p className="memory-sphere-subtitle">旋转探索，每个星球都藏着一段未说出口的故事</p>
        </div>
        <div className="memory-sphere-wrapper">
          <InfiniteMenu
            items={[
              { image: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80', link: '#', title: '全平台语义检索', description: '读懂字里行间的遗憾与温柔' },
              { image: 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=400&q=80', link: '#', title: '情感模式学习', description: '越读越像一个爱过的人' },
              { image: 'https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=400&q=80', link: '#', title: '记忆图谱构建', description: '错过不是孤立事件' },
              { image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80', link: '#', title: '上下文共情推理', description: '像亲历者一样感同身受' },
              { image: 'https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=400&q=80', link: '#', title: '故事时序分析', description: '情感随时间流动' },
              { image: 'https://images.unsplash.com/photo-1534067783941-51c9c23ecefd?w=400&q=80', link: '#', title: '情感向量化', description: '理解遗憾与珍惜的微妙差异' },
              { image: 'https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=400&q=80', link: '#', title: '隐私安全守护', description: '你的故事只属于你' },
              { image: 'https://images.unsplash.com/photo-1519337265831-281ec6cc8514?w=400&q=80', link: '#', title: '智能情感摘要', description: '凝练一封信的灵魂' },
              { image: 'https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=400&q=80', link: '#', title: '跨故事关联', description: '发现隐秘的情感共鸣' }
            ]}
            scale={1.7}
          />
        </div>
      </section>

      {/* 仪式流程区 */}
      <section className="ritual" id="ritual">
        <div className="ritual-inner">
          <div className="ritual-text-block">
            <span className="ritual-label">仪式</span>
            <h2 className="ritual-heading">
              每一封信，都值得被认真对待
            </h2>
            <p className="ritual-desc">
              落笔之前有一段安静的时间。没有催促，没有倒计时。当你准备好了，笔自然落下。
            </p>
          </div>
          <div className="ritual-steps">
            <div className="ritual-step">
              <span className="ritual-step__idx">01</span>
              <div className="ritual-step__text">
                <span className="ritual-step__title">静默</span>
                <span className="ritual-step__desc">一分钟的安静，只听见自己的呼吸</span>
              </div>
            </div>
            <div className="ritual-step">
              <span className="ritual-step__idx">02</span>
              <div className="ritual-step__text">
                <span className="ritual-step__title">三问</span>
                <span className="ritual-step__desc">确认这封信对你真的重要</span>
              </div>
            </div>
            <div className="ritual-step">
              <span className="ritual-step__idx">03</span>
              <div className="ritual-step__text">
                <span className="ritual-step__title">落笔</span>
                <span className="ritual-step__desc">笔落纸声响起，字句落在信纸上</span>
              </div>
            </div>
            <div className="ritual-step">
              <span className="ritual-step__idx">04</span>
              <div className="ritual-step__text">
                <span className="ritual-step__title">散步</span>
                <span className="ritual-step__desc">写完后目送它走向远方，然后放下</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 底部行动区 */}
      <section className="cta-section" id="cta">
        <div className="cta-section__inner">
          <h2 className="cta-heading">有些话，现在不说就永远不会说了</h2>
          <p className="cta-desc">你的每一段经历，都在赋予以外Agent对人类情感的感知能力。落笔吧，让它学会像爱过的人一样思考，不让错过重演。</p>
          <button className="btn-primary breathing" style={{ position: 'relative' }}>
            开始落笔
          </button>
        </div>
      </section>

      {/* 挂饰 — 拖拽互动卡片 */}
      <section className="lanyard-section">
        <div className="lanyard-header">
          <h2 className="lanyard-title">如果您感兴趣，加入我们，请联系</h2>
          <p className="lanyard-subtitle" style={{ fontSize: '1.2rem' }}>构建 一起</p>
        </div>
        <Lanyard
          position={[0, 0, 31]}
          gravity={[0, -40, 0]}
          fov={20}
          frontImage={`${import.meta.env.BASE_URL}card-front.png`}
          backImage={`${import.meta.env.BASE_URL}card-front.png`}
          imageFit="cover"
        />
      </section>

      {/* 页脚 */}
      <footer className="footer">
        <span className="footer-brand">以外 — 你的故事，教会Agent如何去爱</span>
        <div className="footer-links">
          <a className="footer-link" href="#">关于项目</a>
          <a className="footer-link" href="#">开源协议</a>
          <a className="footer-link" href="#">联系我们</a>
        </div>
      </footer>
    </main>
  )
}

export default App
