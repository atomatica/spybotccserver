#!/usr/bin/python
#
# Spybot Command and Control Server threaded TCP server
#
import socket
import threading
import SocketServer

class Server(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass

class ServerListener(SocketServer.BaseRequestHandler):
    def handle(self):
        data = self.request.recv(1024)
        response = "OK: " + data
        self.request.send(response)
