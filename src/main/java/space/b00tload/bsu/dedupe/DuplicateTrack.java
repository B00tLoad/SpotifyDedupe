package space.b00tload.bsu.dedupe;

import se.michaelthelin.spotify.model_objects.specification.Track;

public record DuplicateTrack(Track track, int location) {

}
