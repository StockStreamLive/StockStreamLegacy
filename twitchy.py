import threading
import socket
import string
import random
import time

HOST = "192.168.1.107"
PORT = 6667

NICK = "twitchy"
IDENT = "twitchy"
REALNAME = "twitchy"
MASTER = "twitchy"

actions = ("!buy", "!buy", "!sell", "!sell", "!sell", "!buy")
symbols = ("aapl", "amzn", "msft", "nflx", "jnug", "spy", "uwt", "dwt", "amd", "goog", "yhoo", "ha", "fun",
           "brk.b", "QQQ", "TQQq")

def mkmessage():
    message = ""

    message += random.choice(actions)
    if message == "!hold":
        return message

    message += " " + random.choice(symbols)

    return message


class BotThread(threading.Thread):
    def __init__(self, botname):
        threading.Thread.__init__(self)
        self.botname = botname

    def run(self):
        s = socket.socket()
        print("connecting to:" + HOST)
        s.connect((HOST, PORT))
        s.setblocking(False)

        s.send(bytes(("NICK %s\r\n" % self.botname), "UTF-8"))
        s.send(bytes("USER %s %s bla :%s\r\n" % (self.botname, HOST, self.botname), "UTF-8"))
        s.send(bytes("JOIN #twitchinvests\r\n", "UTF-8"))
        s.send(bytes("PRIVMSG %s :Hello Master\r\n" % self.botname, "UTF-8"))

        print('running ' + self.botname)

        while 1:
            try:
                readbuffer = s.recv(1024).decode("UTF-8")
                temp = str.split(readbuffer, "\n")
                readbuffer=temp.pop( )
                print(readbuffer)

                for line in temp:
                    line = str.rstrip(line)
                    line = str.split(line)

                    if line[0] == "PING":
                        s.send(bytes("PONG %s\r\n" % line[1], "UTF-8"))
            except Exception as ex:
                pass
                #print(ex)

            command = mkmessage()
            #print("Sending " + command)

            s.send(bytes('PRIVMSG #stockstream :' + command + ' \r\n', 'UTF-8'))
            time.sleep(.4)


for i in range(250):
    botname = 'twitchy' + repr(int(random.random()*10000))
    t = BotThread(botname)
    t.start()
    if i % 3 == 0:
        time.sleep(1)



"""
while 1:
    readbuffer = readbuffer+s.recv(1024).decode("UTF-8")
    temp = str.split(readbuffer, "\n")
    readbuffer=temp.pop( )

    for line in temp:
        line = str.rstrip(line)
        line = str.split(line)

        if(line[0] == "PING"):
            s.send(bytes("PONG %s\r\n" % line[1], "UTF-8"))
        if(line[1] == "PRIVMSG"):
            sender = ""
            for char in line[0]:
                if(char == "!"):
                    break
                if(char != ":"):
                    sender += char
            size = len(line)
            i = 3
            message = ""
            while(i < size):
                message += line[i] + " "
                i = i + 1
            message.lstrip(":")
            s.send(bytes("PRIVMSG %s %s \r\n" % (sender, message), "UTF-8"))
        for index, i in enumerate(line):
            print(line[index])
"""
