/*
 * Copyright (C) 2010 Ulteo SAS
 * http://www.ulteo.com
 * Author Thomas MOUTON <thomas@ulteo.com> 2010
 * Author Guillaume DUPAS <guillaume@ulteo.com> 2010
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
 */

package org.ulteo.ovd.client.remoteApps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.propero.rdp.RdpConnection;
import org.apache.log4j.Logger;
import org.ulteo.ovd.Application;
import org.ulteo.ovd.OvdException;
import org.ulteo.ovd.client.authInterface.AuthFrame;
import org.ulteo.ovd.client.authInterface.LoginListener;
import org.ulteo.ovd.client.portal.Menu;
import org.ulteo.ovd.client.portal.PortalFrame;
import org.ulteo.ovd.integrated.Spool;
import org.ulteo.ovd.integrated.SystemAbstract;
import org.ulteo.ovd.integrated.SystemLinux;
import org.ulteo.ovd.integrated.SystemWindows;
import org.ulteo.rdp.OvdAppChannel;
import org.ulteo.rdp.RdpConnectionOvd;

public class OvdClientPortal extends OvdClientRemoteApps {
	private PortalFrame portal = null;
	private boolean publicated = false;
	private SystemAbstract system = null;
	private Spool spool = null;
	private Thread spoolThread = null;
	private List<Application> appsList = null;

	public OvdClientPortal(String fqdn_, boolean use_https_, String login_, String password_) {
		super(fqdn_, use_https_, login_, password_);

		this.init();
	}

	public OvdClientPortal(String fqdn_, boolean use_https_, String login_, String password_, AuthFrame frame_, LoginListener logList_) {
		super(fqdn_, use_https_, login_, password_, frame_, logList_);
	
		this.init();
	}

	private void init() {
		this.system = (System.getProperty("os.name").startsWith("Windows")) ? new SystemWindows() : new SystemLinux();
		this.logger = Logger.getLogger(OvdClientPortal.class);
		this.appsList = new ArrayList<Application>();
		this.portal = new PortalFrame();
	}

	@Override
	protected void runInit() {}

	@Override
	protected void runExit() {
		Collections.sort(this.appsList);
		
		this.portal.getMain().getCenter().getMenu().initButtons(this.appsList);
	}

	@Override
	protected void customizeRemoteAppsConnection(RdpConnectionOvd co) {
		try {
			co.addOvdAppListener(this.portal.getMain().getCenter().getCurrent());
		} catch (OvdException ex) {
			this.logger.error(ex);
		}

		for (Application app : co.getAppsList()) {
			this.appsList.add(app);
		}
	}

	@Override
	protected void uncustomizeRemoteAppsConnection(RdpConnectionOvd co) {
		try {
			co.removeOvdAppListener(this.portal.getMain().getCenter().getCurrent());
		} catch (OvdException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void ovdInited(OvdAppChannel o) {
		for (RdpConnectionOvd rc : this.availableConnections) {
			if (rc.getOvdAppChannel() == o) {
				Menu menu = this.portal.getMain().getCenter().getMenu();
				for (Application app : rc.getAppsList()) {
					System.out.println("install "+app.getName());
					menu.install(app);
				}
				System.out.println("availableConnections.size(): "+this.availableConnections.size());
				if (menu.isScollerInited())
					menu.addScroller();
			}

			this.portal.initButtonPan(this);
		}
	}

	@Override
	protected void quitProperly(int i) {}

	@Override
	protected void display(RdpConnection co) {
		if (! this.portal.isVisible())
			this.portal.setVisible(true);
	}

	@Override
	protected void hide(RdpConnection co) {
		Menu menu = this.portal.getMain().getCenter().getMenu();

		for (Application app : ((RdpConnectionOvd)co).getAppsList()) {
			menu.uninstall(app);
			this.system.uninstall(app);
		}

		if (this.countAvailableConnection() == 0) {
			this.portal.setVisible(false);
			this.portal.dispose();
		}
	}
	
	public boolean togglePublications() {
		if (publicated) {
			this.unpublish();
		}
		else {
			this.publish();
		}
		return publicated;
	}
	
	public void publish() {
		this.spool = new Spool(this);
		portal.getMain().getCenter().getCurrent().setSpool(spool);
		this.spool.createIconsDir();
		this.spoolThread = new Thread(this.spool);
		this.spoolThread.start();
		for (RdpConnectionOvd rc : this.getAvailableConnections()) {
			for (Application app : rc.getAppsList()) {
				this.system.install(app);
			}
		}
		this.publicated = true;
	}
	
	public void unpublish() {
		for (RdpConnectionOvd rc : this.getAvailableConnections()) {
			for (Application app : rc.getAppsList()) {
					this.system.uninstall(app);
			}
		}
		this.spoolThread.interrupt();
		while (spoolThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				this.logger.error(ex);
			}
		}
		this.spool = null;
		portal.getMain().getCenter().getCurrent().setSpool(spool);
		this.publicated = false;
	}
	
	public SystemAbstract getSystem() {
		return this.system;
	}
}
