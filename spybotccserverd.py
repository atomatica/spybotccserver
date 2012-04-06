#!/usr/bin/python
#
# Spybot Command and Control Server host process
#
import sys, os, time
import logging
from signal import SIGTERM
from core import Core

if __name__ == "__main__":
    # Use PID file to ensure Core is a singleton
    pidfile = "/var/run/spybotccserver.pid"

    if len(sys.argv) == 2:
        # Additional parameters
        logfile = "/var/log/spybotccserver.log"
        loglevel = logging.DEBUG
        stdin = "/dev/null"
        stdout = "/var/log/spybotccserver-errors.log"
        stderr = "/var/log/spybotccserver-errors.log"
    
        if 'start' == sys.argv[1]:
            try:
                pf = file(pidfile,'r')
                pid = int(pf.read().strip())
                pf.close()
            except IOError:
                pid = None
    
            if pid:
                sys.stdout.write("Spybot Command and Control Server is already running.\n")
                sys.exit(1)
            
            # Start instance of daemon
            core = Core(pidfile, logfile, loglevel, stdin, stdout, stderr)
            core.start()
            
        elif 'stop' == sys.argv[1]:
            try:
                pf = file(pidfile,'r')
                pid = int(pf.read().strip())
                pf.close()
            except IOError:
                pid = None
    
            if not pid:
                sys.stdout.write("Spybot Command and Control Server is not currently running.\n")
                sys.exit(1)
        
            # Try killing the daemon process    
            try:
                while 1:
                    os.kill(pid, SIGTERM)
                    time.sleep(0.1)
            except OSError, e:
                err = str(e)
                if err.find("No such process") > 0:
                    if os.path.exists(pidfile):
                        os.remove(pidfile)
                else:
                    sys.stdout.write(err)
                    sys.exit(1)

        elif 'restart' == sys.argv[1]:
            try:
                pf = file(pidfile,'r')
                pid = int(pf.read().strip())
                pf.close()
            except IOError:
                pid = None
    
            if pid:
                # Try killing the daemon process    
                try:
                    while 1:
                        os.kill(pid, SIGTERM)
                        time.sleep(0.1)
                except OSError, e:
                    err = str(e)
                    if err.find("No such process") > 0:
                        if os.path.exists(pidfile):
                            os.remove(pidfile)
                    else:
                        sys.stdout.write(err)
                        sys.exit(1)
            else:
                # Do nothing since no instance of daemon is running
                sys.stdout.write("Spybot Command and Control Server is not currently running. Starting new instance.\n")
                
            # Start new instance of daemon
            core = Core(pidfile, logfile, loglevel, stdin, stdout, stderr)
            core.start()
        
        else:
            print "Unknown command"
            sys.exit(2)
            
        sys.exit(0)
        
    else:
        print "usage: %s start|stop|restart" % sys.argv[0]
        sys.exit(2)