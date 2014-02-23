package utils.teamcity.view.wall;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import utils.teamcity.view.UIUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static javafx.beans.binding.Bindings.createIntegerBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

/**
 * Date: 16/02/14
 *
 * @author Cedric Longo
 */
final class WallView extends StackPane {

    public static final int GAP_SPACE = 10;

    private final WallViewModel _model;
    private final Map<Node, FadeTransition> _registeredTransition = Maps.newHashMap( );

    private Node _currentDisplayedScreen;

    @Inject
    WallView( final WallViewModel model ) {
        _model = model;
        setStyle( "-fx-background-color:black;" );

        final ObservableList<TileViewModel> builds = _model.getDisplayedBuilds( );
        builds.addListener( (ListChangeListener<TileViewModel>) c -> updateLayout( ) );

        _model.getMaxTilesByColumnProperty( ).addListener( ( o, oldValue, newalue ) -> updateLayout( ) );
        _model.getMaxTilesByRowProperty( ).addListener( ( o, oldValue, newalue ) -> updateLayout( ) );

        final Timer screenAnimationTimer = new Timer( "WallView Screen switcher", true );
        screenAnimationTimer.scheduleAtFixedRate( new TimerTask( ) {
            @Override
            public void run( ) {
                Platform.runLater( ( ) -> displayNextScreen( ) );
            }
        }, 10000, 10000 );
    }

    private void displayNextScreen( ) {
        if ( getChildren( ).isEmpty( ) )
            return;

        final Node previousScreen = _currentDisplayedScreen;

        final int index = previousScreen == null ? -1 : getChildren( ).indexOf( previousScreen );
        final int nextIndex = ( index == -1 ? 0 : index + 1 ) % getChildren( ).size( );

        final Node nextScreen = getChildren( ).get( nextIndex );

        nextScreen.setVisible( true );
        if ( previousScreen != null && previousScreen != nextScreen )
            previousScreen.setVisible( false );

        _currentDisplayedScreen = nextScreen;
    }

    private void updateLayout( ) {
        getChildren( ).clear( );

        final Collection<TileViewModel> builds = _model.getDisplayedBuilds( );

        final int maxTilesByColumn = _model.getMaxTilesByColumnProperty( ).get( );
        final int maxTilesByRow = _model.getMaxTilesByRowProperty( ).get( );

        final int maxByScreens = max( 1, maxTilesByColumn * maxTilesByRow );

        final int nbScreen = max( 1, builds.size( ) / maxByScreens + ( ( builds.size( ) % maxByScreens > 0 ? 1 : 0 ) ) );
        final int byScreen = max( 1, builds.size( ) / nbScreen + ( ( builds.size( ) % nbScreen > 0 ? 1 : 0 ) ) );

        final Iterable<List<TileViewModel>> screenPartition = Iterables.partition( builds, byScreen );

        final int nbColums = max( 1, byScreen / maxTilesByColumn + ( ( byScreen % maxTilesByColumn > 0 ? 1 : 0 ) ) );
        final int byColums = max( 1, byScreen / nbColums + ( ( byScreen % nbColums > 0 ? 1 : 0 ) ) );

        for ( final List<TileViewModel> buildsInScreen : screenPartition ) {
            final GridPane screenPane = buildScreenPane( buildsInScreen, nbColums, byColums );
            screenPane.setVisible( false );
            getChildren( ).add( screenPane );
        }
        displayNextScreen( );
    }

    private GridPane buildScreenPane( final Iterable<TileViewModel> buildsInScreen, final int nbColums, final int byColums ) {
        final GridPane screenPane = new GridPane( );
        screenPane.setHgap( GAP_SPACE );
        screenPane.setVgap( GAP_SPACE );
        screenPane.setPadding( new Insets( GAP_SPACE ) );
        screenPane.setStyle( "-fx-background-color:black;" );
        screenPane.setAlignment( Pos.CENTER );

        final Iterable<List<TileViewModel>> partition = Iterables.partition( buildsInScreen, byColums );
        int x = 0;
        int y = 0;
        for ( final List<TileViewModel> buildList : partition ) {
            for ( final TileViewModel build : buildList ) {
                createTileForBuildType( screenPane, build, x, y, nbColums, byColums );
                y++;
            }
            y = 0;
            x++;
        }
        return screenPane;
    }

