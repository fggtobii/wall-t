package utils.teamcity.controller.api.json.v0801;

import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import utils.teamcity.controller.api.ApiControllerBase;
import utils.teamcity.controller.api.IApiRequestController;
import utils.teamcity.model.build.BuildData;
import utils.teamcity.model.build.BuildState;
import utils.teamcity.model.build.BuildTypeData;
import utils.teamcity.model.build.IBuildManager;

import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Futures.addCallback;
import static utils.teamcity.controller.api.json.ApiVersion.API_8_1;

/**
 * Date: 15/02/14
 *
 * @author Cedric Longo
 */
public final class ApiController extends ApiControllerBase {

    @Inject
    public ApiController( final IBuildManager buildManager, final IApiRequestController apiRequestController, final EventBus eventBus, final ExecutorService executorService ) {
        super( buildManager, apiRequestController, eventBus, executorService );
    }

    @Override
    public ListenableFuture<Void> loadBuildList( ) {
        final SettableFuture<Void> ackFuture = SettableFuture.create( );

        runInWorkerThread( ( ) -> {
            final ListenableFuture<BuildTypeList> buildListFuture = getApiRequestController( ).sendRequest( API_8_1, "buildTypes", BuildTypeList.class );
            addCallback( buildListFuture, new FutureCallback<BuildTypeList>( ) {
                @Override
                public void onSuccess( final BuildTypeList result ) {
                    final List<BuildTypeData> buildTypes = result.getBuildTypes( ).stream( )
                            .map( ( btype ) -> new BuildTypeData( btype.getId( ), btype.getName( ), btype.getProjectName( ) ) )
                            .collect( Collectors.toList( ) );
                    getBuildManager( ).registerBuildTypes( buildTypes );
                    getEventBus( ).post( getBuildManager( ) );
                    ackFuture.set( null );
                }

                @Override
                public void onFailure( final Throwable t ) {
                    getLogger( ).error( "Error during loading build type list:", t );
                    ackFuture.setException( t );
                }
            } );
        } );

        return ackFuture;
    }

    @Override
    public void requestQueuedBuilds( ) {
        runInWorkerThread( ( ) -> {
            final ListenableFuture<QueuedBuildList> buildQueueFuture = getApiRequestController( ).sendRequest( API_8_1, "buildQueue", QueuedBuildList.class );
            addCallback( buildQueueFuture, new FutureCallback<QueuedBuildList>( ) {
                @Override
                public void onSuccess( final QueuedBuildList queuedBuildList ) {
                    final Set<String> buildTypesInQueue = queuedBuildList.getQueueBuild( ).stream( )
                            .map( QueueBuild::getBuildTypeId )
                            .collect( Collectors.toSet( ) );
                    final List<BuildTypeData> modifiedStatusBuilds = getBuildManager( ).registerBuildTypesInQueue( buildTypesInQueue );
                    for ( final BuildTypeData buildType : modifiedStatusBuilds )
                        getEventBus( ).post( buildType );
                }

                @Override
                public void onFailure( final Throwable throwable ) {
                    getLogger( ).error( "Error during loading build queue:", throwable );
                }
            } );
        } );
    }


    @Override
    public void requestLastBuildStatus( final BuildTypeData buildType ) {
        runInWorkerThread( ( ) -> {
            final ListenableFuture<BuildList> buildListFuture = getApiRequestController( ).sendRequest( API_8_1, "builds/?locator=buildType:" + buildType.getId( ) + ",running:any,count:" + MAX_BUILDS_TO_CONSIDER, BuildList.class );
            addCallback( buildListFuture, new FutureCallback<BuildList>( ) {
                @Override
                public void onSuccess( final BuildList result ) {
                    // We consider only 5 last builds
                    final List<Build> buildToRequest = result.getBuilds( ).stream( )
                            .limit( MAX_BUILDS_TO_CONSIDER )
                            .collect( Collectors.toList( ) );

                    // We removed from list builds which status is already known
                    buildToRequest.removeIf( build -> {
                        final Optional<BuildData> previousBuildStatus = buildType.getBuildById( build.getId( ) );
                        return previousBuildStatus.isPresent( ) && previousBuildStatus.get( ).getState( ) == BuildState.finished;
                    } );

                    for ( final Build build : buildToRequest ) {
                        final ListenableFuture<Build> buildStatusFuture = getApiRequestController( ).sendRequest( API_8_1, "builds/id:" + build.getId( ), Build.class );
                        addCallback( buildStatusFuture, registerBuildStatus( buildType, build ) );
                    }
                    buildType.touch( );
                }

                @Override
                public void onFailure( final Throwable t ) {
                    getLogger( ).error( "Error during loading builds list for build type: " + buildType.getId( ), t );
                }
            } );
        } );
    }

    private FutureCallback<Build> registerBuildStatus( final BuildTypeData buildType, final Build build ) {
        return new FutureCallback<Build>( ) {
            @Override
            public void onSuccess( final Build result ) {
                buildType.registerBuild( _toBuildData.apply( result ) );
                getEventBus( ).post( buildType );
            }

            @Override
            public void onFailure( final Throwable t ) {
                getLogger( ).error( "Error during loading full information for build with id " + build.getId( ) + ", build type: " + buildType.getId( ), t );
            }
        };
    }

    private final Function<Build, BuildData> _toBuildData = build ->
            new BuildData( build.getId( ), build.getBuildType( ), build.getStatus( ),
                    build.getState( ),
                    build.getState( ) == BuildState.running ? build.getRunningInformation( ).getPercentageComplete( ) : 100,
                    Optional.ofNullable( build.getFinishedDate( ) ),
                    build.getState( ) == BuildState.running ? Duration.of( build.getRunningInformation( ).getEstimatedTotalTime( ) - build.getRunningInformation( ).getElapsedTime( ), ChronoUnit.SECONDS ) : Duration.ZERO );

}
