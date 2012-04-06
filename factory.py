#!/usr/bin/python
#
# Spybot Command and Control Server factory thread
#
import time
import threading
import logging
import logging.handlers
from poller import Poller

class Factory(threading.Thread):
    def __init__(self, loghandler):
        threading.Thread.__init__(self, name="FactoryThread")
        self.loghandler = loghandler
        self.logger = logging.getLogger("Factory")
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(loghandler)
        
        # Ensure children threads are singletons
        self.scheduler = None
        self.poller = None
        
    def run(self):
        """
        Starts children threads if they aren't running yet
        """
        while True:
            # Ensure all necessary threads are running every 60 seconds
            self.startPoller()
            time.sleep(60)
            
    def startPoller(self):
        if self.poller is None:
            self.logger.debug("Starting Poller thread.")
            self.poller = Poller(self.loghandler)
            self.poller.start()
        elif not self.poller.isAlive():
            self.logger.debug("Restarting Poller thread.")
            self.poller.run()
