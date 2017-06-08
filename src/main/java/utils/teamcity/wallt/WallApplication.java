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

package utils.teamcity.wallt;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.sun.glass.ui.Screen;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.teamcity.wallt.controller.api.ApiModule;
import utils.teamcity.wallt.controller.api.ApiRequestModule;
import utils.teamcity.wallt.controller.api.IApiMonitoringService;
import utils.teamcity.wallt.controller.configuration.ConfigurationController;
import utils.teamcity.wallt.controller.configuration.ConfigurationModule;
import utils.teamcity.wallt.model.build.BuildDataModule;
import utils.teamcity.wallt.model.event.SceneEvent;
import utils.teamcity.wallt.model.logger.Loggers;
import utils.teamcity.wallt.view.UIUtils;
import utils.teamcity.wallt.view.configuration.ConfigurationScene;
import utils.teamcity.wallt.view.configuration.ConfigurationViewModule;
import utils.teamcity.wallt.view.wall.WallScene;
import utils.teamcity.wallt.view.wall.WallViewModule;

import java.nio.file.Paths;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Date: 09/02/14
 *
 * @author Cedric Longo
 */
public final class WallApplication extends Application {

    public static final int MIN_WIDTH = 1024;
    public static final int MIN_HEIGHT = 600;
    public static final Logger LOGGER = LoggerFactory.getLogger( Loggers.MAIN );

    private final Injector _injector;
    private final ExecutorService _executorService;
    private final ScheduledExecutorService _scheduledExecutorService;
    private final IApiMonitoringService _apiMonitoringService;
    private final EventBus _eventBus;

       
    private static boolean _doRunAppAutomatically = false; 
    private static boolean _startMaximized = false;
    private static Stage _primaryStage;
    private static int _screenIndex = 0;

    public WallApplication( ) {
        LOGGER.info( "Starting ..." );
        _injector = Guice.createInjector( modules( ) );
        _executorService = _injector.getInstance( ExecutorService.class );
        _scheduledExecutorService = _injector.getInstance( ScheduledExecutorService.class );
        _eventBus = _injector.getInstance( EventBus.class );
        _apiMonitoringService = _injector.getInstance( IApiMonitoringService.class );
    }

    public static void main( final String[] args ) {
    	// Parse before standard Java FX parse scheme in order to avoid problems with injection framework
    	parseCommandlineArgs(args);
        Application.launch( WallApplication.class, args );
    }
    
    private static void parseCommandlineArgs(final String[] args) {
    	for (int i=0; i < args.length; ++i) {
    		if ("--help".equals(args[i])) {
    			System.out.println("===================================================================");
    			System.out.println("WallApplication Command Line Options");
    			System.out.println("--help : print help");
    			System.out.println("--config <custom_config_file>.json : runs the application with the custom_config_file.json. Default configuration is config.json");
    			System.out.println("--auto : runs the application with the config.json and connects to the server and switches to wall view automatically");
    			System.out.println("--maximized : starts the application with a maximized application window");
    			System.out.println("--screen : Choose screen index (0 is primary and counting up)");
    			System.out.println("===================================================================");
    			System.exit(0);
    		} else if ("--config".equals(args[i])) {
    			i++;
    			// Expecting config file to run
    			ConfigurationController.setFilePath(Paths.get(args[i]));  			
    		} else if ("--auto".equals(args[i])) {
    			_doRunAppAutomatically = true;
    		} else if ("--maximized".equals(args[i])) {
    			_startMaximized = true;
    		}  else if ("--screen".equals(args[i])) {
    			i++;
    			_screenIndex = Integer.parseInt(args[i]);
    		}
    	}
    }

    public static List<Module> modules( ) {
        return ImmutableList.<Module>of(
                new WallApplicationModule( ),
                new ThreadingModule( ),
                new ApiModule( ),
                new ApiRequestModule( ),
                new BuildDataModule( ),
                new ConfigurationModule( ),
                new ConfigurationViewModule( ),
                new WallViewModule( )
        );
    }

    @Override
    public void init( ) throws Exception {
        _eventBus.register( this );
        super.init( );
        
		// Import auto settings form command line
		if (_doRunAppAutomatically) { 
			ConfigurationController.setDoAutoServerConnect(true);
			ConfigurationController.setDoAutoSwitchToWall(true);
		}
    }

    @Override
    public void start( final Stage primaryStage ) throws Exception {
        _primaryStage = primaryStage;
        
        primaryStage.setTitle( "Wall-T - Teamcity Radiator - " + ConfigurationController.getFilePath() );
        primaryStage.getIcons( ).addAll( UIUtils.createImage( "icons/icon.png" ) );

        primaryStage.setMinWidth( MIN_WIDTH );
        primaryStage.setMinHeight( MIN_HEIGHT );
        primaryStage.setWidth( MIN_WIDTH );
        primaryStage.setHeight( MIN_HEIGHT );
        primaryStage.setMaximized(_startMaximized);
        Screen secondaryScreen = Screen.getScreens().get(_screenIndex);
        int boundsX = secondaryScreen.getX();
        int boundsY = secondaryScreen.getY();
        primaryStage.setX(boundsX+100);
        primaryStage.setY(boundsY+100);

        _apiMonitoringService.start( );

        primaryStage.show( );

        _eventBus.post( new SceneEvent( ConfigurationScene.class ) );
    }
    
    public static void setTitle()
    {
    	_primaryStage.setTitle( "Wall-T - Teamcity Radiator - " + ConfigurationController.getFilePath() );
    }

    @Override
    public void stop( ) throws Exception {
        LOGGER.info( "Stopping ..." );
        LOGGER.info( "----\n" );
        _injector.getInstance( AsyncHttpClientConfig.class ).executorService( ).shutdownNow( );
        _injector.getInstance( AsyncHttpClient.class ).close( );

        _executorService.shutdownNow( );
        _scheduledExecutorService.shutdownNow( );
        super.stop( );
    }

    @Subscribe
    public void requestScene( final SceneEvent sceneType ) {
        final Scene scene = _injector.getInstance( sceneType.getType( ) );
        scene.getAccelerators( ).put( new KeyCodeCombination( KeyCode.F11 ),
                ( ) -> {
                    _primaryStage.setFullScreen( !_primaryStage.isFullScreen( ) );
                } );
        scene.getAccelerators( ).put( new KeyCodeCombination( KeyCode.ESCAPE ),
                ( ) -> {
                    _eventBus.post( new SceneEvent( ConfigurationScene.class ) );
                } );
        LOGGER.info( "Change scene to " + sceneType.getType( ).getSimpleName( ) );
        _primaryStage.setScene( scene );

        if ( scene instanceof WallScene )
            _apiMonitoringService.activate( );
        else
            _apiMonitoringService.pause( );
    }
}
