export default {
  c: {
    createOnce: '创建之后不能修改',
    projectName: '项目名称',
    runProject: '运行项目',
    selectAuthPath: '请选择项目授权路径',
    viewNodeScript: '查看节点脚本',
    configuration: '配置',
    configExample: '配置示例',
    defaultLogPath: '默认是在插件端数据目录/${projectId}/${projectId}.log'
  },
  p: {
    loadingData: '加载项目数据中...',
    projectId: '项目 ID',
    randomGenerate: '随机生成',
    groupName: '分组名称：',
    addNewGroup: '新增分组',
    selectGroup: '选择分组',
    runMode: '运行方式',
    customizeProject: '配合脚本模版实现自定义项目管理',
    staticFolder: '项目为静态文件夹',
    noStatusControl: '没有项目状态以及控制等功能',
    selectRunMode: '请选择运行方式',
    notRecommended: '不推荐',
    softLinkProject: '软链的项目',
    selectSoftLinkProject: '请选择软链的项目',
    projectPath: '项目路径',
    authPathDesc: '授权路径是指项目文件存放到服务中的文件夹',
    modifyAuthConfig: '可以到节点管理中的【插件端配置】=>【授权配置】修改',
    folderName: '项目文件夹是项目实际存放的目录名称',
    storagePath: '项目文件会存放到',
    fullPath: '项目授权路径+项目文件夹',
    preConfigAuthDir: '需要提前为机器配置授权目录',
    quickConfig: '快速配置',
    storageFolder: '项目存储的文件夹',
    completePath: '项目完整目录',
    content: '内容',
    configFormat:
      '以 yaml/yml 格式配置,scriptId 为项目路径下的脚本文件的相对路径或者脚本模版ID，可以到脚本模版编辑弹窗中查看 scriptId',
    supportedVars: '脚本里面支持的变量有：${PROJECT_ID}、${PROJECT_NAME}、${PROJECT_PATH}',
    outputFormat: '流程执行完脚本后，输出的内容最后一行必须为：running',
    processId: '为当前项目实际的进程ID',
    statusCheck: '。如果输出最后一行不是预期格式项目状态将是未运行',
    configReference: '配置详情请参考配置示例',
    useNodeScript: '可以使用节点脚本：',
    fillDSL: '请填写项目 DSL 配置内容,可以点击上方切换 tab 查看配置示例',
    logDir: '日志目录',
    logDirDesc: '日志目录是指控制台日志存储目录',
    selectableList: '可选择的列表和项目授权目录是一致的，即相同配置',
    mainClass: '程序运行的 main 类(jar 模式运行可以不填)',
    fillInXxx: '填写【xxx',
    jvmOptions: '-Dext.dirs=xxx: -cp xx  :xx】',
    jvmParameters: 'JVM 参数',
    parameters: '参数',
    optional: '非必填',
    jvmSettings: 'jvm,.如：-Xms512m -Xmx512m',
    argsParameters: 'args 参数',
    functionArgs: '函数 args 参数，非必填',
    argsExample: '如：--server',
    dslEnvVariables: 'DSL环境变量',
    environmentVariables: '环境变量',
    envExample: '如：key1',
    autoStart: '自启动',
    checkProjectStatusOnStartup: '插件端启动的时候检查项目状态，如果项目状态是未运行则尝试执行启动项目',
    notAutoStartOnBoot: '非服务器开机自启,如需开机自启建议配置',
    pluginAutoStartOnBoot: '插件端开机自启',
    enableAutoStartSwitch: '并开启此开关',
    switchOn: '开',
    switchOff: '关',
    checkAndStartOnPluginStartup: '插件端启动时自动检查项目如未启动将尝试启动',
    disableScanning: '禁止扫描',
    disableScanningForLargeProjects: '如果项目目录较大或者涉及到深目录，建议关闭扫描避免获取项目目录扫描过长影响性能',
    noScanning: '不扫描',
    enableScanning: '扫描',
    notifyUrl: '项目启动,停止,重启,文件变动都将请求对应的地址',
    notifyUrlParams: '传入参数有：projectId、projectName、type、result',
    notifyUrlValues: '的值有：stop、beforeStop、start、beforeRestart、fileChange',
    projectSpecificTypes: '类型项目特有的 type：reload、restart',
    optionalNotifyUrl: '项目启动,停止,重启,文件变动都将请求对应的地址,非必填，GET请求',
    runCommand: '运行命令',
    none: '无',
    authorizedDirectory: '配置授权目录',
    projectIdInput: '请输入项目ID',
    projectNameInput: '请输入项目名称',
    projectRunModeSelection: '请选择项目运行方式',
    projectFolderInput: '请输入项目文件夹',
    distributionManagement: '独立的项目分发请到分发管理中去修改'
  }
}