<?php
/**
 * Copyright (C) 2008 Ulteo SAS
 * http://www.ulteo.com
 * Author Jeremy DESVAGES <jeremy@ulteo.com>
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

if (isset($_GET['action']) && $_GET['action'] == 'get_image') {
	header('Content-Type: image/png');
	die(query_url(SESSIONMANAGER_URL.'/webservices/icon.php?id='.$_GET['id'].'&fqdn='.SERVERNAME));
}

$session = $_SESSION['session'];

if (!isset($session) || $session == '') {
	Logger::critical('main', '(portal/running_apps) No SESSION');
	die('CRITICAL ERROR'); // That's odd !
}

$apps = explode(',', $_GET['apps']);
foreach ($apps as $k => $app) {
	list($id, $access_id, $status) = explode('-', $app);

	if ($id === '' || $id == 'undefined')
		unset($apps[$k]);
}

echo '<table border="0" cellspacing="1" cellpadding="3">';
foreach ($apps as $k => $app) {
	list($id, $access_id, $status) = explode('-', $app);

	$application = query_url(SESSIONMANAGER_URL.'/webservices/application.php?id='.$id.'&fqdn='.SERVERNAME);

	$dom = new DomDocument();
	@$dom->loadXML($application);

	if (! $dom->hasChildNodes())
		continue;

	$application_node = $dom->getElementsByTagname('application')->item(0);
	if (is_null($application_node))
		continue;

	if ($application_node->hasAttribute('name'))
		$name = $application_node->getAttribute('name');

	$executable_node = $application_node->getElementsByTagname('executable')->item(0);
	if (is_null($executable_node))
		continue;

	if ($executable_node->hasAttribute('command'))
		$command = $executable_node->getAttribute('command');

	echo '<tr>';
	echo '<td><img src="apps.php?action=get_image&id='.$id.'" alt="'.$name.'" title="'.$name.'" /></td>';
	echo '<td><strong>'.$name.'</strong>';
	if (isset($_SESSION['parameters']['persistent'])) {
		echo ' ';
		if ($status == 2)
			echo '<a href="javascript:;" onclick="return suspendApplication(\''.$access_id.'\');">'._('suspend').'</a>';
		elseif ($status == 10)
			echo '<a href="javascript:;" onclick="return resumeApplication(\''.$access_id.'\');">'._('resume').'</a>';
	}
	echo '</td>';
	echo '</tr>';
}
echo '</table>';
