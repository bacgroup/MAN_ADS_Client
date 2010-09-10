# -*- coding: utf-8 -*-

# Copyright (C) 2010 Ulteo SAS
# http://www.ulteo.com
# Author Arnaud Legrand <arnaud@ulteo.com> 2010
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

from ovd.Role.Role import Role as AbstractRole
from ovd.Config import Config
from ovd.Logger import Logger
from Dialog import Dialog
from reverseproxy import *

class Role(AbstractRole):
	session_manager = None
	
	def __init__(self, main_instance):
		AbstractRole.__init__(self, main_instance)
		self.dialog = Dialog(self)
		self.has_run = False
		self.REMOTE_SM_FQDN = ""
		self.HTTPS_PORT = 443
		self.RDP_PORT = 3389
		self.FPEM_LOCATION = '/root/ovdserver/ovd/Role/Gateway/pem/gateway.pem'

	def init(self):
		Logger.info("Initiate Gateway")

		if not self.init_config():
			return False
		return True

	@staticmethod
	def getName():
		return "Gateway"

	def stop(self):
		Logger.warn('Closing ovdSlaveServer')
		self.has_run = False
		asyncore.ExitNow()

	def init_config(self):
		if not Config.infos.has_key("session_manager"):
			Logger.error("Role %s need a 'session_manager' config key"%(self.getName()))
			return False
		self.session_manager =  Config.session_manager
		return True

	def run(self):
		self.has_run = True
		self.REMOTE_SM_FQDN = self.session_manager
		ReverseProxy(self.FPEM_LOCATION, self.HTTPS_PORT, self.REMOTE_SM_FQDN, self.HTTPS_PORT, self.RDP_PORT)
		Logger.info('Gateway is running !')
		asyncore.loop()
		Logger.info('Closing gateway...')
