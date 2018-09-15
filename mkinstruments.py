import urllib2
import json
import os
import requests

DIR = "instruments/"
try:
    os.mkdir(DIR)
except:
    pass

session = requests.session()
session_headers = {
    "Accept": "*/*",
    "Accept-Encoding": "gzip, deflate",
    "Accept-Language": "en;q=1, fr;q=0.9, de;q=0.8, ja;q=0.7, nl;q=0.6, it;q=0.5",
    "Content-Type": "application/x-www-form-urlencoded; charset=utf-8",
    "X-Robinhood-API-Version": "1.70.0",
    "Connection": "keep-alive",
    "User-Agent": "Robinhood/823 (iPhone; iOS 7.1.2; Scale/2.00)"
}

endpoint = "https://api.robinhood.com/instruments/"


def getPageText(url):
    return session.get(url).text

def saveTextToFile(text, filename):
    open(DIR + filename, "wb").write(text)


ep = endpoint
fname = "index"
while ep is not None:
    instruments_str = getPageText(ep)
    saveTextToFile(instruments_str, fname)
    instruments_obj = json.loads(instruments_str)

    if 'next' in instruments_obj:
        ep = instruments_obj['next']
        if ep is None or '=' not in ep:
            break
        fname = ep.split("=")[-1]
    else:
        ep = None
        fname = None

    print fname, ep


