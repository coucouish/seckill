# JAVA高并发秒系统

## 项目优化1.0 页面优化
- 1.添加缓存
- 2.页面的静态化，前后端分离
- 3.静态资源的优化

## 项目优化2.0 服务优化
- 减少数据库的访问
- 增强数据库

## 项目优化3.0 接口安全优化
- 虽然对于秒杀按钮在秒杀前和秒杀后是没法点击的，但点击秒杀按钮本质上也是发一个请求到后端。如果有人提前从http中（明文的）获取到秒杀的地址，并在秒杀开始后使用脚本不停刷新发送秒杀请求，脚本的刷新速度是远高于普通用户的秒杀速度的，这将会导致用户的秒杀体验下降并会给服务器带来访问压力。
- 接口安全优化策略
  - 1.隐藏接口地址
  - 2.验证码防控
  - 3.接口限流
	
	
