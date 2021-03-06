/*******************************************************************************
 * Copyright 2014 Cedric Longo.
 *
 * This file is part of Wall-T program.
 *
 * Wall-T is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Wall-T is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Wall-T.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package utils.teamcity.wallt.model.configuration;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import utils.teamcity.wallt.controller.api.ApiVersion;

import java.util.List;

/**
 * Date: 09/02/14
 *
 * @author Cedric Longo
 */
public final class Configuration {

    @SerializedName("proxy.use")
    private boolean _useProxy;

    @SerializedName("proxy.host")
    private String _proxyHost;

    @SerializedName("proxy.port")
    private int _proxyPort = 80;

    @SerializedName("proxy.credentials.user")
    private String _proxyCredentialsUser;

    @SerializedName("proxy.credentials.password")
    private String _proxyCredentialsPassword;

    @SerializedName("server.url")
    private String _serverUrl;

    @SerializedName("server.credentials.user")
    private String _credentialsUser = "guest";

    @SerializedName("server.credentials.password")
    private String _credentialsPassword;

    @SerializedName("api.version")
    private ApiVersion _apiVersion = ApiVersion.API_8_0;

    @SerializedName("pref.max.tiles.by.column")
    private int _maxTilesByColumn = 4;

    @SerializedName("pref.max.tiles.by.row")
    private int _maxTilesByRow = 4;

    @SerializedName("pref.light.mode")
    private boolean _lightMode;

    @SerializedName("monitored_builds")
    private List<SavedBuildTypeData> _savedBuilds = Lists.newArrayList( );

    @SerializedName("monitored_projects")
    private List<SavedProjectData> _savedProjects = Lists.newArrayList( );

    //NOTE(teld): Flags in Configuration to do automatic server connect and switch to wall
    public boolean _doAutoServerConnect = false;
    public boolean _doAutoSwitchToWall = false;
    
    public String getServerUrl( ) {
        return _serverUrl;
    }

    public void setServerUrl( final String serverUrl ) {
        _serverUrl = serverUrl;
    }

    public String getCredentialsUser( ) {
        return _credentialsUser;
    }

    public void setCredentialsUser( final String credentialsUser ) {
        _credentialsUser = credentialsUser;
    }

    public String getCredentialsPassword( ) {
        return _credentialsPassword;
    }

    public void setCredentialsPassword( final String credentialsPassword ) {
        _credentialsPassword = credentialsPassword;
    }

    public List<SavedBuildTypeData> getSavedBuildTypes( ) {
        return _savedBuilds;
    }

    public void setSavedBuilds( final List<SavedBuildTypeData> savedBuilds ) {
        _savedBuilds = savedBuilds;
    }

    public List<SavedProjectData> getSavedProjects( ) {
        return _savedProjects;
    }

    public void setSavedProjects( final List<SavedProjectData> savedProjects ) {
        _savedProjects = savedProjects;
    }

    public ApiVersion getApiVersion( ) {
        return _apiVersion;
    }

    public int getMaxTilesByColumn( ) {
        return _maxTilesByColumn;
    }

    public void setMaxTilesByColumn( final int maxTilesByColumn ) {
        _maxTilesByColumn = maxTilesByColumn;
    }

    public void setApiVersion( final ApiVersion apiVersion ) {
        _apiVersion = apiVersion;
    }

    public int getMaxTilesByRow( ) {
        return _maxTilesByRow;
    }

    public void setMaxTilesByRow( final int maxTilesByRow ) {
        _maxTilesByRow = maxTilesByRow;
    }

    public boolean isLightMode( ) {
        return _lightMode;
    }

    public void setLightMode( final boolean lightMode ) {
        _lightMode = lightMode;
    }

    public boolean isUseProxy( ) {
        return _useProxy;
    }

    public void setUseProxy( final boolean useProxy ) {
        _useProxy = useProxy;
    }

    public String getProxyHost( ) {
        return _proxyHost;
    }

    public void setProxyHost( final String proxyHost ) {
        _proxyHost = proxyHost;
    }

    public int getProxyPort( ) {
        return _proxyPort;
    }

    public void setProxyPort( final int proxyPort ) {
        _proxyPort = proxyPort;
    }

    public String getProxyCredentialsUser( ) {
        return _proxyCredentialsUser;
    }

    public void setProxyCredentialsUser( final String proxyCredentialsUser ) {
        _proxyCredentialsUser = proxyCredentialsUser;
    }

    public String getProxyCredentialsPassword( ) {
        return _proxyCredentialsPassword;
    }

    public void setProxyCredentialsPassword( final String proxyCredentialsPassword ) {
        _proxyCredentialsPassword = proxyCredentialsPassword;
    }
}
