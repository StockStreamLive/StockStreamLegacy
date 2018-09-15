import os
from mutagen.easyid3 import EasyID3
from mutagen.mp3 import MP3
import eyed3

for root, dirs, files in os.walk("music/"):
	for name in files:
		try:
			fullpath = os.path.join(root, name)
			audio = MP3(fullpath, ID3=EasyID3)
			audio.pprint()
			#trackInfo = eyed3.Mp3AudioFile(fullpath)
			#tag = trackInfo.getTag()
			#tag.link(path)
			print(audio)
		except Exception as e:
			print(e)