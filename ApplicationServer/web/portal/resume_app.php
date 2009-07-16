<?php
/**
 * Copyright (C) 2009 Ulteo SAS
 * http://www.ulteo.com
 * Author Jeremy DESVAGES <jeremy@ulteo.com>
 * Author Julien LANGLOIS <julien@ulteo.com>
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

Logger::debug('main', 'Starting portal/resume_app.php');

if (! isset($_SESSION['session']))
  die2(400, 'ERROR - No $session');

$session = $_SESSION['session'];
$session_owner = (isset($_SESSION['owner']) && $_SESSION['owner']);

$application_dir = SESSION_PATH.'/'.$session.'/sessions/'.$_GET['access_id'];

if ($session_owner) {
  $resume_file = $application_dir.'/owner_is_back';
  if (! is_file($resume_file))
    @touch($resume_file);
}

die();