    private void createTileForBuildType( final GridPane screenPane, final TileViewModel build, final int x, final int y, final int nbColumns, final int nbRows ) {
        final StackPane tile = new StackPane( );
        tile.setAlignment( Pos.CENTER_LEFT );
        tile.setStyle( "-fx-border-color:white; -fx-border-radius:5;" );
        tile.backgroundProperty( ).bind( build.backgroundProperty( ) );

        tile.prefWidthProperty( ).bind( widthProperty( ).add( -( nbColumns + 1 ) * GAP_SPACE ).divide( nbColumns ) );
        tile.prefHeightProperty( ).bind( heightProperty( ).add( -( nbRows + 1 ) * GAP_SPACE ).divide( nbRows ) );

        final Pane progressPane = new Pane( );
        progressPane.backgroundProperty( ).bind( build.runningBackgroundProperty( ) );
        progressPane.minWidthProperty( ).bind( tile.widthProperty( ).multiply( build.percentageCompleteProperty( ) ).divide( 100 ) );
        progressPane.maxWidthProperty( ).bind( progressPane.minWidthProperty( ) );
        progressPane.visibleProperty( ).bind( build.runningProperty( ) );
        build.runningProperty( ).addListener( ( o, oldVallue, newValue ) -> {
            if ( newValue )
                startAnimationOnNode( tile );
            else
                stopAnimationOnNode( tile );
        } );

        final HBox tileContent = new HBox( );
        tileContent.setAlignment( Pos.CENTER_LEFT );
        tileContent.setSpacing( 10 );

        final Label tileTitle = new Label( );
        tileTitle.setStyle( "-fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:50px;" );
        tileTitle.setPadding( new Insets( 5 ) );
        tileTitle.setWrapText( true );
        tileTitle.textProperty( ).bind( build.displayedNameProperty( ) );
        tileTitle.prefWidthProperty( ).bind( tile.widthProperty( ) );
        tileTitle.prefHeightProperty( ).bind( tile.heightProperty( ) );
        HBox.setHgrow( tileTitle, Priority.SOMETIMES );
        tileContent.getChildren( ).add( tileTitle );

        final VBox contextPart = createContextPart( build );
        contextPart.visibleProperty( ).bind( _model.lightModeProperty( ).not( ) );
        contextPart.minWidthProperty( ).bind( createIntegerBinding( ( ) -> contextPart.isVisible( ) ? 150 : 0, contextPart.visibleProperty( ) ) );
        contextPart.maxWidthProperty( ).bind( contextPart.minWidthProperty( ) );
        tileContent.getChildren( ).add( contextPart );

        tile.getChildren( ).addAll( progressPane, tileContent );
        screenPane.add( tile, x, y );
    }

    private VBox createContextPart( final TileViewModel build ) {
        final VBox contextPart = new VBox( );
        contextPart.setAlignment( Pos.CENTER );

        final HBox statusBox = new HBox( );
        statusBox.setAlignment( Pos.CENTER );
        statusBox.setSpacing( 5 );

        final ImageView queuedIcon = queueImageView( build );

        final ImageView image = new ImageView( );
        image.setPreserveRatio( true );
        image.setFitWidth( 90 );
        image.imageProperty( ).bind( build.imageProperty( ) );
        statusBox.getChildren( ).addAll( queuedIcon, image );

        final HBox lastBuildInfoPart = createLastBuildInfoBox( build );
        lastBuildInfoPart.visibleProperty( ).bind( build.runningProperty( ).not( ) );

        final HBox timeLeftInfoBox = createTimeLeftInfoBox( build );
        timeLeftInfoBox.visibleProperty( ).bind( build.runningProperty( ) );

        final StackPane infoBox = new StackPane( lastBuildInfoPart, timeLeftInfoBox );
        infoBox.setAlignment( Pos.CENTER );
        infoBox.visibleProperty( ).bind( contextPart.heightProperty( ).greaterThan( 150 ) );

        contextPart.getChildren( ).addAll( statusBox, infoBox );
        return contextPart;
    }

