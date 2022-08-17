## TradingViewHook

# This branch is no longer maintained
Why? I have found options to be way more profitable due to their nature of leverage. It is much more efficient so I have decided to focus on primarily options.
This is a bot that accepts requests from [TradingView](https://www.tradingview.com/) strategies and executes buy/sell orders on Binance based on the webhook POST data.

# Benefits
- Customize any strategy you want in TradingView to your liking, and it will work with the bot
- Access TradingView **web panel** anywhere in the world, and instantly update your bot's strategy
- Easily manage your bot's strategy from the web, and get accurate and instant back-test results
- Stop or intervene at **anywhere**, **anytime** by simply disabling or pausing your alerts on TradingView
- Easily trade **multiple** coins or time-frames at once (Just add multiple alerts)
- Discord alerts

# Usage
1. Download the latest build _(No public JAR available yet, clone the repo if you know how to build one for yourself)_
2. Run the bot once to generate the files with `java -jar tradingviewhook.jar`
3. Input your Binance API key and secret in the `login.yml` file
4. Customize the `config.yml` file to your liking
5. Run the bot again with `java -jar tradingviewhook.jar`
6. Head over to the TradingView web panel, make sure you are on your desired time-frame and coin, add your strategy to the chart, and in the **Strategy Tester** tab, click on the Create Alert button (Alarm Clock)

![TradingView Setup](addalert.png "TradingView Setup")

7. In the edit alert modal:
  - Set the expiration time as far ahead as possible (Or if you have a paid account, check the Open-ended box) 
  - Check the **Webhook URL** box
  - Copy the _public_ url given to you by the bot, and paste it into the box below the checkbox
  - In the message box, paste the following template in. Set the token field to a secure string of your choosing, make sure this matches the config.yml's `server.token` field (Note, requests are also only allowed from TradingView whitelisted IP's)
      ```text
      {
          "action": "{{strategy.order.action}}",
          "type": "{{strategy.market_position}}",
          "currency": "{{ticker}}",
          "price": {{strategy.order.price}},
          "token": "(your-random-string-here)"
      }
       ```
  - Click the **Save** button (Note: For market orders, set price to "MKT" or -1)

![Customize Alert](modifyalert.png "Customize Alert")

8. **Profit**

# Alert Placeholders
Available placeholders in discord alerts. Some may not be available for on-order-placed.
- {newline} - A newline character, only works in the description field
- {side} - The order side (buy/sell)
- {type} - The order type (market/limit)
- {quantity} - The quantity of the order (Ex 0.056 BTC or 15.25 USDT)
- {crypto} - The crypto currency (Ex BTC, ETH, BNB, etc)
- {limit} - The price the order was executed at (Ex 26,251.00 USDT)
- {price} - The cost of the order (Ex 25.00 USDT)
- {fiat} - The fiat currency (Ex USDT, USD, EUR, etc)
- {bal_fiat} - The new fiat currency balance (Ex $1,000.00)
- {isbuy} - If the order was a buy order "true" or "false"
- {symbol} - The symbol of the order (Ex BTCUSDT)
- Only for sell orders on fill:
  - {profit} - The profit of the order (Ex 24.46 USDT or -0.26 USDT)
  - {profit_percent} - The profit percentage of the order (Ex 0.25% or -1.25%)
  - {profit_color} - The color of the profit, auto set to a red or green color
- {reason} - If the order was rejected, the reason why

### Functions
- upper(placeholder) - Returns the placeholder in all caps
- lower(placeholder) - Returns the placeholder in all lowercase
- if(placeholder, true_val, false_val) - If the placeholder resolves to "true" returns the true_val, otherwise returns the false_val

# Toubleshooting
- Make sure that your server's url can be reached publicly
- **If you are testing the endpoint**: The server does not respond to requests with an un-whitelisted IP. (Even localhost)
- **YOU MAY NEED TO PORT FORWARD** (If running on home network and not a dedicated server or VPS)
- TradingView only allows you to use webhook alerts on a **premium** account at around $10/month
- TradingView may not be able to reach your server, if you are behind a firewall or proxy.
- TradingView only supports ports 80 and 443
- TradingView does not support IPv6
- Currently only supports **LONG** orders. Shorting is dangerous, and not supported as of right now (Short signals will be ignored, and your strategy will work fine as long as it signals to close your long position, and not reverse it)
- Make sure you delete, and re-add the alert if you modify the strategy (TradingView will not use the new strategy)
