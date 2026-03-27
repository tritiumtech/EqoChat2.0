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
      attachment: '附件',
      send: '发送',
      sending: '发送中',
      retry: '重试',
      back: '返回',
      online: '在线',
      connecting: '连接中...',
      connection_ok: '连接正常',
      connection_lost: '连接已断开',
      conversation: '会话',
      status: '状态',
      credit: '信用分',
      logout: '退出登录',
      not_logged_in: '未登录',
      you: '你'
    },
    action: {
      start_chat: '发起聊天',
      login: '登录',
      login_loading: '登录中...',
      register: '注册',
      register_loading: '注册中...',
      send_code: '发送验证码',
      create: '发起',
      add: '添加',
      add_friend: '添加好友',
      send_request: '发送申请',
      accept: '同意',
      reject: '拒绝'
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
      new_contact: '输入用户ID添加好友',
      friend_request_message: '请输入验证信息（选填）'
    },
    toast: {
      coming_soon: '敬请期待',
      request_sent: '申请已发送',
      request_accepted: '已同意',
      request_rejected: '已拒绝',
      cancel: '取消',
      phone_password_required: '请输入手机号和密码',
      fill_required: '请填写完整信息',
      send_code_success: '验证码已发送',
      send_failed: '发送失败',
      login_failed: '登录失败',
      register_failed: '注册失败',
      load_failed: '加载失败',
      copy_ok: '已复制到剪贴板',
      create_failed: '创建失败',
      add_failed: '添加失败',
      message_failed: '发送失败',
      download_coming_soon: '下载地址将于后续版本启用',
      post_published: '发布成功'
    },
    nav: {
      chat: '聊天',
      world: '世界',
      contacts: '联系人',
      me: '我的'
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
        no_message: '暂无消息',
        search_placeholder: '搜索会话…',
        yesterday: '昨天',
        agent_badge: '智能体',
        attach_photo: '照片',
        attach_camera: '拍照',
        attach_file: '文件',
        attach_location: '位置',
        file_size_unit_b: 'B',
        file_size_unit_kb: 'KB',
        file_size_unit_mb: 'MB',
        file_size_unit_gb: 'GB',
        default_photo_name: '照片',
        default_camera_name: '拍照',
        default_file_name: '文件',
        pin: '置顶',
        pinned: '已置顶'
      },
      contact: {
        title: '联系人',
        subtitle: '与你常联系的伙伴',
        friend_requests: '好友申请',
        friends: '好友',
        no_requests: '暂无好友申请',
        add_modal_subtitle: '输入对方 ID 并填写验证信息，等待对方同意',
        add_modal_label_id: '对方用户 ID',
        add_modal_label_message: '验证信息',
        add_modal_optional: '选填',
        detail: '好友详情',
        user_id: '用户 ID',
        search_placeholder: '搜索联系人…',
        filter_all: '全部',
        topics_title: '话题标签',
        topics_empty: '还没有标签，添加后可用于顶部快速筛选',
        topics_input_placeholder: '输入 topic 标签，如：同事 / 家人 / 设计',
        detail_topic_count: '已添加 {n} 个兴趣标签',
        detail_no_topic: '还没有兴趣标签',
        detail_recent_topic: '最近参与了一个话题讨论',
        detail_recent_message: '最近与你有一条对话消息',
        timeline_recent_topic_time: '2小时前',
        timeline_recent_message_time: '1天前',
        default_user_name: '用户{id}',
        empty_filter: '没有匹配的联系人',
        empty_filter_hint: '试试调整搜索或筛选'
      },
      world: {
        title: '世界',
        tab_posts: '动态',
        tab_topics: '话题',
        sort_friends: '好友优先',
        sort_upvotes: '热度优先',
        sort_topics: '常看话题优先',
        posts: '条动态',
        followers: '关注',
        follow: '关注',
        followed: '已关注',
        ai_agent: '智能体',
        friend: '好友',
        upvotes: '赞',
        support: '支持',
        reply: '回复',
        share: '分享',
        new_post_short: '发动态',
        new_post_title: '发动态',
        new_post_placeholder: "有什么想说的？可用 {'@'}好友 和 #话题 …",
        add_media: '添加媒体（可选）',
        pick_image: '图片',
        pick_video: '视频',
        video_limit_hint: '视频最长 15 秒',
        media_tip_image: '已选图片，可清除后更换',
        media_tip_video: '已选视频，可清除后更换',
        media_tip_text: "提示：用 {'@'}名字 提及，用 #标签 归类",
        publish: '发布',
        share_modal_title: '分享动态',
        share_to: '分享到',
        copy_link: '复制链接',
        link_copied: '链接已复制',
        share_message_placeholder: '附言（可选）',
        share_link_loading: '正在获取分享链接…',
        twitter: 'Twitter',
        linkedin: 'LinkedIn',
        facebook: 'Facebook',
        whatsapp: 'WhatsApp',
        telegram: 'Telegram',
        email: '邮件',
        video_too_long: '视频需不超过 15 秒',
        open_browser_tip: '链接已复制，请在浏览器中打开',
        posting: '发布中…',
        clear_media: '清除附件'
      },
      profile: {
        title: '我的',
        language: '语言',
        zh: '中文',
        en: 'English',
        level_short: 'Lv.{n} · {name}',
        points: '{n} 积分',
        points_to_level: '还差 {n} 到 {name}',
        level_max: '已达最高等级',
        level_1: '观察者',
        level_2: '入门',
        level_3: '协作伙伴',
        level_4: '专家',
        level_5: '大师',
        menu_notifications: '通知',
        menu_notifications_desc: '管理提醒与消息',
        menu_settings: '设置',
        menu_settings_desc: '应用偏好设置',
        unread_count: '{n} 条未读',
        menu_privacy: '隐私与安全',
        menu_privacy_desc: '账号与数据保护',
        menu_help: '帮助与反馈',
        menu_help_desc: '使用问题与建议'
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
      attachment: 'Attachment',
      send: 'Send',
      sending: 'Sending',
      retry: 'Retry',
      back: 'Back',
      online: 'Online',
      connecting: 'Connecting...',
      connection_ok: 'Connection OK',
      connection_lost: 'Connection lost',
      conversation: 'Conversation',
      status: 'Status',
      credit: 'Credit Score',
      logout: 'Log Out',
      not_logged_in: 'Not logged in',
      you: 'You'
    },
    action: {
      start_chat: 'Start Chat',
      login: 'Log In',
      login_loading: 'Logging in...',
      register: 'Sign Up',
      register_loading: 'Signing up...',
      send_code: 'Send Code',
      create: 'Start',
      add: 'Add',
      add_friend: 'Add Friend',
      send_request: 'Send Request',
      accept: 'Accept',
      reject: 'Reject'
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
      new_contact: 'Enter user ID to add',
      friend_request_message: 'Verification message (optional)'
    },
    toast: {
      coming_soon: 'Coming soon',
      request_sent: 'Request sent',
      request_accepted: 'Accepted',
      request_rejected: 'Rejected',
      cancel: 'Cancel',
      phone_password_required: 'Phone and password are required',
      fill_required: 'Please complete the form',
      send_code_success: 'Code sent',
      send_failed: 'Failed to send',
      login_failed: 'Login failed',
      register_failed: 'Sign up failed',
      load_failed: 'Failed to load',
      copy_ok: 'Copied to clipboard',
      create_failed: 'Failed to create',
      add_failed: 'Failed to add',
      message_failed: 'Failed to send message',
      download_coming_soon: 'Download URL will be available in a later version',
      post_published: 'Published'
    },
    nav: {
      chat: 'Chats',
      world: 'World',
      contacts: 'Contacts',
      me: 'Me'
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
        no_message: 'No messages yet',
        search_placeholder: 'Search conversations…',
        yesterday: 'Yesterday',
        agent_badge: 'AI',
        attach_photo: 'Photo',
        attach_camera: 'Camera',
        attach_file: 'File',
        attach_location: 'Location',
        file_size_unit_b: 'B',
        file_size_unit_kb: 'KB',
        file_size_unit_mb: 'MB',
        file_size_unit_gb: 'GB',
        default_photo_name: 'photo',
        default_camera_name: 'camera',
        default_file_name: 'file',
        pin: 'Pin',
        pinned: 'Pinned'
      },
      contact: {
        title: 'Contacts',
        subtitle: 'People you connect with most',
        friend_requests: 'Friend Requests',
        friends: 'Friends',
        no_requests: 'No friend requests',
        add_modal_subtitle: 'Enter their ID and verification message, wait for approval',
        add_modal_label_id: 'User ID',
        add_modal_label_message: 'Verification message',
        add_modal_optional: 'optional',
        detail: 'Contact Detail',
        user_id: 'User ID',
        search_placeholder: 'Search contacts…',
        filter_all: 'All',
        topics_title: 'Topic Tags',
        topics_empty: 'No tags yet. Add tags to use quick filters.',
        topics_input_placeholder: 'Enter topic tag, e.g. Work / Family / Design',
        detail_topic_count: '{n} interest tags',
        detail_no_topic: 'No interest tags yet',
        detail_recent_topic: 'Recently joined a topic discussion',
        detail_recent_message: 'Recently sent a message with you',
        timeline_recent_topic_time: '2h ago',
        timeline_recent_message_time: '1d ago',
        default_user_name: 'User{id}',
        empty_filter: 'No contacts match',
        empty_filter_hint: 'Try another search or filter'
      },
      world: {
        title: 'World',
        tab_posts: 'Posts',
        tab_topics: 'Topics',
        sort_friends: 'Friends first',
        sort_upvotes: 'Upvotes first',
        sort_topics: 'Favorite topics first',
        posts: 'posts',
        followers: 'followers',
        follow: 'Follow',
        followed: 'Following',
        ai_agent: 'AI Agent',
        friend: 'Friend',
        upvotes: 'upvotes',
        support: 'Support',
        reply: 'Reply',
        share: 'Share',
        new_post_short: 'New',
        new_post_title: 'New post',
        new_post_placeholder: "What's on your mind? Use {'@'}mentions and #hashtags…",
        add_media: 'Add media (optional)',
        pick_image: 'Image',
        pick_video: 'Video',
        video_limit_hint: 'Video up to 15 seconds',
        media_tip_image: 'Image selected — clear to pick another type',
        media_tip_video: 'Video selected — clear to pick another type',
        media_tip_text: "Tip: use {'@'}name to mention and #tag to categorize",
        publish: 'Publish',
        share_modal_title: 'Share post',
        share_to: 'Share to',
        copy_link: 'Copy link',
        link_copied: 'Link copied',
        share_message_placeholder: 'Add a note (optional)',
        share_link_loading: 'Getting share link…',
        twitter: 'Twitter',
        linkedin: 'LinkedIn',
        facebook: 'Facebook',
        whatsapp: 'WhatsApp',
        telegram: 'Telegram',
        email: 'Email',
        video_too_long: 'Video must be 15 seconds or less',
        open_browser_tip: 'Link copied — open it in your browser',
        posting: 'Posting…',
        clear_media: 'Remove media'
      },
      profile: {
        title: 'Me',
        language: 'Language',
        zh: 'Chinese',
        en: 'English',
        level_short: 'Lv.{n} · {name}',
        points: '{n} pts',
        points_to_level: '{n} pts to {name}',
        level_max: 'Max level reached',
        level_1: 'Observer',
        level_2: 'Amateur',
        level_3: 'Collaborator',
        level_4: 'Expert',
        level_5: 'Master',
        menu_notifications: 'Notifications',
        menu_notifications_desc: 'Alerts & messages',
        menu_settings: 'Settings',
        menu_settings_desc: 'App preferences',
        unread_count: '{n} unread',
        menu_privacy: 'Privacy & Security',
        menu_privacy_desc: 'Account protection',
        menu_help: 'Help & Feedback',
        menu_help_desc: 'Support & suggestions'
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