    private ImageView queueImageView( final TileViewModel build ) {
        final ImageView queuedIcon = new ImageView( UIUtils.createImage( "queued.png" ) );
        queuedIcon.setPreserveRatio( true );
        queuedIcon.setFitWidth( 50 );
        queuedIcon.visibleProperty( ).bind( build.queuedProperty( ) );

        final RotateTransition transition = new RotateTransition( Duration.seconds( 3 ), queuedIcon );
        transition.setByAngle( 360 );
        transition.setCycleCount( Timeline.INDEFINITE );
        transition.play( );

        return queuedIcon;
    }

    private HBox createLastBuildInfoBox( final TileViewModel build ) {
        final HBox lastBuildInfoPart = new HBox( );
        lastBuildInfoPart.setSpacing( 5 );
        lastBuildInfoPart.setAlignment( Pos.CENTER );

        final ImageView lastBuildIcon = new ImageView( UIUtils.createImage( "lastBuild.png" ) );
        lastBuildIcon.setPreserveRatio( true );
        lastBuildIcon.setFitWidth( 32 );

        final Label lastBuildDate = new Label( );
        lastBuildDate.setMinWidth( 110 );
        lastBuildDate.setTextAlignment( TextAlignment.CENTER );
        lastBuildDate.setAlignment( Pos.CENTER );
        lastBuildDate.setStyle( "-fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:32px;" );
        lastBuildDate.setWrapText( true );
        lastBuildDate.textProperty( ).bind( createStringBinding( ( ) -> {
            final LocalDateTime localDateTime = build.lastFinishedDateProperty( ).get( );
            if ( localDateTime == null )
                return "00/00\n00:00";
            return localDateTime.format( DateTimeFormatter.ofPattern( "dd/MM\nHH:mm" ) );
        }, build.lastFinishedDateProperty( ) ) );

        lastBuildInfoPart.getChildren( ).addAll( lastBuildIcon, lastBuildDate );
        return lastBuildInfoPart;
    }

    private HBox createTimeLeftInfoBox( final TileViewModel build ) {
        final HBox lastBuildInfoPart = new HBox( );
        lastBuildInfoPart.setSpacing( 5 );
        lastBuildInfoPart.setAlignment( Pos.CENTER );
        final ImageView lastBuildIcon = new ImageView( UIUtils.createImage( "timeLeft.png" ) );
        lastBuildIcon.setPreserveRatio( true );
        lastBuildIcon.setFitWidth( 32 );

        final Label timeLeftLabel = new Label( );
        timeLeftLabel.setMinWidth( 110 );
        timeLeftLabel.setTextAlignment( TextAlignment.CENTER );
        timeLeftLabel.setAlignment( Pos.CENTER );
        timeLeftLabel.setStyle( "-fx-font-weight:bold; -fx-text-fill:white; -fx-font-size:32px;" );
        timeLeftLabel.setWrapText( true );
        timeLeftLabel.textProperty( ).bind( createStringBinding( ( ) -> {
            final java.time.Duration timeLeft = build.timeLeftProperty( ).get( );
            return ( timeLeft.isNegative( ) ? "+ " : "" ) + ( abs( timeLeft.toMinutes( ) ) + 1 ) + "\nmin";
        }, build.timeLeftProperty( ) ) );

        lastBuildInfoPart.getChildren( ).addAll( lastBuildIcon, timeLeftLabel );
        return lastBuildInfoPart;
    }


    public void startAnimationOnNode( final Node node ) {
        FadeTransition transition = _registeredTransition.get( node );
        if ( transition == null ) {
            transition = new FadeTransition( Duration.millis( 1500 ), node );
            transition.setFromValue( 1.0 );
            transition.setToValue( 0.5 );
            transition.setCycleCount( Timeline.INDEFINITE );
            transition.setAutoReverse( true );
            transition.setOnFinished( ( ae ) -> node.setOpacity( 1 ) );
            _registeredTransition.put( node, transition );
        }
        transition.play( );
    }

    public void stopAnimationOnNode( final Node node ) {
        final FadeTransition transition = _registeredTransition.get( node );
        if ( transition != null ) {
            _registeredTransition.remove( node );
            transition.stop( );
        }
    }


}
