# Hermes
币安数字货币量化交易机器人

# 简介
可根据实时行情自动在币安上交易现货的量化交易机器人，支持秒级行情解析、策略分析、交易动作，已经经过数十万USDT的真实金额交易的验证。
在牛市，是可以赚钱的，但在熊市，该交易机器人也一定是会亏钱的。

# 重要
开源只是提供机会给普通程序员一个更广阔的视野，交易有风险，盈利无需打赏，亏损也不负责哈~~

# 功能
1. 毫秒级现货行情获取
2. 实时指标分析计算，目前支持MACD\RSI\BOLL三种
3. 提供多种交易策略，如基于MACD涨跌的交易策略，基于实时价格突变的交易策略等
4. 完备的交易订单管理体系，支持跟踪止盈止损、分段止盈止损、时间止盈止损等
5. 交易动作及行情邮件提醒
6. 可用Swagger来查询所有账户和交易单信息
7. 完整的交易动作日志记录

# 如何使用
## 配置
1. 在 ApiKeyConfig 里配置自己的币安交易API KEY
2. 在 ExchangeInfoConfig 里配置交易账户的金额和交易的币种
3. 在 MailService 里配置邮箱信息用来接受交易结果通知
4. 在 MacdTask 里根据需要选择交易策略

## 启动
1. 服务器运行：用mvn命令打包，并上传jar包到服务器，在服务器启动jar包程序，交易机器人就开始运行啦。
2. 本地运行：本地LvApplication启动


----

# Hermes
Binance Quantitative Trading Robot

# Introduction
A quantitative trading robot that can automatically trade spot on Binance according to real-time market conditions. It supports second-level market analysis, strategy analysis, and trading actions. It has been verified by hundreds of thousands of USDT transactions.
In a bull market, you can make money, but in a bear market, the trading robot will definitely lose money.

Open source only provides opportunities for ordinary programmers to have a broader vision. Transactions are risky, no rewards are required for profits, and no losses are responsible.

# Features
1. Millisecond-level spot market acquisition
2. Real-time indicator analysis and calculation, currently supports three types of MACD\RSI\BOLL
3. Provide a variety of trading strategies, such as trading strategies based on MACD fluctuations, trading strategies based on real-time price changes, etc.
4. Complete transaction order management system, support tracking stop profit and stop loss, segment stop profit and stop loss, time stop profit and stop loss, etc.
5. Transaction action and market email reminder
6. Swagger can be used to query all account and transaction information
7. Complete transaction action logging

# How
1. Configure your own Binance Trading API KEY in ApiKeyConfig
2. Configure the amount of the trading account and the currency of the transaction in ExchangeInfoConfig
3. Configure mailbox information in MailService to receive transaction result notifications
4. Select the trading strategy as needed in MacdTask
5. Use the mvn command to package and upload the jar package to the server. Or start the local LvApplication
6. Start the jar package program on the server, and the trading robot will start running

