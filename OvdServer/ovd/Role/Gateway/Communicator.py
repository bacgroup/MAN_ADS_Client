# -*- coding: utf-8 -*-

# Copyright (C) 2010-2011 Ulteo SAS
# http://www.ulteo.com
# Author Arnaud Legrand <arnaud@ulteo.com> 2010
# Author Samuel BOVEE <samuel@ulteo.com> 2010-2011
# Author Laurent CLOUET <laurent@ulteo.com> 2010-2011
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; version 2
# of the License
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

import asyncore
import httplib
import socket
import xml.etree.ElementTree as parser

from HttpMessage import HttpMessage, page_error
from Config import Config
from ovd.Logger import Logger
from Utils import gunzip, gzip

from OpenSSL import SSL

"""
Common communicators
"""

class Communicator(asyncore.dispatcher):
	
	def __init__(self, sock=None, communicator=None):
		self.communicator = communicator
		self._buffer = ''
		self.closed = False
		asyncore.dispatcher.__init__(self, sock=sock)
	
	
	def handle_read(self):
		self._buffer += self.recv(8192)
	
	
	def readable(self):
		if self.communicator is not None:
			return not self.communicator.closed
		return True
	
	
	def writable(self):
		if self.communicator is None:
			return False
		
		_writable = len(self.communicator._buffer) > 0
		if _writable is False and self.communicator.closed is True:
			self.close()
		return _writable
	
	
	def handle_write(self):
		sent = self.send(self.communicator._buffer)
		self.communicator._buffer = self.communicator._buffer[sent:]
	
	
	def handle_close(self):
		self.close()
		self.closed = True



class SSLCommunicator(Communicator):

	def readable(self):
		if Communicator.readable(self) is False:
			return False
		# hack to support SSL layer
		while self.socket.pending() > 0:
			self.handle_read_event()
		return True


	def handle_read(self):
		try:
			Communicator.handle_read(self)
		except SSL.SysCallError:
			self.handle_close()
		except SSL.ZeroReturnError:
			self.handle_close()
		except SSL.WantReadError:
			pass
		except SSL.Error, e:
			# hack for prevent incomprehensible 'SSL_UNDEFINED_CONST_FUNCTION' error,
			# treated as same as an 'SSL.WantReadError' error
			if e.args[0][0][1] == 'SSL_UNDEFINED_CONST_FUNCTION':
				pass
			else:
				raise
		else:
			return
		
		# error occured
		return -1


	def handle_write(self):
		try:
			Communicator.handle_write(self)
		except SSL.WantWriteError:
			pass



class ServerCommunicator(Communicator):

	def __init__(self, remote=None, communicator=None):
		Communicator.__init__(self, communicator=communicator)
		
		self.set_socket(self.make_socket())
		
		if remote is not None:
			try:
				self.connect(remote)
			except socket.error, e:
				Logger.error("%s:: socket connection failed: %s" % (self.__class__.__name__, e))
	
	
	def make_socket(self):
		return socket.socket(socket.AF_INET, socket.SOCK_STREAM)


"""
RDP Communicators
"""

class RdpClientCommunicator(SSLCommunicator):
	pass


class RdpServerCommunicator(ServerCommunicator):
	pass


"""
HTTP Communicators
"""

class HttpMetaCommunicator(SSLCommunicator):
	
	def __init__(self):
		self.http = HttpMessage()
	
	
	def handle_read(self):
		if self.http.is_body():
			if self._buffer == '':
				self.http = HttpMessage()
			else:
				return
		
		SSLCommunicator.handle_read(self)
		if self.make_http_message() is None:
			return
		self._buffer = self.process()
	
	
	def make_http_message(self):
		if not self.http.is_headers():
			res = self._buffer.partition("\r\n\r\n")
			if res[1] is not '':
				self.http.put_headers(res[0] + "\r\n")
				if self.http.path is not '':
					Logger.debug("Gateway:: HTTP request: " + self.http.path)
				self._buffer = res[2]
			else:
				return None
		
		if not self.http.is_body():
			len_buf = self.http.put_body(self._buffer)
			self._buffer = self._buffer[len_buf:]
			if not self.http.is_body():
				return None
		
		return self.http
	
	
	def process(self):
		raise NotImplemented()



class HttpClientCommunicator(HttpMetaCommunicator, SSLCommunicator):
	
	def __init__(self, sock, ctrl=None, ssl_ctx=None):
		self.f_ctrl = ctrl
		self.ssl_ctx = ssl_ctx

		HttpMetaCommunicator.__init__(self)
		SSLCommunicator.__init__(self, sock)
	
	
	def process(self):
		
		# test path permission
		http_code = self.http.auth()
		if http_code is not httplib.OK:
			self.send(page_error(http_code))
			self.socket.sock_shutdown(socket.SHUT_WR)
			self.handle_close()
			return ''

		# path redirection
		if self.communicator is None:
			addr = None
		else:
			addr = self.communicator.getpeername()[0]
		redirection = self.http.redirect(addr)
		if redirection is not None:
			if self.communicator is not None:
				self.communicator.close()
			remote = ((redirection, Config.https_port), self.ssl_ctx)
			self.communicator = HttpServerCommunicator(remote, communicator=self, ctrl=self.f_ctrl)
		
		# gateway header's tag
		self.http.set_header('Gateway', 'on')
		
		# keep alive header handle
		if not Config.http_keep_alive:
			self.http.set_header('Connection', 'close')
		
		return self.http.show()



class HttpServerCommunicator(HttpMetaCommunicator, ServerCommunicator):
	
	def __init__(self, remote, communicator=None, ctrl=None):
		(addr, self.ssl_ctx) = remote
		self.f_ctrl = ctrl

		HttpMetaCommunicator.__init__(self)
		ServerCommunicator.__init__(self, addr, communicator=communicator)
	
	
	def make_socket(self):
		return SSL.Connection(self.ssl_ctx, ServerCommunicator.make_socket(self))
	
	
	def process(self):
		
		# XML rewriting on start.php request
		if self.communicator.http.path == "/ovd/client/start.php" and \
		   not self.communicator.http.xml_rewrited:
			is_zipped = (self.http.get_header('Content-Encoding') == 'gzip')
			if is_zipped:
				self.http.body = gunzip(self.http.body)
			
			xml = self.rewrite_xml()
			if xml is not None:
				if is_zipped:
					xml = gzip(xml)
				self.http.set_body(xml)
				self.http.xml_rewrited = True
		
		return self.http.show()
	
	
	def rewrite_xml(self):
			try:
					session = parser.XML(self.http.body)
					if session.tag.lower() != 'session':
							raise Exception("not a 'session' XML response")
			except Exception, e:
					Logger.error("Gateway:: parsing XML session failed: %s" % e)
					return None
			
			session.set('mode_gateway', 'on')
			for server in session.findall('server'):
					token = self.f_ctrl.send(('insert_token', server.attrib['fqdn']))
					server.set('token', token)
					del server.attrib['fqdn']
			
			return parser.tostring(session)