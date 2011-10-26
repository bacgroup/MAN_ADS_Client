/**
 * Copyright (C) 2011 Ulteo SAS
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

var ULTEO_OVD_SESSION_MODE_DESKTOP = "desktop";
var ULTEO_OVD_SESSION_MODE_APPLICATIONS = "applications";

function UlteoOVD_session(web_client_url_, mode_) {
	this.web_client_url = web_client_url_;
	this.mode = mode_;
	
	this.extra_args = new Object();
	
	if (this.mode == ULTEO_OVD_SESSION_MODE_APPLICATIONS) {
		this.my_width = 436;
		this.my_height = 270;
		this.pos_top = screen.height-270;
		this.pos_left = screen.width-436;
	}
	else {
		this.my_width = screen.width;
		this.my_height =  screen.height;
		this.pos_top = 0;
		this.pos_left = 0;
	}
	
	
	this.setLanguage = function(language_) {
		this.extra_args['language'] = language_;
	};
	
	
	this.setAuthPassword = function(login_, password_) {
		this.extra_args['login'] = login_;
		this.extra_args['password'] = password_;
	};
	
	
	this.setAuthToken = function(token_) {
		this.extra_args['token'] = token_;
	};
	
	
	this.setApplication = function(application_) {
		this.extra_args['app'] = application_;
	};
	
	
	this.setPath = function(path_) {
		this.extra_args['file'] = path_;
		this.extra_args['file_share'] = path_;
		this.extra_args['file_type'] = 'native';
	};
	
	this.setPathSharedFolder = function(share_name_, path_) {
		this.extra_args['file'] = path_;
		this.extra_args['file_share'] = share_name_;
		this.extra_args['file_type'] = 'sharedfolder';
	};
	
	this.setPathHTTP = function(url_, path_) {
		this.extra_args['file'] = path_;
		this.extra_args['file_share'] = url_;
		this.extra_args['file_type'] = 'http';
	};
	
	
	this.start = function() {
		this.openPopup(this.getURL());
	};
	
	
	this.getURL = function() {
		var url = this.web_client_url+'/external.php?mode='+this.mode;
		
		for (var k in this.extra_args)
			url += '&'+k+'='+this.extra_args[k];
		
		return url;
	};
	
	
	this.openPopup = function(url_) {
		var date = new Date();
		var rand = Math.round(Math.random()*100)+date.getTime();
		
		var w = window.open(url_, 'Ulteo'+rand, 'width='+this.my_width+',height='+this.my_height+',top='+this.pos_top+',left='+this.pos_left+',toolbar=no,status=no,scrollbars=no,resizable=no,resizeable=no,fullscreen=no');
		
		return true;
	};
}
