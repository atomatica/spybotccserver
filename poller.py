#!/usr/bin/python
#
# Spybot Command and Control Server polling thread
#
import time
import threading
import logging
import logging.handlers

class Poller(threading.Thread):
    def __init__(self, loghandler):
        threading.Thread.__init__(self, name="PollerThread")
        self.logger = logging.getLogger("Poller")
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(loghandler)
        
    def run(self):
        while True:
            self.logger.info("Polling for changes...")
            time.sleep(1000)