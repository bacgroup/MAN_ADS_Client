Name: ovd-web-client
Version: @VERSION@
Release: @RELEASE@

Summary: Ulteo Open Virtual Desktop - web client
License: GPL2
Group: Applications/System
Vendor: Ulteo SAS
URL: http://www.ulteo.com
Packager: Samuel Bovée <samuel@ulteo.com>
Distribution: RHEL 5.5

Source: %{name}-%{version}.tar.gz
BuildArch: noarch
Buildrequires: intltool
Buildroot: %{buildroot}

%description
This is a web based client for Ulteo OVD.

###########################################
%package -n ulteo-ovd-web-client
###########################################

Summary: Ulteo Open Virtual Desktop - web client
Group: Applications/System
Requires: php, ulteo-ovd-applets, ulteo-ovd-l10n

%description -n ulteo-ovd-web-client
This is a web based client for Ulteo OVD.

%prep -n ulteo-ovd-web-client
%setup -q

%build -n ulteo-ovd-web-client
./configure --prefix=/usr --sysconfdir=/etc --without-ulteo-applets

%install -n ulteo-ovd-web-client
make DESTDIR=$RPM_BUILD_ROOT install
cp -a ajaxplorer $RPM_BUILD_ROOT/usr/share/ulteo/webclient

%post -n ulteo-ovd-web-client
A2CONFDIR=/etc/apache2/conf.d
CONFDIR=/etc/ulteo/webclient

a2enmod php5 > /dev/null

if [ ! -e $A2CONFDIR/webclient.conf ]; then
    ln -sf $CONFDIR/apache2.conf $A2CONFDIR/webclient.conf
    if apache2ctl configtest 2>/dev/null; then
        service apache2 reload || true
    else
        echo << EOF
"Your apache configuration is broken!
Correct it and restart apache."
EOF
    fi
fi

%postun -n ulteo-ovd-web-client
A2CONFDIR=/etc/apache2/conf.d
if [ -e /etc/apache2/conf.d/webclient ]; then
    rm -f $A2CONFDIR/webclient
    if apache2ctl configtest 2>/dev/null; then
        service apache2 reload || true
    else
        echo << EOF
"Your apache configuration is broken!
Correct it and restart apache."
EOF
    fi
fi

%clean -n ulteo-ovd-web-client
rm -rf $RPM_BUILD_ROOT

%files -n ulteo-ovd-web-client
%defattr(-,root,root)
/usr/*
/etc/*
%config /etc/ulteo/webclient/apache2.conf
%config /etc/ulteo/webclient/config.inc.php

%changelog -n ulteo-ovd-web-client
* Wed Sep 01 2010 Samuel Bovée <samuel@ulteo.com> 99.99.svn05193
- Initial release

##############################################
%package -n ulteo-ovd-web-client-ajaxplorer
##############################################

Summary: Ulteo Open Virtual Desktop - Ajaxplorer portal
Group: Applications/System
Requires: ulteo-ovd-web-client

%description -n ulteo-ovd-web-client-ajaxplorer
This is a web based client for Ulteo OVD.

%files -n ulteo-ovd-web-client-ajaxplorer
%defattr(-,root,root)
/usr/share/ulteo/webclient/ajaxplorer
%defattr(-,apache,apache)
/usr/share/ulteo/webclient/ajaxplorer/server/logs
