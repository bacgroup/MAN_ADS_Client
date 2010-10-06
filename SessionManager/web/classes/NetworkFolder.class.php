<?php
/**
 * Copyright (C) 2010 Ulteo SAS
 * http://www.ulteo.com
 * Author Laurent CLOUET <laurent@ulteo.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 **/
require_once(dirname(__FILE__).'/../includes/core.inc.php');

class NetworkFolder {
	const NF_TYPE_PROFILE = "profile";
	const NF_TYPE_NETWORKFOLDER = "network_folder";

	public $id = NULL;
	public $type = '';
	public $name = '';
	public $server = NULL; // FQDN/ID of the server
	public $status = '';

	public function __construct() {
	}
	
	public function getUsers() {
		$liaisons = Abstract_Liaison::load('UserNetworkFolder', NULL, $this->id);
		if (is_array($liaisons) == false) {
			Logger::error('main', 'NetworkFolder::getUsers()');
			return false;
		}
		
		$userDB = UserDB::getInstance();
		
		$users = array();
		foreach ($liaisons as $liaison) {
			$user = $userDB->import($liaison->element);
			if (! is_object($user))
				continue;
			
			$users[$user->getAttribute('login')] = $user;
		}
		return $users;
	}
	
	public function getUserGroups() {
		$liaisons = Abstract_Liaison::load('UserGroupNetworkFolder', NULL, $this->id);
		if (is_array($liaisons) == false) {
			Logger::error('main', 'NetworkFolder::getUserGroups()');
			return false;
		}
		
		$usergroupDB = UserGroupDB::getInstance();
		
		$usergroups = array();
		foreach ($liaisons as $liaison) {
			$usergroup = $usergroupDB->import($liaison->element);
			if (! is_object($usergroup))
				continue;
			
			$usergroups[$usergroup->getUniqueID()] = $usergroup;
		}
		return $usergroups;
	}
	
	public function addUser($user_) {
		return Abstract_NetworkFolder::add_user_to_NetworkFolder($user_, $this);
	}
	
	public function delUser($user_) {
		return Abstract_NetworkFolder::delete_user_from_NetworkFolder($user_, $this);
	}
	
	public function addUserGroup($usergroup_) {
		return Abstract_NetworkFolder::add_usergroup_to_NetworkFolder($usergroup_, $this);
	}
	
	public function delUserGroup($usergroup_) {
		return Abstract_NetworkFolder::delete_usergroup_from_NetworkFolder($usergroup_, $this);
	}
	
	public function chooseFileServer() {
		$available_servers = Servers::getAvailableByRoleSortedByLoadBalancing(Server::SERVER_ROLE_FS);
		if (is_array($available_servers)) {
			$server = array_shift($available_servers);
			if (is_object($server)) {
				return $server;
			}
		}
		return false;
	}
	
	public function __toString() {
		return get_class($this).'(id \''.$this->id.'\' name \''.$this->name.'\' server \''.$this->server.'\' status \''.$this->status.'\' )';
	}
}
