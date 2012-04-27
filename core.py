#!/usr/bin/python
#
# Spybot Command and Control Server core process
#
import sys, os, time, atexit, signal
import threading
import logging
import logging.handlers
from factory import Factory
from server import Server, ServerListener

class Core:
    def __init__(self, pidfile, logfile, loglevel, stdin, stdout, stderr):
        self.stdin = stdin
        self.stdout = stdout
        self.stderr = stderr
        self.pidfile = pidfile
        
        # Set up event logging
        self.loghandler = logging.handlers.RotatingFileHandler(logfile, maxBytes=100000, backupCount=10)
        self.loghandler.setFormatter(logging.Formatter("[%(asctime)s] [%(levelname)s] %(name)s: %(message)s"))
        self.loghandler.setLevel(loglevel)
        self.logger = logging.getLogger("Core")
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.loghandler)
    
    def daemonize(self):
        """
        Daemonizes the calling process using the UNIX double-fork method
        """
        try: 
            pid = os.fork() 
            if pid > 0:
                # Exit first parent
                sys.exit(0) 
        except OSError, e: 
            sys.stderr.write("Fork #1 failed: %d (%s)\n" % (e.errno, e.strerror))
            sys.exit(1)
    
        # Decouple from parent environment
        os.chdir("/") 
        os.setsid() 
        os.umask(0) 
    
        # Do second fork
        try: 
            pid = os.fork() 
            if pid > 0:
                # Exit from second parent
                sys.exit(0) 
        except OSError, e: 
            sys.stderr.write("Fork #2 failed: %d (%s)\n" % (e.errno, e.strerror))
            sys.exit(1) 
    
        # Redirect standard file descriptors
        sys.stdout.flush()
        sys.stderr.flush()
        si = file(self.stdin, 'r')
        so = file(self.stdout, 'a+')
        se = file(self.stderr, 'a+', 0)
        os.dup2(si.fileno(), sys.stdin.fileno())
        os.dup2(so.fileno(), sys.stdout.fileno())
        os.dup2(se.fileno(), sys.stderr.fileno())
    
        # Write pidfile
        atexit.register(self.delpid)
        pid = str(os.getpid())
        file(self.pidfile,'w+').write("%s\n" % pid)
    
    def delpid(self):
        os.remove(self.pidfile)

    def start(self):
        """
        Starts the daemon
        """
        # Daemonize the process
        self.daemonize()
        
        # Log daemon starting
        self.logger.info("Starting Spybot Command and Control Server.")
        
        # Run the daemon
        self.run()

    def run(self):
        """
        Main function
        """
        # Create factory
        self.logger.debug("Starting Factory thread.")
        factory = Factory(self.loghandler)
        # Set factory thread as daemon thread to ensure all children threads are daemon threads
        factory.daemon = True
        factory.start()
        
        # Create server
        self.logger.debug("Starting threaded Server.")
        HOST, PORT = "", 9103
        server = Server((HOST, PORT), self.loghandler)
        serverThread = threading.Thread(target=server.serve_forever)
        # Set main server thread as daemon thread to ensure all server threads are daemon threads
        serverThread.setDaemon(True)
        serverThread.start()
        
        # Register signal handler
        signal.signal(signal.SIGTERM, self.handleSignal)

        # Main loop
        while True:
            # Sleep core process until a signal is received from the OS
            signal.pause()
            
    def stop(self):
        """
        Stops the daemon
        """
        # Log daemon stopping
        self.logger.info("Stopping Spybot Command and Control Server.")
        
        # Stop main thread, all children threads are daemon threads and should terminate
        sys.exit(0)
              
    def handleSignal(self, signum, frame):
        """
        Handles asynchronous system signals
        """
        # Override default SIGTERM handler to execute custom exiting routine
        if signum == signal.SIGTERM:
            self.stop()
        
