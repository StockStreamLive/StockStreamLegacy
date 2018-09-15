example = "2017-05-31 06:10:01,990 [pool-2-thread-16] INFO  ScoreEngine - TradeChoice ScoreEngine.TradeChoice(player=, command=BUY TSLA, lastTradePrice=337.74, isPossible=true) resulted in score -0.06809394072357403 with recent quote of Quote(robinhoodQuote={ask_price=337.7900, ask_size=100, bid_price=337.6200, bid_size=100, last_trade_price=335.1000, last_extended_hours_trade_price=337.5100, previous_close=325.1400, adjusted_previous_close=325.1400, previous_close_date=2017-05-26, symbol=TSLA, trading_halted=false, has_traded=true, last_trade_price_source=consolidated, updated_at=2017-05-31T13:08:37Z, instrument=https://api.robinhood.com/instruments/e39ed23a-7bd1-4587-b060-71988d9ef483/})"

import sys
import json

def getTimeStamp(line):
	return line.split(" ")[0] + " " + line.split(" ")[1]

def getCommandType(line):
	t = line.split("command=")[1].split(" ")[0]
	return t

def getSymbol(line):
	t = line.split("command=")[1].split(" ")[1]
	return t

def getUsername(line):
	#print line
	name = line.split("player=")[1]
	name = name.split(",")[0]
	if len(name) == 0:
		name = "StockStream"
	return name

def getBeforePrice(line):
	price = line.split("=")[3].split(",")[0]
	return price

def getAfterPrice(line):
	price = line.split("last_trade_price=")[1].split(",")[0]
	priceAH = line.split("last_extended_hours_trade_price=")[1].split(",")[0]

	realprice = price
	if not priceAH == "null":
		realprice = priceAH

	return realprice

def get_change(current, previous):
	if current == previous:
		return 0
	try:
		return round((current - previous)/previous * 100.0, 4)
	except ZeroDivisionError:
		return 0

fdata = open(sys.argv[1]).read()
lines = fdata.split("\n")

scores = {}
scorecount = {}

votes = 0

for line in lines:
	if len(line) == 0:
		continue
	timestamp = getTimeStamp(line)
	username = getUsername(line)
	beforePrice = float(getBeforePrice(line))
	afterPrice = float(getAfterPrice(line))
	ctype = getCommandType(line)
	symbol = getSymbol(line)

	if username not in scores:
		scores[username] = 0
		scorecount[username] = 0

	newScore = get_change(afterPrice, beforePrice)
	if ctype == "SELL":
		newScore = newScore * -1

	#print `newScore` + "|" + timestamp + " - " + username + " - " + ctype + " " + symbol + " " + `beforePrice` + " -> " + `afterPrice` + " = " + `newScore`
	#print "-----------------------------------\n"
	scores[username] = scores[username] + newScore
	scorecount[username] = scorecount[username] + 1

	votes += 1

#for name in scores:
#	print `scores[name]` + "|" + name
#print len(scores)
#print votes
#print scores
print json.dumps(scores)
