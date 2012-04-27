#!/usr/bin/python
#
# Spybot Command and Control Server threaded TCP server
#
import socket
import threading
import SocketServer
import logging
import logging.handlers

class Server(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    def __init__(self, server_address, loghandler):
        self.logger = logging.getLogger("Server")
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(loghandler)
        SocketServer.TCPServer.__init__(self, server_address, ServerListener)
        return
    
    def process_request(self, request, client_address):
        """Start a new thread to process the request."""
        self.logger.info("Handling request (%s, %s)", request, client_address)
        t = threading.Thread(target = self.process_request_thread,
                             args = (request, client_address))
        if self.daemon_threads:
            t.setDaemon (1)
        t.start()

class ServerListener(SocketServer.BaseRequestHandler):
    def handle(self):
        data = self.request.recv(1024)
        response = "OK: " + data
        self.request.send(response)
