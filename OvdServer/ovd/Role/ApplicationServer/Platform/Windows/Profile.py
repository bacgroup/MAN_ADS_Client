# -*- coding: UTF-8 -*-

# Copyright (C) 2010 Ulteo SAS
# http://www.ulteo.com
# Author Julien LANGLOIS <julien@ulteo.com> 2010
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

import os
import random
import win32api
import win32con
import win32file
import win32netcon
import win32security
import win32wnet
import _winreg

from ovd.Logger import Logger
from ovd.Role.ApplicationServer.Profile import Profile as AbstractProfile

class Profile(AbstractProfile):	
	def init(self):
		self.mountPoint = None
	
	def mount(self):
		buf = self.getFreeLetter()
		if buf is None:
			Logger.warn("No drive letter available: unable to init profile")
			return
		
		self.mountPoint = "%s:"%(buf)
		
		try:
			win32wnet.WNetAddConnection2(win32netcon.RESOURCETYPE_DISK, self.mountPoint, r"\\%s\%s"%(self.host, self.directory), None, self.login, self.password)
		
		except Exception, err:
			Logger.error("Unable to mount drive")
			Logger.debug("Unable to mount drive, '%s', try the net use command equivalent: '%s'"%(str(err), "net use %s \\\\%s\\%s %s /user:%s"%(self.mountPoint, self.host, self.directory, self.password, self.login)))
			
			self.mountPoint = None
			return False
		
		return True
	
	
	def umount(self):
		if self.mountPoint is None:
			return
		
		try:
			win32wnet.WNetCancelConnection2(self.mountPoint, 0, True)
		
		except Exception, err:
			Logger.error("Unable to umount drive")
			Logger.debug("Unable to umount drive, net use command equivalent: '%s'"%("net use %s: /delete"%(self.mountPoint)))
	
	
	def copySessionStart(self):
		for f in [self.DesktopDir, self.DocumentsDir]:
			d = os.path.join(self.mountPoint, f)
			if not os.path.exists(d):
				os.makedirs(d)
		
		
		d = os.path.join(self.mountPoint, "conf.Windows")
		if os.path.exists(d):
			# Copy user registry
			src = os.path.join(d, "NTUSER.DAT")
			
			if os.path.exists(src):
				dst = os.path.join(self.session.windowsProfileDir, "NTUSER.DAT")
				
				try:
					win32file.CopyFile(src, dst, False)
				except:
					Logger.error("Unable to copy registry from profile")
		
		
		print "Should copy AppData"
	
	
	def copySessionStop(self):
		# etre sur que le type est logoff !
		
		
		d = os.path.join(self.mountPoint, "conf.Windows")
		if not os.path.exists(d):
			os.makedirs(d)
		
		# Copy user registry
		src = os.path.join(self.session.windowsProfileDir, "NTUSER.DAT")
		dst = os.path.join(d, "NTUSER.DAT")
		
		if os.path.exists(src):
			try:
				win32file.CopyFile(src, dst, False)
			except:
				Logger.error("Unable to copy registry to profile")
		else:
			Logger.warn("Weird: no NTUSER.DAT in user home dir ...")
		
		print "Should sync AppData"
	
	
	def overrideRegistry(self, hiveName):
		key = win32api.RegOpenKey(win32con.HKEY_USERS, hiveName+r"\Software", 0, win32con.KEY_SET_VALUE)
		win32api.RegCreateKey(key, r"Ulteo")
		win32api.RegCloseKey(key)
		
		key = win32api.RegOpenKey(win32con.HKEY_USERS, hiveName+r"\Software\Ulteo", 0, win32con.KEY_SET_VALUE)
		win32api.RegCreateKey(key, r"ovd")
		win32api.RegCloseKey(key)
		
		key = win32api.RegOpenKey(win32con.HKEY_USERS, hiveName+r"\Software\Ulteo\ovd", 0, win32con.KEY_SET_VALUE)
		win32api.RegSetValueEx(key, "profile_host", 0, win32con.REG_SZ, self.host)
		win32api.RegSetValueEx(key, "profile_directory", 0, win32con.REG_SZ, self.directory)
		win32api.RegSetValueEx(key, "profile_login", 0, win32con.REG_SZ, self.login)
		win32api.RegSetValueEx(key, "profile_password", 0, win32con.REG_SZ, self.password)
		win32api.RegCloseKey(key)
		
		
		# Rediect the Shell Folders to the remote profile
		path = hiveName+r"\Software\Microsoft\Windows\CurrentVersion\Explorer\User Shell Folders"
		
		key = win32api.RegOpenKey(win32con.HKEY_USERS, path, 0, win32con.KEY_SET_VALUE)
		win32api.RegSetValueEx(key, "Desktop",  0, win32con.REG_SZ, os.path.join("U:\\", self.DesktopDir))
		win32api.RegSetValueEx(key, "Personal", 0, win32con.REG_SZ, os.path.join("U:\\", self.DocumentsDir))
		win32api.RegCloseKey(key)
	
	
	def getFreeLetter(self):
		# ToDo: manage a global LOCK system to avoid two threads get the same result
		
		drives = win32api.GetLogicalDriveStrings().split('\x00')[:-1]
	
		for i in "ZYXWVUTSRQPONMLKJIHGFEDCBA":
			letter = "%s:\\"%(i.upper())
			#print letter
			if letter not in drives:
				return i
		
		return None

