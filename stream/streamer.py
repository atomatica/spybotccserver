#!/usr/bin/python
#
# Spybot Command and Control Server video streamer
#
import os
from subprocess import Popen
from signal import SIGTERM

p = None

def startStream():
    if p is None:
        p = Popen("cvlc" + " v4l2:// :v4l2-dev='/dev/video0' :v4l2-input=2 :v4l2-standard=3 :v4l2-adev='hw' --sout '#transcode{vcodec=mp4v,acodec=mp4a,vb=3000,ab=256,venc=ffmpeg{keyint=80,hurry-up,vt=800000}}:std{access=http,mux=ts,dst=0.0.0.0:8000/stream.mp4}' --ttl 14")
        
def stopStream():
    if p is not None:
        os.kill(p.pid, SIGTERM)
        p = None