<?php
/**
 * Copyright (C) 2008 Ulteo SAS
 * http://www.ulteo.com
 * Author Jeremy DESVAGES <jeremy@ulteo.com>
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
require_once(dirname(__FILE__).'/includes/core.inc.php');

$plugins = new Plugins();
$plugins->doLoad();

$plugins->doInit();

$prefs = Preferences::getInstance();
if (! $prefs)
	die_error('get Preferences failed',__FILE__,__LINE__);

$default_settings = $prefs->get('general', 'session_settings_defaults');
$session_mode = $default_settings['session_mode'];
$windows_keymap = $default_settings['windows_keymap'];
$desktop_size = 'auto';
$desktop_quality = $default_settings['quality'];
$desktop_timeout = $default_settings['timeout'];
$timeout_message = $default_settings['session_timeout_msg'];
$start_app = '';
$persistent = $default_settings['persistent'];
$shareable = $default_settings['shareable'];
$desktop_icons = $default_settings['desktop_icons'];
$allow_shell = $default_settings['allow_shell'];
$app_with_desktop = $default_settings['app_with_desktop'];
$debug = 0;

$default_settings = $prefs->get('general', 'web_interface_settings');
$allow_proxy = $default_settings['allow_proxy'];
$popup = $default_settings['use_popup'];

$advanced_settings = array();
$buf = $prefs->get('general', 'session_settings_defaults');
foreach ($buf['advanced_settings_startsession'] as $v)
	$advanced_settings[] = $v;

$buf = $prefs->get('general', 'web_interface_settings');
foreach ($buf['advanced_settings_startsession'] as $v)
	$advanced_settings[] = $v;

if (! is_array($advanced_settings))
	$advanced_settings = array();

if (! isset($_SESSION['login'])) {
	$ret = do_login();
	if (! $ret)
		die_error(_('Authentication failed'),__FILE__,__LINE__);
}

if (! isset($_SESSION['login']))
	die_error(_('Authentication failed'),__FILE__,__LINE__);

$user_login = $_SESSION['login'];

$mods_enable = $prefs->get('general', 'module_enable');
if (! in_array('UserDB', $mods_enable))
	die_error('Module UserDB must be enabled',__FILE__,__LINE__);

$mod_user_name = 'UserDB_'.$prefs->get('UserDB', 'enable');
$userDB = new $mod_user_name();

$user = $userDB->import($user_login);
if (! is_object($user))
	die_error('User importation failed',__FILE__,__LINE__);

$language = $user->getLocale();

$protocol_vars = array('session_mode', 'language', 'windows_keymap', 'quality', 'timeout', 'application', 'document', 'persistent', 'shareable', 'desktop_icons', 'app_with_desktop', 'popup', 'debug', 'start_app');
foreach ($protocol_vars as $protocol_var) {
	if (in_array($protocol_var, $advanced_settings) && isset($_REQUEST[$protocol_var]) && $_REQUEST[$protocol_var] != '')
		$$protocol_var = $_REQUEST[$protocol_var];
}

$client = 'unknown';
$other_vars = array('timezone', 'client');
foreach ($other_vars as $other_var) {
	if (isset($_REQUEST[$other_var]) && $_REQUEST[$other_var] != '')
		$$other_var = $_REQUEST[$other_var];
}

Logger::debug('main', '(startsession) Now checking for old session');

$ev = new SessionStart(array('user' => $user));

$already_online = 0;
$sessions = Sessions::getByUser($user->getAttribute('login'));
if ($sessions > 0) {
	foreach ($sessions as $session) {
		if ($session->isSuspended()) {
			$old_session_id = $session->id;
			$old_session_server = $session->server;
		} elseif ($session->isAlive()) {
			$already_online = 1;

			$buf = $prefs->get('general', 'session_settings_defaults');
			$buf = $buf['action_when_active_session'];

			if ($buf == 0)
				die_error(_('You already have an active session'),__FILE__,__LINE__);
			elseif ($buf == 1) {
				$invite = new Invite(gen_string(5));
				$invite->session = $session->id;
				$invite->settings = array(
					'invite_email'	=>	$user->getAttribute('displayname'),
					'view_only'		=>	0
				);
				$invite->email = 'none';
				$invite->valid_until = (time()+(60*30));
				Abstract_Invite::save($invite);

				$token = new Token(gen_string(5));
				$token->type = 'invite';
				$token->link_to = $invite->id;
				$token->valid_until = (time()+(60*30));
				Abstract_Token::save($token);

				$server = Abstract_Server::load($session->server);

				redirect('http://'.$server->getAttribute('external_name').'/index.php?token='.$token->id);
			}
		} else
			die_error(_('You already have a session, please contact your administrator'),__FILE__,__LINE__);
	}
}

if (in_array('server', $advanced_settings) && isset($_REQUEST['force']) && $_REQUEST['force'] != '')
	$random_server = $_REQUEST['force'];
else {
	$serv_tmp = $user->getAvailableServer('linux'); // FIXME : session server != linux server...
	if (is_object($serv_tmp))
		$random_server = $serv_tmp->fqdn;

	if ((! isset($random_server)) || is_null($random_server) || $random_server == '') {
		$ev->setAttribute('ok', false);
		$ev->setAttribute('error', _('No available server'));
		$ev->emit();
		Logger::error('main', '(startsession) no linux server found for \''.$user->getAttribute('login').'\' -> abort');
		die_error(_('You don\'t have access to a linux server for now'),__FILE__,__LINE__);
	}
}

if (isset($old_session_id) && isset($old_session_server)) {
	$session = Abstract_Session::load($old_session_id);

	$session_type = 'resume';

	$ret = true;

	Logger::info('main', '(startsession) Resuming session for '.$user->getAttribute('login').' ('.$old_session_id.' => '.$old_session_server.')');
} else {
	$random_session_id = gen_string(5);

	$session_type = 'start';

	$session = new Session($random_session_id);
	$session->server = $random_server;
	$session->mode = $session_mode;
	$session->type = $session_type;
	$session->status = -1;
	$session->user_login = $user->getAttribute('login');
	$session->user_displayname = $user->getAttribute('displayname');

	$ret = true;

	Logger::info('main', '(startsession) Creating new session for '.$user->getAttribute('login').' ('.$random_session_id.' => '.$random_server.')');
}

if ($ret === false)
	die_error(_('No available session'),__FILE__,__LINE__);

$fs = $prefs->get('plugins', 'FS');
// for now we can use one FS at the same time
//if (!is_array($fs) || count($fs) == 0)
if (is_null($fs))
	die_error(_('No available filesystem'),__FILE__,__LINE__);
//$module_fs = $fs[0];
$module_fs = $fs;

$default_args = array(
// 	'user_id'			=>	$user->getAttribute('uid'),
	'client'			=>	$client,
	'user_login'		=>	$user->getAttribute('login'),
	'user_displayname'	=>	$user->getAttribute('displayname'),
	'locale'			=>	locale2unix($language),
	'windows_keymap'	=>	$windows_keymap,
	'quality'			=>	$desktop_quality,
);

$optional_args = array();
if (isset($timezone))
	$optional_args['timezone'] = $timezone;
if (isset($desktop_timeout) && $desktop_timeout != -1) {
	$optional_args['timeout'] = (time()+$desktop_timeout);
	$optional_args['timeout_message'] = $timeout_message;
}
if (isset($start_app) && $start_app != '') {
	$applicationDB = ApplicationDB::getInstance();
	$app = $applicationDB->import($start_app);

	if (! is_object($app)) {
		Logger::error('main', '(startsession) No such application for id \''.$start_app.'\'');
		die_error(_('Application does not exist'), __FILE__, __LINE__);
	}

	$apps = $user->applications();

	$ok = false;
	foreach ($apps as $user_app) {
		if ($user_app->getAttribute('id') == $start_app) {
			$ok = true;
			break;
		}
	}

	if ($ok === false) {
		Logger::error('main', '(startsession) Application not available for user \''.$user->getAttribute('login').'\' id \''.$start_app.'\'');
		die_error(_('Application not available'), __FILE__, __LINE__);
	}

	$optional_args['start_app'] = $app->getAttribute('executable_path');
	$optional_args['start_app_id'] = $start_app;
}
if (isset($open_doc) && $open_doc != '')
	$optional_args['open_doc'] = $open_doc;
if (isset($popup))
	$optional_args['popup'] = (int)$popup;
if (isset($debug) && $debug != '0')
	$optional_args['debug'] = 1;
if (isset($persistent) && $persistent != '0')
	$optional_args['persistent'] = 1;
if (isset($shareable) && $shareable != '0')
	$optional_args['shareable'] = 1;
if (isset($desktop_icons) && $desktop_icons != '0')
	$optional_args['desktop_icons'] = 1;
if (isset($allow_shell) && $allow_shell != '0')
	$optional_args['allow_shell'] = 1;
if (isset($app_with_desktop) && $app_with_desktop != '0')
	$optional_args['app_with_desktop'] = 1;
if (isset($allow_proxy) && $allow_proxy != '0') {
	if (isset($_REQUEST['proxy_host']) && $_REQUEST['proxy_host'] != '') {
		$optional_args['enable_proxy'] = 1;
		if (isset($_REQUEST['proxy_type']))
			$optional_args['proxy_type'] = $_REQUEST['proxy_type'];
		if (isset($_REQUEST['proxy_host']))
			$optional_args['proxy_host'] = $_REQUEST['proxy_host'];
		if (isset($_REQUEST['proxy_port']))
			$optional_args['proxy_port'] = $_REQUEST['proxy_port'];
		if (isset($_REQUEST['proxy_username']))
			$optional_args['proxy_username'] = $_REQUEST['proxy_username'];
		if (isset($_REQUEST['proxy_password']))
			$optional_args['proxy_password'] = $_REQUEST['proxy_password'];
	}
}

switch ($prefs->get('UserDB', 'enable')) {
	case 'activedirectory':
		$prefs_ad = $prefs->get('UserDB', 'activedirectory');
		$windows_login = $user->getAttribute('real_login').'@'.$prefs_ad['domain'];
		break;
	case 'ldap':
		$prefs_ldap = $prefs->get('UserDB', 'ldap');
		if ($prefs_ldap['ad'] == 1) {
			$buf = $prefs_ldap['suffix'];
			$suffix = suffix2domain($buf);
			if (! is_string($suffix)) {
				Logger::error('main', 'LDAP suffix is invalid for AD usage : '.$buf);
				break;
			}
			$windows_login = $user->getAttribute('login').'@'.$suffix;
		}
		break;
}

if (isset($windows_login) && $windows_login != '' && count($user->applications('windows')) > 0) {
	$windows_server = $user->getAvailableServer('windows');
	if (is_object($windows_server)) {
		$optional_args['windows_server'] = $windows_server->fqdn;
		$optional_args['windows_login'] = $windows_login;
		$optional_args['windows_password'] = $_SESSION['password'];
	} else {
		Logger::error('main', '(startsession) No windows server available for user \''.$user->getAttribute('login').'\'');
		die_error(_('You don\'t have access to a windows server for now'),__FILE__,__LINE__);
	}
}

$plugins->doStartsession(array(
	'fqdn'	=>	$session->server,
	'session'	=>	$session->id
));

$plugins_args = array();
foreach ($plugins->plugins as $plugin) {
	foreach ($plugin->redir_args as $k => $v)
		if ($k != 'session')
			$plugins_args[$k] = $v;

	if (substr(get_class($plugin), 0, 3) == 'FS_')
		$plugins_args['home_dir_type'] = $plugin->getHomeDirType();
}

$data = array();
foreach ($default_args as $k => $v)
	$data[$k] = $v;
foreach ($optional_args as $k => $v)
	$data[$k] = $v;
foreach ($plugins_args as $k => $v)
	$data[$k] = $v;

$session->setAttribute('settings', $data);
$session->setAttribute('start_time', time());
$save_session = Abstract_Session::save($session);
if ($save_session === true) {
	Logger::info('main', '(startsession) session \''.$session->id.'\' actually saved on DB for user \''.$user->getAttribute('login').'\'');
}
else {
	Logger::error('main', '(startsession) failed to save session \''.$session->id.'\' for user \''.$user->getAttribute('login').'\'');
	die_error(_('Internal error'), __FILE__, __LINE__);
}

$token = new Token(gen_string(5));
$token->type = $session_type;
$token->link_to = $session->id;
$token->valid_until = (time()+(60*5));
$save_token = Abstract_Token::save($token);
if ($save_token === false) {
	Logger::error('main', '(startsession) failed to save token \''.$token.'\' for session \''.$session->id.'\'');
	die_error(_('Internal error'), __FILE__, __LINE__);
}

$buf = Abstract_Server::load($session->server);

$redir = $buf->getBaseURL(true).'/index.php?token='.$token->id;

$ev->setAttributes(array(
	'ok'	=> true,
	'server'	=>	$session->server,
	'resume'	=>	$session->isSuspended(),
	'token'	=>	$token->id,
	'sessid'	=>	$session->id
));
$ev->emit();

if (isset($_SESSION['login']))
	unset($_SESSION['login']);

redirect($redir);
