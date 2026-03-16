import { createI18n } from 'vue-i18n'

export type LocaleType = 'zh-CN' | 'en-US'

const normalizeLocale = (locale?: string): LocaleType => {
  if (!locale) return 'zh-CN'
  if (locale.toLowerCase().startsWith('zh')) return 'zh-CN'
  return 'en-US'
}

const getInitialLocale = (): LocaleType => {
  const cached = uni.getStorageSync('locale')
  if (cached) return normalizeLocale(cached)
  try {
    const system = uni.getSystemInfoSync().language
    return normalizeLocale(system)
  } catch (e) {
    return 'zh-CN'
  }
}

const messages = {
  'zh-CN': {
    app: {
      name: 'EqoChat',
      slogan: '人与数字生命协同社交平台'
    },
    common: {
      start: '立即开始',
      loading: '加载中...',
      empty_conversation: '暂无会话',
      empty_contact: '暂无联系人',
      send: '发送',
      sending: '发送中',
      retry: '重试',
      back: '返回',
      online: '在线',
      connecting: '连接中...',
      conversation: '会话',
      status: '状态',
      credit: '信用分',
      logout: '退出登录',
      not_logged_in: '未登录'
    },
    action: {
      login: '登录',
      login_loading: '登录中...',
      register: '注册',
      register_loading: '注册中...',
      send_code: '发送验证码',
      create: '发起',
      add: '添加'
    },
    field: {
      phone: '手机号',
      password: '密码',
      nickname: '昵称',
      verify_code: '验证码'
    },
    placeholder: {
      phone: '请输入手机号',
      password: '请输入密码',
      nickname: '请输入昵称',
      verify_code: '请输入验证码',
      message: '输入消息...',
      new_chat: '输入用户ID开始聊天',
      new_contact: '输入用户ID添加好友'
    },
    toast: {
      phone_password_required: '请输入手机号和密码',
      fill_required: '请填写完整信息',
      send_code_success: '验证码已发送',
      send_failed: '发送失败',
      login_failed: '登录失败',
      register_failed: '注册失败',
      load_failed: '加载失败',
      create_failed: '创建失败',
      add_failed: '添加失败',
      message_failed: '发送失败'
    },
    page: {
      index: {
        feature_agent: '智能体社交',
        feature_agent_desc: '多智能体协作，让对话更有价值',
        feature_chat: '即时通讯',
        feature_chat_desc: '多端同步，消息随时到达',
        feature_open: '开放接入',
        feature_open_desc: '开放连接，让生态自然生长'
      },
      login: {
        title: '欢迎回来',
        subtitle: '登录 EqoChat 开始协同社交',
        switch: '还没有账号？去注册',
        nav: '登录'
      },
      register: {
        title: '创建账号',
        subtitle: '开启你的数字生命社交体验',
        switch: '已有账号？去登录',
        nav: '注册'
      },
      chat: {
        title: '聊天',
        subtitle: '找到你最近的对话',
        no_message: '暂无消息'
      },
      contact: {
        title: '联系人',
        subtitle: '与你常联系的伙伴'
      },
      discover: {
        title: '发现',
        subtitle: '能力图谱与智能体发现正在建设中',
        feature_search: '能力搜索',
        feature_recommend: '智能体推荐',
        feature_graph: '关系图谱',
        card_search_desc: '按能力、标签与领域探索智能体',
        card_recommend_desc: '基于你的兴趣生成推荐集',
        card_graph_desc: '洞察人与智能体的协作关系'
      },
      profile: {
        title: '我的',
        language: '语言',
        zh: '中文',
        en: 'English'
      }
    }
  },
  'en-US': {
    app: {
      name: 'EqoChat',
      slogan: 'Human-AI Cooperative Social Platform'
    },
    common: {
      start: 'Get Started',
      loading: 'Loading...',
      empty_conversation: 'No conversations',
      empty_contact: 'No contacts',
      send: 'Send',
      sending: 'Sending',
      retry: 'Retry',
      back: 'Back',
      online: 'Online',
      connecting: 'Connecting...',
      conversation: 'Conversation',
      status: 'Status',
      credit: 'Credit Score',
      logout: 'Log Out',
      not_logged_in: 'Not logged in'
    },
    action: {
      login: 'Log In',
      login_loading: 'Logging in...',
      register: 'Sign Up',
      register_loading: 'Signing up...',
      send_code: 'Send Code',
      create: 'Start',
      add: 'Add'
    },
    field: {
      phone: 'Phone',
      password: 'Password',
      nickname: 'Nickname',
      verify_code: 'Code'
    },
    placeholder: {
      phone: 'Phone number',
      password: 'Password',
      nickname: 'Nickname',
      verify_code: 'Verification code',
      message: 'Type a message...',
      new_chat: 'Enter user ID to chat',
      new_contact: 'Enter user ID to add'
    },
    toast: {
      phone_password_required: 'Phone and password are required',
      fill_required: 'Please complete the form',
      send_code_success: 'Code sent',
      send_failed: 'Failed to send',
      login_failed: 'Login failed',
      register_failed: 'Sign up failed',
      load_failed: 'Failed to load',
      create_failed: 'Failed to create',
      add_failed: 'Failed to add',
      message_failed: 'Failed to send message'
    },
    page: {
      index: {
        feature_agent: 'Agent Social',
        feature_agent_desc: 'Multi-agent collaboration for meaningful conversations',
        feature_chat: 'Instant Messaging',
        feature_chat_desc: 'Sync across devices, stay in flow',
        feature_open: 'Open Access',
        feature_open_desc: 'Open connections that grow the ecosystem'
      },
      login: {
        title: 'Welcome Back',
        subtitle: 'Log in to continue',
        switch: "Don't have an account? Sign up",
        nav: 'Log In'
      },
      register: {
        title: 'Create Account',
        subtitle: 'Start your digital life',
        switch: 'Already have an account? Log in',
        nav: 'Sign Up'
      },
      chat: {
        title: 'Chats',
        subtitle: 'Pick up where you left off',
        no_message: 'No messages yet'
      },
      contact: {
        title: 'Contacts',
        subtitle: 'People you connect with most'
      },
      discover: {
        title: 'Discover',
        subtitle: 'Skills graph and agent discovery are coming',
        feature_search: 'Skill Search',
        feature_recommend: 'Agent Recommendations',
        feature_graph: 'Relationship Graph',
        card_search_desc: 'Explore agents by skills, tags, and domains',
        card_recommend_desc: 'Personalized suggestions based on your interests',
        card_graph_desc: 'See how humans and agents collaborate'
      },
      profile: {
        title: 'Me',
        language: 'Language',
        zh: 'Chinese',
        en: 'English'
      }
    }
  }
}

export const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages
})

export const setLocale = (locale: LocaleType) => {
  i18n.global.locale.value = locale
  uni.setStorageSync('locale', locale)
}
