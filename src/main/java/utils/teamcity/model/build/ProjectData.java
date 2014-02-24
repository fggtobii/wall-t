package utils.teamcity.model.build;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Date: 23/02/14
 *
 * @author Cedric Longo
 */
public final class ProjectData {

    private final String _id;
    private final String _name;

    private final List<BuildTypeData> _buildTypes = Lists.newArrayList();
    private String _aliasName;

    public ProjectData( final String id, final String name ) {
        _id = id;
        _name = name;
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public String getAliasName() {
        return _aliasName;
    }

    public void setAliasName( final String aliasName ) {
        _aliasName = aliasName;
    }

    public synchronized void registerBuildType( final BuildTypeData buildTypeData ) {
        _buildTypes.add( buildTypeData );
    }

    public synchronized List<BuildTypeData> getBuildTypes() {
        return ImmutableList.copyOf( _buildTypes );
    }

    public int getBuildTypeCount( final BuildStatus... status ) {
        final List<BuildStatus> keptStatus = Arrays.asList( status );
        return (int) getBuildTypes().stream()
                .filter( bt -> {
                    final Optional<BuildData> lastBuild = bt.getLastBuild( BuildState.finished );
                    return lastBuild.isPresent() && keptStatus.contains( lastBuild.get().getStatus() );
                } )
                .count();
    }
}